# Personal Health Vault

![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![.NET](https://img.shields.io/badge/.NET-purple)
![C#](https://img.shields.io/badge/C%23-blue)
![.NET MAUI](https://img.shields.io/badge/.NET%20MAUI-purple)

Cross-platform personal health vault. Local-first, encrypted, and fully user-controlled. No backend, no tracking, no cloud required.

Personal Health Vault is a privacy-first health app for iOS, Android, Windows, and macOS. It helps people organize personal medical records, documents, treatments, appointments, reminders, emergency contacts, and health summaries while keeping their health data on their own devices.

## Project Status

This project is in early development.

The current goal is to build a safe, local-first foundation before adding more advanced features such as document import, export, encrypted backup, OCR, and local AI assistance.

Personal Health Vault is not ready for production use with real medical data yet.

## Core Principles

- Local-first by default
- Encrypted storage on the device
- No backend required
- No tracking
- No ads
- No cloud account required
- No cloud AI required
- User-controlled backup only when explicitly enabled
- Health data should never leave the device in plaintext

## What It Does

Personal Health Vault is designed to help users keep a private, structured copy of their health information, including:

- Medical documents
- Treatments and medications
- Appointments
- Reminders
- Vaccinations
- Emergency contacts
- Personal health summary
- Important notes for future consultations
- Future encrypted export and backup flows

## What It Is Not

Personal Health Vault is not a medical device, diagnostic app, emergency app, or replacement for a healthcare professional.

It does not provide:

- Medical advice
- Diagnosis
- Emergency recommendations
- Triage
- Prescriptions
- Clinical certification
- Regulatory compliance claims

Do not use this app as a replacement for a doctor, pharmacist, emergency service, hospital system, or official medical record platform.

## Privacy Model

Personal Health Vault follows a simple privacy model:

- Health data is stored locally by default.
- Saved vault data is encrypted.
- The app does not require a backend to work.
- The app does not send health data to a server.
- The app does not include ads or tracking.
- Backup and sync, when added, must be user-controlled.
- Backup data must be encrypted before leaving the device.
- Backup providers must never receive plaintext health data.

## Security Model

The app uses a local encrypted vault model.

Plaintext health data should only exist in memory while the app is actively using it. Saved records should be encrypted before being written to disk.

Current security boundaries:

- Vault data is encrypted locally.
- Device biometric or credential unlock can be used as an app access gate.
- App lock is separate from vault encryption.
- Biometric unlock must not be described as hardware-bound encryption unless that is guaranteed on every supported platform.
- Real health data must never be committed to the repository.

## Supported Platforms

- iOS
- Android
- Windows
- macOS through Mac Catalyst

## Tech Stack

- .NET 11 preview
- C# preview
- .NET MAUI
- xUnit v3
- Microsoft Testing Platform
- Authenticated encryption
- Platform biometric and device unlock APIs

## Feature Status

| Area | Status |
| --- | --- |
| Cross-platform app shell | In progress |
| Dashboard | Foundation available |
| Local vault | Foundation available |
| Encrypted local storage | Foundation available |
| Health summary | Foundation available |
| Medications | Foundation available |
| Appointments | Foundation available |
| Settings | Foundation available |
| Demo records | Available |
| Document import | Planned |
| Export | Planned |
| Encrypted backup | Planned |
| Local notifications | Planned |
| OCR | Planned |
| Local document classification | Planned |
| Local health summary assistance | Planned |

## Getting Started

Clone the repository:

```bash
git clone https://github.com/mamby/personal-health-vault.git
cd personal-health-vault
```

Restore dependencies:

```bash
dotnet restore
```

Build the app:

```bash
dotnet build
```

Run tests:

```bash
dotnet test
```

For platform-specific MAUI builds, use the target framework that matches your platform.

Example for Windows:

```bash
dotnet build src/PersonalHealthVault/PersonalHealthVault.csproj -f net11.0-windows10.0.19041.0
```

## Repository Structure

```text
src/
  PersonalHealthVault/
    Domain/
    Crypto/
    Storage/
    Security/
    Features/
    Shared/

test/
  PersonalHealthVault.Tests/
```

Suggested responsibilities:

- `Domain`: health records, reminders, documents, medications, appointments, and vault models
- `Crypto`: encryption services and key contracts
- `Storage`: encrypted vault persistence and backup abstractions
- `Security`: app lock and platform unlock boundaries
- `Features`: MAUI pages and feature-specific UI
- `Shared`: reusable UI components and helpers
- `Tests`: unit and integration tests

## Roadmap

Planned areas include:

- Better document import
- User-created health summaries
- Medication and appointment editing
- Local notifications
- Encrypted export
- Encrypted user-controlled backup
- Local OCR
- Local document classification
- Local-only health summary assistance
- Accessibility improvements
- Localization

## Design Goals

Personal Health Vault should be:

- Simple enough for non-technical users
- Safe by default
- Useful without an account
- Useful without the cloud
- Transparent about what is encrypted
- Honest about what is not implemented yet
- Easy to audit
- Easy to run locally
- Friendly to contributors

## Contributing

Contributions are welcome, especially around:

- .NET MAUI UI improvements
- Accessibility
- Localization
- Encrypted local storage tests
- Document import
- Export flows
- Local-only AI experiments
- Platform-specific app lock behavior
- Documentation

Please do not add:

- Backend health-data storage
- Cloud AI processing of health data
- Tracking
- Ads
- Real health data
- Medical advice features
- Diagnosis features
- Emergency decision features

## Security

Please do not open public issues for vulnerabilities involving health data, encryption, local storage, app lock behavior, or backup behavior.

Use responsible disclosure for security issues.

## License

This project is licensed under the MIT License.
----------------------------------
# Self-Custody Health

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![.NET](https://img.shields.io/badge/.NET-11-512BD4)](https://dotnet.microsoft.com/)
[![C#](https://img.shields.io/badge/C%23-preview-239120.svg)](https://learn.microsoft.com/dotnet/csharp/)
[![MAUI](https://img.shields.io/badge/MAUI-native-512BD4.svg)](https://learn.microsoft.com/dotnet/maui/)

A local-first personal health records app for iOS, Android, Windows, and macOS.

## Purpose

Self-Custody Health helps people organize their health documents, treatments, appointments, reminders, vaccinations, emergency contacts, and health summary.

It is not a diagnostic app. It does not provide medical advice, medical decisions, prescriptions, triage, emergency recommendations, or clinical certification claims.

## Privacy Principles

- Your health data stays on this device by default.
- Saved vault data is encrypted locally.
- Backup and sync are user-controlled future capabilities.
- Backup data, when added, must be encrypted before leaving the device.
- No backend health-data storage is included.
- No cloud AI is included.
- No ads or tracking are included by default.

## Supported Platforms

- iOS
- Android
- Windows
- macOS through Mac Catalyst

## Tech Stack

- .NET 11 preview
- C# preview
- .NET MAUI
- xUnit v3 on Microsoft Testing Platform
- Authenticated encryption with `AesGcm`
- Platform biometric/device unlock APIs

## Getting Started

```bash
git clone https://github.com/mamby/self-custody-health.git
cd self-custody-health
dotnet restore PersonalHealthVault.sln
dotnet build src/maui/PersonalHealthVault.csproj -f net11.0-windows10.0.19041.0
dotnet test test/maui/PersonalHealthVault.Tests.csproj
```

## Current Limitations

- Document import is scaffolded but not connected to real files yet.
- Demo records are shown before a user-created encrypted vault exists.
- Backup, export, OCR, classification, and local summaries are placeholders.
- Biometric/device unlock is an app access gate; it is not a claim that encryption keys are hardware-bound.
