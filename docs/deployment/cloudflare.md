# Cloudflare Workers Deployment Guide

Complete guide for deploying the TBD frontend to Cloudflare Workers.

**Last Updated**: December 2025

## Overview

The TBD frontend is deployed as a Cloudflare Worker with static assets. This provides:
- Global edge deployment
- Clean URLs (no `.html` extensions)
- Fast performance
- Easy custom domain setup

## Prerequisites

- Cloudflare account (https://cloudflare.com)
- Node.js 18+ installed
- Wrangler CLI installed (`npm install -g wrangler`)

## Project Structure

```
frontend/
├── pages/              # HTML pages
│   ├── index.html
│   ├── dashboard.html
│   └── ...
├── styles/             # CSS files
├── scripts/            # JavaScript files
│   ├── token-manager.js   # OAuth token management
│   ├── config.js          # API configuration
│   └── ...
├── sdk-demos/          # SDK demo pages
├── worker.js           # Cloudflare Worker (URL routing)
├── wrangler.jsonc      # Cloudflare Worker config
└── favicon.svg         # Site favicon
```

## Deployment

### First-Time Setup

1. **Login to Cloudflare:**
   ```bash
   npx wrangler login
   ```

2. **Navigate to frontend directory:**
   ```bash
   cd frontend
   ```

3. **Deploy:**
   ```bash
   npx wrangler deploy
   ```

### Subsequent Deployments

```bash
cd frontend
npx wrangler deploy
```

## Configuration

### wrangler.jsonc

```jsonc
{
  "name": "tbd",
  "main": "worker.js",
  "compatibility_date": "2024-01-01",
  "assets": {
    "directory": "."
  }
}
```

### worker.js

The worker handles clean URL routing:

```javascript
// /dashboard → /pages/dashboard.html
// /api-reference → /pages/api-reference.html
// etc.
```

## URL Routing

| Clean URL | Actual File |
|-----------|-------------|
| `/` | `pages/index.html` |
| `/dashboard` | `pages/dashboard.html` |
| `/dashboard-yield` | `pages/dashboard-yield.html` |
| `/dashboard-webhooks` | `pages/dashboard-webhooks.html` |
| `/dashboard-logs` | `pages/dashboard-logs.html` |
| `/api-reference` | `pages/api-reference.html` |
| `/guides` | `pages/guides.html` |
| `/quickstart` | `pages/quickstart.html` |
| `/signin` | `pages/signin.html` |
| `/account` | `pages/account.html` |
| `/sdks` | `pages/sdks.html` |

## Local Development

### Start Development Server

```bash
cd frontend
npx wrangler dev
```

This starts a local server at `http://localhost:8787` with:
- Hot reloading
- Clean URL routing
- Same behavior as production

### Alternative: Simple HTTP Server

For quick testing without clean URLs:

```bash
cd frontend
python3 -m http.server 3000
```

## Custom Domains

### Via Cloudflare Dashboard

1. Go to Workers & Pages
2. Select your worker (`tbd`)
3. Settings → Triggers → Custom Domains
4. Add your domain

### Via wrangler.jsonc

```jsonc
{
  "name": "tbd",
  "routes": [
    { "pattern": "your-domain.com", "zone_name": "your-domain.com" }
  ]
}
```

## Environment Detection

The frontend automatically detects the environment:

```javascript
// config.js
const hostname = window.location.hostname;
const isSandboxDomain = hostname.includes('staging');

// API URLs
const API_URLS = {
  local: 'http://localhost:8080',
  sandbox: 'https://flow-platform-staging.up.railway.app',
  production: 'https://flow-platform-production.up.railway.app'
};
```

## Token Management

The frontend includes `token-manager.js` for OAuth 2.0 token handling:

- **Automatic refresh**: Silently refreshes tokens before expiration
- **Session modal**: Graceful handling of expired sessions
- **Token rotation**: Works with backend token rotation

### Usage

```javascript
// Check if authenticated
if (TokenManager.isAuthenticated()) {
  // User is logged in
}

// Make authenticated API call
const response = await TokenManager.apiCall('/v1/yield/accounts');

// Logout
TokenManager.logout();
```

## Troubleshooting

### Deployment Not Updating

1. Clear Cloudflare cache:
   ```bash
   npx wrangler pages deployment tail
   ```

2. Check deployment status in Cloudflare dashboard

3. Verify correct directory:
   ```bash
   pwd  # Should be in frontend/
   ```

### 404 Errors

1. Check `worker.js` has route defined
2. Verify file exists in correct location
3. Check file name matches route

### API Connection Issues

1. Check `config.js` has correct API URLs
2. Verify CORS is configured on backend
3. Check browser console for errors

## Best Practices

1. **Test locally first**: Use `npx wrangler dev` before deploying
2. **Keep assets small**: Optimize images and CSS
3. **Use clean URLs**: All routes should work without `.html`
4. **Check console**: Browser console shows API errors

## Deployment Workflow

After making changes:

```bash
# 1. Test locally
cd frontend
npx wrangler dev

# 2. Deploy to Cloudflare
npx wrangler deploy

# 3. Verify deployment
# Visit your Cloudflare Workers URL
```

## Related Documentation

- [Railway Deployment](./railway.md) - Backend deployment
- [Development Setup](../development/setup.md) - Local development
- [Architecture Overview](../architecture/overview.md) - System design
