# Custom Domain Setup Guide

This guide will help you set up custom domains for your Ground API:
- **Production**: `api.ground.com`
- **Sandbox**: `api-sandbox.ground.com`

## Prerequisites

1. You own the domain `tbd.com` (or your chosen domain)
2. Access to your domain's DNS management (e.g., Cloudflare, Namecheap, GoDaddy)
3. Railway account with deployed services

## Step 1: Configure Custom Domains in Railway

### For Production Service

1. Go to Railway dashboard: https://railway.app
2. Open your project
3. Click on **`flow-platform-production`** service
4. Go to **Settings** tab
5. Scroll to **Networking** section
6. Click **"Custom Domain"** or **"Add Domain"**
7. Enter: `api.ground.com`
8. Railway will show you DNS records to add (see Step 2)

### For Sandbox Service

1. In the same Railway project
2. Click on **`flow-platform-staging`** service
3. Go to **Settings** tab
4. Scroll to **Networking** section
5. Click **"Custom Domain"** or **"Add Domain"**
6. Enter: `api-sandbox.ground.com`
7. Railway will show you DNS records to add

## Step 2: Configure DNS Records

Railway will provide you with DNS records. Typically, you'll need:

### Option A: CNAME Records (Recommended)

Add these CNAME records in your DNS provider:

```
Type: CNAME
Name: api
Value: <railway-provided-value>.up.railway.app
TTL: 3600 (or Auto)

Type: CNAME
Name: api-sandbox
Value: <railway-provided-value>.up.railway.app
TTL: 3600 (or Auto)
```

### Option B: A Records (If CNAME not supported)

Railway will provide A record IP addresses. Add:

```
Type: A
Name: api
Value: <railway-ip-address>
TTL: 3600

Type: A
Name: api-sandbox
Value: <railway-ip-address>
TTL: 3600
```

## Step 3: DNS Provider Instructions

### Cloudflare

1. Log in to Cloudflare dashboard
2. Select your domain (`tbd.com`)
3. Go to **DNS** → **Records**
4. Click **"Add record"**
5. For each domain:
   - **Type**: CNAME (or A if Railway requires)
   - **Name**: `api` (or `api-sandbox`)
   - **Target**: Railway-provided value
   - **Proxy status**: ⚠️ **Disable proxy** (gray cloud) for API endpoints
   - **TTL**: Auto
6. Click **Save**

**Important**: Make sure the proxy is **OFF** (gray cloud icon) for API endpoints. Railway needs direct connections.

### Namecheap

1. Log in to Namecheap
2. Go to **Domain List** → Select `tbd.com`
3. Click **"Manage"**
4. Go to **Advanced DNS** tab
5. Click **"Add New Record"**
6. For each domain:
   - **Type**: CNAME Record
   - **Host**: `api` (or `api-sandbox`)
   - **Value**: Railway-provided value
   - **TTL**: Automatic
7. Click **Save**

### GoDaddy

1. Log in to GoDaddy
2. Go to **My Products** → **DNS**
3. Click **"Add"** in the Records section
4. For each domain:
   - **Type**: CNAME
   - **Name**: `api` (or `api-sandbox`)
   - **Value**: Railway-provided value
   - **TTL**: 1 Hour
5. Click **Save**

### Other DNS Providers

The process is similar:
1. Find DNS management section
2. Add CNAME record
3. Name: `api` or `api-sandbox`
4. Value: Railway-provided target
5. Save

## Step 4: Wait for DNS Propagation

DNS changes can take anywhere from a few minutes to 48 hours to propagate:

1. **Check propagation status**: Use https://dnschecker.org
   - Search for: `api.ground.com`
   - Search for: `api-sandbox.ground.com`
   - Wait until all locations show the Railway IP/domain

2. **Verify in Railway**: 
   - Railway dashboard will show domain status
   - Wait for "Active" or "Verified" status
   - This usually takes 5-30 minutes after DNS propagates

## Step 5: Verify SSL Certificates

Railway automatically provisions SSL certificates via Let's Encrypt:

1. Railway will automatically request SSL certificates
2. This happens after DNS is verified
3. Usually takes 5-10 minutes
4. Check Railway dashboard for SSL status

## Step 6: Test Your Custom Domains

### Test Production Domain

```bash
# Health check
curl https://api.ground.com/health

# Should return:
# {"status":"healthy","timestamp":"...","version":"1.0.0"}
```

### Test Sandbox Domain

```bash
# Health check
curl https://api-sandbox.ground.com/health

# Should return:
# {"status":"healthy","timestamp":"...","version":"1.0.0"}
```

### Test with Frontend

1. The frontend `config.js` is already configured with these domains
2. No changes needed - it will automatically use:
   - `https://api.ground.com` for production
   - `https://api-sandbox.ground.com` for sandbox

## Step 7: Update Any Remaining References

If you have any hardcoded Railway URLs elsewhere:

1. **Backend environment variables**: No changes needed (Railway handles routing)
2. **Documentation**: Already updated to use placeholders
3. **Test scripts**: Can use environment variables (see `test-environments.sh`)

## Troubleshooting

### Domain Not Resolving

1. **Check DNS propagation**: Use https://dnschecker.org
2. **Verify DNS records**: Make sure CNAME/A records are correct
3. **Check TTL**: Lower TTL (300 seconds) for faster updates during setup
4. **Wait longer**: DNS can take up to 48 hours

### SSL Certificate Issues

1. **Wait for DNS**: SSL can't be issued until DNS is fully propagated
2. **Check Railway logs**: Railway dashboard shows SSL certificate status
3. **Verify domain**: Make sure domain is correctly configured in Railway
4. **Contact Railway support**: If issues persist after 24 hours

### 502/503 Errors

1. **Check Railway service**: Make sure services are running
2. **Verify domain routing**: Check Railway dashboard → Networking
3. **Check DNS**: Make sure DNS points to correct Railway service
4. **Review Railway logs**: Check for service errors

### CORS Issues

1. **Update CORS settings**: Make sure `CorsMiddleware.kt` allows your frontend domain
2. **Check allowed origins**: Verify frontend domain is in allowed list
3. **Test with curl first**: Isolate if it's a CORS or domain issue

## Verification Checklist

- [ ] DNS records added for `api.ground.com`
- [ ] DNS records added for `api-sandbox.ground.com`
- [ ] DNS propagation verified (dnschecker.org)
- [ ] Railway shows domains as "Active" or "Verified"
- [ ] SSL certificates issued (check Railway dashboard)
- [ ] `curl https://api.ground.com/health` returns 200
- [ ] `curl https://api-sandbox.ground.com/health` returns 200
- [ ] Frontend can connect to custom domains
- [ ] Environment toggle works with custom domains

## Quick Reference

### Production
- **Domain**: `api.ground.com`
- **Railway Service**: `flow-platform-production`
- **Branch**: `main`

### Sandbox
- **Domain**: `api-sandbox.ground.com`
- **Railway Service**: `flow-platform-staging`
- **Branch**: `staging`

## Next Steps

Once custom domains are set up:

1. ✅ Update any external documentation with new URLs
2. ✅ Test all API endpoints with custom domains
3. ✅ Update any third-party integrations
4. ✅ Share new URLs with your team
5. ✅ Monitor Railway dashboard for any issues

## Need Help?

- **Railway Docs**: https://docs.railway.app/guides/custom-domains
- **Railway Support**: https://railway.app/help
- **DNS Issues**: Contact your DNS provider support

🎉 **Once complete, your API will be accessible at professional custom domains!**

