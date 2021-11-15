FROM mozilla/sbt AS builder
USER root
WORKDIR /builder
ADD . /builder
RUN export SBT_OPTS="-Xmx2G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xss2M  -Duser.timezone=GMT" && \
    sbt amm-executor/assembly && \
    mv /builder/modules/amm-executor/target/scala-*/AmmExecutor-assembly-*.jar ./AmmExecutor.jar

from adoptopenjdk:11-jre
WORKDIR /app
COPY --from=builder /builder/AmmExecutor.jar .
CMD ["java", "-jar", "AmmExecutor.jar"]