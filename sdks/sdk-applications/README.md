# Flow Applications SDK

Embeddable UI component for managing Flow applications within your platform.

## Features

- ✅ Create and manage applications
- ✅ API key generation and management
- ✅ Environment configuration (sandbox/production)
- ✅ RPC URL configuration
- ✅ Webhook setup
- ✅ Fully customizable and skinnable
- ✅ Responsive design

## Installation

### Via CDN

```html
<iframe
  src="https://sdks.flow.com/applications?apiKey=YOUR_API_KEY&theme=light"
  width="100%"
  height="600"
  frameborder="0"
  allowtransparency="true"
></iframe>
```

### Via NPM

```bash
npm install @flow/sdk-applications
```

```javascript
import { FlowApplications } from '@flow/sdk-applications';

const applications = new FlowApplications({
  apiKey: 'sk_live_...',
  container: '#applications-container',
  theme: 'light',
  onApplicationCreated: (app) => {
    console.log('Application created:', app);
  }
});

applications.render();
```

## Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `apiKey` | string | Your Flow API key (required) |
| `theme` | 'light' \| 'dark' | UI theme |
| `primaryColor` | string | Primary brand color (hex) |
| `onApplicationCreated` | function | Callback when app is created |
| `onApplicationUpdated` | function | Callback when app is updated |
| `hideHeader` | boolean | Hide SDK header |

## Examples

See [examples directory](./examples) for complete integration examples.

## Documentation

Full documentation: https://docs.flow.com/sdks/applications

## Repository

https://github.com/flow-platform/sdk-applications

## License

MIT

