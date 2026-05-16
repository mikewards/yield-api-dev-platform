# Flow Embeddable SDKs

Three fully embeddable, customizable UI components for integrating Flow functionality into your application.

## SDKs Overview

### 1. Applications SDK
**Purpose:** Manage Flow applications, API keys, and configurations

**Repository:** https://github.com/flow-platform/sdk-applications
**Documentation:** https://docs.flow.com/sdks/applications
**NPM:** `@flow/sdk-applications`

### 2. Wallets SDK
**Purpose:** Create and manage Ethereum wallets, view balances, transaction history

**Repository:** https://github.com/flow-platform/sdk-wallets
**Documentation:** https://docs.flow.com/sdks/wallets
**NPM:** `@flow/sdk-wallets`

### 3. Yield Accounts SDK
**Purpose:** Create yield accounts, deposit/withdraw funds, track earnings

**Repository:** https://github.com/flow-platform/sdk-yield-accounts
**Documentation:** https://docs.flow.com/sdks/yield-accounts
**NPM:** `@flow/sdk-yield-accounts`

## Quick Start

### Via CDN (Simplest)

```html
<!-- Applications SDK -->
<iframe
  src="https://sdks.flow.com/applications?apiKey=YOUR_API_KEY&theme=light"
  width="100%"
  height="600"
  frameborder="0"
></iframe>

<!-- Wallets SDK -->
<iframe
  src="https://sdks.flow.com/wallets?apiKey=YOUR_API_KEY&applicationId=APP_ID"
  width="100%"
  height="600"
  frameborder="0"
></iframe>

<!-- Yield Accounts SDK -->
<iframe
  src="https://sdks.flow.com/yield-accounts?apiKey=YOUR_API_KEY&walletId=WALLET_ID"
  width="100%"
  height="700"
  frameborder="0"
></iframe>
```

### Via NPM

```bash
npm install @flow/sdk-applications
npm install @flow/sdk-wallets
npm install @flow/sdk-yield-accounts
```

## Customization

All SDKs support:
- **Theme:** `light` or `dark`
- **Primary Color:** Custom brand color (hex)
- **Callbacks:** Event handlers for user actions
- **Styling:** Fully skinnable via CSS variables

## Examples

Each SDK includes:
- Basic integration example
- React example
- Vue example
- Custom styling example

See individual SDK repositories for examples.

## Documentation

- **Applications SDK:** https://docs.flow.com/sdks/applications
- **Wallets SDK:** https://docs.flow.com/sdks/wallets
- **Yield Accounts SDK:** https://docs.flow.com/sdks/yield-accounts

## Support

- GitHub Issues: https://github.com/flow-platform
- Documentation: https://docs.flow.com
- Email: support@flow.com

