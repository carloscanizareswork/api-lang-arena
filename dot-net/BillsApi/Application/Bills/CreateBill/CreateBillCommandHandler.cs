using BillsApi.Application.Abstractions;
using BillsApi.Application.Abstractions.Exceptions;
using BillsApi.Application.Abstractions.Messaging;
using BillsApi.Domain.Bills;
using BillsApi.Domain.Common;
using Microsoft.EntityFrameworkCore;
using Npgsql;

namespace BillsApi.Application.Bills.CreateBill;

public sealed class CreateBillCommandHandler(
    IBillRepository billRepository,
    IUnitOfWork unitOfWork,
    IIntegrationEventPublisher integrationEventPublisher)
    : ICommandHandler<CreateBillCommand, CreateBillResult>
{
    public async Task<CreateBillResult> Handle(CreateBillCommand request, CancellationToken cancellationToken)
    {
        if (await billRepository.ExistsByBillNumberAsync(request.BillNumber.Trim(), cancellationToken))
        {
            throw new ConflictException($"Bill number '{request.BillNumber}' already exists.");
        }

        var bill = Bill.Create(
            request.BillNumber,
            request.IssuedAt,
            request.CustomerName,
            request.Currency,
            request.Tax);

        foreach (var line in request.Lines)
        {
            bill.AddLine(line.Concept, line.Quantity, line.UnitAmount);
        }

        await billRepository.AddAsync(bill, cancellationToken);

        try
        {
            await unitOfWork.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException ex) when (ex.InnerException is PostgresException { SqlState: PostgresErrorCodes.UniqueViolation })
        {
            throw new ConflictException($"Bill number '{request.BillNumber}' already exists.");
        }

        var total = bill.CalculateTotal();

        var createdEvent = new BillCreatedIntegrationEvent(
            bill.Id,
            bill.BillNumber,
            bill.IssuedAt,
            bill.Subtotal,
            bill.Tax,
            total,
            bill.Currency,
            DateTime.UtcNow);

        await integrationEventPublisher.PublishAsync("bill.created", createdEvent, cancellationToken);

        return new CreateBillResult(
            bill.Id,
            bill.BillNumber,
            bill.IssuedAt,
            bill.Subtotal,
            bill.Tax,
            total,
            bill.Currency);
    }
}
