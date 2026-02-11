using MediatR;

namespace BillsApi.Application.Abstractions;

public interface ICommand<out TResponse> : IRequest<TResponse>;
