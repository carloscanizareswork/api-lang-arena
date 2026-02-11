function env(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;
  if (!value || value.trim() === "") {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function envInt(name: string, fallback: string): number {
  const value = Number(env(name, fallback));
  if (!Number.isFinite(value)) {
    throw new Error(`Invalid numeric environment variable: ${name}`);
  }
  return value;
}

export const config = {
  port: envInt("PORT", "8080"),
  serviceName: "node-bills-api",
  postgres: {
    host: env("POSTGRES_HOST", "localhost"),
    port: envInt("POSTGRES_PORT", "5440"),
    db: env("POSTGRES_DB", "api_lang_arena"),
    user: env("POSTGRES_USER", "api_lang_user"),
    password: env("POSTGRES_PASSWORD", "api_lang_password"),
    maxPool: envInt("NODE_DB_MAX_POOL_SIZE", "10")
  },
  rabbit: {
    host: env("RABBITMQ_HOST", "localhost"),
    port: envInt("RABBITMQ_PORT", "5672"),
    user: env("RABBITMQ_USER", "guest"),
    password: env("RABBITMQ_PASSWORD", "guest"),
    vhost: env("RABBITMQ_VHOST", "/"),
    queue: env("RABBITMQ_BILL_CREATED_QUEUE", "bill-created")
  }
};

export function buildPgConnectionString(): string {
  const p = config.postgres;
  return `postgresql://${encodeURIComponent(p.user)}:${encodeURIComponent(p.password)}@${p.host}:${p.port}/${p.db}`;
}

export function buildAmqpUrl(): string {
  const r = config.rabbit;
  const vhost = r.vhost === "/" ? "%2f" : encodeURIComponent(r.vhost);
  return `amqp://${encodeURIComponent(r.user)}:${encodeURIComponent(r.password)}@${r.host}:${r.port}/${vhost}`;
}
