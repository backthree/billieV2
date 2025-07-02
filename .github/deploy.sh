#!/bin/bash
set -e

# 이 스크립트는 서버에서 실행됩니다.
# 이미지 이름을 직접 사용합니다.

NETWORK="nextdoor-net"
REMOTE_DIR=$(pwd)

# 1) Docker 네트워크 확인/생성
docker network inspect $NETWORK >/dev/null 2>&1 \
  || docker network create $NETWORK

# 2) 의존 서비스 컨테이너 재시작
for svc in cassandra elasticsearch rabbitmq redis; do
  cname=nextdoor-$svc
  docker rm -f $cname || true
done

# Cassandra
docker run -d --name nextdoor-cassandra \
  --network $NETWORK \
  -p 9042:9042 \
  cassandra:4.0

# Elasticsearch
docker run -d --name nextdoor-elasticsearch \
  --network $NETWORK \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  --ulimit memlock=-1:-1 --memory=1g \
  -p 9200:9200 \
  docker.elastic.co/elasticsearch/elasticsearch:8.6.3

# RabbitMQ
docker run -d --name nextdoor-rabbitmq \
  --network $NETWORK \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management

# Redis
docker run -d --name nextdoor-redis \
  --network $NETWORK \
  -p 6379:6379 \
  redis:6.2

echo "Waiting for services to be ready..."
sleep 30

# 3) Cassandra 초기 스키마 적용
cat << 'EOF' > schema.cql
CREATE KEYSPACE IF NOT EXISTS nextdoor_chat
  WITH replication = {'class':'SimpleStrategy','replication_factor':1};
USE nextdoor_chat;
CREATE TABLE IF NOT EXISTS conversation (
  conversation_id uuid PRIMARY KEY,
  participant_ids list<bigint>,
  created_at timestamp
);
CREATE TABLE IF NOT EXISTS chat_messages (
  conversation_id uuid,
  sent_at timestamp,
  message_id uuid,
  sender_id bigint,
  content text,
  is_read boolean,
  PRIMARY KEY ((conversation_id), sent_at, message_id)
) WITH CLUSTERING ORDER BY (sent_at DESC);
CREATE TABLE IF NOT EXISTS unread_counters (
  conversation_id uuid,
  user_id bigint,
  unread_count counter,
  PRIMARY KEY ((conversation_id), user_id)
);
EOF

timeout=60
while ! docker exec nextdoor-cassandra cqlsh -e "DESCRIBE KEYSPACES;" \
      >/dev/null 2>&1; do
  timeout=$((timeout-5))
  if [ $timeout -le 0 ]; then
    echo "Cassandra did not start in time."
    exit 1
  fi
  echo "Waiting for Cassandra..."
  sleep 5
done
docker exec -i nextdoor-cassandra cqlsh < schema.cql
rm schema.cql

# 4) 기존 애플리케이션 종료
pkill -f 'java.*app.jar' || true

# 5) 새 애플리케이션 기동
nohup java -jar \
  -Dspring.profiles.active=prod,secret \
  -Dspring.config.additional-location=./config/ \
  app.jar > app.log 2>&1 &

echo "Deployed at $(date)"