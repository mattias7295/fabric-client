#!/bin/bash

UP_DOWN=$1
CH_NAME="mychannel"
CHANNELS=1
CHAINCODES=1
ENDORSERS=$2

: ${ENDORSERS:="2"}

COMPOSE_FILE=docker-compose.yaml

function printHelp () {
	echo "Usage: ./network_setup <up|down|restart> [channel-name] [total-channels [chaincodes] [endorsers count]"
}

function validateArgs () {
	if [ -z "${UP_DOWN}" ]; then
		echo "Option up / down / restart not mentioned"
		printHelp
		exit 1
	fi
}

function clearContainers () {
        CONTAINER_IDS=$(docker ps -aq)
        if [ -z "$CONTAINER_IDS" -o "$CONTAINER_IDS" = " " ]; then
                echo "---- No containers available for deletion ----"
        else
                docker rm -f $CONTAINER_IDS
        fi
}

function removeUnwantedImages() {
        DOCKER_IMAGE_IDS=$(docker images | grep "dev\|none\|test-vp\|peer[0-9]-" | awk '{print $3}')
        if [ -z "$DOCKER_IMAGE_IDS" -o "$DOCKER_IMAGE_IDS" = " " ]; then
                echo "---- No images available for deletion ----"
        else
                docker rmi -f $DOCKER_IMAGE_IDS
        fi
}

function networkUp () {
	CURRENT_DIR=$PWD
        source generateCfgTrx.sh
	cd $CURRENT_DIR

	CHANNEL_NAME=$CH_NAME CHANNELS_NUM=$CHANNELS CHAINCODES_NUM=$CHAINCODES ENDORSERS_NUM=$ENDORSERS docker-compose -f $COMPOSE_FILE up -d 2>&1
	if [ $? -ne 0 ]; then
		echo "ERROR !!!! Unable to pull the images "
		exit 1
	fi
}

function networkDown () {
        docker-compose -f $COMPOSE_FILE down
        #Cleanup the chaincode containers
	clearContainers
	#Cleanup images
	removeUnwantedImages
        #remove orderer and config txn
        rm -rf $PWD/channel/orderer.block
        rm -rf $PWD/channel/channel*.tx
}

validateArgs

#Create the network using docker compose
if [ "${UP_DOWN}" == "up" ]; then
	networkUp
elif [ "${UP_DOWN}" == "down" ]; then ## Clear the network
	networkDown
elif [ "${UP_DOWN}" == "restart" ]; then ## Restart the network
	networkDown
	networkUp
else
	printHelp
	exit 1
fi
