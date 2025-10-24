# Quick Deploy Guide

Get Flow deployed in 10 minutes! 🚀

## Prerequisites

- GitHub account
- Railway account (free) - https://railway.app

## Steps

### 1. Push to GitHub (5 min)

```bash
# Run setup script
./setup-git.sh

# Create repo on GitHub: https://github.com/new
# Name: flow-platform, Private: Yes

# Connect and push
git remote add origin https://github.com/YOUR_USERNAME/flow-platform.git
git branch -M main
git push -u origin main
```

### 2. Deploy to Railway (5 min)

1. Go to https://railway.app
2. Click "New Project"
3. Select "Deploy from GitHub repo"
4. Choose `flow-platform`
5. Railway auto-detects the config!

### 3. Add Database

1. In Railway dashboard, click "+ New"
2. Select "Database" → "Add PostgreSQL"
3. Done! Railway sets `DATABASE_URL` automatically

### 4. Set Environment Variables

Go to your service → Variables tab, add:

```
JWT_SECRET=<generate: openssl rand -hex 32>
MASTER_ENCRYPTION_KEY=<generate: openssl rand -hex 32>
DATABASE_USER=postgres
DATABASE_PASSWORD=<copy from database service>
```

**Generate secrets:**
```bash
openssl rand -hex 32  # Run twice, once for each
```

### 5. Deploy Frontend

**Option A: Railway Static Files**
- Add frontend files to a static service
- Or use the same service with nginx

**Option B: Netlify (Easiest)**
1. Go to https://netlify.com
2. Drag & drop your project folder
3. Update API URLs in frontend to point to Railway URL

### 6. Update Frontend API URLs

In `dashboard.html`, `account.html`, etc., change:

```javascript
const API_BASE_URL = 'https://your-app.railway.app';
```

### 7. Test!

1. Visit your Railway URL: `https://your-app.railway.app/health`
2. Should return: `{"status":"healthy",...}`
3. Visit frontend and create an account!

## That's It! 🎉

Your Flow platform is now live and ready to share with friends!

## Next Steps

- Add custom domain (optional)
- Set up monitoring (optional)
- Complete DeFi integrations (when ready)

## Need Help?

See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed instructions.

