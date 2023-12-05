#/bin/bash

/opt/bitnami/kafka/bin/kafka-topics.sh --create --topic mempool-tx-topic --bootstrap-server kafka:9092
/opt/bitnami/kafka/bin/kafka-topics.sh --create --topic ledger-tx-topic --bootstrap-server kafka:9092
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
