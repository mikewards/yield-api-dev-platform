# Cloudflare Pages Setup Guide

## Step-by-Step Instructions

### 1. Sign Up / Log In to Cloudflare Pages

1. Go to https://pages.cloudflare.com
2. Sign up or log in with your Cloudflare account (or create one - it's free)
3. Click "Create a project"

### 2. Connect Your GitHub Repository

1. Click "Connect to Git"
2. Authorize Cloudflare to access your GitHub account
3. Select repository: `wardmic4/flow-platform`
4. Click "Begin setup"

### 3. Configure Build Settings

**Project name:** `tbd-platform` (or whatever you prefer)

**Build settings:**
- **Framework preset:** None (or "Plain HTML")
- **Build command:** (leave empty - no build needed)
- **Build output directory:** `/` (root directory)
- **Root directory:** `/` (leave as root)

**Environment variables:** (Optional - add if needed)
- None needed for static site

### 4. Deploy

1. Click "Save and Deploy"
2. Cloudflare will automatically deploy your site
3. Wait 1-2 minutes for deployment to complete

### 5. Custom Domain (Optional)

After deployment:
1. Go to your project → "Custom domains"
2. Click "Set up a custom domain"
3. Enter your domain (e.g., `tbd.com`)
4. Follow DNS setup instructions

### 6. Update API URLs (If Needed)

If your frontend references the Railway API URL, make sure it's correct:
- Check `account.html`, `signin.html`, `dashboard.html` for `API_BASE_URL`
- Should be: `https://flow-platform-production.up.railway.app` (or your Railway URL)

## What Gets Deployed

Cloudflare Pages will deploy:
- All HTML files (`index.html`, `guides.html`, `api-reference.html`, etc.)
- All CSS files (`styles.css`, `api-styles.css`, etc.)
- All JavaScript files (`script.js`, `nav-auth.js`, etc.)
- `sdk-demos/` folder
- `tbd-logo.svg`
- All other static assets

## Benefits

✅ **Unlimited bandwidth** (free tier)
✅ **Global CDN** (fast worldwide)
✅ **Automatic deployments** (on every git push)
✅ **Free forever** (no credit system)
✅ **Custom domains** (free SSL)

## After Setup

1. Your site will be available at: `https://<project-name>.pages.dev`
2. Every push to `main` branch will auto-deploy
3. You can view deployment history in Cloudflare dashboard

## Troubleshooting

**If deployment fails:**
- Check build logs in Cloudflare dashboard
- Verify root directory is correct (`/`)
- Make sure all files are committed to GitHub

**If site doesn't load:**
- Check that `index.html` is in the root directory
- Verify custom domain DNS settings (if using custom domain)
- Check Cloudflare dashboard for error messages

## Next Steps

After deployment:
1. Test all pages work correctly
2. Update any hardcoded URLs if needed
3. Share the new Cloudflare Pages URL
4. (Optional) Set up custom domain

---

**Your GitHub repo is ready:** `wardmic4/flow-platform`
**Branch to deploy:** `main`
**No build needed** - it's a static site!

