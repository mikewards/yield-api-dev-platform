# Flow API Gateway - Testing Guide

## ✅ Your API is Live!

Your API is running at: `https://flow-platform-production.up.railway.app`

## Quick Tests

### 1. Health Check
```bash
curl https://flow-platform-production.up.railway.app/health
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "1234567890",
  "version": "1.0.0"
}
```

### 2. Create Account
```bash
curl -X POST https://flow-platform-production.up.railway.app/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass123"
  }'
```

Expected response:
```json
{
  "id": "uuid-here",
  "username": "testuser",
  "status": "active",
  "createdAt": "2025-11-28T..."
}
```

### 3. Sign In
```bash
curl -X POST https://flow-platform-production.up.railway.app/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass123"
  }'
```

Expected response:
```json
{
  "token": "jwt-token-here",
  "expiresAt": "2025-11-28T..."
}
```

## Available Endpoints

### Account Management
- `POST /v1/accounts` - Create account
- `GET /v1/accounts/{id}` - Get account

### Authentication
- `POST /v1/auth/authenticate` - Sign in

### Applications
- `POST /v1/applications` - Create application
- `GET /v1/applications` - List applications
- `GET /v1/applications/{id}` - Get application

### Yield Accounts
- `POST /v1/yield/accounts` - Create yield account
- `GET /v1/yield/accounts` - List yield accounts
- `POST /v1/yield/accounts/{id}/deposit` - Deposit funds
- `POST /v1/yield/accounts/{id}/withdraw` - Withdraw funds

## Using the API

### With cURL
All examples above use cURL. Make sure to:
- Include `Content-Type: application/json` header for POST requests
- Include `Authorization: Bearer <token>` header for authenticated requests

### With Frontend
Update your frontend code to use:
```javascript
const API_URL = 'https://flow-platform-production.up.railway.app';
```

### With Postman/Insomnia
1. Create a new request
2. Set method (GET/POST)
3. Enter URL: `https://flow-platform-production.up.railway.app/v1/...`
4. Add headers as needed
5. Add body (JSON) for POST requests

## Next Steps

1. ✅ API is deployed and working
2. ✅ Test endpoints above
3. ⏭️ Update frontend to use Railway URL
4. ⏭️ Deploy frontend (optional)
5. ⏭️ Share with friends!

## Troubleshooting

### 404 Errors
- Make sure you're using the correct endpoint path
- Check that the route exists in the API

### 401 Errors
- You need to authenticate first
- Use `POST /v1/auth/authenticate` to get a token
- Include `Authorization: Bearer <token>` header

### 500 Errors
- Check Railway logs for details
- Verify database connection is working

🎉 **Your API is live and ready to use!**

