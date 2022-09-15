# Spectrum.DEX off-chain services

A set of off-chain services facilitating Spectrum.DEX functioning.

AMM DEX services:
 - UTXO Tracker - extracts AMM orders and pool state updates from the UTXO feed
 - AMM Executor - executes AMM orders into a transaction chain
 - Pool Resolver - tracks pool updates

![AMM Services](docs/AMM_Backend.jpg)

OrderBook DEX services:
 - UTXO Tracker - extracts orders from the UTXO feed
 - Matcher - order-book matching engine
 - Orders Executor - executes orders
 - Markets API - aggregates market data and provides a convenient API to access it

## Building & Running the off-chain services

### Prerequisites
The services require access to an Ergo node, so if you do not have one yet install as instructed here: [Ergo github](https://github.com/ergoplatform/ergo)
Besides the node the services depend on tools such as Kafka and Redis to run, to make it easier to manage a docker based solution has been made to allow for easy building and running of the services.
The only requirements besides the node are that you have the following installed:
 - GIT to download the code and help fetch updates. [GIT](https://git-scm.com/)
 - SBT (which requires Java) for building the bots. [SBT](https://www.scala-sbt.org/index.html)
 - Docker and Docker-compose (included in Docker for Windows). [Docker](https://www.docker.com/get-started)

### Building
First you need to download the code from this repo. The easiest way to keep it updated in the future is by using git:
```
cd <the folder you want to keep the off-chain services code in>
git clone https://github.com/ergolabs/ergo-dex-backend.git
```
Instructions for building the services are all combined in the build script and the docker-compose.yml file. The only configuration needed for running the services need to be stored in a file called config.env. An example can be found in config-example.env
Make a copy of the example file, name it config.env and edit the file to match your values:
```
cd ergo-dex-backend
cp ./config-example.env ./config.env
```
The 2 values that need to be changed in the config.env file are the address you want to recieve fees on and the URI to your node (localhost/127.0.0.1 might not be accessible from within a docker container, it is best to use the local lan ip if the node is running on the same host).
Finally the Docker images need to be build before running them:
```
./build
```
### Running the services
Once the Docker images are built the only thing left to do is to run them:
```
./run
```
#### Verifying the services are running correctly
You can look into the logs of the services to ensure they are running correctly. To look at a combined log for all services use the following command:
Windows:
```
cd ergo-dex-backend
docker compose logs -f
```
Linux:
```
cd ergo-dex-backend
sudo docker-compose logs -f
```
