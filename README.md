# Flow - DeFi Platform for Developers

A complete DeFi platform providing APIs, SDKs, and developer tools for integrating cryptocurrency yield generation into applications. Flow handles compliance, regulation, and foreign exchange complexity—you focus on building.

## 🚀 Features

### Developer Portal
- **Modern Landing Page** - Professional design inspired by Square and Stripe
- **Complete API Reference** - Detailed documentation for all endpoints
- **Getting Started Guide** - Step-by-step tutorials
- **Visual Guides** - Beautiful flowcharts and scenarios
- **Interactive Dashboard** - Manage applications, API keys, and yield accounts

### API Gateway
- **RESTful API** - Clean, consistent API design
- **Authentication** - JWT tokens and Personal Access Tokens (PATs)
- **Account Management** - User accounts with secure password hashing
- **Application Management** - Create and manage multiple applications
- **Wallet Management** - Automatic Ethereum wallet generation with encrypted keys
- **Yield Accounts** - Create and manage yield accounts for multiple currencies
- **Protocol Integration** - Wrappers for Morpho Blue and Aave Pool
- **Webhooks** - Event-driven notifications (stubbed)

### Security
- **Encrypted Wallets** - Private keys encrypted with AES-256-GCM
- **Secure Authentication** - JWT and PAT-based auth
- **Environment Separation** - Sandbox and production modes
- **CORS Protection** - Configurable CORS policies

## 📁 Project Structure

```
P1/
├── flow-api/              # Kotlin API Gateway
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/    # Kotlin source code
│   │       └── resources/ # Config files, ABIs
│   ├── Dockerfile         # Docker configuration
│   ├── railway.json       # Railway deployment config
│   └── build.gradle.kts   # Gradle build config
├── index.html            # Landing page
├── dashboard.html        # User dashboard
├── api-reference.html    # API documentation
├── guides.html           # Visual guides
├── getting-started.html  # Getting started guide
└── DEPLOYMENT.md         # Deployment instructions
```

## 🛠️ Tech Stack

### Frontend
- HTML5, CSS3, JavaScript
- Modern, responsive design
- No framework dependencies

### Backend
- **Kotlin** 1.9.20
- **Ktor** 2.3.5 - Lightweight web framework
- **PostgreSQL** - Database
- **Exposed** - SQL framework
- **Web3j** - Ethereum integration
- **BouncyCastle** - Encryption

## 🏃 Quick Start

### Prerequisites
- JDK 17+
- PostgreSQL 12+
- Node.js (optional, for local frontend server)

### Local Development

1. **Clone the repository:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/flow-platform.git
   cd flow-platform
   ```

2. **Set up the database:**
   ```bash
   createdb flow_api
   ```

3. **Configure environment variables:**
   ```bash
   cd flow-api
   cp .env.example .env
   # Edit .env with your database credentials
   ```

4. **Start the API:**
   ```bash
   cd flow-api
   ./run.sh
   ```

5. **Start the frontend:**
   ```bash
   # From project root
   python3 -m http.server 3000
   # Or
   npm start
   ```

6. **Access the platform:**
   - Frontend: http://localhost:3000
   - API: http://localhost:8080

## 📚 Documentation

- [API Specification](./API_SPECIFICATION.md) - Complete API reference
- [Deployment Guide](./DEPLOYMENT.md) - Deploy to Railway or Render
- [Getting Started](./getting-started.html) - Interactive getting started guide
- [Guides](./guides.html) - Visual guides and scenarios

## 🌐 Deployment

### Railway (Recommended)
1. Push code to GitHub
2. Connect repository to Railway
3. Add PostgreSQL database
4. Set environment variables
5. Deploy!

See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed instructions.

### Render
1. Push code to GitHub
2. Create new Blueprint in Render
3. Connect repository
4. Render uses `render.yaml` automatically

## 🔐 Environment Variables

Required:
- `DATABASE_URL` - PostgreSQL connection string
- `DATABASE_USER` - Database username
- `DATABASE_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret (min 32 chars)
- `MASTER_ENCRYPTION_KEY` - Encryption key for wallets (32 bytes hex)

Optional:
- `ETH_SANDBOX_RPC_URL` - Sepolia testnet RPC
- `ETH_PRODUCTION_RPC_URL` - Mainnet RPC

See `flow-api/.env.example` for template.

## 🧪 Testing

```bash
# Run API tests
cd flow-api
./gradlew test

# Build
./gradlew build
```

## 📝 API Examples

### Create Account
```bash
curl -X POST http://localhost:8080/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "password": "secure_password"
  }'
```

### Authenticate
```bash
curl -X POST http://localhost:8080/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "password": "secure_password"
  }'
```

### Create Yield Account
```bash
curl -X POST http://localhost:8080/v1/yield/accounts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currency": "USDC"
  }'
```

## 🛣️ Roadmap

### Completed ✅
- [x] Developer portal with landing page
- [x] Complete API reference
- [x] Account creation and authentication
- [x] Application management
- [x] API key generation
- [x] Wallet generation and encryption
- [x] Yield account management
- [x] Dashboard UI
- [x] Deployment configurations

### In Progress 🚧
- [ ] Complete Morpho/Aave integration
- [ ] Transaction execution
- [ ] Webhook delivery
- [ ] Position tracking

### Planned 📋
- [ ] Rate limiting
- [ ] Monitoring and logging
- [ ] Email notifications
- [ ] Admin dashboard
- [ ] SDKs (JavaScript, Python)

## 🤝 Contributing

This is a private project for friends. If you'd like to contribute:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

MIT License - See LICENSE file for details

## 🙏 Acknowledgments

- Inspired by Square and Stripe developer portals
- Built with Kotlin, Ktor, and modern web technologies

## 📞 Support

For questions or issues:
- Check the [Guides](./guides.html)
- Review [API Documentation](./api-reference.html)
- See [Deployment Guide](./DEPLOYMENT.md)

---

**Built with ❤️ for developers who want to integrate DeFi yield into their applications.**
