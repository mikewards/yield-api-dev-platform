# Install Homebrew

## Quick Install

Open your terminal and run:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

## What This Does

1. Downloads the official Homebrew installer
2. Installs Homebrew to `/opt/homebrew` (Apple Silicon) or `/usr/local` (Intel)
3. Sets up the PATH automatically

## After Installation

The installer will tell you to add Homebrew to your PATH. It usually looks like:

```bash
# For Apple Silicon Macs:
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"

# For Intel Macs:
echo 'eval "$(/usr/local/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/usr/local/bin/brew shellenv)"
```

## Verify Installation

After installing, verify it works:

```bash
brew --version
```

Should show something like: `Homebrew 4.x.x`

## Common Packages You Might Want

```bash
# Git (if not already installed)
brew install git

# Node.js (for frontend development)
brew install node

# PostgreSQL client tools
brew install postgresql@18

# Java (if needed)
brew install openjdk@17
```

## That's It!

Homebrew is now installed and ready to use! 🍺

