#!/bin/bash

# Script to push Flow platform to GitHub
# Usage: ./PUSH_TO_GITHUB.sh YOUR_GITHUB_USERNAME

set -e

GITHUB_USERNAME=$1

if [ -z "$GITHUB_USERNAME" ]; then
    echo "❌ Error: GitHub username required"
    echo ""
    echo "Usage: ./PUSH_TO_GITHUB.sh YOUR_GITHUB_USERNAME"
    echo ""
    echo "Example: ./PUSH_TO_GITHUB.sh edsmith"
    exit 1
fi

REPO_NAME="flow-platform"
REPO_URL="https://github.com/${GITHUB_USERNAME}/${REPO_NAME}.git"

echo "🚀 Pushing Flow Platform to GitHub"
echo ""
echo "Repository: ${REPO_URL}"
echo ""

# Check if remote already exists
if git remote get-url origin &>/dev/null; then
    echo "⚠️  Remote 'origin' already exists"
    read -p "Remove and re-add? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git remote remove origin
    else
        echo "Cancelled."
        exit 1
    fi
fi

# Add remote
echo "📡 Adding GitHub remote..."
git remote add origin "${REPO_URL}"

# Ensure we're on main branch
echo "🌿 Ensuring main branch..."
git branch -M main 2>/dev/null || true

# Check if repository exists
echo "🔍 Checking if repository exists on GitHub..."
if ! git ls-remote --heads origin main &>/dev/null; then
    echo ""
    echo "❌ Repository not found on GitHub!"
    echo ""
    echo "Please create it first:"
    echo "1. Go to: https://github.com/new"
    echo "2. Repository name: ${REPO_NAME}"
    echo "3. Set to Private"
    echo "4. DO NOT initialize with README"
    echo "5. Click 'Create repository'"
    echo ""
    read -p "Press Enter after creating the repository, or Ctrl+C to cancel..."
fi

# Push to GitHub
echo ""
echo "📤 Pushing to GitHub..."
echo ""
echo "You'll be prompted for:"
echo "  - Username: ${GITHUB_USERNAME}"
echo "  - Password: Use a Personal Access Token (not your GitHub password)"
echo ""
echo "💡 To get a token: https://github.com/settings/tokens"
echo "   Select 'repo' scope"
echo ""

git push -u origin main

echo ""
echo "✅ Successfully pushed to GitHub!"
echo ""
echo "View your repository:"
echo "https://github.com/${GITHUB_USERNAME}/${REPO_NAME}"
echo ""
echo "Next steps:"
echo "1. Deploy to Railway: See QUICK_DEPLOY.md"
echo "2. Share with friends: Add them as collaborators in GitHub settings"

