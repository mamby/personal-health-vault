using SelfCustodyHealth.Domain;

namespace SelfCustodyHealth.Storage;

public sealed class HealthDataService(IVaultStore vaultStore)
{
	private readonly SemaphoreSlim _gate = new(1, 1);
	private HealthVaultSnapshot? _snapshot;

	public bool IsDemoData { get; private set; } = true;

	public async Task<HealthVaultSnapshot> GetSnapshotAsync(CancellationToken cancellationToken = default)
	{
		await _gate.WaitAsync(cancellationToken).ConfigureAwait(false);
		try
		{
			if (_snapshot is not null)
			{
				return _snapshot;
			}

			var saved = await vaultStore.LoadAsync(cancellationToken).ConfigureAwait(false);
			if (saved is not null)
			{
				IsDemoData = false;
				_snapshot = saved;
				return saved;
			}

			IsDemoData = true;
			_snapshot = DemoHealthVaultFactory.Create(DateTimeOffset.Now);
			return _snapshot;
		}
		finally
		{
			_gate.Release();
		}
	}

	public async Task SaveAsync(HealthVaultSnapshot snapshot, CancellationToken cancellationToken = default)
	{
		await vaultStore.SaveAsync(snapshot, cancellationToken).ConfigureAwait(false);
		_snapshot = snapshot;
		IsDemoData = false;
	}

	public async Task DeleteLocalVaultAsync(CancellationToken cancellationToken = default)
	{
		await vaultStore.DeleteAsync(cancellationToken).ConfigureAwait(false);
		_snapshot = DemoHealthVaultFactory.Create(DateTimeOffset.Now);
		IsDemoData = true;
	}
}
