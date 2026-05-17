using System.Security.Cryptography;

namespace SelfCustodyHealth.Crypto;

public sealed class AesGcmEncryptionService : IEncryptionService
{
	public const int KeySizeInBytes = 32;
	private const int NonceSizeInBytes = 12;
	private const int TagSizeInBytes = 16;

	public EncryptionResult Encrypt(byte[] plaintext, byte[] key, byte[]? associatedData = null)
	{
		ArgumentNullException.ThrowIfNull(plaintext);
		ValidateKey(key);

		var nonce = RandomNumberGenerator.GetBytes(NonceSizeInBytes);
		var ciphertext = new byte[plaintext.Length];
		var tag = new byte[TagSizeInBytes];

		using var aes = new AesGcm(key, TagSizeInBytes);
		aes.Encrypt(nonce, plaintext, ciphertext, tag, associatedData);

		return new EncryptionResult(nonce, ciphertext, tag);
	}

	public byte[] Decrypt(EncryptionResult encrypted, byte[] key, byte[]? associatedData = null)
	{
		ArgumentNullException.ThrowIfNull(encrypted);
		ValidateKey(key);

		var plaintext = new byte[encrypted.Ciphertext.Length];
		using var aes = new AesGcm(key, TagSizeInBytes);
		aes.Decrypt(encrypted.Nonce, encrypted.Ciphertext, encrypted.Tag, plaintext, associatedData);

		return plaintext;
	}

	private static void ValidateKey(byte[] key)
	{
		ArgumentNullException.ThrowIfNull(key);
		if (key.Length is not KeySizeInBytes)
		{
			throw new ArgumentException("Vault keys must be 256-bit AES keys.", nameof(key));
		}
	}
}
