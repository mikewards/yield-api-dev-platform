# GitHub Repository Setup

Follow these steps to create your GitHub repository and push your code.

## Step 1: Create GitHub Repository

1. Go to https://github.com/new
2. Repository name: `flow-platform` (or your preferred name)
3. Description: "DeFi platform providing APIs and developer tools for cryptocurrency yield generation"
4. **Visibility: Private** (for sharing with friends only)
5. **DO NOT** initialize with README, .gitignore, or license (we already have these)
6. Click "Create repository"

## Step 2: Run Setup Script

From the project root:

```bash
./setup-git.sh
```

This will:
- Initialize git repository (if not already done)
- Add all files
- Create initial commit

## Step 3: Connect to GitHub and Push

```bash
# Add your GitHub repository as remote
git remote add origin https://github.com/YOUR_USERNAME/flow-platform.git

# Rename branch to main (if needed)
git branch -M main

# Push to GitHub
git push -u origin main
```

**Note:** You'll be prompted for your GitHub username and password (or personal access token).

### Using Personal Access Token

If you have 2FA enabled, you'll need a Personal Access Token:

1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token with `repo` scope
3. Use the token as your password when pushing

## Step 4: Verify

1. Go to your repository on GitHub
2. Verify all files are there
3. Check that `.env` files are NOT included (they should be in `.gitignore`)

## Step 5: Deploy

Now you can deploy to Railway or Render. See [DEPLOYMENT.md](./DEPLOYMENT.md) for instructions.

## Troubleshooting

### "Repository not found"
- Check the repository name and your username
- Ensure the repository exists and you have access

### "Permission denied"
- Check your GitHub credentials
- Use Personal Access Token if 2FA is enabled

### "Files not showing"
- Check `.gitignore` isn't excluding important files
- Verify files are committed: `git status`

### "Large files"
- If you have large files, consider using Git LFS
- Or exclude build artifacts (already in `.gitignore`)

