FROM mozilla/sbt AS builder
USER root
WORKDIR /builder
ADD . /builder
RUN export SBT_OPTS="-Xmx2G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xss2M  -Duser.timezone=GMT" && \
    sbt pool-resolver/assembly && \
    mv /builder/modules/pool-resolver/target/scala-*/PoolResolver-assembly-*.jar ./PoolResolver.jar

from adoptopenjdk:11-jre
WORKDIR /app
COPY --from=builder /builder/PoolResolver.jar .
CMD ["java", "-jar", "PoolResolver.jar"]