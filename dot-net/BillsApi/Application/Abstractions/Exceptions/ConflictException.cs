namespace BillsApi.Application.Abstractions.Exceptions;

public sealed class ConflictException(string message) : Exception(message);
