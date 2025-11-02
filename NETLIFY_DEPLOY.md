# Deploy to Netlify - Quick Guide

## Option 1: Drag and Drop (Fastest - 2 minutes)

### Step 1: Prepare Your Files

You need to deploy **only the frontend files**, not the entire project. Create a deployment folder:

**Files to include:**
- ✅ `index.html`
- ✅ `account.html`
- ✅ `signin.html`
- ✅ `dashboard.html`
- ✅ `api-reference.html`
- ✅ `api-detail.html`
- ✅ `getting-started.html`
- ✅ `guides.html`
- ✅ `styles.css`
- ✅ `dashboard-styles.css`
- ✅ `api-styles.css`
- ✅ `script.js`
- ✅ `nav-auth.js`
- ✅ `api-script.js`
- ✅ `api-detail-script.js`
- ✅ Any images/assets

**Files to EXCLUDE:**
- ❌ `flow-api/` folder (backend code)
- ❌ `.git/` folder
- ❌ `node_modules/` (if any)
- ❌ `.env` files
- ❌ Documentation markdown files (optional)

### Step 2: Deploy

1. **On Netlify**, you should see: "Drag and drop your project folder here"
2. **Open Finder** and go to `/Users/ed/Desktop/P1/`
3. **Select all frontend files** (or just drag the whole P1 folder - Netlify will ignore backend files)
4. **Drag and drop** onto Netlify
5. **Wait ~30 seconds**
6. **You'll get a URL!** Like: `https://random-name-123.netlify.app`

## Option 2: Import from Git (Better for Updates)

### Step 1: Connect GitHub

1. Click **"Import from Git"**
2. Choose **GitHub**
3. Authorize Netlify
4. Select your repo: `wardmic4/flow-platform`

### Step 2: Configure Build

**Build settings:**
- **Base directory:** Leave empty (or `/` if needed)
- **Build command:** Leave empty (no build needed for static HTML)
- **Publish directory:** `/` (root)

**Important:** You might want to exclude the `flow-api` folder:
- Add to `netlify.toml` (create this file):

```toml
[build]
  publish = "."

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

### Step 3: Deploy

1. Click **"Deploy site"**
2. Wait ~1 minute
3. You'll get a URL!

## 🎯 Recommended: Quick Deploy (Option 1)

**Just drag and drop your P1 folder!** Netlify will:
- Deploy all HTML files
- Ignore the `flow-api` folder automatically
- Give you a URL in 30 seconds

## ✅ After Deployment

1. **Test your site** at the Netlify URL
2. **Share with friends!**
3. **Custom domain** (optional) - Add your own domain in Netlify settings

## 🎉 That's It!

Your website will be live at: `https://your-site-name.netlify.app`

