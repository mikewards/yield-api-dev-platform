# Staging Environment Verification

## ✅ Setup Complete!

You've successfully configured:
- ✅ Branch set to `staging`
- ✅ Environment variable set to `staging`
- ✅ New secrets generated and set
- ✅ Services renamed
- ✅ Database password updated

## 🧪 Test Your Environments

### Quick Test Script

Run the test script:
```bash
./test-environments.sh
```

### Manual Testing

#### Test Staging:
```bash
curl https://flow-platform-staging.up.railway.app/health
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-15T..."
}
```

#### Test Production:
```bash
curl https://flow-platform-production.up.railway.app/health
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-15T..."
}
```

## 🔍 Verify Deployment

### Check Staging Deployment
1. Go to Railway → `flow-platform-staging` → **Deployments**
2. Look for:
   - ✅ "Deployment successful"
   - ✅ "Database tables created successfully" in logs
   - ✅ No errors

### Check Production Deployment
1. Go to Railway → `flow-platform-production` → **Deployments**
2. Should show latest deployment from `main` branch

## 🎯 Next Steps

### 1. Test Account Creation on Staging
```bash
curl https://flow-platform-staging.up.railway.app/v1/accounts \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_staging",
    "password": "test123",
    "email": "test@example.com"
  }'
```

### 2. Test Account Creation on Production
```bash
curl https://flow-platform-production.up.railway.app/v1/accounts \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_production",
    "password": "test123",
    "email": "test@example.com"
  }'
```

### 3. Verify Environment Toggle in Documentation
1. Go to your API documentation page
2. Click the environment toggle (Production/Sandbox)
3. Verify the curl examples update with the correct URLs

## 📊 Environment URLs

- **Staging**: `https://flow-platform-staging.up.railway.app`
- **Production**: `https://flow-platform-production.up.railway.app`

## 🔐 Environment Variables Summary

### Staging (`flow-platform-staging`):
```
ENVIRONMENT=staging
DATABASE_USER=postgres
DATABASE_PASSWORD=<from flow-db-staging>
JWT_SECRET=4f9cf977a66b1cbd429a6e983978f31b62f681ca8122c67538695a9572bbb0ae
MASTER_ENCRYPTION_KEY=a03b219f5f465078191bc942780460e3ecc4d7c8ae1d7bcbfbf2cfc4c44551e6
DATABASE_URL=<auto-set by Railway>
```

### Production (`flow-platform-production`):
```
ENVIRONMENT=production
DATABASE_USER=postgres
DATABASE_PASSWORD=<from flow-db-production>
JWT_SECRET=<your production secret>
MASTER_ENCRYPTION_KEY=<your production key>
DATABASE_URL=<auto-set by Railway>
```

## ✅ Verification Checklist

- [ ] Staging health endpoint returns 200
- [ ] Production health endpoint returns 200
- [ ] Staging deployment shows "Database tables created successfully"
- [ ] Can create account on staging
- [ ] Can create account on production
- [ ] Environment toggle works in documentation (shows Production/Sandbox)
- [ ] Staging uses `staging` branch
- [ ] Production uses `main` branch

## 🚀 You're All Set!

Both environments are now configured and ready to use:
- **Sandbox**: For testing new features before production
- **Production**: For live users

Remember:
- Push to `staging` branch → Auto-deploys to staging
- Push to `main` branch → Auto-deploys to production

Happy deploying! 🎉

