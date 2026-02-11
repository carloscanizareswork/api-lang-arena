FROM golang:1.23-alpine AS build
WORKDIR /src

COPY go.mod ./
COPY go.sum ./
RUN /usr/local/go/bin/go mod download

COPY . ./
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 /usr/local/go/bin/go build -o /app/go-bills-api ./cmd/api

FROM alpine:3.21
WORKDIR /app
COPY --from=build /app/go-bills-api ./go-bills-api

EXPOSE 8080
ENTRYPOINT ["./go-bills-api"]
