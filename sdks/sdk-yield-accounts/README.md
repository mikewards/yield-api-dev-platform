# Flow Yield Accounts SDK

Embeddable UI component for creating and managing yield accounts with deposit/withdraw functionality.

## Features

- ✅ Create yield accounts
- ✅ Deposit funds
- ✅ Withdraw funds
- ✅ Real-time yield tracking
- ✅ Performance analytics
- ✅ Transaction history
- ✅ Fully customizable and skinnable
- ✅ Responsive design

## Installation

### Via CDN

```html
<iframe
  src="https://sdks.flow.com/yield-accounts?apiKey=YOUR_API_KEY&walletId=WALLET_ID"
  width="100%"
  height="700"
  frameborder="0"
></iframe>
```

### Via NPM

```bash
npm install @flow/sdk-yield-accounts
```

```javascript
import { FlowYieldAccounts } from '@flow/sdk-yield-accounts';

const yieldAccounts = new FlowYieldAccounts({
  apiKey: 'sk_live_...',
  walletId: 'wallet_123...',
  container: '#yield-accounts-container',
  theme: 'light',
  onAccountCreated: (account) => {
    console.log('Yield account created:', account);
  },
  onDeposit: (transaction) => {
    console.log('Deposit completed:', transaction);
  }
});

yieldAccounts.render();
```

## Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `apiKey` | string | Your Flow API key (required) |
| `walletId` | string | Wallet ID (required) |
| `theme` | 'light' \| 'dark' | UI theme |
| `primaryColor` | string | Primary brand color (hex) |
| `onAccountCreated` | function | Callback when account is created |
| `onDeposit` | function | Callback when deposit completes |
| `onWithdraw` | function | Callback when withdraw completes |

## Examples

See [examples directory](./examples) for complete integration examples.

## Documentation

Full documentation: https://docs.flow.com/sdks/yield-accounts

## Repository

https://github.com/flow-platform/sdk-yield-accounts

## License

MIT

