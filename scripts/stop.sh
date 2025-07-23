#!/bin/bash
cd /home/ubuntu/app

if [ -f "docker-compose.yml" ]; then
    docker compose down
    docker rmi choijake/billie-app:latest 2>/dev/null || true
fi