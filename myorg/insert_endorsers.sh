#!/usr/bin/env bash

ENDORSERS=$1

: ${ENDORSERS:="1"}

echo "inserting $ENDORSERS in docker-compose.yaml"

for (( i = 1; $i <= $ENDORSERS; i++ ))
do
    printf '  peer'$i':
    container_name: peer'$i'
    extends:
        file: peer-base/peer-base.yaml
        service: peer-base
    cpuset: "'$i'"
    environment:
      - CORE_PEER_ID=peer'$i'
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer1:7051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer'$i':7051
    volumes:
        - /var/run/:/host/var/run/
        - ./crypto-config/peerOrganizations/peerOrg1/peers/peerOrg1Peer'$i'/:/var/hyperledger/msp/peer
    ports:
      - %d:7051
      - %d:7053
    depends_on:
      - orderer\n' $((6051+($i*1000))) $((6053+($i*1000))) >> test.yaml
      for ((j = 1; $j < $i; j++))
      do
        printf '      - peer'$j'\n' >> test.yaml
      done
      printf '\n' >> test.yaml
done

echo "done inserting endorsers in docker-compose.yaml"