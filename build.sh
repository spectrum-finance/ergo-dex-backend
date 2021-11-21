git pull
git checkout $1
sbt docker:stage
export DEX_SOURCES_PATH=${PWD}
docker-compose build $2