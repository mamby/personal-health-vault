# Privacy Model

Self-Custody Health is designed around local ownership of personal health records.

## Defaults

- Health data stays on the user's device by default.
- The app has no backend health-data storage.
- The app has no cloud AI integration.
- The app has no ads or tracking by default.

## Local Vault

Saved vault data is encrypted locally with authenticated encryption. Demo records are not user health data and are labeled as demo content.

## App Lock

App lock uses the device's biometric or credential unlock APIs when enabled. This is an access gate for the app. It is not presented as medical-grade security or as hardware-bound key encryption.

## Backup Direction

Backup and sync are not implemented yet. When added, they must be user-controlled and must store only encrypted data outside the device.

## AI Direction

AI features are placeholders. Future AI should run locally when available and help organize records through OCR, classification, search, and user-verified summaries. The app must not provide medical advice.
