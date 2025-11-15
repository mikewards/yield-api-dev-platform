#!/bin/bash

# Nuclear fusion icon SVG - two nuclei merging with energy waves
NEW_ICON='<svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <!-- TBD Logo: Nuclear Fusion Icon - representing energy, growth, and optimization -->
                        <!-- Two nuclei merging in the center -->
                        <circle cx="12" cy="16" r="3" fill="#0F172A" opacity="0.8"/>
                        <circle cx="20" cy="16" r="3" fill="#0F172A" opacity="0.8"/>
                        <!-- Fusion core (merged nuclei) -->
                        <circle cx="16" cy="16" r="4" fill="#0F172A"/>
                        <!-- Energy waves radiating outward -->
                        <path d="M16 8 Q20 12 16 16 Q12 12 16 8" stroke="#0F172A" stroke-width="1.5" fill="none" opacity="0.4"/>
                        <path d="M16 24 Q20 20 16 16 Q12 20 16 24" stroke="#0F172A" stroke-width="1.5" fill="none" opacity="0.4"/>
                        <path d="M8 16 Q12 20 16 16 Q12 12 8 16" stroke="#0F172A" stroke-width="1.5" fill="none" opacity="0.4"/>
                        <path d="M24 16 Q20 12 16 16 Q20 20 24 16" stroke="#0F172A" stroke-width="1.5" fill="none" opacity="0.4"/>
                        <!-- Particle paths -->
                        <circle cx="10" cy="10" r="1" fill="#0F172A" opacity="0.6"/>
                        <circle cx="22" cy="10" r="1" fill="#0F172A" opacity="0.6"/>
                        <circle cx="10" cy="22" r="1" fill="#0F172A" opacity="0.6"/>
                        <circle cx="22" cy="22" r="1" fill="#0F172A" opacity="0.6"/>
                    </svg>'

# Find and replace in all HTML files
find . -name "*.html" -type f | while read file; do
    # Replace the old target icon with new fusion icon
    sed -i '' 's|<circle cx="16" cy="16" r="4" fill="#0F172A"/>.*<circle cx="21.5" cy="21.5" r="1.5" fill="#0F172A"/>|'"$NEW_ICON"'|g' "$file"
done

echo "Icon replacement complete!"

