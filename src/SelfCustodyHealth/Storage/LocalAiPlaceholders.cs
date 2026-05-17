using SelfCustodyHealth.Domain;

namespace SelfCustodyHealth.Storage;

public sealed class PendingLocalDocumentClassifier : ILocalDocumentClassifier
{
	public Task<DocumentCategory> ClassifyAsync(string fileName, CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();
		return Task.FromResult(DocumentCategoryRules.InferFromText(fileName));
	}
}

public sealed class PendingLocalOcrService : ILocalOcrService
{
	public Task<string?> ExtractTextAsync(Stream documentStream, CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();
		return Task.FromResult<string?>(null);
	}
}

public sealed class PendingLocalSummaryService : ILocalSummaryService
{
	public Task<string?> SummarizeAsync(string text, CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();
		return Task.FromResult<string?>(null);
	}
}
