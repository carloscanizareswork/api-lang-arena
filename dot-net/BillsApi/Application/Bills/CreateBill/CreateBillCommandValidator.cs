using FluentValidation;

namespace BillsApi.Application.Bills.CreateBill;

public sealed class CreateBillCommandValidator : AbstractValidator<CreateBillCommand>
{
    public CreateBillCommandValidator()
    {
        RuleFor(x => x.BillNumber)
            .NotEmpty()
            .MaximumLength(50);

        RuleFor(x => x.CustomerName)
            .NotEmpty()
            .MaximumLength(200);

        RuleFor(x => x.IssuedAt)
            .NotEqual(default(DateOnly));

        RuleFor(x => x.Currency)
            .NotEmpty()
            .Length(3);

        RuleFor(x => x.Tax)
            .GreaterThanOrEqualTo(0m);

        RuleFor(x => x.Lines)
            .NotNull()
            .Must(lines => lines.Count > 0)
            .WithMessage("At least one line is required.");

        RuleForEach(x => x.Lines)
            .SetValidator(new CreateBillLineInputValidator());
    }

    private sealed class CreateBillLineInputValidator : AbstractValidator<CreateBillLineInput>
    {
        public CreateBillLineInputValidator()
        {
            RuleFor(x => x.Concept)
                .NotEmpty()
                .MaximumLength(200);

            RuleFor(x => x.Quantity)
                .GreaterThan(0m);

            RuleFor(x => x.UnitAmount)
                .GreaterThanOrEqualTo(0m);
        }
    }
}
