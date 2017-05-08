package se.umu.c12msr.fabricbenchmark;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.events.BlockListener;
import org.hyperledger.fabric.sdk.events.EventHub;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

public class Payment {

    private static final Log logger = LogFactory.getLog(Payment.class);


    static final String CHAIN_CODE_NAME = "payment.go";
    static final String CHAIN_CODE_PATH = "github.com/c12msr/fabrictest/main";
    static final String CHAIN_CODE_VERSION = "1.0";


    static final String MY_CHAIN = "mychannel";

    String testTxID = null ;  // save the CC invoke TxID and use in queries


    final static Collection<String> PEER_LOCATIONS = Arrays.asList("grpc://localhost:7051", "grpc://localhost:8051"
            , "grpc://localhost:9051", "grpc://localhost:10051", "grpc://localhost:11051", "grpc://localhost:12051"
            , "grpc://localhost:13051", "grpc://localhost:14051", "grpc://localhost:15051", "grpc://localhost:16051"
            , "grpc://localhost:17051", "grpc://localhost:18051", "grpc://localhost:19051", "grpc://localhost:20051"
            , "grpc://localhost:21051");


    final static Collection<String> ORDERER_LOCATIONS = Arrays.asList("grpc://localhost:7050");

    final static Collection<String> EVENTHUB_LOCATIONS = Arrays.asList("grpc://localhost:7053");

    final static String FABRIC_CA_SERVICES_LOCATION = "http://localhost:7054";


    private HFClient client;
    private Chain chain;
    private Collection<Peer> peers;
    private Collection<Orderer> orderers;
    private ChainCodeID chainCodeID;

    public Payment(boolean constructChain) throws Exception {


        ////////////////////////////
        // Setup client

        //Create instance of client.
        client = HFClient.createNewInstance();

        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        HFCAClient fabricCA = new HFCAClient(FABRIC_CA_SERVICES_LOCATION, null);
        client.setMemberServices(fabricCA);

        ////////////////////////////
        //Set up USERS

        //Persistence is not part of SDK. Sample file store is for demonstration purposes only!
        //   MUST be replaced with more robust application implementation  (Database, LDAP)
        File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }

        final SampleStore sampleStore = new SampleStore(sampleStoreFile);
        sampleStoreFile.deleteOnExit();

        //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface
        SampleUser admin = sampleStore.getMember("admin");
        if(!admin.isEnrolled()){  //Preregistered admin only needs to be enrolled with Fabric CA.
            admin.setEnrollment(fabricCA.enroll(admin.getName(), "adminpw"));
            admin.setMPSID("Org1MSP"); //This is out of band information provided by the user's registration organisation
        }

        client.setUserContext(admin);
        if (constructChain) {
            chain = constructChain(MY_CHAIN, client);
        } else {
            chain = reconstructChain(MY_CHAIN, client);
        }

        final String chainName = chain.getName();
        logger.info("Running Chain " + chainName);
        chain.setInvokeWaitTime(100000);
        chain.setDeployWaitTime(120000);
        chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();

        peers = chain.getPeers();
        orderers = chain.getOrderers();
    }



    public void install() throws Exception {
        ////////////////////////////
        // Install Proposal Request
        //

        //out("Creating install proposal");
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        Collection<ProposalResponse> responses;

        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chainCodeID);
        installProposalRequest.setChaincodeSourceLocation(new File("/home/mattiasscherer/Documents/gostuff"));

        responses = chain.sendInstallProposal(installProposalRequest, peers);


        for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);

            } else {
                failed.add(response);
            }

        }
        //out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());

        if (successful.size() < 1) { // TODO choose this as an arbitrary limit right now.
            if (failed.size() == 0) {
                throw new Exception("No endorsers found for CC instantiate proposal");
            }
            ProposalResponse first = failed.iterator().next();
            throw new Exception("Not enough endorsers for instantiate  :" + successful.size() + ".  " + first.getMessage() + ". Was verified:" + first.isVerified());
        }
    }


    public CompletableFuture<BlockEvent.TransactionEvent> init(String[] args) throws Exception {
        //out("Creating Instantiate proposal");
        ///////////////
        //// Instantiate chain code.

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        Collection<ProposalResponse> responses;

        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();

        instantiateProposalRequest.setChaincodeID(chainCodeID);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setArgs(args);


        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy(new File("myorg/policy/policyBits"));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);


        responses = chain.sendInstantiationProposal(instantiateProposalRequest, peers);



        for (ProposalResponse response : responses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
            } else {
                failed.add(response);
            }
        }
        //out("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());

        if (successful.size() < 1) { // TODO choose this as an arbitrary limit right now.
            if (failed.size() == 0) {
                throw new Exception("No endorsers found for CC instantiate proposal");
            }
            ProposalResponse first = failed.iterator().next();

            throw new Exception("Not enough endorsers for instantiate  :" + successful.size() + ".  " + first.getMessage() + ". Was verified:" + first.isVerified());
        }
        return chain.sendTransaction(successful, orderers);
    }



    public CompletableFuture<BlockEvent.TransactionEvent> invoke(String[] args) throws Exception {
        //out("Creating 'pay' proposal");

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        InvokeProposalRequest invokeProposalRequest = client.newInvokeProposalRequest();

        invokeProposalRequest.setChaincodeID(chainCodeID);
        invokeProposalRequest.setFcn("invoke");
        invokeProposalRequest.setArgs(args);

        Collection<ProposalResponse> invokePropResp = chain.sendInvokeProposal(invokeProposalRequest, peers);



        for (ProposalResponse response : invokePropResp) {

            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
            } else {
                failed.add(response);
            }

        }
        //out("Received %d invoke proposal responses. Successful+verified: %d . Failed: %d",
         //       invokePropResp.size(), successful.size(), failed.size());


        if (successful.size() < 1) {  //choose this as an arbitrary limit right now.

            if (failed.size() == 0) {
                throw new Exception("No endorsers found ");

            }
            ProposalResponse firstInvokeProposalResponse = failed.iterator().next();


            throw new Exception("Not enough endorsers :" + successful.size() + ".  " +
                    firstInvokeProposalResponse.getMessage() +
                    ". Was verified: " + firstInvokeProposalResponse.isVerified());


        }
        //out("Successfully received 'pay' proposal response.");


        //out("Invoking chain code transaction 'pay' %s from %s to %s.", args[3], args[1], args[2]);

        return chain.sendTransaction(successful, orderers);
    }

    public String query(String[] args) throws Exception {
        //out("Creating query proposal");


        // InvokeProposalRequest qr = InvokeProposalRequest.newInstance();
        QueryProposalRequest queryProposalRequest = client.newQueryProposalRequest();

        queryProposalRequest.setArgs(args);
        queryProposalRequest.setFcn("invoke");
        queryProposalRequest.setChaincodeID(chainCodeID);


        Collection<ProposalResponse> queryProposals = chain.sendQueryProposal(queryProposalRequest, peers);

        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                throw new Exception("Failed invoke proposal.  status: " + proposalResponse.getStatus() +
                        ". messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());

            }

        }

        //out("Successfully received query response.");

        String payload = queryProposals.iterator().next().getProposalResponse().getResponse().getPayload().toStringUtf8();

        //out("Query payload of '%s' returned '%s'", args[1], payload);

        return payload;
    }

    public void startBlockListener() {
        logger.info("Starting blocklistener");
        chain.registerBlockListener(new BlockListener() {
            @Override
            public void received(BlockEvent blockEvent) {
                logger.info("Received BlockEvent channelId: " + blockEvent.getChannelID() + "\nNumber of transactionEvent: " + blockEvent.getTransactionEvents().size());
            }
        });
    }






    private static Chain constructChain(String name, HFClient client) throws Exception {
        //////////////////////////// TODo Needs to be made out of bounds and here chain just retrieved
        //Construct the chain
        //


        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderloc : ORDERER_LOCATIONS) {
            orderers.add(client.newOrderer(orderloc));

        }

        //Just pick the first order in the list to create the chain.

        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        ChainConfiguration chainConfiguration = new ChainConfiguration(new File("myorg/channel/channel.tx"));

        Chain newChain = client.newChain(name, anOrderer, chainConfiguration);

        int i = 0;
        for (String peerloc : PEER_LOCATIONS) {
            Peer peer = client.newPeer(peerloc);
            peer.setName("peer_" + i);
            newChain.joinPeer(peer); // have Peers join the chain
        }

        for (String orderloc : ORDERER_LOCATIONS) {
            Orderer orderer = client.newOrderer(orderloc);
            newChain.addOrderer(orderer);
        }

        for (String eventHubLoc : EVENTHUB_LOCATIONS) {
            EventHub eventHub = client.newEventHub(eventHubLoc);
            newChain.addEventHub(eventHub);
        }


        newChain.initialize();
        return newChain;

    }

    /**
     * Sample how to reconstruct chain
     * @param name
     * @param client
     * @return
     * @throws Exception
     */
    private static Chain reconstructChain(String  name, HFClient client) throws Exception {

        //Construct the chain
        //
        Chain newChain = client.newChain(name);

        int i = 0;
        for (String peerloc : PEER_LOCATIONS) {
            Peer peer = client.newPeer(peerloc);
            peer.setName("peer_" + i);
            newChain.addPeer(peer);

        }

        for (String orderloc : ORDERER_LOCATIONS) {
            Orderer orderer = client.newOrderer(orderloc);
            newChain.addOrderer(orderer);
        }

        for (String eventHubLoc : EVENTHUB_LOCATIONS) {
            EventHub eventHub = client.newEventHub(eventHubLoc);
            newChain.addEventHub(eventHub);
        }

        newChain.initialize();
        return newChain;

    }


    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(String.format(format, args));
        System.err.flush();
        System.out.flush();

    }
}
