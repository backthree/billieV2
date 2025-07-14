#!/bin/bash
cd /home/ubuntu/app

if [ -f "docker-compose.yml" ]; then
    docker-compose down
fi