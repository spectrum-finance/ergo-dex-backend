# ErgoDEX off-chain services

A set of off-chain services facilitating ErgoDEX functioning.

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

<<<<<<< HEAD
## Building & Running the off-chain services

### Prerequisites
=======
##Building & Running the off-chain services

###Prerequisites
>>>>>>> e0d9dc36597247943c446402172442c5c02173fd
The services require access to an Ergo node, so if you do not have one yet install as instructed here: [Ergo github](https://github.com/ergoplatform/ergo)
Besides the node the services depend on tools such as Kafka and Redis to run, to make it easier to manage a docker based solution has been made to allow for easy building and running of the services.
The only requirements are that you have Docker and Docker-compose (included in Docker for Windows) installed on your system.

<<<<<<< HEAD
### Building
=======
###Building
>>>>>>> e0d9dc36597247943c446402172442c5c02173fd
First you need to download the code from this repo. The easiest way to keep it updated in the future is by using git:
```
cd <the folder you want to keep the off-chain services code in>
git clone https://github.com/ergolabs/ergo-dex-backend.git
```
Instructions for building the services are all combined in the docker-compose.yml file. The only configuration needed for running the services need to be stored in a file called config.env. An example can be found in config-example.env
Make a copy of the example file, name it config.env and edit the file to match your values:
```
cd ergo-dex-backend
cp ./config-example.env ./config.env
```
The 2 values that need to be changed in the config.env file are the address you want to recieve fees on and the URI to your node.
Finally the Docker images need to be build before running them:
Windows:
```
docker compose build
```
Linux:
```
docker-compose build
```
<<<<<<< HEAD
### Running the services
=======
###Running the services
>>>>>>> e0d9dc36597247943c446402172442c5c02173fd
Once the Docker images are built the only thing left to do is to run them:
Windows:
```
docker compose up -d
```
Linux:
```
docker-compose up -d
```
<<<<<<< HEAD
#### Verifying the services are running correctly
=======
####Verifying the services are running correctly
>>>>>>> e0d9dc36597247943c446402172442c5c02173fd
You can look into the logs of the services to ensure they are running correctly. For example to look into the logs of the aamexecutor you can use the following command:
```
docker logs ammexecutor -f
```
