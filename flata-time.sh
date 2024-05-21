#!/bin/bash

# Script to run old and new program versions on benchmarks and measure execution time

# Directories for old and new program versions
OLD_VERSION_DIR="./flataArchived"
NEW_VERSION_DIR="./dist"

# Paths to benchmark folders and their corresponding scripts
declare -A BENCHMARKS_DIRS=(
    ["reach"]="./flataProvidedBenchmarks/benchmarks-reach"
    ["term"]="./flataProvidedBenchmarks/benchmarks-term"
    ["sil"]="./flataProvidedBenchmarks/benchmarks-sil"
    ["recur"]="./flataProvidedBenchmarks/benchmarks-recur"
)

# Mapping of scripts for different benchmark categories
declare -A SCRIPTS=(
    ["reach"]="./flata-reachability.sh"
    ["term"]="./flata-termination.sh"
    ["sil"]="./flata-sil.sh"
    ["recur"]="./flata-reachability.sh" # Using the reachability script for recursivity benchmarks
)

# Output directory for timings
TIMINGS_OUTPUT_DIR="./output_timings/"
mkdir -p "$TIMINGS_OUTPUT_DIR"

# Function to execute a version of the program with timeout and timing
run_program() {
    local program_dir="$1"
    local script_name="$2"
    local benchmark="$3"
    local output_file="$4"
    local timeout_duration="$5"
    local time_file="$6"
    local absolute_path=$(realpath "$benchmark")  # Convert to absolute path

    # Temporarily move to program directory to respect relative classpath settings
    pushd "$program_dir" > /dev/null

    # Time and execute the script with the absolute path to the benchmark using timeout
    /usr/bin/time -f "%e" -o "$time_file" timeout "$timeout_duration" "./$script_name" "$absolute_path" > "$output_file"
    local status=$?

    # Return to the original directory
    popd > /dev/null

    # Check if the process was killed by the timeout
    if [ $status -eq 124 ]; then
        echo "Timeout: Processing of $benchmark exceeded $timeout_duration seconds."
        echo "999" > "$time_file"  # Indicating a timeout as maximum time
    fi
}

# Process each category and benchmark
for category in "${!BENCHMARKS_DIRS[@]}"; do
    echo "Processing category: $category"
    BENCHMARKS_DIR="${BENCHMARKS_DIRS[$category]}"
    SCRIPT_NAME="${SCRIPTS[$category]}"
    TIMEOUT_DURATION=60

    # Get the total number of benchmark files
    total_files=$(find "$BENCHMARKS_DIR" -type f -name "*.nts" | wc -l)
    current_file=0

    # JSON file to store timings
    json_output_file="${TIMINGS_OUTPUT_DIR}${category}_timings.json"
    echo "[" > "$json_output_file"

    # Process each benchmark file
    find "$BENCHMARKS_DIR" -type f -name "*.nts" | while read benchmark; do
        current_file=$((current_file + 1))
        echo "Running $category benchmark $(basename "$benchmark") ($current_file of $total_files)..."

        # Prepare temporary files for output and timing
        old_output=$(mktemp)
        new_output=$(mktemp)
        old_time=$(mktemp)
        new_time=$(mktemp)

        # Execute old and new versions of the script with timeout and timing
        run_program "$OLD_VERSION_DIR" "$SCRIPT_NAME" "$benchmark" "$old_output" "$TIMEOUT_DURATION" "$old_time"
        run_program "$NEW_VERSION_DIR" "$SCRIPT_NAME" "$benchmark" "$new_output" "$TIMEOUT_DURATION" "$new_time"

        # Read timings
        old_duration=$(cat "$old_time")
        new_duration=$(cat "$new_time")

        # Append results to JSON
        echo "{\"benchmark\": \"$(basename "$benchmark")\", \"old_duration\": $old_duration, \"new_duration\": $new_duration}," >> "$json_output_file"

        # Clean up temporary files
        rm "$old_output" "$new_output" "$old_time" "$new_time"
    done

    # Properly close the JSON array
    sed -i '$ s/,$//' "$json_output_file"  # Remove the last comma
    echo "]" >> "$json_output_file"

    echo "All $category benchmarks processed ($total_files files)."
done

echo "All categories have been processed. Check the '$TIMINGS_OUTPUT_DIR' directory for the timing results."