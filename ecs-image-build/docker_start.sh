#!/bin/bash
#
# Start script for authentication-service

PORT=8080

exec java -jar -Dserver.port="${PORT}" "authentication-service.jar"