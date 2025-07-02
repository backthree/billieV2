#!/bin/bash
set -e

NETWORK="nextdoor-net"
REMOTE_DIR=$(pwd)

ES_IMAGE="docker.elastic.co/elasticsearch/elasticsearch:8.6.3"
CASSANDRA_IMAGE="cassandra:4.0"
RABBITMQ_IMAGE="rabbitmq:3-management"
REDIS_IMAGE="redis:6.2"

# 1) Docker 네트워크 확인/생성
docker network inspect "$NETWORK" >/dev/null 2>&1 \
  || docker network create "$NETWORK"

# 2) 의존 서비스 컨테이너 재시작
for svc in cassandra elasticsearch rabbitmq redis; do
  docker rm -f nextdoor-"$svc" || true
done

# 3-1) Cassandra
docker run -d --name nextdoor-cassandra \
  --network "$NETWORK" \
  -p 9042:9042 \
  "$CASSANDRA_IMAGE"

# 3-2) Elasticsearch
docker run -d --name nextdoor-elasticsearch \
  --network "$NETWORK" \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  --ulimit memlock=-1:-1 --memory=1g \
  -p 9200:9200 \
  "$ES_IMAGE"

# 3-3) RabbitMQ
docker run -d --name nextdoor-rabbitmq \
  --network "$NETWORK" \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  -p 5672:5672 -p 15672:15672 \
  "$RABBITMQ_IMAGE"

# 3-4) Redis
docker run -d --name nextdoor-redis \
  --network "$NETWORK" \
  -p 6379:6379 \
  "$REDIS_IMAGE"

echo "Waiting for services to be ready..."
sleep 30

# 4) Cassandra 초기 스키마 적용
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

# 5) 기존 애플리케이션 종료
pkill -f 'java.*app.jar' || true

# 6) 새 애플리케이션 기동
nohup java -jar \
  -Dspring.profiles.active=prod,secret \
  -Dspring.config.additional-location=./config/ \
  app.jar > app.log 2>&1 &

echo "Deployed at $(date)"