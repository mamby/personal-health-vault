namespace SelfCustodyHealth.Crypto;

public sealed record EncryptionResult(
	byte[] Nonce,
	byte[] Ciphertext,
	byte[] Tag);

public interface IEncryptionService
{
	EncryptionResult Encrypt(byte[] plaintext, byte[] key, byte[]? associatedData = null);

	byte[] Decrypt(EncryptionResult encrypted, byte[] key, byte[]? associatedData = null);
}

public interface IVaultKeyProvider
{
	Task<byte[]> GetOrCreateKeyAsync(CancellationToken cancellationToken);

	Task DeleteKeyAsync(CancellationToken cancellationToken);
}
