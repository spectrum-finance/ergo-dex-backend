FROM openjdk:11-jre-slim as builder
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https apt-utils bc dirmngr gnupg && \
    echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends sbt
COPY ../dex-tracker /ergo-dex-backend
WORKDIR /ergo-dex-backend
RUN sbt dex-tracker/assembly
RUN mv `find . -name UtxoTracker-assembly-*.jar` /dex-tracker.jar
CMD ["/usr/bin/java", "-jar", "/utxo-tracker.jar"]

FROM openjdk:11-jre-slim
COPY --from=builder /utxo-tracker.jar /utxo-tracker.jar
ENTRYPOINT java -jar /utxo-tracker.jar $0