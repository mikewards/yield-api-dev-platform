# Flow Wallets SDK

Embeddable UI component for managing Ethereum wallets within your Flow applications.

## Features

- ✅ Create and manage wallets
- ✅ Real-time balance tracking
- ✅ Transaction history
- ✅ Multi-chain support
- ✅ Deposit/withdraw interface
- ✅ Fully customizable and skinnable
- ✅ Responsive design

## Installation

### Via CDN

```html
<iframe
  src="https://sdks.flow.com/wallets?apiKey=YOUR_API_KEY&applicationId=APP_ID"
  width="100%"
  height="600"
  frameborder="0"
></iframe>
```

### Via NPM

```bash
npm install @flow/sdk-wallets
```

```javascript
import { FlowWallets } from '@flow/sdk-wallets';

const wallets = new FlowWallets({
  apiKey: 'sk_live_...',
  applicationId: 'app_123...',
  container: '#wallets-container',
  theme: 'light',
  onWalletCreated: (wallet) => {
    console.log('Wallet created:', wallet);
  }
});

wallets.render();
```

## Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `apiKey` | string | Your Flow API key (required) |
| `applicationId` | string | Application ID (required) |
| `theme` | 'light' \| 'dark' | UI theme |
| `primaryColor` | string | Primary brand color (hex) |
| `onWalletCreated` | function | Callback when wallet is created |
| `onBalanceUpdated` | function | Callback when balance changes |

## Examples

See [examples directory](./examples) for complete integration examples.

## Documentation

Full documentation: https://docs.flow.com/sdks/wallets

## Repository

https://github.com/flow-platform/sdk-wallets

## License

MIT

