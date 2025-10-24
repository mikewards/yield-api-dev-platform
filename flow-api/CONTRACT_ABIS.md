# Contract ABIs Implementation

## ✅ What's Been Completed

### 1. **ABI Files Downloaded & Created**
- **MorphoBlue.json**: Complete ABI with `supply()`, `withdraw()`, `position()`, and `market()` functions
- **AavePool.json**: Complete ABI with `supply()`, `withdraw()`, `getUserAccountData()`, and `getReserveData()` functions

**Location**: `src/main/resources/abis/`

### 2. **Contract Wrappers Created**
- **MorphoContractWrapper.kt**: Web3j wrapper for Morpho Blue contract
- **AaveContractWrapper.kt**: Web3j wrapper for Aave Pool contract

These wrappers:
- Load ABIs from resources
- Encode function calls using Web3j
- Decode return values
- Handle transaction execution
- Support both read and write operations

### 3. **Integration Updated**
- **MorphoClient**: Now uses `MorphoContractWrapper` for real contract calls
- **AaveClient**: Now uses `AaveContractWrapper` for real contract calls
- Both support `supply()`, `withdraw()`, and position queries

## 📋 ABI Functions Available

### Morpho Blue
```kotlin
// Supply assets
supply(marketParams, assets, shares, onBehalf, data) -> (assetsUsed, sharesSupplied)

// Withdraw assets  
withdraw(marketParams, assets, shares, onBehalf, receiver, data) -> (assetsWithdrawn, sharesWithdrawn)

// Get position
position(marketParams, user) -> Position { supplyShares, borrowShares, collateral }

// Get market data
market(marketParams) -> Market { totalSupplyAssets, totalSupplyShares, ... }
```

### Aave Pool
```kotlin
// Supply assets
supply(asset, amount, onBehalfOf, referralCode) -> void

// Withdraw assets
withdraw(asset, amount, to) -> amount

// Get user account data
getUserAccountData(user) -> UserAccountData { totalCollateral, totalDebt, healthFactor, ... }

// Get reserve data
getReserveData(asset) -> ReserveData { liquidityRate, variableBorrowRate, ... }
```

## 🔧 Usage Example

### Morpho Supply
```kotlin
val morphoClient = MorphoClient()
val marketParams = MarketParams(
    loanToken = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", // USDC
    collateralToken = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
    oracle = "0x...",
    irm = "0x...",
    lltv = BigInteger("950000000000000000") // 95%
)

val receipt = morphoClient.supply(
    walletId = walletId,
    environment = "sandbox",
    marketParams = marketParams,
    amount = BigInteger("1000000000"), // 1000 USDC (6 decimals)
    onBehalf = walletAddress
)
```

### Aave Supply
```kotlin
val aaveClient = AaveClient()
val receipt = aaveClient.supply(
    walletId = walletId,
    environment = "sandbox",
    asset = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", // USDC
    amount = BigInteger("1000000000"),
    onBehalf = walletAddress
)
```

## ⚠️ Important Notes

1. **Market Parameters**: Morpho requires full `MarketParams` struct. You'll need:
   - Loan token address
   - Collateral token address  
   - Oracle address
   - IRM (Interest Rate Model) address
   - LLTV (Loan-to-Value Threshold)

2. **Token Approvals**: Before calling `supply()`, you must approve the protocol contract to spend tokens:
   ```kotlin
   // ERC20 approve needed first
   tokenContract.approve(protocolAddress, amount).send()
   ```

3. **Gas Management**: Transactions require ETH for gas. Ensure wallets have sufficient ETH.

4. **Testnet vs Mainnet**: 
   - Sandbox uses Sepolia testnet
   - Production uses Ethereum mainnet
   - Contract addresses differ per network

## 🚀 Next Steps

1. **Token Approval Service**: Create helper to approve tokens before supply
2. **Market Parameter Lookup**: Service to find market params for common pairs (USDC/USDC, etc.)
3. **Transaction Monitoring**: Background job to track transaction status
4. **Error Handling**: Better error messages for failed transactions
5. **Gas Estimation**: Estimate gas before sending transactions

## 📁 Files Created

```
flow-api/src/main/
├── resources/abis/
│   ├── MorphoBlue.json          # Morpho Blue ABI
│   └── AavePool.json            # Aave Pool ABI
└── kotlin/com/flow/integration/
    ├── morpho/
    │   ├── MorphoClient.kt       # Updated with contract calls
    │   └── MorphoContractWrapper.kt  # Web3j wrapper
    └── aave/
        ├── AaveClient.kt        # Updated with contract calls
        └── AaveContractWrapper.kt   # Web3j wrapper
```

## ✅ Status

- ✅ ABIs downloaded and stored
- ✅ Contract wrappers created
- ✅ Integration with clients complete
- ✅ Build successful
- ⏳ Token approval service (next)
- ⏳ Market parameter service (next)

