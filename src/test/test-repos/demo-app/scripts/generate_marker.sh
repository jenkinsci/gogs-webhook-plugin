#!/usr/bin/env bash

# build unique filename
marker_file="marker.txt"

# Dump some useful info in the marker file if available
echo "# --== ${marker_file} ==--" > ${marker_file}
echo " "  >> ${marker_file}

#
echo "BUILD_NUMBER=${BUILD_NUMBER:-nihil}" >> ${marker_file}
echo "GIT_COMMIT=${GIT_COMMIT:-nihil}" >> ${marker_file}
echo "GIT_URL=${GIT_URL:-nihil}" >> ${marker_file}
echo "GIT_BRANCH=${GIT_BRANCH:-nihil}" >> ${marker_file}
echo "JOB_NAME=${JOB_NAME:-nihil}" >> ${marker_file}
echo "WORKSPACE=${WORKSPACE:-nihil}" >> ${marker_file}
