namespace SelfCustodyHealth.Domain;

public static class DocumentCategoryRules
{
	public static bool Matches(MedicalDocument document, DocumentCategory category) =>
		category is DocumentCategory.All || document.Category == category;

	public static DocumentCategory InferFromText(string? text)
	{
		if (string.IsNullOrWhiteSpace(text))
		{
			return DocumentCategory.Other;
		}

		var normalized = text.Trim();

		return normalized switch
		{
			_ when ContainsAny(normalized, "lab", "blood", "cbc", "panel", "result") => DocumentCategory.LabResults,
			_ when ContainsAny(normalized, "prescription", "rx", "pharmacy", "medicine") => DocumentCategory.Prescriptions,
			_ when ContainsAny(normalized, "vaccine", "vaccination", "immunization", "booster") => DocumentCategory.Vaccinations,
			_ when ContainsAny(normalized, "report", "scan", "mri", "x-ray", "ultrasound", "discharge") => DocumentCategory.Reports,
			_ => DocumentCategory.Other
		};
	}

	private static bool ContainsAny(string value, params string[] terms) =>
		terms.Any(term => value.Contains(term, StringComparison.OrdinalIgnoreCase));
}
