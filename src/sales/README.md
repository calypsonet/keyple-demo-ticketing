# Keyple Sales Demo

[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../LICENSE)

Client/server demonstration of the
[Eclipse Keyple Distributed Solution](https://keyple.org/learn/user-guide/distributed-application/), where all the
ticketing procedures and sessions are managed by the server, the clients only giving access to the card.

[⬅️ Back to Main Project](../../README.md)

The server, using the [Eclipse Keyple](https://keyple.org) SDK and a SAM, personalizes the cards and loads the contracts
(season passes, multi-trip tickets) on Calypso cards, which can hold both, and on storage cards, which hold a single
contract. Several clients are provided:
- **Android client** using the Keyple middleware: contactless cards (Android NFC reader, or the reader of the Bluebird
  terminals) and SIM cards (OMAPI reader, work in progress);
- **Kotlin Multiplatform client** (Android, iOS, desktop) using the Keyple Interop libraries and the Server JSON API;
- **.NET client** (C#), showing how an application can interact with the server without using the Keyple middleware.

## Keyple Demos

This demo is part of the Keyple Demo Ticketing ecosystem, described in the [main README](../../README.md) with the
validation and inspection demos, and uses the [common library](../common/README.md) implementing the data model of
the card.

## Code organization

This demo is organized in four source code folders, each one containing a `README` file giving more information:

- **server**: source code of the server part of the demo. The server should run on a computer with a JVM and have a card
  reader with a SAM connected.
- **client/interop-mobile-multiplatform**: source code of interop client part of the demo as a Kotlin Multiplatform
  project.
- **client/keyple-mobile-android**: source code of the client part of the demo as an Android project.
- **client/pc-dotnet**: source code of the client part of the demo not using the Keyple middleware as Microsoft Visual
  Studio project.

## Card personalization and contract loading procedures

The server personalizes the cards and loads the contracts, the clients only giving access to the card. The steps of
these procedures, on the Calypso cards and on the storage cards, are described in the annex
[Card procedures](../../docs/functional-documentation/index.adoc#card-procedures) of the Functional Documentation
([card personalization](../../docs/functional-documentation/index.adoc#personalization-procedure),
[contract loading](../../docs/functional-documentation/index.adoc#loading-procedure)).
