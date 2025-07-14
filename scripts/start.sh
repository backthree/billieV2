#!/bin/bash
cd /home/ubuntu/app

SPRING_DATASOURCE_URL=$(aws ssm get-parameter --name "/billie/prod/SPRING_DATASOURCE_URL" --with-decryption --query "Parameter.Value" --output text)
SPRING_DATASOURCE_USERNAME=$(aws ssm get-parameter --name "/billie/prod/SPRING_DATASOURCE_USERNAME" --with-decryption --query "Parameter.Value" --output text)
SPRING_DATASOURCE_PASSWORD=$(aws ssm get-parameter --name "/billie/prod/SPRING_DATASOURCE_PASSWORD" --with-decryption --query "Parameter.Value" --output text)
SPRING_DATA_MONGODB_URI=$(aws ssm get-parameter --name "/billie/prod/SPRING_DATA_MONGODB_URI" --with-decryption --query "Parameter.Value" --output text)
SPRING_DATA_REDIS_HOST=$(aws ssm get-parameter --name "/billie/prod/SPRING_DATA_REDIS_HOST" --with-decryption --query "Parameter.Value" --output text)
SPRING_ELASTICSEARCH_URIS=$(aws ssm get-parameter --name "/billie/prod/SPRING_ELASTICSEARCH_URIS" --with-decryption --query "Parameter.Value" --output text)

cat > .env << EOF
SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
SPRING_DATA_MONGODB_URI=${SPRING_DATA_MONGODB_URI}
SPRING_DATA_REDIS_HOST=${SPRING_DATA_REDIS_HOST}
SPRING_ELASTICSEARCH_URIS=${SPRING_ELASTICSEARCH_URIS}
EOF

docker pull choijake/billie-app:latest
docker compose up -d