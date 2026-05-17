namespace SelfCustodyHealth.Storage;

public sealed class PendingBackupService : IBackupService
{
	public Task<bool> IsConfiguredAsync(CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();
		return Task.FromResult(false);
	}
}
