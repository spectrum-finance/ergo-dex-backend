FROM openjdk:8-jre-slim as builder
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https apt-utils bc dirmngr gnupg && \
    echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends sbt
COPY . /ergo-dex-backend
WORKDIR /ergo-dex-backend
RUN sbt dex-executor/assembly
RUN mv `find . -name DexExecutor-assembly-*.jar` /dex-executor.jar
CMD ["/usr/bin/java", "-jar", "/dex-executor.jar"]

FROM openjdk:8-jre-slim
COPY --from=builder /dex-executor.jar /dex-executor.jar
ENTRYPOINT java -jar /dex-executor.jar $0