# Flow API Gateway

Kotlin REST API gateway for Flow DeFi platform, wrapping Morpho and Aave protocols.

## Features

- ✅ Account creation and authentication
- ✅ Personal Access Token management
- ✅ Yield account creation and management
- ✅ Deposit and withdrawal operations
- ✅ Integration stubs for Morpho and Aave
- ✅ PostgreSQL database with Exposed ORM
- ✅ JWT authentication
- ✅ CORS support
- ✅ Error handling

## Tech Stack

- **Kotlin** 1.9.20
- **Ktor** 2.3.5 (HTTP server)
- **Exposed** (SQL framework)
- **PostgreSQL** (database)
- **JWT** (authentication)
- **BCrypt** (password hashing)

## Setup

### Prerequisites

- JDK 17+
- PostgreSQL 12+
- Gradle 8.4+

### Local Development

1. **Set up PostgreSQL database:**
```bash
createdb flow_api
```

2. **Set environment variables:**
```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/flow_api"
export DATABASE_USER="your_username"
export DATABASE_PASSWORD="your_password"
export JWT_SECRET="your-secret-key-min-32-chars"
```

3. **Run the application:**
```bash
./gradlew run
```

The API will be available at `http://localhost:8080`

### Using Docker

```bash
docker build -t flow-api .
docker run -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/flow_api" \
  -e DATABASE_USER="your_username" \
  -e DATABASE_PASSWORD="your_password" \
  -e JWT_SECRET="your-secret-key" \
  flow-api
```

## API Endpoints

### Authentication
- `POST /v1/auth/authenticate` - Authenticate with username/password

### Accounts
- `POST /v1/accounts` - Create account
- `GET /v1/accounts/{accountId}` - Get account details

### Access Tokens
- `POST /v1/access-tokens` - Create Personal Access Token
- `GET /v1/access-tokens` - List tokens
- `DELETE /v1/access-tokens/{tokenId}` - Revoke token

### Yield Accounts
- `POST /v1/yield/accounts` - Create yield account
- `GET /v1/yield/accounts` - List yield accounts
- `GET /v1/yield/accounts/{yieldAccountId}` - Get yield account
- `POST /v1/yield/accounts/{yieldAccountId}/deposit` - Deposit funds
- `POST /v1/yield/accounts/{yieldAccountId}/withdraw` - Withdraw funds

## Example Usage

### 1. Create Account
```bash
curl -X POST http://localhost:8080/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer123",
    "password": "secure_password_123",
    "email": "developer@example.com"
  }'
```

### 2. Authenticate
```bash
curl -X POST http://localhost:8080/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer123",
    "password": "secure_password_123"
  }'
```

### 3. Create Access Token
```bash
curl -X POST http://localhost:8080/v1/access-tokens \
  -H "Authorization: Bearer <temp_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Production API Key",
    "expires_in": 31536000
  }'
```

### 4. Create Yield Account
```bash
curl -X POST http://localhost:8080/v1/yield/accounts \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "currency": "USDC",
    "initial_deposit": {
      "amount": "1000.00",
      "currency": "USDC"
    },
    "protocol_preference": "auto"
  }'
```

## Database Schema

The application automatically creates the following tables:
- `accounts` - User accounts
- `access_tokens` - Personal access tokens
- `yield_accounts` - Yield accounts
- `positions` - Yield positions
- `transactions` - Transaction history
- `webhooks` - Webhook subscriptions

## Configuration

Edit `src/main/resources/application.conf` to configure:
- Server port and host
- Database connection
- JWT settings
- Morpho/Aave API URLs

## Deployment

### Railway
1. Connect your GitHub repo
2. Add PostgreSQL service
3. Set environment variables
4. Deploy!

### Render
1. Create new Web Service
2. Connect GitHub repo
3. Add PostgreSQL database
4. Set environment variables
5. Deploy!

## Development

### Running Tests
```bash
./gradlew test
```

### Building
```bash
./gradlew build
```

## Next Steps

- [ ] Implement Morpho GraphQL integration
- [ ] Implement Aave REST API integration
- [ ] Add rate limiting
- [ ] Add webhook delivery system
- [ ] Add transaction monitoring
- [ ] Add blockchain wallet integration
- [ ] Add comprehensive error handling
- [ ] Add request validation
- [ ] Add API documentation (OpenAPI/Swagger)

## License

MIT
