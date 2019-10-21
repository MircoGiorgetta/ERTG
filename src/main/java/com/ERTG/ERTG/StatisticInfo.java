package com.ERTG.ERTG;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StatisticInfo {

    public StatisticInfo() {

    }

    AtomicInteger peerSize = new AtomicInteger(0);
    AtomicInteger bondedPeerSize = new AtomicInteger(0);
    AtomicInteger unbondedPeerSize = new AtomicInteger(0);
    AtomicInteger hashedPeerSize = new AtomicInteger(0);
    AtomicInteger numSentPing = new AtomicInteger(0);
    AtomicInteger numSentPong = new AtomicInteger(0);
    AtomicInteger numSentFindnode = new AtomicInteger(0);
    AtomicInteger numSentGenerationFindnode = new AtomicInteger(0);
    AtomicInteger numSentNeighbors = new AtomicInteger(0);
    AtomicInteger numReceivedPing = new AtomicInteger(0);
    AtomicInteger numReceivedPong = new AtomicInteger(0);
    AtomicInteger numReceivedFindnode = new AtomicInteger(0);
    AtomicInteger numReceivedNeighbors = new AtomicInteger(0);
    AtomicInteger numReceivedGenerationNeighbors = new AtomicInteger(0);
    AtomicInteger numDifferentIdButSameIPandPort = new AtomicInteger(0);
    AtomicInteger numSameIdButDifferentIPandPort = new AtomicInteger(0);
    AtomicInteger numAddedPeersInLastExecution = new AtomicInteger(0);
    AtomicInteger numAddedPeersDuringGeneration = new AtomicInteger(0);
    AtomicInteger numAddedPeersFromBasicCrawler = new AtomicInteger(0);
    AtomicInteger numBootnode = new AtomicInteger(0);
    AtomicInteger numAddedPeersFromPreviousExecution = new AtomicInteger(0);



    public AtomicInteger getPeerSize() {
        return peerSize;
    }

    public AtomicInteger getBondedPeerSize() {
        return bondedPeerSize;
    }

    public AtomicInteger getUnbondedPeerSize() {
        return unbondedPeerSize;
    }

    public AtomicInteger getHashedPeerSize() {
        return hashedPeerSize;
    }

    public AtomicInteger getNumSentPing() {
        return numSentPing;
    }

    public AtomicInteger getNumSentPong() {
        return numSentPong;
    }

    public AtomicInteger getNumSentFindnode() {
        return numSentFindnode;
    }

    public AtomicInteger getNumSentGenerationFindnode() {
        return numSentGenerationFindnode;
    }

    public AtomicInteger getNumSentNeighbors() {
        return numSentNeighbors;
    }

    public AtomicInteger getNumReceivedPing() {
        return numReceivedPing;
    }

    public AtomicInteger getNumReceivedPong() {
        return numReceivedPong;
    }

    public AtomicInteger getNumReceivedFindnode() {
        return numReceivedFindnode;
    }

    public AtomicInteger getNumReceivedNeighbors() {
        return numReceivedNeighbors;
    }

    public AtomicInteger getNumReceivedGenerationNeighbors() {
        return numReceivedGenerationNeighbors;
    }

    public AtomicInteger getNumDifferentIdButSameIPandPort() {
        return numDifferentIdButSameIPandPort;
    }


    public AtomicInteger getNumSameIdButDifferentIPandPort() {
        return numSameIdButDifferentIPandPort;
    }


    public AtomicInteger getNumAddedPeersInLastExecution() {
        return numAddedPeersInLastExecution;
    }


    public AtomicInteger getNumAddedPeersFromBasicCrawler() {
        return numAddedPeersFromBasicCrawler;
    }


    public AtomicInteger getNumBootnode() {
        return numBootnode;
    }


    public AtomicInteger getNumAddedPeersDuringGeneration() {
        return numAddedPeersDuringGeneration;
    }


    public AtomicInteger getNumAddedPeersFromPreviousExecution() {
        return numAddedPeersFromPreviousExecution;
    }



    public void print() {
        try {
            File f = new File("Statistic");

            if (!f.exists())
                f.createNewFile();

            try(FileWriter fr = new FileWriter(f);
                BufferedWriter out = new BufferedWriter(fr)){
                out.write("Peers Size: "+peerSize.get()+"\n");
                out.write("Bonded Peers Size: "+bondedPeerSize.get()+"\n");
                out.write("Unbonded Peers Size: "+unbondedPeerSize.get()+"\n");
                out.write("Hashed Peers Size: "+hashedPeerSize.get()+"\n");
                out.write("Sent Ping Number: "+numSentPing.get()+"\n");
                out.write("Sent Pong Number: "+numSentPong.get()+"\n");
                out.write("Sent Findnode Number: "+numSentFindnode.get()+"\n");
                out.write("Sent Findnode Generation Number: "+numSentGenerationFindnode.get()+"\n");
                out.write("Sent Neighbors Number: "+numSentNeighbors.get()+"\n");
                out.write("Received Ping Number: "+numReceivedPing.get()+"\n");
                out.write("Received Pong Number: "+numReceivedPong.get()+"\n");
                out.write("Received Findnode Number: "+numReceivedFindnode.get()+"\n");
                out.write("Received Neighbors Number: "+numReceivedNeighbors.get()+"\n");
                out.write("Received Neighbors Generation Number: "+numReceivedGenerationNeighbors.get()+"\n");
                out.write("Number of case peer with different Id but same Ip and Port: "+numDifferentIdButSameIPandPort.get()+"\n");
                out.write("Number of case peer with same Id but different Ip and Port: "+numSameIdButDifferentIPandPort.get()+"\n");
                out.write("Number of added bootnode peers: "+numBootnode.get()+"\n");
                out.write("Number of added peers from the Basic Crawler: "+numAddedPeersFromBasicCrawler.get()+"\n");
                out.write("Number of added peers from previous executions of the program: "+numAddedPeersFromPreviousExecution.get()+"\n");
                out.write("Number of added peers during the last execution of the program from the discovery: "+numAddedPeersInLastExecution.get()+"\n");
                out.write("Number of added peers during the generation of a routing table: "+numAddedPeersDuringGeneration.get()+"\n");


            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }










}
