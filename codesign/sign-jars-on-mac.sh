#!/bin/bash

# Check if JAR path pattern and Developer ID are provided
if [ "$#" -ne 2 ]; then
  echo "Usage: $0 path/to/jar 'YOUR_DEVELOPER_ID'"
  exit 1
fi

# Assign arguments and resolve JAR path pattern to absolute paths
JAR_PATTERN="$1"
DEVELOPER_ID="Developer ID Application: $2"

# Convert JAR_PATTERN to absolute paths for each matching file
for JAR_PATH in $JAR_PATTERN; do
  if [ -f "$JAR_PATH" ]; then
    # Convert JAR_PATH to an absolute path
    ABS_JAR_PATH="$(cd "$(dirname "$JAR_PATH")"; pwd)/$(basename "$JAR_PATH")"
    echo "Processing $ABS_JAR_PATH"

    # Create temporary directory for extraction
    TEMP_DIR=$(mktemp -d)
    echo "Using temporary directory $TEMP_DIR"

    # Extract the JAR contents
    unzip -q "$ABS_JAR_PATH" -d "$TEMP_DIR"

    # Find and sign all .dylib and .jnilib files
    find "$TEMP_DIR" \( -name "*.dylib" -o -name "*.jnilib" \) -print0 | while IFS= read -r -d '' LIB_FILE; do
      echo "Signing $LIB_FILE"
      codesign --force --sign "$DEVELOPER_ID" --timestamp "$LIB_FILE"
    done

    # Repackage the JAR with ditto, ensuring only the contents are included
    echo "Repackaging JAR with ditto..."
    (cd "$TEMP_DIR" && ditto -c -k --rsrc --sequesterRsrc . "$ABS_JAR_PATH")

    # Sign the new JAR
    echo "Signing JAR at $ABS_JAR_PATH"
    codesign --force --sign "$DEVELOPER_ID" --timestamp "$ABS_JAR_PATH"

    # Clean up
    rm -rf "$TEMP_DIR"

    echo "Signed JAR created at $ABS_JAR_PATH"
  else
    echo "No JAR files matched pattern: $JAR_PATTERN"
  fi
done
