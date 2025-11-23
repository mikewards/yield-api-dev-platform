# Fix Cloudflare Pages Build Settings

## The Problem
Cloudflare Pages is trying to run `npx wrangler deploy` which is for Workers, not Pages. This is a static site, so we don't need any build or deploy commands.

## Solution: Update Cloudflare Pages Settings

1. Go to your Cloudflare Pages dashboard
2. Click on your project (`tbd-platform` or similar)
3. Go to **Settings** → **Builds & deployments**
4. Scroll to **Build configuration**
5. Update these settings:

### Build Settings:
- **Framework preset**: Select **"None"** or **"Plain HTML"**
- **Build command**: Leave **EMPTY** (or set to `echo "No build needed"`)
- **Build output directory**: Set to `/` or `.` (root directory)
- **Root directory**: Leave as `/` (root)

### Deploy Settings:
- **Deploy command**: Leave **EMPTY** (remove `npx wrangler deploy` if it's there)

## Alternative: Use Direct Upload

If the build settings keep causing issues, you can:
1. Go to **Settings** → **Builds & deployments**
2. Under **Build configuration**, click **"Edit configuration"**
3. Set **Build command** to: `echo "Static site - no build needed"`
4. Set **Deploy command** to: (leave empty)

## What Should Happen

After updating:
- Cloudflare Pages will detect it's a static site
- It will serve files directly from the repository
- No build or deploy commands will run
- Your site will deploy successfully

## Verify

After updating settings:
1. Trigger a new deployment (or push a new commit)
2. Check the build logs - you should see:
   - "No build command specified" or similar
   - Files being copied/served
   - Deployment successful

