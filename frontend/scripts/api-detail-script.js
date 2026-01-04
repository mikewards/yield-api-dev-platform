// API Detail Page Script

// Rate limit configuration (must match backend RateLimitConfig.kt)
const rateLimits = {
    // Reduced limits (75 req/min)
    'GET /v1/markets': { limit: 75, window: '1 minute' },
    'GET /v1/yield/rates': { limit: 75, window: '1 minute' },
    
    // Standard limits (100 req/min)
    'POST /v1/auth/authenticate': { limit: 100, window: '1 minute' },
    'POST /v1/accounts': { limit: 100, window: '1 minute' },
    'GET /v1/yield/accounts': { limit: 100, window: '1 minute' },
    'POST /v1/yield/accounts': { limit: 100, window: '1 minute' },
    'GET /v1/yield/positions': { limit: 100, window: '1 minute' },
    'GET /v1/transactions': { limit: 100, window: '1 minute' },
    'POST /v1/applications': { limit: 100, window: '1 minute' },
    'GET /v1/applications': { limit: 100, window: '1 minute' },
    
    // Higher limits
    'GET /health': { limit: 300, window: '1 minute' },
    
    // Default for unlisted endpoints
    'default': { limit: 100, window: '1 minute' }
};

// Get rate limit for an endpoint
function getRateLimit(method, path) {
    const key = `${method} ${path}`;
    return rateLimits[key] || rateLimits['default'];
}

// Comprehensive API endpoint data with detailed request body definitions
const apiData = {
    'yield-accounts': {
        create: {
            method: 'POST',
            path: '/v1/yield/accounts',
            title: 'Create yield account',
            summary: 'Create a new yield account to start earning interest on cryptocurrency deposits. Ground automatically routes funds to the best available protocol (Morpho or Aave) to ensure 6% yield.',
            description: 'Creates a yield account using the provided parameters.',
            permissions: 'YIELD_WRITE',
            requestBody: [
                {
                    name: 'currency',
                    type: 'string',
                    required: true,
                    description: 'The cryptocurrency to use for this yield account. This determines which markets and protocols will be available for yield generation.',
                    constraints: [
                        { type: 'enum', values: ['USDC', 'USDT', 'DAI', 'ETH', 'WBTC'] }
                    ]
                },
                {
                    name: 'initial_deposit',
                    type: 'Money',
                    required: false,
                    description: 'Optional initial deposit amount. If provided, funds will be deposited immediately upon account creation. The amount must be specified as a string in the smallest denomination of the applicable currency.',
                    nested: [
                        {
                            name: 'amount',
                            type: 'string',
                            required: true,
                            description: 'The amount of money, in the smallest denomination of the applicable currency. For example, USDC amounts are specified in the smallest unit (e.g., "1000.00" for 1000 USDC).',
                            constraints: [
                                { type: 'min_length', value: 1 },
                                { type: 'pattern', value: '^[0-9]+(\\.[0-9]{1,18})?$' }
                            ]
                        },
                        {
                            name: 'currency',
                            type: 'string',
                            required: true,
                            description: 'The type of currency, in ISO 4217 format. Must match the currency specified in the parent currency field.',
                            constraints: [
                                { type: 'enum', values: ['USDC', 'USDT', 'DAI', 'ETH', 'WBTC'] }
                            ]
                        }
                    ]
                },
                {
                    name: 'protocol_preference',
                    type: 'string',
                    required: false,
                    description: 'Preferred DeFi protocol for this account. Use "auto" to let Ground automatically select the best protocol based on current rates and availability. Specify "morpho" or "aave" to force a specific protocol.',
                    constraints: [
                        { type: 'enum', values: ['auto', 'morpho', 'aave'] },
                        { type: 'default', value: 'auto' }
                    ]
                }
            ],
            successStatus: '201 Created',
            successResponse: {
                account_id: 'ya_1234567890abcdef',
                currency: 'USDC',
                protocol: 'morpho',
                annual_yield_rate: 0.06,
                status: 'active',
                created_at: '2025-01-15T10:30:00Z',
                balance: {
                    amount: '1000.00',
                    currency: 'USDC'
                }
            },
            errorStatus: '400 Bad Request',
            errorResponse: {
                error: {
                    code: 'INSUFFICIENT_FUNDS',
                    message: 'Insufficient funds to create yield account',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/yield/accounts \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "currency": "USDC",
    "initial_deposit": {
      "amount": "1000.00",
      "currency": "USDC"
    },
    "protocol_preference": "auto"
  }'`
        },
        deposit: {
            method: 'POST',
            path: '/v1/yield/accounts/{account_id}/deposit',
            title: 'Deposit funds',
            summary: 'Deposit funds into an existing yield account. The funds will immediately begin earning yield at the account\'s current rate.',
            description: 'Deposits funds into a yield account.',
            permissions: 'YIELD_WRITE',
            requestBody: [
                {
                    name: 'amount',
                    type: 'string',
                    required: true,
                    description: 'The amount of money to deposit, in the smallest denomination of the applicable currency. For example, USDC amounts are specified as strings (e.g., "1000.00" for 1000 USDC).',
                    constraints: [
                        { type: 'min_length', value: 1 },
                        { type: 'pattern', value: '^[0-9]+(\\.[0-9]{1,18})?$' }
                    ]
                },
                {
                    name: 'currency',
                    type: 'string',
                    required: true,
                    description: 'The type of currency being deposited. Must match the currency of the yield account.',
                    constraints: [
                        { type: 'enum', values: ['USDC', 'USDT', 'DAI', 'ETH', 'WBTC'] }
                    ]
                },
                {
                    name: 'source_address',
                    type: 'string',
                    required: false,
                    description: 'Optional source wallet address for the deposit. If not provided, TBD will use the default deposit address for your account.',
                    constraints: [
                        { type: 'pattern', value: '^0x[a-fA-F0-9]{40}$' }
                    ]
                }
            ],
            successStatus: '200 OK',
            successResponse: {
                transaction_id: 'txn_1234567890abcdef',
                account_id: 'ya_1234567890abcdef',
                amount: {
                    amount: '1000.00',
                    currency: 'USDC'
                },
                status: 'pending',
                created_at: '2025-01-15T10:30:00Z'
            },
            errorStatus: '400 Bad Request',
            errorResponse: {
                error: {
                    code: 'INVALID_CURRENCY',
                    message: 'Currency does not match account currency',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/yield/accounts/ya_1234567890abcdef/deposit \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": "1000.00",
    "currency": "USDC"
  }'`
        },
        withdraw: {
            method: 'POST',
            path: '/v1/yield/accounts/{account_id}/withdraw',
            title: 'Withdraw funds',
            summary: 'Withdraw funds from a yield account. The withdrawal will be processed and funds will be sent to the specified destination address.',
            description: 'Withdraws funds from a yield account.',
            permissions: 'YIELD_WRITE',
            requestBody: [
                {
                    name: 'amount',
                    type: 'string',
                    required: true,
                    description: 'The amount of money to withdraw, in the smallest denomination of the applicable currency. The amount cannot exceed the available balance in the account.',
                    constraints: [
                        { type: 'min_length', value: 1 },
                        { type: 'pattern', value: '^[0-9]+(\\.[0-9]{1,18})?$' }
                    ]
                },
                {
                    name: 'currency',
                    type: 'string',
                    required: true,
                    description: 'The type of currency being withdrawn. Must match the currency of the yield account.',
                    constraints: [
                        { type: 'enum', values: ['USDC', 'USDT', 'DAI', 'ETH', 'WBTC'] }
                    ]
                },
                {
                    name: 'destination_address',
                    type: 'string',
                    required: true,
                    description: 'The destination wallet address where the withdrawn funds will be sent. Must be a valid Ethereum-compatible address.',
                    constraints: [
                        { type: 'pattern', value: '^0x[a-fA-F0-9]{40}$' },
                        { type: 'min_length', value: 42 },
                        { type: 'max_length', value: 42 }
                    ]
                }
            ],
            successStatus: '200 OK',
            successResponse: {
                transaction_id: 'txn_1234567890abcdef',
                account_id: 'ya_1234567890abcdef',
                amount: {
                    amount: '500.00',
                    currency: 'USDC'
                },
                destination_address: '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb',
                status: 'pending',
                created_at: '2025-01-15T10:30:00Z'
            },
            errorStatus: '400 Bad Request',
            errorResponse: {
                error: {
                    code: 'INSUFFICIENT_BALANCE',
                    message: 'Insufficient balance for withdrawal',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/yield/accounts/ya_1234567890abcdef/withdraw \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": "500.00",
    "currency": "USDC",
    "destination_address": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb"
  }'`
        }
    },
    'auth': {
        authenticate: {
            method: 'POST',
            path: '/v1/auth/authenticate',
            title: 'Authenticate',
            summary: 'Authenticate with your username and password to receive a temporary access token. Use this token to create Personal Access Tokens for API authentication.',
            description: 'Authenticates a user and returns a temporary access token.',
            permissions: 'PUBLIC',
            requestBody: [
                {
                    name: 'username',
                    type: 'string',
                    required: true,
                    description: 'Your Ground account username. This is the username you used when creating your account.',
                    constraints: [
                        { type: 'min_length', value: 3 },
                        { type: 'max_length', value: 50 },
                        { type: 'pattern', value: '^[a-zA-Z0-9_]+$' }
                    ]
                },
                {
                    name: 'password',
                    type: 'string',
                    required: true,
                    description: 'Your Ground account password. Must be at least 8 characters long.',
                    constraints: [
                        { type: 'min_length', value: 8 }
                    ]
                }
            ],
            successStatus: '200 OK',
            successResponse: {
                access_token: 'sk_live_temp_1234567890abcdef',
                token_type: 'Bearer',
                expires_in: 3600,
                account_id: 'acc_1234567890'
            },
            errorStatus: '401 Unauthorized',
            errorResponse: {
                error: {
                    code: 'INVALID_CREDENTIALS',
                    message: 'Invalid username or password',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/auth/authenticate \\
  -X POST \\
  -H "Content-Type: application/json" \\
  -d '{
    "username": "developer@example.com",
    "password": "your_password"
  }'`
        }
    },
    'accounts': {
        create: {
            method: 'POST',
            path: '/v1/accounts',
            title: 'Create account',
            summary: 'Create a new developer account. After account creation, you can authenticate and generate Personal Access Tokens for API authentication.',
            description: 'Creates a new developer account.',
            permissions: 'PUBLIC',
            requestBody: [
                {
                    name: 'username',
                    type: 'string',
                    required: true,
                    description: 'Unique username for your account. Can contain letters, numbers, and underscores. This will be used to authenticate API requests.',
                    constraints: [
                        { type: 'min_length', value: 3 },
                        { type: 'max_length', value: 50 },
                        { type: 'pattern', value: '^[a-zA-Z0-9_]+$' }
                    ]
                },
                {
                    name: 'password',
                    type: 'string',
                    required: true,
                    description: 'Account password. Must be at least 8 characters long. Choose a strong password to secure your account.',
                    constraints: [
                        { type: 'min_length', value: 8 }
                    ]
                },
                {
                    name: 'email',
                    type: 'string',
                    required: false,
                    description: 'Optional email address for account notifications, password recovery, and important updates. Must be a valid email format.',
                    constraints: [
                        { type: 'format', value: 'email' },
                        { type: 'max_length', value: 255 }
                    ]
                }
            ],
            successStatus: '201 Created',
            successResponse: {
                account_id: 'acc_1234567890abcdef',
                username: 'developer123',
                created_at: '2025-01-15T10:30:00Z',
                status: 'active'
            },
            errorStatus: '409 Conflict',
            errorResponse: {
                error: {
                    code: 'USERNAME_TAKEN',
                    message: 'Username is already taken',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/accounts \\
  -X POST \\
  -H "Content-Type: application/json" \\
  -d '{
    "username": "developer123",
    "password": "secure_password_123",
    "email": "developer@example.com"
  }'`
        }
    },
    'tokens': {
        create: {
            method: 'POST',
            path: '/v1/access-tokens',
            title: 'Create access token',
            summary: 'Generate a new Personal Access Token for API authentication. Tokens are scoped to your account and can be revoked at any time. Save your token securely as you won\'t be able to see it again after creation.',
            description: 'Creates a new Personal Access Token.',
            permissions: 'AUTHENTICATED',
            requestBody: [
                {
                    name: 'name',
                    type: 'string',
                    required: true,
                    description: 'A descriptive name for this token to help you identify it later (e.g., "Production API Key", "Development Key", "Mobile App Key"). This name will be shown in your token list.',
                    constraints: [
                        { type: 'min_length', value: 1 },
                        { type: 'max_length', value: 100 }
                    ]
                },
                {
                    name: 'expires_in',
                    type: 'integer',
                    required: false,
                    description: 'Token expiration time in seconds. If not provided, the token will never expire. Minimum is 3600 seconds (1 hour), maximum is 31536000 seconds (1 year).',
                    constraints: [
                        { type: 'minimum', value: 3600 },
                        { type: 'maximum', value: 31536000 }
                    ]
                }
            ],
            successStatus: '201 Created',
            successResponse: {
                token_id: 'tok_1234567890abcdef',
                access_token: 'sk_live_1234567890abcdef',
                name: 'Production API Key',
                created_at: '2025-01-15T10:30:00Z',
                expires_at: null
            },
            errorStatus: '401 Unauthorized',
            errorResponse: {
                error: {
                    code: 'UNAUTHORIZED',
                    message: 'Authentication required',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/access-tokens \\
  -X POST \\
  -H "Authorization: Bearer sk_live_temp_..." \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "Production API Key",
    "expires_in": 31536000
  }'`
        }
    },
    'applications': {
        create: {
            method: 'POST',
            path: '/v1/applications',
            title: 'Create application',
            summary: 'Create a new application. Each application automatically gets an Ethereum wallet and can be configured for sandbox (testnet) or production (mainnet) environments.',
            description: 'Creates a new application with the specified settings. A default wallet is automatically created for the application.',
            permissions: 'APPLICATION_WRITE',
            requestBody: [
                {
                    name: 'name',
                    type: 'string',
                    required: true,
                    description: 'A human-readable name for your application. This name will be displayed in the dashboard and can be used to identify different applications.',
                    constraints: [
                        { type: 'min_length', value: 1 },
                        { type: 'max_length', value: 100 }
                    ]
                },
                {
                    name: 'description',
                    type: 'string',
                    required: false,
                    description: 'An optional description of your application. Useful for documenting the purpose and scope of the application.',
                    constraints: [
                        { type: 'max_length', value: 500 }
                    ]
                },
                {
                    name: 'environment',
                    type: 'string',
                    required: true,
                    description: 'The environment for this application. Use "sandbox" for testing on testnets (Sepolia) or "production" for mainnet. This determines which blockchain network the application will interact with.',
                    constraints: [
                        { type: 'enum', values: ['sandbox', 'production'] }
                    ]
                },
                {
                    name: 'webhook_url',
                    type: 'string',
                    required: false,
                    description: 'The URL where webhook events will be sent. Must be a valid HTTPS URL. Webhooks will be signed with the application\'s webhook secret.',
                    constraints: [
                        { type: 'pattern', value: '^https://.*' }
                    ]
                },
                {
                    name: 'allowed_origins',
                    type: 'array',
                    required: false,
                    description: 'An array of allowed CORS origins. These origins will be allowed to make cross-origin requests to the Ground API when using this application\'s API keys.',
                    nested: [
                        {
                            name: 'origin',
                            type: 'string',
                            required: true,
                            description: 'A valid origin URL (e.g., "https://myapp.com")',
                            constraints: [
                                { type: 'pattern', value: '^https?://.*' }
                            ]
                        }
                    ]
                },
                {
                    name: 'permissions',
                    type: 'array',
                    required: false,
                    description: 'An array of permission strings that define what this application can do. If not specified, the application will have all available permissions.',
                    nested: [
                        {
                            name: 'permission',
                            type: 'string',
                            required: true,
                            description: 'A permission string (e.g., "yield:read", "yield:write", "wallet:read")',
                            constraints: [
                                { type: 'enum', values: ['yield:read', 'yield:write', 'wallet:read', 'wallet:write', 'transaction:read', 'transaction:write'] }
                            ]
                        }
                    ]
                },
                {
                    name: 'sandbox_rpc_url',
                    type: 'string',
                    required: true,
                    description: 'Ethereum RPC URL for sandbox/testnet environment. Required for blockchain interactions. Get free API keys from Infura (https://infura.io) or Alchemy (https://alchemy.com). For Sepolia testnet, use: https://sepolia.infura.io/v3/YOUR_KEY or https://eth-sepolia.g.alchemy.com/v2/YOUR_KEY',
                    constraints: [
                        { type: 'pattern', value: '^https://.*' },
                        { type: 'min_length', value: 1 }
                    ]
                },
                {
                    name: 'production_rpc_url',
                    type: 'string',
                    required: true,
                    description: 'Ethereum RPC URL for production/mainnet environment. Required for blockchain interactions. Get free API keys from Infura (https://infura.io) or Alchemy (https://alchemy.com). For Ethereum mainnet, use: https://mainnet.infura.io/v3/YOUR_KEY or https://eth-mainnet.g.alchemy.com/v2/YOUR_KEY',
                    constraints: [
                        { type: 'pattern', value: '^https://.*' },
                        { type: 'min_length', value: 1 }
                    ]
                }
            ],
            successStatus: '201 Created',
            successResponse: {
                application_id: 'app_1234567890abcdef',
                name: 'My Production App',
                description: 'Production application for mainnet',
                environment: 'production',
                status: 'active',
                webhook_url: 'https://myapp.com/webhooks',
                webhook_secret: 'whsec_abc123...',
                allowed_origins: ['https://myapp.com'],
                permissions: ['yield:read', 'yield:write'],
                created_at: '2025-01-15T10:30:00Z',
                updated_at: '2025-01-15T10:30:00Z'
            },
            errorStatus: '400 Bad Request',
            errorResponse: {
                error: {
                    code: 'INVALID_ENVIRONMENT',
                    message: 'Invalid environment specified',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/applications \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "My Production App",
    "description": "Production application for mainnet",
    "environment": "production",
    "webhook_url": "https://myapp.com/webhooks",
    "allowed_origins": ["https://myapp.com"],
    "permissions": ["yield:read", "yield:write"],
    "sandbox_rpc_url": "https://sepolia.infura.io/v3/YOUR_KEY",
    "production_rpc_url": "https://mainnet.infura.io/v3/YOUR_KEY"
  }'`
        },
        list: {
            method: 'GET',
            path: '/v1/applications',
            title: 'List applications',
            summary: 'List all applications for your account.',
            description: 'Returns a list of all applications associated with your account.',
            permissions: 'APPLICATION_READ',
            requestBody: [],
            successStatus: '200 OK',
            successResponse: {
                applications: [
                    {
                        application_id: 'app_1234567890abcdef',
                        name: 'My Production App',
                        environment: 'production',
                        status: 'active',
                        created_at: '2025-01-15T10:30:00Z'
                    }
                ]
            },
            errorStatus: '401 Unauthorized',
            errorResponse: {
                error: {
                    code: 'UNAUTHORIZED',
                    message: 'Authentication required',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/applications \\
  -X GET \\
  -H "Authorization: Bearer sk_live_1234567890abcdef"`
        }
    },
    'wallets': {
        create: {
            method: 'POST',
            path: '/v1/applications/{application_id}/wallets',
            title: 'Create wallet',
            summary: 'Create a new Ethereum wallet for an application. Applications can have multiple wallets for different purposes.',
            description: 'Creates a new Ethereum wallet and stores the encrypted private key securely. The wallet address is returned immediately, but the private key is never exposed.',
            permissions: 'WALLET_WRITE',
            requestBody: [
                {
                    name: 'label',
                    type: 'string',
                    required: false,
                    description: 'An optional human-readable label for this wallet. Useful for identifying wallets used for different purposes (e.g., "Primary Wallet", "Gas Wallet").',
                    constraints: [
                        { type: 'max_length', value: 100 }
                    ]
                },
                {
                    name: 'chain',
                    type: 'string',
                    required: false,
                    description: 'The blockchain network for this wallet. Currently only "ethereum" is supported.',
                    constraints: [
                        { type: 'enum', values: ['ethereum'] },
                        { type: 'default', value: 'ethereum' }
                    ]
                }
            ],
            successStatus: '201 Created',
            successResponse: {
                wallet_id: 'wal_1234567890abcdef',
                address: '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb',
                environment: 'sandbox',
                chain: 'ethereum',
                label: 'Secondary Wallet',
                status: 'active',
                created_at: '2025-01-15T10:30:00Z'
            },
            errorStatus: '404 Not Found',
            errorResponse: {
                error: {
                    code: 'APPLICATION_NOT_FOUND',
                    message: 'Application not found',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/applications/app_1234567890abcdef/wallets \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "label": "Secondary Wallet",
    "chain": "ethereum"
  }'`
        },
        list: {
            method: 'GET',
            path: '/v1/applications/{application_id}/wallets',
            title: 'List wallets',
            summary: 'List all wallets for an application.',
            description: 'Returns a list of all wallets associated with the specified application.',
            permissions: 'WALLET_READ',
            requestBody: [],
            successStatus: '200 OK',
            successResponse: {
                wallets: [
                    {
                        wallet_id: 'wal_1234567890abcdef',
                        address: '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb',
                        environment: 'sandbox',
                        chain: 'ethereum',
                        label: 'Primary Wallet',
                        status: 'active',
                        created_at: '2025-01-15T10:30:00Z'
                    }
                ]
            },
            errorStatus: '404 Not Found',
            errorResponse: {
                error: {
                    code: 'APPLICATION_NOT_FOUND',
                    message: 'Application not found',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/applications/app_1234567890abcdef/wallets \\
  -X GET \\
  -H "Authorization: Bearer sk_live_1234567890abcdef"`
        }
    },
    'app-tokens': {
        create: {
            method: 'POST',
            path: '/v1/applications/{application_id}/tokens',
            title: 'Create application token',
            summary: 'Create a new API key scoped to a specific application. These tokens inherit the application\'s environment and can be used for authenticated API calls.',
            description: 'Creates a new Personal Access Token that is scoped to the specified application. The token will have the same environment (sandbox/production) as the application.',
            permissions: 'TOKEN_WRITE',
            requestBody: [
                {
                    name: 'name',
                    type: 'string',
                    required: true,
                    description: 'A human-readable name for this API key. This name will be displayed in the dashboard and can be used to identify different keys.',
                    constraints: [
                        { type: 'min_length', value: 1 },
                        { type: 'max_length', value: 100 }
                    ]
                },
                {
                    name: 'expires_in',
                    type: 'integer',
                    required: false,
                    description: 'The number of seconds until this token expires. If not specified, the token will never expire. Minimum: 3600 (1 hour), Maximum: 31536000 (1 year).',
                    constraints: [
                        { type: 'min', value: 3600 },
                        { type: 'max', value: 31536000 }
                    ]
                }
            ],
            successStatus: '201 Created',
            successResponse: {
                token_id: 'tok_1234567890abcdef',
                application_id: 'app_1234567890abcdef',
                access_token: 'flow_prod_1234567890abcdef',
                name: 'Production Server Key',
                environment: 'production',
                permissions: null,
                created_at: '2025-01-15T10:30:00Z',
                expires_at: null,
                last_used_at: null
            },
            errorStatus: '404 Not Found',
            errorResponse: {
                error: {
                    code: 'APPLICATION_NOT_FOUND',
                    message: 'Application not found',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/applications/app_1234567890abcdef/tokens \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "Production Server Key",
    "expires_in": 31536000
  }'`
        }
    },
    'rates': {
        get: {
            method: 'GET',
            path: '/v1/yield/rates',
            title: 'Get yield rates',
            summary: 'Get current yield rates for all supported cryptocurrencies and protocols. Returns rates from both Morpho and Aave protocols.',
            description: 'Retrieves current yield rates (APY) for supported currencies across both Morpho and Aave protocols.',
            permissions: 'YIELD_READ',
            requestBody: [],
            successStatus: '200 OK',
            successResponse: {
                rates: [
                    {
                        currency: 'USDC',
                        protocol: 'morpho',
                        network: 'ethereum_mainnet',
                        annual_yield_rate: 0.044,
                        apy: 0.044,
                        updated_at: '2025-01-15T10:30:00Z'
                    },
                    {
                        currency: 'USDC',
                        protocol: 'aave',
                        network: 'ethereum_mainnet',
                        annual_yield_rate: 0.036,
                        apy: 0.036,
                        updated_at: '2025-01-15T10:30:00Z'
                    }
                ]
            },
            errorStatus: '401 Unauthorized',
            errorResponse: {
                error: {
                    code: 'UNAUTHORIZED',
                    message: 'Invalid or missing authentication token',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/yield/rates \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json"`
        }
    },
    'markets': {
        list: {
            method: 'GET',
            path: '/v1/markets',
            title: 'List markets',
            summary: 'List all available markets across Morpho and Aave protocols with current rates and availability.',
            description: 'Returns a list of all available yield markets from both Morpho and Aave protocols, including current APY rates and network information.',
            permissions: 'MARKETS_READ',
            requestBody: [],
            successStatus: '200 OK',
            successResponse: {
                markets: [
                    {
                        market_id: 'morpho_usdc_eth',
                        protocol: 'morpho',
                        currency: 'USDC',
                        currency_address: '0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48',
                        network: 'ethereum_mainnet',
                        apy: 0.044,
                        status: 'active',
                        updated_at: '2025-01-15T10:30:00Z'
                    },
                    {
                        market_id: 'aave_usdc',
                        protocol: 'aave',
                        currency: 'USDC',
                        currency_address: '0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48',
                        network: 'ethereum_mainnet',
                        apy: 0.036,
                        status: 'active',
                        updated_at: '2025-01-15T10:30:00Z'
                    }
                ]
            },
            errorStatus: '401 Unauthorized',
            errorResponse: {
                error: {
                    code: 'UNAUTHORIZED',
                    message: 'Invalid or missing authentication token',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/markets \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json"`
        }
    },
    
    // Webhooks endpoints
    webhooks: {
        'event-types': {
            method: 'GET',
            path: '/v1/webhooks/event-types',
            title: 'List event types',
            summary: 'Get a list of all available webhook event types you can subscribe to.',
            description: 'Returns all event types that can trigger webhook notifications. This endpoint is public and does not require authentication.',
            permissions: 'PUBLIC',
            requestBody: [],
            successStatus: '200 OK',
            successResponse: {
                event_types: [
                    { name: 'deposit.completed', description: 'Triggered when funds are successfully deposited to a yield account' },
                    { name: 'withdrawal.completed', description: 'Triggered when funds are successfully withdrawn from a yield account' },
                    { name: 'yield.accrued', description: 'Triggered when yield is accrued to an account (daily)' },
                    { name: 'rate.changed', description: 'Triggered when yield rates change significantly (>0.5%)' },
                    { name: 'account.status.changed', description: 'Triggered when a yield account status changes' },
                    { name: 'application.created', description: 'Triggered when a new application is created' },
                    { name: 'api_key.created', description: 'Triggered when a new API key is generated' }
                ]
            },
            errorStatus: '500 Internal Server Error',
            errorResponse: {
                error: {
                    code: 'INTERNAL_ERROR',
                    message: 'Failed to fetch event types',
                    type: 'server_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/webhooks/event-types`
        },
        create: {
            method: 'POST',
            path: '/v1/webhooks',
            title: 'Create webhook endpoint',
            summary: 'Register a new webhook endpoint to receive real-time event notifications.',
            description: 'Creates a webhook endpoint that will receive HTTP POST requests when specified events occur. Webhooks are delivered via Svix with automatic retries and signature verification.',
            permissions: 'WEBHOOK_WRITE',
            requestBody: [
                {
                    name: 'url',
                    type: 'string',
                    required: true,
                    description: 'The HTTPS URL where webhook events will be sent. Must use HTTPS.',
                    constraints: [
                        { type: 'pattern', value: '^https://.*' }
                    ]
                },
                {
                    name: 'description',
                    type: 'string',
                    required: false,
                    description: 'A human-readable description for this webhook endpoint.'
                },
                {
                    name: 'filter_types',
                    type: 'array',
                    required: false,
                    description: 'Array of event types to subscribe to. If omitted, the endpoint receives all events.',
                    constraints: [
                        { type: 'enum', values: ['deposit.completed', 'withdrawal.completed', 'yield.accrued', 'rate.changed', 'account.status.changed', 'application.created', 'api_key.created'] }
                    ]
                }
            ],
            successStatus: '201 Created',
            successResponse: {
                id: 'ep_1234567890abcdef',
                url: 'https://your-server.com/webhooks',
                description: 'Production webhook endpoint',
                filter_types: ['deposit.completed', 'withdrawal.completed'],
                created_at: '2025-01-15T10:30:00Z',
                updated_at: '2025-01-15T10:30:00Z',
                disabled: false
            },
            errorStatus: '400 Bad Request',
            errorResponse: {
                error: {
                    code: 'INVALID_URL',
                    message: 'Webhook URL must use HTTPS',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/webhooks \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "url": "https://your-server.com/webhooks",
    "description": "Production webhook endpoint",
    "filter_types": ["deposit.completed", "withdrawal.completed"]
  }'`
        },
        list: {
            method: 'GET',
            path: '/v1/webhooks',
            title: 'List webhook endpoints',
            summary: 'List all webhook endpoints registered for your account.',
            description: 'Returns a list of all webhook endpoints associated with your account.',
            permissions: 'WEBHOOK_READ',
            requestBody: [],
            successStatus: '200 OK',
            successResponse: {
                endpoints: [
                    {
                        id: 'ep_1234567890abcdef',
                        url: 'https://your-server.com/webhooks',
                        description: 'Production webhook endpoint',
                        filter_types: ['deposit.completed', 'withdrawal.completed'],
                        created_at: '2025-01-15T10:30:00Z',
                        updated_at: '2025-01-15T10:30:00Z',
                        disabled: false
                    }
                ],
                total: 1
            },
            errorStatus: '401 Unauthorized',
            errorResponse: {
                error: {
                    code: 'UNAUTHORIZED',
                    message: 'Invalid or missing authentication token',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/webhooks \\
  -H "Authorization: Bearer sk_live_1234567890abcdef"`
        },
        get: {
            method: 'GET',
            path: '/v1/webhooks/{id}',
            title: 'Get webhook endpoint',
            summary: 'Retrieve details of a specific webhook endpoint.',
            description: 'Returns the details of a webhook endpoint by its ID.',
            permissions: 'WEBHOOK_READ',
            requestBody: [],
            successStatus: '200 OK',
            successResponse: {
                id: 'ep_1234567890abcdef',
                url: 'https://your-server.com/webhooks',
                description: 'Production webhook endpoint',
                filter_types: ['deposit.completed', 'withdrawal.completed'],
                created_at: '2025-01-15T10:30:00Z',
                updated_at: '2025-01-15T10:30:00Z',
                disabled: false
            },
            errorStatus: '404 Not Found',
            errorResponse: {
                error: {
                    code: 'NOT_FOUND',
                    message: 'Endpoint not found',
                    type: 'not_found_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/webhooks/ep_1234567890abcdef \\
  -H "Authorization: Bearer sk_live_1234567890abcdef"`
        },
        delete: {
            method: 'DELETE',
            path: '/v1/webhooks/{id}',
            title: 'Delete webhook endpoint',
            summary: 'Delete a webhook endpoint.',
            description: 'Permanently deletes a webhook endpoint. This action cannot be undone.',
            permissions: 'WEBHOOK_WRITE',
            requestBody: [],
            successStatus: '204 No Content',
            successResponse: null,
            errorStatus: '404 Not Found',
            errorResponse: {
                error: {
                    code: 'NOT_FOUND',
                    message: 'Endpoint not found or could not be deleted',
                    type: 'not_found_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/webhooks/ep_1234567890abcdef \\
  -X DELETE \\
  -H "Authorization: Bearer sk_live_1234567890abcdef"`
        },
        test: {
            method: 'POST',
            path: '/v1/webhooks/{id}/test',
            title: 'Test webhook endpoint',
            summary: 'Send a test event to a webhook endpoint.',
            description: 'Sends a test webhook event to verify your endpoint is configured correctly and can receive events.',
            permissions: 'WEBHOOK_WRITE',
            requestBody: [
                {
                    name: 'event_type',
                    type: 'string',
                    required: true,
                    description: 'The type of test event to send.',
                    constraints: [
                        { type: 'enum', values: ['deposit.completed', 'withdrawal.completed', 'yield.accrued', 'rate.changed', 'account.status.changed', 'application.created', 'api_key.created'] }
                    ]
                }
            ],
            successStatus: '200 OK',
            successResponse: {
                success: true,
                message: 'Test webhook sent successfully'
            },
            errorStatus: '400 Bad Request',
            errorResponse: {
                error: {
                    code: 'INVALID_EVENT_TYPE',
                    message: 'Invalid event type specified',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl {{API_URL}}/v1/webhooks/ep_1234567890abcdef/test \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "event_type": "deposit.completed"
  }'`
        }
    },
    
    // Webhook Events - detailed payload documentation for each event type
    'webhook-events': {
        'deposit-completed': {
            method: 'EVENT',
            path: 'deposit.completed',
            title: 'deposit.completed',
            summary: 'Triggered when funds are successfully deposited to a yield account.',
            description: 'This event is fired after a deposit transaction has been confirmed and the funds are available in the yield account. The payload includes details about the deposit amount, currency, protocol used, and the application that initiated the request.',
            permissions: 'Webhook subscription required',
            requestBody: [
                {
                    name: 'application_id',
                    type: 'string',
                    required: true,
                    description: 'The ID of the application that made the API call. Null if the request was made via dashboard.'
                },
                {
                    name: 'application_name',
                    type: 'string',
                    required: true,
                    description: 'Human-readable name of the application.'
                },
                {
                    name: 'environment',
                    type: 'string',
                    required: true,
                    description: 'The environment where the deposit occurred.',
                    constraints: [{ type: 'enum', values: ['sandbox', 'production'] }]
                },
                {
                    name: 'yield_account_id',
                    type: 'string',
                    required: true,
                    description: 'The unique identifier of the yield account that received the deposit.'
                },
                {
                    name: 'amount',
                    type: 'string',
                    required: true,
                    description: 'The deposit amount as a decimal string (e.g., "1000.00").'
                },
                {
                    name: 'currency',
                    type: 'string',
                    required: true,
                    description: 'The cryptocurrency that was deposited.',
                    constraints: [{ type: 'enum', values: ['USDC', 'USDT', 'DAI', 'ETH', 'WBTC'] }]
                },
                {
                    name: 'protocol',
                    type: 'string',
                    required: true,
                    description: 'The DeFi protocol where funds were deposited.',
                    constraints: [{ type: 'enum', values: ['morpho', 'aave', 'auto'] }]
                },
                {
                    name: 'transaction_id',
                    type: 'string',
                    required: true,
                    description: 'The unique identifier of the deposit transaction.'
                },
                {
                    name: 'timestamp',
                    type: 'integer',
                    required: true,
                    description: 'Unix timestamp (milliseconds) when the event occurred.'
                }
            ],
            successResponse: {
                event_type: 'deposit.completed',
                event_id: 'evt_abc123def456',
                timestamp: 1701456789000,
                data: {
                    application_id: 'app_1234567890abcdef',
                    application_name: 'Production App',
                    environment: 'production',
                    yield_account_id: 'ya_xyz789',
                    amount: '1000.00',
                    currency: 'USDC',
                    protocol: 'aave',
                    transaction_id: 'txn_abc123',
                    timestamp: 1701456789000
                }
            },
            errorResponse: null,
            curlExample: `# Example webhook payload sent to your endpoint
# POST https://your-server.com/webhooks

{
  "event_type": "deposit.completed",
  "event_id": "evt_abc123def456",
  "timestamp": 1701456789000,
  "data": {
    "application_id": "app_1234567890abcdef",
    "application_name": "Production App",
    "environment": "production",
    "yield_account_id": "ya_xyz789",
    "amount": "1000.00",
    "currency": "USDC",
    "protocol": "aave",
    "transaction_id": "txn_abc123",
    "timestamp": 1701456789000
  }
}`
        },
        'withdrawal-completed': {
            method: 'EVENT',
            path: 'withdrawal.completed',
            title: 'withdrawal.completed',
            summary: 'Triggered when funds are successfully withdrawn from a yield account.',
            description: 'This event is fired after a withdrawal transaction has been confirmed and the funds have been sent to the destination address. Use this event to update your records and notify users of completed withdrawals.',
            permissions: 'Webhook subscription required',
            requestBody: [
                {
                    name: 'application_id',
                    type: 'string',
                    required: true,
                    description: 'The ID of the application that made the API call. Null if the request was made via dashboard.'
                },
                {
                    name: 'application_name',
                    type: 'string',
                    required: true,
                    description: 'Human-readable name of the application.'
                },
                {
                    name: 'environment',
                    type: 'string',
                    required: true,
                    description: 'The environment where the withdrawal occurred.',
                    constraints: [{ type: 'enum', values: ['sandbox', 'production'] }]
                },
                {
                    name: 'yield_account_id',
                    type: 'string',
                    required: true,
                    description: 'The unique identifier of the yield account from which funds were withdrawn.'
                },
                {
                    name: 'amount',
                    type: 'string',
                    required: true,
                    description: 'The withdrawal amount as a decimal string (e.g., "500.00").'
                },
                {
                    name: 'currency',
                    type: 'string',
                    required: true,
                    description: 'The cryptocurrency that was withdrawn.',
                    constraints: [{ type: 'enum', values: ['USDC', 'USDT', 'DAI', 'ETH', 'WBTC'] }]
                },
                {
                    name: 'transaction_id',
                    type: 'string',
                    required: true,
                    description: 'The unique identifier of the withdrawal transaction.'
                },
                {
                    name: 'timestamp',
                    type: 'integer',
                    required: true,
                    description: 'Unix timestamp (milliseconds) when the event occurred.'
                }
            ],
            successResponse: {
                event_type: 'withdrawal.completed',
                event_id: 'evt_def456ghi789',
                timestamp: 1701456789000,
                data: {
                    application_id: 'app_1234567890abcdef',
                    application_name: 'Production App',
                    environment: 'production',
                    yield_account_id: 'ya_xyz789',
                    amount: '500.00',
                    currency: 'USDC',
                    transaction_id: 'txn_def456',
                    timestamp: 1701456789000
                }
            },
            errorResponse: null,
            curlExample: `# Example webhook payload sent to your endpoint
# POST https://your-server.com/webhooks

{
  "event_type": "withdrawal.completed",
  "event_id": "evt_def456ghi789",
  "timestamp": 1701456789000,
  "data": {
    "application_id": "app_1234567890abcdef",
    "application_name": "Production App",
    "environment": "production",
    "yield_account_id": "ya_xyz789",
    "amount": "500.00",
    "currency": "USDC",
    "transaction_id": "txn_def456",
    "timestamp": 1701456789000
  }
}`
        },
        'yield-accrued': {
            method: 'EVENT',
            path: 'yield.accrued',
            title: 'yield.accrued',
            summary: 'Triggered when yield is accrued to a yield account.',
            description: 'This event is fired daily when interest earnings are calculated and added to yield accounts. Use this to track yield generation and update user balances in your application.',
            permissions: 'Webhook subscription required',
            requestBody: [
                {
                    name: 'application_id',
                    type: 'string',
                    required: false,
                    description: 'The ID of the application associated with the account. May be null for account-level events.'
                },
                {
                    name: 'application_name',
                    type: 'string',
                    required: false,
                    description: 'Human-readable name of the application.'
                },
                {
                    name: 'environment',
                    type: 'string',
                    required: true,
                    description: 'The environment where yield was accrued.',
                    constraints: [{ type: 'enum', values: ['sandbox', 'production'] }]
                },
                {
                    name: 'yield_account_id',
                    type: 'string',
                    required: true,
                    description: 'The unique identifier of the yield account.'
                },
                {
                    name: 'yield_amount',
                    type: 'string',
                    required: true,
                    description: 'The amount of yield accrued as a decimal string (e.g., "0.16").'
                },
                {
                    name: 'total_balance',
                    type: 'string',
                    required: true,
                    description: 'The new total balance after yield accrual.'
                },
                {
                    name: 'currency',
                    type: 'string',
                    required: true,
                    description: 'The cryptocurrency of the yield account.',
                    constraints: [{ type: 'enum', values: ['USDC', 'USDT', 'DAI', 'ETH', 'WBTC'] }]
                },
                {
                    name: 'apy',
                    type: 'number',
                    required: true,
                    description: 'The annual percentage yield at the time of accrual (e.g., 0.06 for 6%).'
                },
                {
                    name: 'timestamp',
                    type: 'integer',
                    required: true,
                    description: 'Unix timestamp (milliseconds) when the event occurred.'
                }
            ],
            successResponse: {
                event_type: 'yield.accrued',
                event_id: 'evt_yield123',
                timestamp: 1701456789000,
                data: {
                    application_id: null,
                    application_name: null,
                    environment: 'production',
                    yield_account_id: 'ya_xyz789',
                    yield_amount: '0.16',
                    total_balance: '1000.16',
                    currency: 'USDC',
                    apy: 0.06,
                    timestamp: 1701456789000
                }
            },
            errorResponse: null,
            curlExample: `# Example webhook payload sent to your endpoint
# POST https://your-server.com/webhooks

{
  "event_type": "yield.accrued",
  "event_id": "evt_yield123",
  "timestamp": 1701456789000,
  "data": {
    "application_id": null,
    "application_name": null,
    "environment": "production",
    "yield_account_id": "ya_xyz789",
    "yield_amount": "0.16",
    "total_balance": "1000.16",
    "currency": "USDC",
    "apy": 0.06,
    "timestamp": 1701456789000
  }
}`
        },
        'rate-changed': {
            method: 'EVENT',
            path: 'rate.changed',
            title: 'rate.changed',
            summary: 'Triggered when yield rates change significantly (>0.5%).',
            description: 'This event is fired when the yield rate for a currency/protocol combination changes by more than 0.5%. Use this to update displayed rates in your application or alert users to rate changes.',
            permissions: 'Webhook subscription required',
            requestBody: [
                {
                    name: 'application_id',
                    type: 'string',
                    required: false,
                    description: 'The ID of the application. May be null for broadcast events.'
                },
                {
                    name: 'application_name',
                    type: 'string',
                    required: false,
                    description: 'Human-readable name of the application.'
                },
                {
                    name: 'environment',
                    type: 'string',
                    required: true,
                    description: 'The environment where the rate changed.',
                    constraints: [{ type: 'enum', values: ['sandbox', 'production'] }]
                },
                {
                    name: 'currency',
                    type: 'string',
                    required: true,
                    description: 'The cryptocurrency whose rate changed.',
                    constraints: [{ type: 'enum', values: ['USDC', 'USDT', 'DAI', 'ETH', 'WBTC'] }]
                },
                {
                    name: 'protocol',
                    type: 'string',
                    required: true,
                    description: 'The DeFi protocol where the rate changed.',
                    constraints: [{ type: 'enum', values: ['morpho', 'aave'] }]
                },
                {
                    name: 'old_rate',
                    type: 'number',
                    required: true,
                    description: 'The previous yield rate (e.g., 0.055 for 5.5%).'
                },
                {
                    name: 'new_rate',
                    type: 'number',
                    required: true,
                    description: 'The new yield rate (e.g., 0.062 for 6.2%).'
                },
                {
                    name: 'change_percent',
                    type: 'number',
                    required: true,
                    description: 'The percentage change in rate (e.g., 12.7 for a 12.7% increase).'
                },
                {
                    name: 'timestamp',
                    type: 'integer',
                    required: true,
                    description: 'Unix timestamp (milliseconds) when the event occurred.'
                }
            ],
            successResponse: {
                event_type: 'rate.changed',
                event_id: 'evt_rate456',
                timestamp: 1701456789000,
                data: {
                    application_id: null,
                    application_name: null,
                    environment: 'production',
                    currency: 'USDC',
                    protocol: 'aave',
                    old_rate: 0.055,
                    new_rate: 0.062,
                    change_percent: 12.7,
                    timestamp: 1701456789000
                }
            },
            errorResponse: null,
            curlExample: `# Example webhook payload sent to your endpoint
# POST https://your-server.com/webhooks

{
  "event_type": "rate.changed",
  "event_id": "evt_rate456",
  "timestamp": 1701456789000,
  "data": {
    "application_id": null,
    "application_name": null,
    "environment": "production",
    "currency": "USDC",
    "protocol": "aave",
    "old_rate": 0.055,
    "new_rate": 0.062,
    "change_percent": 12.7,
    "timestamp": 1701456789000
  }
}`
        },
        'account-status-changed': {
            method: 'EVENT',
            path: 'account.status.changed',
            title: 'account.status.changed',
            summary: 'Triggered when a yield account status changes.',
            description: 'This event is fired when a yield account transitions between states (e.g., active, paused, closed). Use this to update your UI and notify users of account status changes.',
            permissions: 'Webhook subscription required',
            requestBody: [
                {
                    name: 'application_id',
                    type: 'string',
                    required: false,
                    description: 'The ID of the application associated with the account.'
                },
                {
                    name: 'application_name',
                    type: 'string',
                    required: false,
                    description: 'Human-readable name of the application.'
                },
                {
                    name: 'environment',
                    type: 'string',
                    required: true,
                    description: 'The environment where the status changed.',
                    constraints: [{ type: 'enum', values: ['sandbox', 'production'] }]
                },
                {
                    name: 'yield_account_id',
                    type: 'string',
                    required: true,
                    description: 'The unique identifier of the yield account.'
                },
                {
                    name: 'old_status',
                    type: 'string',
                    required: true,
                    description: 'The previous account status.',
                    constraints: [{ type: 'enum', values: ['active', 'paused', 'closed', 'pending'] }]
                },
                {
                    name: 'new_status',
                    type: 'string',
                    required: true,
                    description: 'The new account status.',
                    constraints: [{ type: 'enum', values: ['active', 'paused', 'closed', 'pending'] }]
                },
                {
                    name: 'timestamp',
                    type: 'integer',
                    required: true,
                    description: 'Unix timestamp (milliseconds) when the event occurred.'
                }
            ],
            successResponse: {
                event_type: 'account.status.changed',
                event_id: 'evt_status789',
                timestamp: 1701456789000,
                data: {
                    application_id: 'app_1234567890abcdef',
                    application_name: 'Production App',
                    environment: 'production',
                    yield_account_id: 'ya_xyz789',
                    old_status: 'active',
                    new_status: 'paused',
                    timestamp: 1701456789000
                }
            },
            errorResponse: null,
            curlExample: `# Example webhook payload sent to your endpoint
# POST https://your-server.com/webhooks

{
  "event_type": "account.status.changed",
  "event_id": "evt_status789",
  "timestamp": 1701456789000,
  "data": {
    "application_id": "app_1234567890abcdef",
    "application_name": "Production App",
    "environment": "production",
    "yield_account_id": "ya_xyz789",
    "old_status": "active",
    "new_status": "paused",
    "timestamp": 1701456789000
  }
}`
        },
        'application-created': {
            method: 'EVENT',
            path: 'application.created',
            title: 'application.created',
            summary: 'Triggered when a new application is created in your account.',
            description: 'This event is fired when a new application is created via the API or dashboard. Use this for audit logging or to trigger onboarding workflows.',
            permissions: 'Webhook subscription required',
            requestBody: [
                {
                    name: 'application_id',
                    type: 'string',
                    required: true,
                    description: 'The unique identifier of the newly created application.'
                },
                {
                    name: 'application_name',
                    type: 'string',
                    required: true,
                    description: 'The name given to the application.'
                },
                {
                    name: 'environment',
                    type: 'string',
                    required: true,
                    description: 'The environment(s) the application is configured for.',
                    constraints: [{ type: 'enum', values: ['sandbox', 'production', 'both'] }]
                },
                {
                    name: 'timestamp',
                    type: 'integer',
                    required: true,
                    description: 'Unix timestamp (milliseconds) when the event occurred.'
                }
            ],
            successResponse: {
                event_type: 'application.created',
                event_id: 'evt_app123',
                timestamp: 1701456789000,
                data: {
                    application_id: 'app_newapp123',
                    application_name: 'My New App',
                    environment: 'both',
                    timestamp: 1701456789000
                }
            },
            errorResponse: null,
            curlExample: `# Example webhook payload sent to your endpoint
# POST https://your-server.com/webhooks

{
  "event_type": "application.created",
  "event_id": "evt_app123",
  "timestamp": 1701456789000,
  "data": {
    "application_id": "app_newapp123",
    "application_name": "My New App",
    "environment": "both",
    "timestamp": 1701456789000
  }
}`
        },
        'api-key-created': {
            method: 'EVENT',
            path: 'api_key.created',
            title: 'api_key.created',
            summary: 'Triggered when a new API key is generated for an application.',
            description: 'This event is fired when a new API key (access token) is created for an application. Use this for security auditing and monitoring API key creation.',
            permissions: 'Webhook subscription required',
            requestBody: [
                {
                    name: 'application_id',
                    type: 'string',
                    required: true,
                    description: 'The ID of the application the API key was created for.'
                },
                {
                    name: 'application_name',
                    type: 'string',
                    required: true,
                    description: 'The name of the application.'
                },
                {
                    name: 'token_id',
                    type: 'string',
                    required: true,
                    description: 'The unique identifier of the new API key (not the key itself for security).'
                },
                {
                    name: 'environment',
                    type: 'string',
                    required: true,
                    description: 'The environment the API key is valid for.',
                    constraints: [{ type: 'enum', values: ['sandbox', 'production'] }]
                },
                {
                    name: 'timestamp',
                    type: 'integer',
                    required: true,
                    description: 'Unix timestamp (milliseconds) when the event occurred.'
                }
            ],
            successResponse: {
                event_type: 'api_key.created',
                event_id: 'evt_key456',
                timestamp: 1701456789000,
                data: {
                    application_id: 'app_1234567890abcdef',
                    application_name: 'Production App',
                    token_id: 'tok_newkey789',
                    environment: 'production',
                    timestamp: 1701456789000
                }
            },
            errorResponse: null,
            curlExample: `# Example webhook payload sent to your endpoint
# POST https://your-server.com/webhooks

{
  "event_type": "api_key.created",
  "event_id": "evt_key456",
  "timestamp": 1701456789000,
  "data": {
    "application_id": "app_1234567890abcdef",
    "application_name": "Production App",
    "token_id": "tok_newkey789",
    "environment": "production",
    "timestamp": 1701456789000
  }
}`
        }
    }
};

// Syntax highlighting for cURL commands (local copy for this script)
function highlightCurlLocal(text) {
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    
    // IMPORTANT: Highlight quoted strings FIRST (before adding any spans)
    // This prevents the regex from matching span class attributes
    html = html.replace(/"([^"{}]+)"/g, '%%QUOTE_START%%$1%%QUOTE_END%%');
    
    // Highlight curl command
    html = html.replace(/^(curl)\s/gm, '<span class="token-cmd">$1</span> ');
    
    // Highlight URLs (http/https)
    html = html.replace(/(https?:\/\/[^\s\\]+)/g, '<span class="token-url">$1</span>');
    
    // Highlight flags like -X, -H, -d
    html = html.replace(/(\s)(-[A-Za-z]+)(\s)/g, '$1<span class="token-flag">$2</span>$3');
    
    // Now convert the quote placeholders to actual spans
    html = html.replace(/%%QUOTE_START%%([^%]+)%%QUOTE_END%%/g, '<span class="token-string">"$1"</span>');
    
    return html;
}

// Syntax highlighting for JSON
function highlightJsonLocal(text) {
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    
    // Highlight property keys
    html = html.replace(/"([^"]+)"(\s*:)/g, '<span class="token-property">"$1"</span>$2');
    
    // Highlight string values (after colon)
    html = html.replace(/:(\s*)"([^"]*)"/g, ':$1<span class="token-string-value">"$2"</span>');
    
    // Highlight numbers
    html = html.replace(/:(\s*)(\d+\.?\d*)/g, ':$1<span class="token-number">$2</span>');
    
    // Highlight booleans
    html = html.replace(/:(\s*)(true|false)/g, ':$1<span class="token-boolean">$2</span>');
    
    // Highlight null
    html = html.replace(/:(\s*)(null)/g, ':$1<span class="token-null">$2</span>');
    
    return html;
}

// Function to update curl example with current environment URL
function updateCurlExample(curlExample) {
    const env = window.getApiEnvironment ? window.getApiEnvironment() : 'production';
    const apiUrls = window.API_URLS || {
        sandbox: 'https://api-sandbox.ground.com',
        production: 'https://api.ground.com'
    };
    
    const currentUrl = apiUrls[env] || apiUrls.production;
    
    // Replace placeholder with actual URL
    let updatedExample = curlExample.replace(/\{\{API_URL\}\}/g, currentUrl);
    
    // Replace any hardcoded URLs (order matters - more specific first)
    const urlPatterns = [
        /https:\/\/flow-platform-flow-platform-staging\.up\.railway\.app/g,
        /https:\/\/flow-platform-staging\.up\.railway\.app/g,
        /https:\/\/flow-platform-production\.up\.railway\.app/g,
        /https:\/\/api-sandbox\.ground\.com/g,
        /https:\/\/api\.ground\.com/g,
        /https:\/\/api-sandbox\.tbd\.com/g,
        /https:\/\/api\.tbd\.com/g,
        /https:\/\/api-sandbox\.flow\.com/g,
        /https:\/\/api\.flow\.com/g,
        /https:\/\/[a-z0-9-]+\.up\.railway\.app(?=\/v1)/g
    ];
    
    urlPatterns.forEach(pattern => {
        updatedExample = updatedExample.replace(pattern, currentUrl);
    });
    
    // Replace bearer token placeholder based on environment
    const apiKeyPlaceholder = env === 'sandbox' ? 'YOUR_SANDBOX_API_KEY' : 'YOUR_PRODUCTION_API_KEY';
    updatedExample = updatedExample.replace(/Bearer\s+sk_live_[a-zA-Z0-9_]+/g, `Bearer ${apiKeyPlaceholder}`);
    updatedExample = updatedExample.replace(/Bearer\s+YOUR_(SANDBOX|PRODUCTION)_API_KEY/g, `Bearer ${apiKeyPlaceholder}`);
    
    const curlExampleEl = document.getElementById('curl-example');
    if (curlExampleEl) {
        // Apply syntax highlighting
        curlExampleEl.innerHTML = highlightCurlLocal(updatedExample);
    }
}

// Function to update all CURL examples on the page (for when environment changes)
function updateAllCurlExamples() {
    const env = window.getApiEnvironment ? window.getApiEnvironment() : 'production';
    const apiUrls = window.API_URLS || {
        sandbox: 'https://api-sandbox.ground.com',
        production: 'https://api.ground.com'
    };
    
    const currentUrl = apiUrls[env] || apiUrls.production;
    console.log('🔄 Updating CURL examples to:', env, currentUrl);
    
    // Helper function to replace URLs in text
    function replaceUrls(text) {
        // List of URL patterns to replace (order matters - more specific first)
        const patterns = [
            /https:\/\/flow-platform-flow-platform-staging\.up\.railway\.app/g,
            /https:\/\/flow-platform-staging\.up\.railway\.app/g,
            /https:\/\/flow-platform-production\.up\.railway\.app/g,
            /https:\/\/api-sandbox\.tbd\.com/g,
            /https:\/\/api\.tbd\.com/g,
            /https:\/\/api-sandbox\.flow\.com/g,
            /https:\/\/api\.flow\.com/g,
            // Match any Railway URL ending before /v1
            /https:\/\/[a-z0-9-]+\.up\.railway\.app(?=\/v1)/g
        ];
        
        let result = text;
        patterns.forEach(pattern => {
            result = result.replace(pattern, currentUrl);
        });
        return result;
    }
    
    // Update the #curl-example element
    const curlExampleEl = document.getElementById('curl-example');
    if (curlExampleEl) {
        const original = curlExampleEl.textContent;
        const updated = replaceUrls(original);
        if (updated !== original) {
            // Apply syntax highlighting when updating
            curlExampleEl.innerHTML = highlightCurlLocal(updated);
            console.log('✅ Updated curl-example URL');
        }
    }
    
    // Find all code blocks that contain curl commands
    document.querySelectorAll('code, pre').forEach(el => {
        if (el.id === 'curl-example') return; // Already handled
        
        let text = el.textContent;
        if (text.includes('curl ') && text.includes('/v1/')) {
            const updated = replaceUrls(text);
            if (updated !== text) {
                el.textContent = updated;
            }
        }
    });
}

// Function to set up environment toggle
function setupEnvironmentToggle() {
    const envButtons = document.querySelectorAll('.env-btn');
    if (envButtons.length === 0) return;
    
    const currentEnv = window.getApiEnvironment ? window.getApiEnvironment() : 'production';
    
    // Map internal env names to user-facing labels
    const envLabels = {
        'production': 'Production',
        'sandbox': 'Sandbox'
    };
    
    // Update button text to show user-facing labels
    envButtons.forEach(btn => {
        const env = btn.dataset.env;
        if (envLabels[env]) {
            btn.textContent = envLabels[env];
        }
    });
    
    // Set initial active state
    envButtons.forEach(btn => {
        if (btn.dataset.env === currentEnv) {
            btn.classList.add('active');
            btn.style.background = '#0f172a';
            btn.style.color = 'white';
            btn.style.fontWeight = '600';
        } else {
            btn.classList.remove('active');
            btn.style.background = 'transparent';
            btn.style.color = '#64748b';
            btn.style.fontWeight = '500';
        }
    });
    
    // Add click handlers
    envButtons.forEach(btn => {
        // Remove any existing listeners by cloning
        const newBtn = btn.cloneNode(true);
        btn.parentNode.replaceChild(newBtn, btn);
        
        newBtn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            const selectedEnv = this.dataset.env;
            
            // Update active state
            document.querySelectorAll('.env-btn').forEach(b => {
                b.classList.remove('active');
                b.style.background = 'transparent';
                b.style.color = '#64748b';
                b.style.fontWeight = '500';
            });
            
            this.classList.add('active');
            this.style.background = '#0f172a';
            this.style.color = 'white';
            this.style.fontWeight = '600';
            
            // Update global environment
            if (window.setApiEnvironment) {
                window.setApiEnvironment(selectedEnv);
            }
            
            // Update curl example
            const endpoint = getCurrentEndpoint();
            if (endpoint && endpoint.curlExample) {
                updateCurlExample(endpoint.curlExample);
            }
            
            // Also update all CURL examples on the page
            updateAllCurlExamples();
        });
    });
    
    // Listen for environment changes from other sources (like env-toggle.js)
    window.addEventListener('apiEnvironmentChanged', function(event) {
        const env = event.detail;
        
        // Update button states
        document.querySelectorAll('.env-btn').forEach(btn => {
            if (btn.dataset.env === env) {
                btn.classList.add('active');
                btn.style.background = '#0f172a';
                btn.style.color = 'white';
                btn.style.fontWeight = '600';
            } else {
                btn.classList.remove('active');
                btn.style.background = 'transparent';
                btn.style.color = '#64748b';
                btn.style.fontWeight = '500';
            }
        });
        
        // Update curl example
        const endpoint = getCurrentEndpoint();
        if (endpoint && endpoint.curlExample) {
            updateCurlExample(endpoint.curlExample);
        }
        
        // Also update all CURL examples on the page
        updateAllCurlExamples();
    });
    
    // Listen for environment changes from other parts of the app
    window.addEventListener('apiEnvironmentChanged', function(event) {
        const newEnv = event.detail;
        envButtons.forEach(btn => {
            // Update button text to show user-facing label
            const env = btn.dataset.env;
            if (envLabels[env]) {
                btn.textContent = envLabels[env];
            }
            
            if (btn.dataset.env === newEnv) {
                btn.classList.add('active');
                btn.style.background = '#0f172a';
                btn.style.color = 'white';
                btn.style.fontWeight = '600';
            } else {
                btn.classList.remove('active');
                btn.style.background = 'transparent';
                btn.style.color = '#64748b';
                btn.style.fontWeight = '500';
            }
        });
        
        const endpoint = getCurrentEndpoint();
        if (endpoint && endpoint.curlExample) {
            updateCurlExample(endpoint.curlExample);
        }
        
        // Also update all CURL examples on the page
        updateAllCurlExamples();
    });
}

// Helper function to get current endpoint data
function getCurrentEndpoint() {
    const urlParams = new URLSearchParams(window.location.search);
    const resource = urlParams.get('resource');
    const action = urlParams.get('action');
    
    if (resource && action && apiData[resource] && apiData[resource][action]) {
        return apiData[resource][action];
    }
    return null;
}

// Initialize page based on URL parameters
function initializePage() {
    const urlParams = new URLSearchParams(window.location.search);
    const resource = urlParams.get('resource');
    const action = urlParams.get('action');

    if (resource && action && apiData[resource] && apiData[resource][action]) {
        const endpoint = apiData[resource][action];
        populatePage(endpoint);
    } else {
        // Default to create yield account
        const endpoint = apiData['yield-accounts']['create'];
        populatePage(endpoint);
    }
}

function populatePage(endpoint) {
    const isWebhookEvent = endpoint.method === 'EVENT';
    
    // Update title and description
    document.getElementById('api-title').textContent = endpoint.title;
    document.getElementById('api-description').textContent = endpoint.description;
    document.getElementById('summary-text').textContent = endpoint.summary;
    
    // Update method badge
    const methodBadge = document.getElementById('method-badge');
    methodBadge.textContent = endpoint.method;
    methodBadge.className = `method-badge ${endpoint.method.toLowerCase()}`;
    // Add purple background for EVENT type
    if (isWebhookEvent) {
        methodBadge.style.background = '#8b5cf6';
        methodBadge.style.color = 'white';
    }
    
    // Update endpoint info
    const endpointMethodBadge = document.getElementById('endpoint-method-badge');
    endpointMethodBadge.textContent = endpoint.method;
    endpointMethodBadge.className = `endpoint-method-badge ${endpoint.method.toLowerCase()}`;
    if (isWebhookEvent) {
        endpointMethodBadge.style.background = '#8b5cf6';
        endpointMethodBadge.style.color = 'white';
    }
    document.getElementById('endpoint-path-inline').textContent = endpoint.path;
    
    // Update rate limit info - hide for webhook events
    const rateLimit = getRateLimit(endpoint.method, endpoint.path);
    const rateLimitEl = document.getElementById('rate-limit-inline');
    const rateLimitContainer = rateLimitEl?.closest('.endpoint-meta-item');
    if (rateLimitEl) {
        if (isWebhookEvent) {
            if (rateLimitContainer) rateLimitContainer.style.display = 'none';
        } else {
            if (rateLimitContainer) rateLimitContainer.style.display = '';
            rateLimitEl.textContent = `${rateLimit.limit} req/min`;
        }
    }
    document.getElementById('permissions-inline').textContent = endpoint.permissions;
    
    // Update Request Body label for webhook events
    const requestBodyLabel = document.querySelector('.detail-left h3');
    if (requestBodyLabel && requestBodyLabel.textContent.includes('Request')) {
        requestBodyLabel.textContent = isWebhookEvent ? 'Response Body' : 'Request body';
    }
    
    // Populate request body / event payload
    const requestBody = document.getElementById('request-body');
    requestBody.innerHTML = '';
    
    if (endpoint.requestBody && endpoint.requestBody.length > 0) {
        endpoint.requestBody.forEach(param => {
            const paramItem = createParamElement(param, isWebhookEvent);
            requestBody.appendChild(paramItem);
        });
    } else {
        const emptyMessage = isWebhookEvent 
            ? '<p style="color: var(--text-secondary);">No additional payload fields.</p>'
            : '<p style="color: var(--text-secondary);">No request body required for this endpoint.</p>';
        requestBody.innerHTML = emptyMessage;
    }
    
    // Update example section header for webhook events
    const exampleHeader = document.querySelector('.example-section .code-block-header .code-block-label');
    if (exampleHeader) {
        exampleHeader.textContent = isWebhookEvent ? 'Example Response' : 'Example request';
    }
    
    // Hide entire response section for webhook events
    const responseSectionRight = document.querySelector('.response-section-right');
    if (responseSectionRight) {
        responseSectionRight.style.display = isWebhookEvent ? 'none' : '';
    }
    
    // Populate responses with syntax highlighting (only for non-webhook events)
    if (!isWebhookEvent) {
        const successJson = JSON.stringify(endpoint.successResponse, null, 2);
        const errorJson = endpoint.errorResponse ? JSON.stringify(endpoint.errorResponse, null, 2) : null;
        document.getElementById('success-response').innerHTML = highlightJsonLocal(successJson);
        
        // Set success label with HTTP status code
        const successLabel = document.getElementById('success-label');
        if (successLabel) {
            successLabel.textContent = endpoint.successStatus || '200 OK';
            // Remove error class from parent .code-block-label
            const successLabelParent = successLabel.closest('.code-block-label');
            if (successLabelParent) successLabelParent.classList.remove('error');
        }
        
        // Set error label with HTTP status code
        const errorLabel = document.getElementById('error-label');
        if (errorLabel && endpoint.errorResponse) {
            errorLabel.textContent = endpoint.errorStatus || '400 Bad Request';
            // Add error class to parent .code-block-label for red traffic lights
            const errorLabelParent = errorLabel.closest('.code-block-label');
            if (errorLabelParent) errorLabelParent.classList.add('error');
        }
        
        const successTab = document.querySelector('[data-tab="success-tab"]');
        const errorTab = document.querySelector('[data-tab="error-tab"]');
        if (successTab) successTab.textContent = 'Success';
        if (errorTab) errorTab.style.display = '';
        if (errorJson) {
            document.getElementById('error-response').innerHTML = highlightJsonLocal(errorJson);
        }
    }
    
    // Hide environment toggle for webhook events
    const envToggle = document.querySelector('.env-toggle');
    if (envToggle) {
        envToggle.style.display = isWebhookEvent ? 'none' : '';
    }
    
    // Populate CURL example / webhook payload
    const curlExampleEl = document.getElementById('curl-example');
    if (curlExampleEl) {
        if (isWebhookEvent) {
            // For webhook events, show JSON payload directly
            curlExampleEl.innerHTML = highlightJsonLocal(endpoint.curlExample);
        } else {
            // Update curl example with current environment URL
            updateCurlExample(endpoint.curlExample);
        }
    }
    
    // Set up environment toggle (only for non-webhook events)
    if (!isWebhookEvent) {
        setupEnvironmentToggle();
    }
}

function createParamElement(param, isWebhookEvent = false) {
    const paramItem = document.createElement('div');
    paramItem.className = 'param-item';
    
    // Build constraints HTML
    let constraintsHtml = '';
    if (param.constraints && param.constraints.length > 0) {
        constraintsHtml = '<div class="param-constraints">';
        param.constraints.forEach(constraint => {
            if (constraint.type === 'enum') {
                constraintsHtml += `<div class="param-constraint"><strong>Enum:</strong> ${constraint.values.join(', ')}</div>`;
            } else if (constraint.type === 'min_length') {
                constraintsHtml += `<div class="param-constraint"><strong>Min Length:</strong> ${constraint.value}</div>`;
            } else if (constraint.type === 'max_length') {
                constraintsHtml += `<div class="param-constraint"><strong>Max Length:</strong> ${constraint.value}</div>`;
            } else if (constraint.type === 'minimum') {
                constraintsHtml += `<div class="param-constraint"><strong>Minimum:</strong> ${constraint.value}</div>`;
            } else if (constraint.type === 'maximum') {
                constraintsHtml += `<div class="param-constraint"><strong>Maximum:</strong> ${constraint.value}</div>`;
            } else if (constraint.type === 'pattern') {
                constraintsHtml += `<div class="param-constraint"><strong>Pattern:</strong> ${constraint.value}</div>`;
            } else if (constraint.type === 'format') {
                constraintsHtml += `<div class="param-constraint"><strong>Format:</strong> ${constraint.value}</div>`;
            } else if (constraint.type === 'default') {
                constraintsHtml += `<div class="param-constraint"><strong>Default:</strong> ${constraint.value}</div>`;
            }
        });
        constraintsHtml += '</div>';
    }
    
    // Build nested object HTML
    let nestedHtml = '';
    if (param.nested && param.nested.length > 0) {
        const nestedId = `nested-${param.name}-${Math.random().toString(36).substr(2, 9)}`;
        nestedHtml = `
            <div class="param-nested">
                <div class="param-nested-header" onclick="toggleNested('${nestedId}')">
                    <span class="param-nested-toggle" id="toggle-${nestedId}">+</span>
                    <span class="param-nested-title">Show attributes</span>
                </div>
                <div class="param-nested-content" id="${nestedId}">
                    ${param.nested.map(nestedParam => {
                        let nestedConstraints = '';
                        if (nestedParam.constraints) {
                            nestedConstraints = nestedParam.constraints.map(c => {
                                if (c.type === 'enum') return `<strong>Enum:</strong> ${c.values.join(', ')}`;
                                if (c.type === 'min_length') return `<strong>Min Length:</strong> ${c.value}`;
                                if (c.type === 'max_length') return `<strong>Max Length:</strong> ${c.value}`;
                                if (c.type === 'pattern') return `<strong>Pattern:</strong> ${c.value}`;
                                return '';
                            }).filter(c => c).join(', ');
                        }
                        // Hide Required/Optional tags for webhook events
                        const requiredTag = isWebhookEvent ? '' : 
                            (nestedParam.required ? '<span class="param-required">Required</span>' : '<span class="param-optional">Optional</span>');
                        return `
                            <div class="param-item" style="margin-top: 12px;">
                                <div class="param-header">
                                    <span class="param-name">${nestedParam.name}</span>
                                    <span class="param-type">${nestedParam.type}</span>
                                    ${requiredTag}
                                </div>
                                <div class="param-description">${nestedParam.description}</div>
                                ${nestedConstraints ? `<div class="param-constraints"><div class="param-constraint">${nestedConstraints}</div></div>` : ''}
                            </div>
                        `;
                    }).join('')}
                </div>
            </div>
        `;
    }
    
    // Hide Required/Optional tags for webhook events
    const requiredTag = isWebhookEvent ? '' : 
        (param.required ? '<span class="param-required">Required</span>' : '<span class="param-optional">Optional</span>');
    
    paramItem.innerHTML = `
        <div class="param-header">
            <span class="param-name">${param.name}</span>
            <span class="param-type">${param.type}</span>
            ${requiredTag}
        </div>
        <div class="param-description">${param.description}</div>
        ${constraintsHtml}
        ${nestedHtml}
    `;
    
    return paramItem;
}

// Toggle nested object visibility
window.toggleNested = function(nestedId) {
    const content = document.getElementById(nestedId);
    const toggle = document.getElementById(`toggle-${nestedId}`);
    
    if (content.classList.contains('expanded')) {
        content.classList.remove('expanded');
        toggle.classList.remove('expanded');
        toggle.textContent = '+';
    } else {
        content.classList.add('expanded');
        toggle.classList.add('expanded');
        toggle.textContent = '−';
    }
};

// Tab switching
document.addEventListener('DOMContentLoaded', () => {
    initializePage();
    
    // Ensure URLs are updated after page init based on current environment
    setTimeout(() => {
        updateAllCurlExamples();
    }, 100);
    
    // Response tabs
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.dataset.tab;
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById(`${tab}-tab`).classList.add('active');
        });
    });
});