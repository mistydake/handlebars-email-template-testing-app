#!/bin/bash
echo "Building Handlebars Email Template Testing App..."
echo ""

if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found. Please install Maven: https://maven.apache.org/download.cgi"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Please install Java 11 or higher."
    exit 1
fi

mvn clean package -q

if [ $? -ne 0 ]; then
    echo ""
    echo "BUILD FAILED. See output above for errors."
    exit 1
fi

echo ""
echo "============================================"
echo " BUILD SUCCESSFUL"
echo " JAR created: target/handlebars-email-template-testing-app-1.0.jar"
echo "============================================"
echo ""
echo "To run the tester:"
echo "  java -jar target/handlebars-email-template-testing-app-1.0.jar"
echo ""
echo "Place these files in the same folder as the JAR:"
echo "  - template.html"
echo "  - payload.json"
echo "  - customhandlebar.js"
