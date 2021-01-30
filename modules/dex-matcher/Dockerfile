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
RUN sbt dex-matcher/assembly
RUN mv `find . -name DexMatcher-assembly-*.jar` /dex-matcher.jar
CMD ["/usr/bin/java", "-jar", "/dex-matcher.jar"]

FROM openjdk:8-jre-slim
COPY --from=builder /dex-matcher.jar /dex-matcher.jar
ENTRYPOINT java -jar /dex-matcher.jar $0