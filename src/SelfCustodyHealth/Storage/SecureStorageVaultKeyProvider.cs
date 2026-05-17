using System.Security.Cryptography;
using SelfCustodyHealth.Crypto;

namespace SelfCustodyHealth.Storage;

public sealed class SecureStorageVaultKeyProvider : IVaultKeyProvider
{
	private const string VaultKeyName = "self_custody_health_vault_key_v1";

	public async Task<byte[]> GetOrCreateKeyAsync(CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();

		var stored = await SecureStorage.Default.GetAsync(VaultKeyName).ConfigureAwait(false);
		if (!string.IsNullOrWhiteSpace(stored))
		{
			return Convert.FromBase64String(stored);
		}

		var key = RandomNumberGenerator.GetBytes(AesGcmEncryptionService.KeySizeInBytes);
		await SecureStorage.Default.SetAsync(VaultKeyName, Convert.ToBase64String(key)).ConfigureAwait(false);

		return key;
	}

	public Task DeleteKeyAsync(CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();
		SecureStorage.Default.Remove(VaultKeyName);
		return Task.CompletedTask;
	}
}
