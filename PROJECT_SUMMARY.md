# TBD

## The Pitch

We're building the Square of DeFi.

Remember when Square made it possible for anyone to accept credit cards with a simple API and a headphone jack? We're doing the same thing for cryptocurrency yield.

Right now, if a developer wants to let their users earn interest on crypto, they need to:
- Integrate with multiple DeFi protocols (Morpho, Aave, Compound...)
- Manage smart contract interactions
- Handle wallet security and key management
- Navigate compliance in every jurisdiction
- Monitor positions across fragmented systems

It's a mess. So developers don't bother.

**TBD fixes this with one REST endpoint:**

```bash
curl https://api.tbd.com/v1/yield/accounts \
  -d '{"currency": "USDC", "amount": "10000"}'
```

That's it. Behind the scenes, we route to the best protocol, handle the smart contracts, secure the keys, and manage compliance. The developer gets yield. Their users get yield. Everyone wins.

---

## What's Live Today

We have a working API gateway hitting real DeFi protocols:

- **`/v1/yield/rates`** — Real-time APY from Morpho and Aave
- **`/v1/markets`** — Every available yield opportunity, one call
- **Applications & wallets** — Each app gets its own segregated wallet, encrypted keys
- **Sandbox + Production** — Test on Sepolia, deploy to mainnet

The developer portal is live. Interactive docs. SDK playgrounds. Environment toggles. It feels like Stripe docs because that's what developers expect now.

**Stack:** Kotlin/Ktor, PostgreSQL, web3j, Railway, Cloudflare Pages.

---

## What's Next

The read-side is done. Now we're wiring up the write-side:

1. **Deposits & withdrawals** — The code exists, needs end-to-end testing with real transactions
2. **Position tracking** — Show developers their users' balances and accrued yield
3. **More protocols** — We started with Morpho and Aave because they're the best. We'll add more as they prove themselves.

After that: Python and Node SDKs, webhook delivery, and yield optimization (automatically route to highest APY).

---

## Why This Matters

DeFi has $50B+ locked in lending protocols. But it's inaccessible to 99% of developers because the integration is too hard.

We're the abstraction layer that unlocks it.

Every fintech app, neobank, and crypto wallet becomes a potential customer. They want to offer yield. They don't want to become DeFi experts.

That's us.

---

**Live now:** [tbd.kcwn89.workers.dev](https://tbd.kcwn89.workers.dev)

*One API. Multiple protocols. Yield for everyone.*
