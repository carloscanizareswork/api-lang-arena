export class DomainValidationError extends Error {
  constructor(public readonly errors: Record<string, string[]>) {
    super("Domain validation failed");
  }
}
