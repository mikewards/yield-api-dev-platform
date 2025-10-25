#!/bin/bash

# Setup script for initializing Git repository and preparing for deployment

echo "🚀 Setting up Flow Platform for GitHub and Deployment"
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "❌ Git is not installed. Please install Git first."
    exit 1
fi

# Check if already a git repository
if [ -d .git ]; then
    echo "⚠️  Git repository already initialized"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "📦 Initializing Git repository..."
    git init
fi

# Add all files
echo "📝 Adding files to Git..."
git add .

# Check if there are changes to commit
if git diff --staged --quiet; then
    echo "ℹ️  No changes to commit"
else
    echo "💾 Creating initial commit..."
    git commit -m "Initial commit: Flow DeFi Platform

- Complete developer portal with landing page, API reference, guides
- Kotlin REST API gateway with PostgreSQL
- Account management, authentication, and API key generation
- Application and wallet management
- Yield account creation and management
- Integration stubs for Morpho and Aave protocols
- Modern UI with dashboard
- Deployment configurations for Railway and Render"
fi

echo ""
echo "✅ Git repository ready!"
echo ""
echo "📋 Next steps:"
echo "1. Create a private GitHub repository:"
echo "   https://github.com/new"
echo ""
echo "2. Add the remote and push:"
echo "   git remote add origin https://github.com/YOUR_USERNAME/flow-platform.git"
echo "   git branch -M main"
echo "   git push -u origin main"
echo ""
echo "3. Deploy to Railway or Render:"
echo "   See DEPLOYMENT.md for detailed instructions"
echo ""

