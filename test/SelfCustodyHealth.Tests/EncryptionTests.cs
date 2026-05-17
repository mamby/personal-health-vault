using System.Security.Cryptography;
using SelfCustodyHealth.Crypto;

namespace SelfCustodyHealth.Tests;

public sealed class EncryptionTests
{
	[Fact]
	public void EncryptThenDecrypt_ReturnsOriginalPlaintext()
	{
		var service = new AesGcmEncryptionService();
		var key = RandomNumberGenerator.GetBytes(AesGcmEncryptionService.KeySizeInBytes);
		var plaintext = "local health vault"u8.ToArray();
		var associatedData = "test-vault"u8.ToArray();

		var encrypted = service.Encrypt(plaintext, key, associatedData);
		var decrypted = service.Decrypt(encrypted, key, associatedData);

		Assert.Equal(plaintext, decrypted);
		Assert.NotEqual(plaintext, encrypted.Ciphertext);
	}
}
