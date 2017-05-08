#!/bin/bash
CHANNEL_NAME="mychannel"


echo "Channel name - "$CHANNEL_NAME
echo

#Backup the original configtx.yaml
cp $GOPATH/src/github.com/hyperledger/fabric/common/configtx/tool/configtx.yaml $GOPATH/src/github.com/hyperledger/fabric/common/configtx/tool/configtx.yaml.orig
cp channel/configtx.yaml $GOPATH/src/github.com/hyperledger/fabric/common/configtx/tool/configtx.yaml


PROJECT_DIR=$PWD
cd $GOPATH/src/github.com/hyperledger/fabric/
echo "Building configtxgen"
make configtxgen

echo "Generating genesis block"
./build/bin/configtxgen -profile SampleSingleMSPSolo -outputBlock orderer.block -channelID $CHANNEL_NAME
mv orderer.block $PROJECT_DIR/channel/orderer.block


echo "Generating channel configuration transaction for channel '$CHANNEL_NAME'"
./build/bin/configtxgen -profile SampleSingleMSPSolo -outputCreateChannelTx channel.tx -channelID $CHANNEL_NAME
mv channel.tx $PROJECT_DIR/channel/channel.tx


#reset configtx.yaml file to its original
cp $GOPATH/src/github.com/hyperledger/fabric/common/configtx/tool/configtx.yaml.orig $GOPATH/src/github.com/hyperledger/fabric/common/configtx/tool/configtx.yaml
rm $GOPATH/src/github.com/hyperledger/fabric/common/configtx/tool/configtx.yaml.orig