using BillsApi.Application.Abstractions.Exceptions;
using BillsApi.Domain.Common;
using FluentValidation;

namespace BillsApi.Presentation.Middleware;

public sealed class ExceptionHandlingMiddleware(RequestDelegate next, ILogger<ExceptionHandlingMiddleware> logger)
{
    public async Task Invoke(HttpContext context)
    {
        try
        {
            await next(context);
        }
        catch (ValidationException ex)
        {
            logger.LogWarning(ex, "Validation failed");
            await WriteProblemAsync(context, StatusCodes.Status400BadRequest, "Validation failed", BuildValidationErrors(ex));
        }
        catch (ConflictException ex)
        {
            logger.LogWarning(ex, "Conflict while processing request");
            await WriteProblemAsync(context, StatusCodes.Status409Conflict, ex.Message);
        }
        catch (DomainException ex)
        {
            logger.LogWarning(ex, "Domain rule violation");
            await WriteProblemAsync(context, StatusCodes.Status400BadRequest, ex.Message);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Unhandled exception");
            await WriteProblemAsync(context, StatusCodes.Status500InternalServerError, "An unexpected error occurred.");
        }
    }

    private static async Task WriteProblemAsync(
        HttpContext context,
        int statusCode,
        string title,
        IDictionary<string, string[]>? errors = null)
    {
        context.Response.StatusCode = statusCode;
        context.Response.ContentType = "application/json";

        var body = new
        {
            type = "about:blank",
            title,
            status = statusCode,
            errors
        };

        await context.Response.WriteAsJsonAsync(body);
    }

    private static Dictionary<string, string[]> BuildValidationErrors(ValidationException exception)
    {
        return exception.Errors
            .GroupBy(x => x.PropertyName)
            .ToDictionary(
                group => group.Key,
                group => group.Select(x => x.ErrorMessage).Distinct().ToArray());
    }
}
