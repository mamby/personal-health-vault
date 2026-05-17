# Architecture Overview

This repository uses one production .NET MAUI project with clear internal folders.

## Structure

- `src/SelfCustodyHealth`
  - `Domain`: health records, reminders, document categories, and vault models
  - `Crypto`: authenticated encryption and vault key contracts
  - `Storage`: encrypted local vault persistence and future backup/local AI contracts
  - `Security`: app lock and platform device unlock boundaries
  - `Features`: MAUI pages for Dashboard, Vault, Health Summary, Medications, Appointments, Settings, and Lock
  - `Shared`: reusable UI helpers and bottom sheet

## Local-First Model

Health records are loaded from an encrypted local vault file in app data storage. If no vault exists, the app displays clearly labeled demo records and does not save them as user health data.

No backend is introduced for health-data storage.

## Encrypted Vault Design

The vault store serializes the health snapshot to JSON in memory, encrypts it with `AesGcm`, then writes only an encrypted envelope to disk. The vault key is stored through MAUI `SecureStorage`.

Biometric/device unlock protects app access. It is separate from vault encryption and should not be described as hardware-bound key protection.

## Storage Boundaries

UI pages depend on services, not files. `IVaultStore` owns encrypted save/load/delete behavior. `IDocumentStore` owns document search/filter behavior.

## Future Backup Model

Backup and sync must be user-controlled. Data must be encrypted before leaving the device, and backup providers must not receive plaintext health data.

## Future Local AI Model

AI contracts exist for OCR, document classification, and summaries. Implementations must run locally by default and assist with organization only. They must not provide medical advice or diagnostic decisions.
