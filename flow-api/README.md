# Ground API Gateway

Kotlin REST API gateway for the Ground DeFi platform, wrapping Morpho and Aave protocols.

**Last Updated**: December 2025

## Features

- OAuth 2.0-style authentication (access + refresh tokens)
- Personal Access Token (PAT) management
- Multi-application support with environment-specific credentials
- Webhook delivery via Svix
- Configurable per-endpoint rate limiting
- Request/response logging (7-day retention)
- Morpho and Aave protocol integration
- PostgreSQL database with Exposed ORM
- Encrypted wallet storage

## Tech Stack

- **Kotlin** 1.9.20
- **Ktor** 2.3.5 (HTTP server)
- **Exposed** (SQL framework)
- **PostgreSQL** (database)
- **JWT** (authentication)
- **BCrypt** (password hashing)
- **Web3j** (Ethereum integration)
- **Svix** (webhook delivery)

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
   export MASTER_ENCRYPTION_KEY="your-32-byte-hex-key"
   # export SVIX_API_KEY="optional-for-local"
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
  -e MASTER_ENCRYPTION_KEY="your-encryption-key" \
  flow-api
```

## API Endpoints

### Authentication
| Endpoint | Description |
|----------|-------------|
| `POST /v1/auth/authenticate` | Login, returns access + refresh tokens |
| `POST /v1/auth/refresh` | Exchange refresh token for new tokens |
| `POST /v1/auth/logout` | Revoke all refresh tokens |

### Accounts
| Endpoint | Description |
|----------|-------------|
| `POST /v1/accounts` | Create account |
| `GET /v1/accounts/{accountId}` | Get account details |

### Applications
| Endpoint | Description |
|----------|-------------|
| `POST /v1/applications` | Create application |
| `GET /v1/applications` | List applications |
| `GET /v1/applications/{id}` | Get application |
| `DELETE /v1/applications/{id}` | Delete application |
| `POST /v1/applications/{id}/tokens` | Create API key |
| `GET /v1/applications/{id}/tokens` | List API keys |
| `DELETE /v1/applications/{id}/tokens/{tokenId}` | Revoke API key |

### Webhooks
| Endpoint | Description |
|----------|-------------|
| `GET /v1/webhooks/event-types` | List event types |
| `POST /v1/webhooks` | Create endpoint |
| `GET /v1/webhooks` | List endpoints |
| `DELETE /v1/webhooks/{id}` | Delete endpoint |
| `POST /v1/webhooks/{id}/test` | Send test event |
| `GET /v1/webhooks/portal` | Get Svix portal link |

### Yield
| Endpoint | Description |
|----------|-------------|
| `POST /v1/yield/accounts` | Create yield account |
| `GET /v1/yield/accounts` | List yield accounts |
| `POST /v1/yield/accounts/{id}/deposit` | Deposit funds |
| `POST /v1/yield/accounts/{id}/withdraw` | Withdraw funds |
| `GET /v1/yield/rates` | Get current rates |

### Markets
| Endpoint | Description |
|----------|-------------|
| `GET /v1/markets` | List all markets (Morpho + Aave) |

### Logs
| Endpoint | Description |
|----------|-------------|
| `GET /v1/logs` | Get request logs |
| `GET /v1/logs/stats` | Get log statistics |

## Database Schema

Auto-created tables:
- `accounts` - User accounts
- `applications` - Developer applications
- `access_tokens` - API keys (PATs)
- `refresh_tokens` - OAuth refresh tokens
- `yield_accounts` - Yield accounts
- `positions` - Yield positions
- `transactions` - Transaction history
- `application_wallets` - Ethereum wallets
- `webhooks` - Webhook metadata
- `request_logs` - API logs (7-day retention)

## Configuration

Edit `src/main/resources/application.conf` to configure:
- Server port and host
- Database connection
- JWT settings
- Protocol API URLs

## Development

### Running Tests
```bash
./gradlew test
```

### Building
```bash
./gradlew build
```

### Building Docker Image
```bash
docker build -t flow-api .
```

## Deployment

See [Railway Deployment Guide](../docs/deployment/railway.md) for production deployment.

## Project Structure

```
flow-api/
├── src/main/kotlin/com/ground/
│   ├── api/routes/          # API endpoints
│   ├── dto/                 # Data transfer objects
│   ├── middleware/          # Auth, rate limiting, logging
│   ├── model/               # Database models
│   ├── service/             # Business logic
│   ├── integration/         # Morpho/Aave clients
│   └── Application.kt       # Entry point
├── src/main/resources/
│   └── application.conf     # Configuration
├── build.gradle.kts         # Build config
└── Dockerfile               # Container config
```

## License

MIT
