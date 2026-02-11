using MediatR;

namespace BillsApi.Application.Abstractions;

public interface IQuery<out TResponse> : IRequest<TResponse>;
