# Flow Platform Deployment Guide

This guide will help you deploy Flow to Railway or Render for sharing with friends.

## Prerequisites

- GitHub account
- Railway.app account (free) OR Render.com account (free)
- Domain name (optional, but recommended)

## Quick Deploy Options

### Option 1: Railway (Recommended - Easiest)

Railway is the simplest option with automatic deployments from GitHub.

#### Steps:

1. **Prepare your code:**
   ```bash
   # Make sure you're in the project root
   cd /Users/ed/Desktop/P1
   
   # Initialize git (if not already done)
   git init
   git add .
   git commit -m "Initial commit"
   ```

2. **Create GitHub Repository:**
   - Go to https://github.com/new
   - Create a **private** repository named `flow-platform`
   - Don't initialize with README (we already have one)
   - Copy the repository URL

3. **Push to GitHub:**
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/flow-platform.git
   git branch -M main
   git push -u origin main
   ```

4. **Deploy to Railway:**
   - Go to https://railway.app
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Choose your `flow-platform` repository
   - Railway will auto-detect the `flow-api/railway.json` config

5. **Add PostgreSQL Database:**
   - In Railway dashboard, click "+ New"
   - Select "Database" → "Add PostgreSQL"
   - Railway will automatically set `DATABASE_URL` environment variable

6. **Set Environment Variables:**
   - Go to your service → Variables tab
   - Add these variables:
     ```
     DATABASE_USER=postgres
     DATABASE_PASSWORD=<from database service>
     JWT_SECRET=<generate: openssl rand -hex 32>
     MASTER_ENCRYPTION_KEY=<generate: openssl rand -hex 32>
     ETH_SANDBOX_RPC_URL=https://sepolia.infura.io/v3/YOUR_KEY
     ETH_PRODUCTION_RPC_URL=https://mainnet.infura.io/v3/YOUR_KEY
     ```
   - Railway will auto-set `DATABASE_URL` from the database service

7. **Deploy Frontend:**
   - Railway can serve static files, but for better performance:
   - Option A: Use Railway's static file serving
   - Option B: Deploy frontend separately to Netlify/Vercel

8. **Get Your URL:**
   - Railway will provide a URL like: `flow-api-production.up.railway.app`
   - You can add a custom domain in Settings → Domains

### Option 2: Render

Render is another great free option with similar features.

#### Steps:

1. **Push to GitHub** (same as Railway steps 1-3)

2. **Deploy to Render:**
   - Go to https://render.com
   - Sign up/login
   - Click "New +" → "Blueprint"
   - Connect your GitHub repository
   - Render will use the `render.yaml` file automatically

3. **Set Environment Variables:**
   - Go to your service → Environment
   - Add the same variables as Railway
   - Render will auto-create the PostgreSQL database

4. **Get Your URL:**
   - Render provides: `flow-api.onrender.com`
   - Custom domains available in Settings

## Environment Variables

Create these in your hosting platform:

### Required:
- `DATABASE_URL` - Auto-set by Railway/Render (PostgreSQL connection string)
- `DATABASE_USER` - Usually `postgres`
- `DATABASE_PASSWORD` - From your database service
- `JWT_SECRET` - Generate with: `openssl rand -hex 32`
- `MASTER_ENCRYPTION_KEY` - Generate with: `openssl rand -hex 32`

### Optional:
- `ETH_SANDBOX_RPC_URL` - Sepolia testnet RPC (Infura/Alchemy)
- `ETH_PRODUCTION_RPC_URL` - Mainnet RPC (Infura/Alchemy)
- `PORT` - Defaults to 8080

## Frontend Deployment

The frontend is static HTML/CSS/JS. You have several options:

### Option A: Same Service (Railway/Render)
- Add frontend files to a static file server
- Configure nginx or serve from the API service

### Option B: Separate Service (Recommended)
- **Netlify** (easiest):
  1. Go to https://netlify.com
  2. Drag & drop your project folder
  3. Set build command: (none, it's static)
  4. Set publish directory: `/` (root)
  5. Update API URLs in frontend code to point to your Railway/Render URL

- **Vercel**:
  1. Go to https://vercel.com
  2. Import GitHub repository
  3. Configure as static site
  4. Update API URLs

### Update Frontend API URLs

After deploying the backend, update these files to use your production URL:

- `dashboard.html` - Change `API_BASE_URL`
- `account.html` - Change API URLs
- `signin.html` - Change API URLs
- `api-detail-script.js` - Change base URL
- Any other files making API calls

Or better: Use environment-based configuration:

```javascript
const API_BASE_URL = window.location.hostname === 'localhost' 
  ? 'http://localhost:8080' 
  : 'https://your-railway-url.railway.app';
```

## Database Setup

The database schema will be created automatically on first run. However, if you need to run migrations manually:

1. Connect to your database (Railway/Render provide connection strings)
2. Run the schema creation (it's in `DatabaseFactory.kt`)

## Testing Your Deployment

1. **Health Check:**
   ```bash
   curl https://your-api-url.railway.app/health
   ```

2. **Create Account:**
   ```bash
   curl -X POST https://your-api-url.railway.app/v1/accounts \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"test123456"}'
   ```

3. **Test Frontend:**
   - Visit your frontend URL
   - Try creating an account
   - Sign in and access dashboard

## Troubleshooting

### API Not Starting
- Check logs in Railway/Render dashboard
- Verify all environment variables are set
- Check database connection string

### Database Connection Issues
- Verify `DATABASE_URL` is correct
- Check database is running
- Ensure database allows connections from your service

### Frontend Can't Connect to API
- Check CORS settings in `CorsMiddleware.kt`
- Verify API URL is correct in frontend code
- Check browser console for errors

### Build Failures
- Check build logs
- Verify Dockerfile is correct
- Ensure all dependencies are in `build.gradle.kts`

## Security Notes

⚠️ **Important for Production:**

1. **Never commit `.env` files** - They're in `.gitignore`
2. **Use strong secrets** - Generate with `openssl rand -hex 32`
3. **Enable HTTPS** - Railway/Render provide this automatically
4. **Set up rate limiting** - Prevent abuse
5. **Monitor logs** - Watch for suspicious activity

## Next Steps

After deployment:

1. ✅ Test all features
2. ✅ Share URL with friends
3. ✅ Set up monitoring (optional)
4. ✅ Add custom domain (optional)
5. ✅ Set up backups (optional)

## Support

If you run into issues:
- Check Railway/Render logs
- Review this documentation
- Check GitHub issues (if public)
- Contact support through the platform

