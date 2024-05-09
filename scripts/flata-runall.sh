# Script for running all the benchmarks

#!/bin/bash

# Define the path to the benchmarks-reach folder
BENCHMARKS_DIR="../flataProvidedBenchmarks/benchmarks-reach"

# Define the path to the flata-reachability.sh script
FLATA_SCRIPT="./flata-reachability.sh"

# Redirect standard input from the terminal
exec 3<&0

# Find all .nts files in the benchmarks-reach directory and execute the script on each file
find "$BENCHMARKS_DIR" -type f -name "*.nts" | while read file; do
    echo "Processing $file..."
    "$FLATA_SCRIPT" "$file"
    echo "Press any key to continue to the next file..."
    read -n 1 -s -u 3  # Read a single character in silent mode from terminal
done

# Restore standard input
exec 3<&-

echo "All files have been processed."
