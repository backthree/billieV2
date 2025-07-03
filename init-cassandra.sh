#!/bin/bash

until cqlsh -e "describe keyspaces"; do
  echo "Waiting for Cassandra to be ready..."
  sleep 5
done

echo "Cassandra is up - executing schema setup"

# CQL 스크립트 실행
cqlsh -e "
CREATE KEYSPACE IF NOT EXISTS nextdoor_chat WITH replication = {'class':'SimpleStrategy','replication_factor':1};
USE nextdoor_chat;
CREATE TABLE IF NOT EXISTS conversation (conversation_id uuid PRIMARY KEY, participant_ids list<bigint>, created_at timestamp);
CREATE TABLE IF NOT EXISTS chat_messages (conversation_id uuid, sent_at timestamp, message_id uuid, sender_id bigint, content text, is_read boolean, PRIMARY KEY ((conversation_id), sent_at, message_id)) WITH CLUSTERING ORDER BY (sent_at DESC);
CREATE TABLE IF NOT EXISTS unread_counters (conversation_id uuid, user_id bigint, unread_count counter, PRIMARY KEY ((conversation_id), user_id));
"