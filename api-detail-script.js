// API Detail Page Script

// Comprehensive API endpoint data with detailed request body definitions
const apiData = {
    'yield-accounts': {
        create: {
            method: 'POST',
            path: '/v1/yield/accounts',
            title: 'Create yield account',
            summary: 'Create a new yield account to start earning interest on cryptocurrency deposits. Flow automatically routes funds to the best available protocol (Morpho or Aave) to ensure 6% yield.',
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
                    description: 'Preferred DeFi protocol for this account. Use "auto" to let Flow automatically select the best protocol based on current rates and availability. Specify "morpho" or "aave" to force a specific protocol.',
                    constraints: [
                        { type: 'enum', values: ['auto', 'morpho', 'aave'] },
                        { type: 'default', value: 'auto' }
                    ]
                }
            ],
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
            errorResponse: {
                error: {
                    code: 'INSUFFICIENT_FUNDS',
                    message: 'Insufficient funds to create yield account',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/yield/accounts \\
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
                    description: 'Optional source wallet address for the deposit. If not provided, Flow will use the default deposit address for your account.',
                    constraints: [
                        { type: 'pattern', value: '^0x[a-fA-F0-9]{40}$' }
                    ]
                }
            ],
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
            errorResponse: {
                error: {
                    code: 'INVALID_CURRENCY',
                    message: 'Currency does not match account currency',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/yield/accounts/ya_1234567890abcdef/deposit \\
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
            errorResponse: {
                error: {
                    code: 'INSUFFICIENT_BALANCE',
                    message: 'Insufficient balance for withdrawal',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/yield/accounts/ya_1234567890abcdef/withdraw \\
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
                    description: 'Your Flow account username. This is the username you used when creating your account.',
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
                    description: 'Your Flow account password. Must be at least 8 characters long.',
                    constraints: [
                        { type: 'min_length', value: 8 }
                    ]
                }
            ],
            successResponse: {
                access_token: 'sk_live_temp_1234567890abcdef',
                token_type: 'Bearer',
                expires_in: 3600,
                account_id: 'acc_1234567890'
            },
            errorResponse: {
                error: {
                    code: 'INVALID_CREDENTIALS',
                    message: 'Invalid username or password',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/auth/authenticate \\
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
            successResponse: {
                account_id: 'acc_1234567890abcdef',
                username: 'developer123',
                created_at: '2025-01-15T10:30:00Z',
                status: 'active'
            },
            errorResponse: {
                error: {
                    code: 'USERNAME_TAKEN',
                    message: 'Username is already taken',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/accounts \\
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
            successResponse: {
                token_id: 'tok_1234567890abcdef',
                access_token: 'sk_live_1234567890abcdef',
                name: 'Production API Key',
                created_at: '2025-01-15T10:30:00Z',
                expires_at: null
            },
            errorResponse: {
                error: {
                    code: 'UNAUTHORIZED',
                    message: 'Authentication required',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/access-tokens \\
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
                    description: 'An array of allowed CORS origins. These origins will be allowed to make cross-origin requests to the Flow API when using this application\'s API keys.',
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
            errorResponse: {
                error: {
                    code: 'INVALID_ENVIRONMENT',
                    message: 'Invalid environment specified',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/applications \\
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
            errorResponse: {
                error: {
                    code: 'UNAUTHORIZED',
                    message: 'Authentication required',
                    type: 'authentication_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/applications \\
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
            successResponse: {
                wallet_id: 'wal_1234567890abcdef',
                address: '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb',
                environment: 'sandbox',
                chain: 'ethereum',
                label: 'Secondary Wallet',
                status: 'active',
                created_at: '2025-01-15T10:30:00Z'
            },
            errorResponse: {
                error: {
                    code: 'APPLICATION_NOT_FOUND',
                    message: 'Application not found',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/applications/app_1234567890abcdef/wallets \\
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
            errorResponse: {
                error: {
                    code: 'APPLICATION_NOT_FOUND',
                    message: 'Application not found',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/applications/app_1234567890abcdef/wallets \\
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
            errorResponse: {
                error: {
                    code: 'APPLICATION_NOT_FOUND',
                    message: 'Application not found',
                    type: 'invalid_request_error'
                }
            },
            curlExample: `curl https://api.flow.com/v1/applications/app_1234567890abcdef/tokens \\
  -X POST \\
  -H "Authorization: Bearer sk_live_1234567890abcdef" \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "Production Server Key",
    "expires_in": 31536000
  }'`
        }
    }
};

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
    // Update title and description
    document.getElementById('api-title').textContent = endpoint.title;
    document.getElementById('api-description').textContent = endpoint.description;
    document.getElementById('summary-text').textContent = endpoint.summary;
    
    // Update method badge
    const methodBadge = document.getElementById('method-badge');
    methodBadge.textContent = endpoint.method;
    methodBadge.className = `method-badge ${endpoint.method.toLowerCase()}`;
    
    // Update endpoint info
    document.getElementById('endpoint-method-badge').textContent = endpoint.method;
    document.getElementById('endpoint-path-inline').textContent = endpoint.path;
    document.getElementById('permissions-inline').textContent = endpoint.permissions;
    
    // Populate request body
    const requestBody = document.getElementById('request-body');
    requestBody.innerHTML = '';
    
    if (endpoint.requestBody && endpoint.requestBody.length > 0) {
        endpoint.requestBody.forEach(param => {
            const paramItem = createParamElement(param);
            requestBody.appendChild(paramItem);
        });
    } else {
        requestBody.innerHTML = '<p style="color: var(--text-secondary);">No request body required for this endpoint.</p>';
    }
    
    // Populate responses
    document.getElementById('success-response').textContent = JSON.stringify(endpoint.successResponse, null, 2);
    document.getElementById('error-response').textContent = JSON.stringify(endpoint.errorResponse, null, 2);
    
    // Populate CURL example
    document.getElementById('curl-example').textContent = endpoint.curlExample;
}

function createParamElement(param) {
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
                        return `
                            <div class="param-item" style="margin-top: 12px;">
                                <div class="param-header">
                                    <span class="param-name">${nestedParam.name}</span>
                                    <span class="param-type">${nestedParam.type}</span>
                                    ${nestedParam.required ? '<span class="param-required">Required</span>' : '<span class="param-optional">Optional</span>'}
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
    
    paramItem.innerHTML = `
        <div class="param-header">
            <span class="param-name">${param.name}</span>
            <span class="param-type">${param.type}</span>
            ${param.required ? '<span class="param-required">Required</span>' : '<span class="param-optional">Optional</span>'}
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