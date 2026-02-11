import { FastifyReply, FastifyRequest } from "fastify";
import { ZodError } from "zod";
import { ConflictError } from "../application/common/ConflictError";
import { DomainValidationError } from "../domain/common/DomainValidationError";

export function handleApiError(error: unknown, _: FastifyRequest, reply: FastifyReply): void {
  if (error instanceof ZodError) {
    const errors: Record<string, string[]> = {};
    for (const issue of error.issues) {
      const key = issue.path.length === 0 ? "request" : issue.path.join(".");
      if (!errors[key]) errors[key] = [];
      errors[key].push(issue.message);
    }

    void reply.status(400).send(problem(400, "Validation failed", errors));
    return;
  }

  if (error instanceof DomainValidationError) {
    void reply.status(400).send(problem(400, "Validation failed", error.errors));
    return;
  }

  if (error instanceof ConflictError) {
    void reply.status(409).send(problem(409, error.message));
    return;
  }

  const amqpCode = (error as { code?: string })?.code;
  if (amqpCode === "ECONNREFUSED" || amqpCode === "EPIPE") {
    void reply.status(503).send(problem(503, `Message broker error: ${amqpCode}`));
    return;
  }

  console.error("Unhandled API error", error);
  void reply.status(500).send(problem(500, "An unexpected error occurred."));
}

function problem(status: number, title: string, errors?: Record<string, string[]>): object {
  return {
    type: "about:blank",
    title,
    status,
    errors: errors ?? null
  };
}
