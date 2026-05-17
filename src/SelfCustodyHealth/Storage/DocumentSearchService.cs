using SelfCustodyHealth.Domain;

namespace SelfCustodyHealth.Storage;

public sealed class DocumentSearchService(HealthDataService dataService) : IDocumentStore
{
	public async Task<IReadOnlyList<MedicalDocument>> SearchAsync(
		DocumentCategory category,
		string? searchText,
		CancellationToken cancellationToken)
	{
		var snapshot = await dataService.GetSnapshotAsync(cancellationToken).ConfigureAwait(false);
		var normalizedSearch = string.IsNullOrWhiteSpace(searchText) ? null : searchText.Trim();

		return [.. snapshot.Documents
			.Where(document => DocumentCategoryRules.Matches(document, category))
			.Where(document => normalizedSearch is null ||
				document.Title.Contains(normalizedSearch, StringComparison.OrdinalIgnoreCase) ||
				document.Source.Contains(normalizedSearch, StringComparison.OrdinalIgnoreCase) ||
				document.Tags.Any(tag => tag.Contains(normalizedSearch, StringComparison.OrdinalIgnoreCase)))
			.OrderByDescending(document => document.DocumentDate)];
	}
}
