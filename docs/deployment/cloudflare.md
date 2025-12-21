# Cloudflare Pages Deployment Guide

Complete guide for deploying the TBD frontend to Cloudflare Pages.

## Overview

Cloudflare Pages provides:
- Global CDN distribution
- Automatic deployments from GitHub
- Free SSL certificates
- Custom domain support
- Fast, reliable hosting

## Prerequisites

- Cloudflare account (free tier works)
- GitHub repository with frontend code
- Railway API deployed (for backend)

## Initial Setup

### 1. Sign Up / Log In

1. Go to https://pages.cloudflare.com
2. Sign up or log in with your Cloudflare account
3. Click **"Create a project"**

### 2. Connect GitHub Repository

1. Click **"Connect to Git"**
2. Authorize Cloudflare to access your GitHub account
3. Select repository: `wardmic4/flow-platform` (or your repo)
4. Click **"Begin setup"**

### 3. Configure Build Settings

**Project name:** `tbd-platform` (or your preferred name)

**Build settings:**
- **Framework preset:** None (or "Plain HTML")
- **Build command:** (leave empty - no build needed)
- **Build output directory:** `/` (root of frontend directory)
- **Root directory:** `frontend` (set to frontend directory)

**Environment variables:** (Optional)
- None needed for static site

### 4. Deploy

1. Click **"Save and Deploy"**
2. Cloudflare will automatically deploy your site
3. Wait 1-2 minutes for deployment to complete
4. Your site will be available at `tbd-platform.pages.dev`

## Automatic Deployments

Cloudflare Pages automatically deploys:
- **Production**: Deploys from `main` branch
- **Preview**: Creates preview deployments for pull requests

### Branch Configuration

1. Go to project **Settings** → **Builds & deployments**
2. Configure which branches trigger deployments
3. Set production branch (usually `main`)

## Custom Domain

### Setup Custom Domain

1. Go to your project → **"Custom domains"**
2. Click **"Set up a custom domain"**
3. Enter your domain (e.g., `tbd.com`)
4. Follow DNS setup instructions

### DNS Configuration

Cloudflare will provide DNS records to add:
- **CNAME**: `tbd.com` → `tbd-platform.pages.dev`
- **CNAME**: `www.tbd.com` → `tbd-platform.pages.dev`

Add these records in your domain's DNS settings.

## Environment Variables

For static sites, environment variables are typically not needed. If you need to configure API URLs:

1. Go to **Settings** → **Environment variables**
2. Add variables (e.g., `API_BASE_URL`)
3. Access in JavaScript via `process.env.API_BASE_URL` (if using build tools)

## Updating API URLs

If your frontend references the Railway API URL, ensure it's correct:

1. Check `config.js` for API base URL
2. Update to match your Railway deployment:
   - Production: `https://flow-platform-production.up.railway.app`
   - Staging: `https://flow-platform-staging.up.railway.app`

## Troubleshooting

### Deployment Fails

1. Check **Deployments** tab for error messages
2. Verify build settings are correct
3. Ensure all required files are in repository

### Custom Domain Not Working

1. Verify DNS records are correct
2. Wait for DNS propagation (can take up to 24 hours)
3. Check domain is verified in Cloudflare dashboard

### Site Not Updating

1. Verify code is pushed to correct branch
2. Check deployment logs for errors
3. Manually trigger deployment if needed

## Performance Optimization

Cloudflare Pages automatically provides:
- Global CDN distribution
- Automatic compression
- HTTP/2 and HTTP/3 support
- Image optimization (if enabled)

### Cache Settings

1. Go to **Settings** → **Cache**
2. Configure cache rules if needed
3. Default settings work well for most sites

## Related Documentation

- [Railway Deployment](./railway.md) - Backend API deployment
- [Development Setup](../development/setup.md) - Local development

