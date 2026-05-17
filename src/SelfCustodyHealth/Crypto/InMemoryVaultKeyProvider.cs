using System.Security.Cryptography;

namespace SelfCustodyHealth.Crypto;

public sealed class InMemoryVaultKeyProvider : IVaultKeyProvider
{
	private byte[]? _key;

	public Task<byte[]> GetOrCreateKeyAsync(CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();
		_key ??= RandomNumberGenerator.GetBytes(AesGcmEncryptionService.KeySizeInBytes);
		return Task.FromResult(_key);
	}

	public Task DeleteKeyAsync(CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();
		_key = null;
		return Task.CompletedTask;
	}
}
