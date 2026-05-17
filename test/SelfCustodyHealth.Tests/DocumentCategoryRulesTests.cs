using SelfCustodyHealth.Domain;

namespace SelfCustodyHealth.Tests;

public sealed class DocumentCategoryRulesTests
{
	[Theory]
	[InlineData("CBC lab result", DocumentCategory.LabResults)]
	[InlineData("inhaler prescription", DocumentCategory.Prescriptions)]
	[InlineData("flu vaccination receipt", DocumentCategory.Vaccinations)]
	[InlineData("radiology report", DocumentCategory.Reports)]
	[InlineData("wallet card", DocumentCategory.Other)]
	public void InferFromText_ReturnsExpectedCategory(string text, DocumentCategory expected)
	{
		Assert.Equal(expected, DocumentCategoryRules.InferFromText(text));
	}

	[Fact]
	public void Matches_AllCategory_ReturnsTrue()
	{
		var document = new MedicalDocument
		{
			Id = Guid.NewGuid(),
			Title = "Annual blood panel",
			Category = DocumentCategory.LabResults,
			DocumentDate = new DateOnly(2026, 1, 10),
			Source = "Lab"
		};

		Assert.True(DocumentCategoryRules.Matches(document, DocumentCategory.All));
		Assert.False(DocumentCategoryRules.Matches(document, DocumentCategory.Prescriptions));
	}
}
