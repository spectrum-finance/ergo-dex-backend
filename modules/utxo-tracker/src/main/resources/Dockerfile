FROM mozilla/sbt AS builder
USER root
WORKDIR /builder
ADD . /builder
RUN sbt utxo-tracker/assembly && \
    mv /builder/modules/utxo-tracker/target/scala-*/UtxoTracker-assembly-*.jar ./UtxoTracker.jar

from adoptopenjdk:11-jre
WORKDIR /app
COPY --from=builder /builder/UtxoTracker.jar .
CMD ["java", "-jar", "UtxoTracker.jar"]