git pull
git checkout $1
sbt docker:stage
$env:DEX_SOURCES_PATH=$PSScriptRoot
docker compose build $2