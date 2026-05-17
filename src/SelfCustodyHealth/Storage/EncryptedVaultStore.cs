using System.Text;
using System.Text.Json;
using SelfCustodyHealth.Crypto;
using SelfCustodyHealth.Domain;

namespace SelfCustodyHealth.Storage;

public sealed class EncryptedVaultStore(
	string vaultFilePath,
	IVaultKeyProvider keyProvider,
	IEncryptionService encryptionService) : IVaultStore
{
	private const int CurrentEnvelopeVersion = 1;
	private static readonly byte[] AssociatedData = Encoding.UTF8.GetBytes("SelfCustodyHealth.LocalVault.v1");

	private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
	{
		WriteIndented = false
	};

	public async Task<HealthVaultSnapshot?> LoadAsync(CancellationToken cancellationToken)
	{
		if (!File.Exists(vaultFilePath))
		{
			return null;
		}

		await using var stream = File.OpenRead(vaultFilePath);
		var envelope = await JsonSerializer.DeserializeAsync<VaultEnvelope>(
			stream,
			JsonOptions,
			cancellationToken).ConfigureAwait(false);

		if (envelope is not { Version: CurrentEnvelopeVersion })
		{
			return null;
		}

		var key = await keyProvider.GetOrCreateKeyAsync(cancellationToken).ConfigureAwait(false);
		var plaintext = encryptionService.Decrypt(
			new EncryptionResult(
				Convert.FromBase64String(envelope.Nonce),
				Convert.FromBase64String(envelope.Ciphertext),
				Convert.FromBase64String(envelope.Tag)),
			key,
			AssociatedData);

		return JsonSerializer.Deserialize<HealthVaultSnapshot>(plaintext, JsonOptions);
	}

	public async Task SaveAsync(HealthVaultSnapshot snapshot, CancellationToken cancellationToken)
	{
		ArgumentNullException.ThrowIfNull(snapshot);

		var directory = Path.GetDirectoryName(vaultFilePath);
		if (!string.IsNullOrWhiteSpace(directory))
		{
			Directory.CreateDirectory(directory);
		}

		var key = await keyProvider.GetOrCreateKeyAsync(cancellationToken).ConfigureAwait(false);
		var plaintext = JsonSerializer.SerializeToUtf8Bytes(snapshot, JsonOptions);
		var encrypted = encryptionService.Encrypt(plaintext, key, AssociatedData);
		var now = DateTimeOffset.UtcNow;
		var envelope = new VaultEnvelope
		{
			Version = CurrentEnvelopeVersion,
			CreatedAt = now,
			UpdatedAt = now,
			Nonce = Convert.ToBase64String(encrypted.Nonce),
			Ciphertext = Convert.ToBase64String(encrypted.Ciphertext),
			Tag = Convert.ToBase64String(encrypted.Tag)
		};

		var tempPath = $"{vaultFilePath}.part";
		await using (var stream = File.Create(tempPath))
		{
			await JsonSerializer.SerializeAsync(stream, envelope, JsonOptions, cancellationToken).ConfigureAwait(false);
		}

		if (File.Exists(vaultFilePath))
		{
			File.Delete(vaultFilePath);
		}

		File.Move(tempPath, vaultFilePath);
	}

	public async Task DeleteAsync(CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();

		if (File.Exists(vaultFilePath))
		{
			File.Delete(vaultFilePath);
		}

		await keyProvider.DeleteKeyAsync(cancellationToken).ConfigureAwait(false);
	}

	private sealed record VaultEnvelope
	{
		public required int Version { get; init; }
		public required DateTimeOffset CreatedAt { get; init; }
		public required DateTimeOffset UpdatedAt { get; init; }
		public required string Nonce { get; init; }
		public required string Ciphertext { get; init; }
		public required string Tag { get; init; }
	}
}
