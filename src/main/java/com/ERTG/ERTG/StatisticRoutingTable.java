package com.ERTG.ERTG;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class StatisticRoutingTable {

    public StatisticRoutingTable() {

    }

    public ConcurrentLinkedQueue<String> generationMessagesSent = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<String> generationMessagesReceived = new ConcurrentLinkedQueue<>();

    AtomicInteger numSentGenerationFindnode = new AtomicInteger(0);
    AtomicInteger numReceivedGenerationNeighbors = new AtomicInteger(0);
    AtomicInteger numPeerArlreadyInsideRoutingTable = new AtomicInteger(0);
    AtomicInteger numPeerReceivedDuringGeneration = new AtomicInteger(0);

    public AtomicInteger getNumSentGenerationFindnode() {
        return numSentGenerationFindnode;
    }

    public AtomicInteger getNumReceivedGenerationNeighbors() {
        return numReceivedGenerationNeighbors;
    }

    public AtomicInteger getNumPeerArlreadyInsideRoutingTable() {
        return numPeerArlreadyInsideRoutingTable;
    }


    public AtomicInteger getNumPeerReceivedDuringGeneration() {
        return numPeerReceivedDuringGeneration;
    }

    public boolean addToMessagesSent(String str) {
        return generationMessagesSent.add(str);
    }

    public boolean addToMessagesReceived(String str) {
        return generationMessagesReceived.add(str);
    }

    public void print(Peer peerOwner, int size) {
        try {
            File f = new File(Main.routingTablesFolder+"/"+peerOwner.getIdStringHex()+"/StatsTable.dat");

            if (!f.exists())
                f.createNewFile();

            try(FileWriter fr = new FileWriter(f);
                BufferedWriter out = new BufferedWriter(fr)){
                out.write("Sent Generation Findnode Number: "+numSentGenerationFindnode.get()+"\n");
                out.write("Received Generation Neighbors Number: "+numReceivedGenerationNeighbors.get()+"\n");
                out.write("Number of peer received already in the routing table during generation: "+numPeerArlreadyInsideRoutingTable.get()+"\n");
                out.write("Number of peer received during generation: "+numPeerReceivedDuringGeneration.get()+"\n");
                out.write("Size: "+size+"\n");
                out.write("\n\n\n\n\n");


                out.write("ID DESTINATARIO: "+peerOwner.getIdStringHex()+"\n");
                out.write("ID HASH BIT DESTINATARIO: "+peerOwner.getHash256String()+"\n");
                out.write("INDIRIZZO DESTINATARIO: "+peerOwner.getAddressPort()+"\n");


                out.write("--------------------------------------------------------------\n");
                for (String s : generationMessagesSent) {
                    out.write(s+"\n");
                }
                out.write("--------------------------------------------------------------\n");
                out.flush();


                for (String s : generationMessagesReceived) {
                    out.write(s);
                }
                out.flush();



            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
