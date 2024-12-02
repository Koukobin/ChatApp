#!/bin/bash

folder=.
search_string="ermis_client"
replace_string="ermis_client"

# Find and replace strings in all files
find "$folder" -type f -exec sed -i "s/$search_string/$replace_string/g" {} +

echo "Replaced all occurrences of '$search_string' with '$replace_string' in folder '$folder' and its subfolders."

