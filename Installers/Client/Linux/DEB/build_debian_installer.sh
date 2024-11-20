#!/bin/bash

# Define variables for folder and file paths
SOURCE_FOLDER="$(git rev-parse --show-toplevel)/ErmisClient/Desktop"
TARGET_JAR="$SOURCE_FOLDER/target/ErmisClient.jar"
LIB_FOLDER="$SOURCE_FOLDER/target/lib"
DOC_FILES=("README.md" "LICENSE" "NOTICE")

INSTALL_FOLDER="Ermis-Client_amd64/opt/Ermis-Client"
BIN="$INSTALL_FOLDER/bin"

# Create necessary directories
mkdir -p "$BIN/lib"

# Copy the main JAR file and libraries
cp "$TARGET_JAR" "$BIN" || { echo "Failed to copy ErmisClient.jar"; exit 1; }
cp "$LIB_FOLDER"/* "$BIN/lib" || { echo "Failed to copy library files"; exit 1; }

# Copy documentation files
for file in "${DOC_FILES[@]}"; do
    cp "$SOURCE_FOLDER/$file" "$INSTALL_FOLDER" || { echo "Failed to copy $file"; exit 1; }
done

# Create DEB package
sudo dpkg-deb --build Ermis-Client_amd64 || { echo "Failed to build DEB package"; exit 1; }

echo "DEB package succesfully created!"
