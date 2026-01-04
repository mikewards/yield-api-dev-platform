# Railway Custom Domain - Quick Start

## TL;DR

1. Railway → Service → Settings → Networking → Add Custom Domain
2. Add DNS CNAME record pointing to Railway
3. Wait 5-30 minutes for DNS + SSL
4. Done! ✅

## Step-by-Step

### 1. Add Domain in Railway (Production)

```
Railway Dashboard
  → flow-platform-production
    → Settings
      → Networking
        → Custom Domain
          → Enter: api.ground.com
          → Copy DNS value shown
```

### 2. Add Domain in Railway (Sandbox)

```
Railway Dashboard
  → flow-platform-staging
    → Settings
      → Networking
        → Custom Domain
          → Enter: api-sandbox.ground.com
          → Copy DNS value shown
```

### 3. Add DNS Records

In your DNS provider (Cloudflare, Namecheap, etc.):

**For Production:**
```
Type: CNAME
Name: api
Value: <railway-provided-value>
Proxy: OFF (important!)
```

**For Sandbox:**
```
Type: CNAME
Name: api-sandbox
Value: <railway-provided-value>
Proxy: OFF (important!)
```

### 4. Wait & Verify

```bash
# Check DNS propagation
dig api.ground.com
dig api-sandbox.ground.com

# Test after 5-30 minutes
curl https://api.ground.com/health
curl https://api-sandbox.ground.com/health
```

### 5. Verify in Railway

- Railway dashboard should show domain as "Active"
- SSL certificate should be "Issued"
- No errors in service logs

## Common Issues

**Domain not resolving?**
- Wait longer (DNS can take up to 48 hours)
- Check DNS records are correct
- Verify proxy is OFF in Cloudflare

**SSL not working?**
- Wait for DNS to fully propagate first
- Check Railway shows domain as verified
- SSL auto-provisions after DNS is ready

**502 errors?**
- Check Railway service is running
- Verify DNS points to correct service
- Check Railway logs for errors

## That's It!

Your `config.js` is already set up with these domains. No code changes needed! 🎉

