using SelfCustodyHealth.Domain;

namespace SelfCustodyHealth.Storage;

public interface IVaultStore
{
	Task<HealthVaultSnapshot?> LoadAsync(CancellationToken cancellationToken);

	Task SaveAsync(HealthVaultSnapshot snapshot, CancellationToken cancellationToken);

	Task DeleteAsync(CancellationToken cancellationToken);
}

public interface IDocumentStore
{
	Task<IReadOnlyList<MedicalDocument>> SearchAsync(
		DocumentCategory category,
		string? searchText,
		CancellationToken cancellationToken);
}

public interface IBackupService
{
	Task<bool> IsConfiguredAsync(CancellationToken cancellationToken);
}

public interface ILocalDocumentClassifier
{
	Task<DocumentCategory> ClassifyAsync(string fileName, CancellationToken cancellationToken);
}

public interface ILocalOcrService
{
	Task<string?> ExtractTextAsync(Stream documentStream, CancellationToken cancellationToken);
}

public interface ILocalSummaryService
{
	Task<string?> SummarizeAsync(string text, CancellationToken cancellationToken);
}
