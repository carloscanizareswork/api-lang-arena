package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"

	_ "github.com/jackc/pgx/v5/stdlib"
	gormpg "gorm.io/driver/postgres"
	"gorm.io/gorm"

	billlist "go-bills-api/internal/application/bill/list"
	"go-bills-api/internal/infrastructure/postgres"
	httpapi "go-bills-api/internal/interfaces/http"
)

type config struct {
	port            string
	dbHost          string
	dbPort          string
	dbName          string
	dbUser          string
	dbPassword      string
	dbMaxOpenConns  int
	dbMaxIdleConns  int
	dbConnMaxLifeMs int
}

func main() {
	cfg := loadConfig()

	dsn := fmt.Sprintf(
		"host=%s port=%s dbname=%s user=%s password=%s sslmode=disable",
		cfg.dbHost,
		cfg.dbPort,
		cfg.dbName,
		cfg.dbUser,
		cfg.dbPassword,
	)

	db, err := sql.Open("pgx", dsn)
	if err != nil {
		log.Fatalf("open db: %v", err)
	}
	defer db.Close()

	db.SetMaxOpenConns(cfg.dbMaxOpenConns)
	db.SetMaxIdleConns(cfg.dbMaxIdleConns)
	db.SetConnMaxLifetime(time.Duration(cfg.dbConnMaxLifeMs) * time.Millisecond)

	if err := db.Ping(); err != nil {
		log.Fatalf("ping db: %v", err)
	}

	gormDB, err := gorm.Open(gormpg.New(gormpg.Config{
		Conn: db,
	}), &gorm.Config{
		SkipDefaultTransaction: true,
		PrepareStmt:            true,
	})
	if err != nil {
		log.Fatalf("init gorm: %v", err)
	}

	repo := postgres.NewBillRepository(gormDB)
	service := billlist.NewService(repo)
	handler := httpapi.NewHandler(db, service)

	mux := http.NewServeMux()
	handler.RegisterRoutes(mux)

	server := &http.Server{
		Addr:              ":" + cfg.port,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
	}

	log.Printf("go bills api listening on :%s", cfg.port)
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("server: %v", err)
	}
}

func loadConfig() config {
	return config{
		port:            env("PORT", "8080"),
		dbHost:          env("POSTGRES_HOST", "localhost"),
		dbPort:          env("POSTGRES_PORT", "5440"),
		dbName:          env("POSTGRES_DB", "api_lang_arena"),
		dbUser:          env("POSTGRES_USER", "api_lang_user"),
		dbPassword:      env("POSTGRES_PASSWORD", "api_lang_password"),
		dbMaxOpenConns:  envInt("GO_DB_MAX_OPEN_CONNS", 20),
		dbMaxIdleConns:  envInt("GO_DB_MAX_IDLE_CONNS", 10),
		dbConnMaxLifeMs: envInt("GO_DB_CONN_MAX_LIFE_MS", 300000),
	}
}

func env(key string, defaultValue string) string {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}
	return value
}

func envInt(key string, defaultValue int) int {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return defaultValue
	}
	return parsed
}
