#!/bin/bash

# Setup Java environment for Flow API
# This script loads SDKMAN and sets up Java

# Load SDKMAN
if [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    echo "✓ SDKMAN loaded"
    
    # Set Java 17 as default if not already set
    sdk default java 17.0.9-tem 2>/dev/null || true
    
    # Verify Java
    if java -version 2>&1 | grep -q "17"; then
        echo "✓ Java 17 is ready"
        echo "Java version:"
        java -version
        return 0
    else
        echo "⚠️  Java 17 not found, installing..."
        sdk install java 17.0.9-tem
        sdk default java 17.0.9-tem
    fi
else
    echo "⚠️  SDKMAN not found. Please install it first:"
    echo "  curl -s \"https://get.sdkman.io\" | bash"
    return 1
fi
