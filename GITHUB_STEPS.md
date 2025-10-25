# GitHub Setup - Step by Step

## Quick Steps (5 minutes)

### Step 1: Create GitHub Repository (2 min)

1. **Open your browser** and go to: https://github.com/new

2. **Fill in the form:**
   - **Repository name:** `flow-platform` (or your preferred name)
   - **Description:** "DeFi platform providing APIs and developer tools for cryptocurrency yield generation"
   - **Visibility:** Select **Private** (for sharing with friends only)
   - **DO NOT check** any of these boxes:
     - ❌ Add a README file
     - ❌ Add .gitignore
     - ❌ Choose a license
   (We already have all of these!)

3. **Click "Create repository"**

4. **Copy the repository URL** - You'll see a page with instructions. Copy the HTTPS URL, it looks like:
   ```
   https://github.com/YOUR_USERNAME/flow-platform.git
   ```

### Step 2: Connect and Push (3 min)

**Open your terminal** and run these commands (replace `YOUR_USERNAME` with your actual GitHub username):

```bash
# Navigate to your project
cd /Users/ed/Desktop/P1

# Add GitHub as remote
git remote add origin https://github.com/YOUR_USERNAME/flow-platform.git

# Rename branch to main (if needed)
git branch -M main

# Push to GitHub
git push -u origin main
```

**When prompted:**
- **Username:** Your GitHub username
- **Password:** You'll need a **Personal Access Token** (not your GitHub password)

### Step 3: Get Personal Access Token (if needed)

If you have 2FA enabled or GitHub asks for a token:

1. Go to: https://github.com/settings/tokens
2. Click **"Generate new token"** → **"Generate new token (classic)"**
3. **Name it:** `flow-platform-deploy`
4. **Select scopes:** Check `repo` (full control of private repositories)
5. **Click "Generate token"**
6. **Copy the token** (you won't see it again!)
7. Use this token as your password when pushing

### Step 4: Verify

1. Go to: https://github.com/YOUR_USERNAME/flow-platform
2. You should see all your files
3. Verify `.env` files are **NOT** visible (they should be ignored)

## Troubleshooting

### "Repository not found"
- Check the repository name matches exactly
- Ensure the repository exists on GitHub
- Verify you're using the correct username

### "Permission denied"
- Use Personal Access Token instead of password
- Ensure token has `repo` scope
- Check 2FA is properly configured

### "Remote origin already exists"
```bash
# Remove existing remote
git remote remove origin

# Add it again
git remote add origin https://github.com/YOUR_USERNAME/flow-platform.git
```

### "Authentication failed"
- Use Personal Access Token, not password
- Token must have `repo` scope
- Check token hasn't expired

## After Pushing

Once your code is on GitHub, you can:
1. ✅ Deploy to Railway (see `QUICK_DEPLOY.md`)
2. ✅ Share repository with friends
3. ✅ Set up CI/CD (optional)

## Quick Copy-Paste Commands

Replace `YOUR_USERNAME` with your GitHub username:

```bash
cd /Users/ed/Desktop/P1
git remote add origin https://github.com/YOUR_USERNAME/flow-platform.git
git branch -M main
git push -u origin main
```

That's it! 🚀

