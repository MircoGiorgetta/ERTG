package com.ERTG.ERTG;

import java.io.*;

public class RoutingTable {

    private StatisticRoutingTable stats;
    private Bucket[] table;
    private Peer peerOwner;
    private int size=0;
    private int numberOfGenerationTried = 0;



    public RoutingTable(Peer p) {
        stats=new StatisticRoutingTable();
        peerOwner=p;
        table = new Bucket[EthConstants.ROUTING_TABLE_SIZE];
        for (int i = 0; i < EthConstants.ROUTING_TABLE_SIZE; i++)
        {
            table[i] = new Bucket();
        }
    }


    public synchronized void add(Peer p) {
        int position = firstHashBitDifferent(peerOwner, p);
        if (position==-1) {
            return;
        }
        if (table[position].add(p)) {
            size++;
        } else {
            stats.getNumPeerArlreadyInsideRoutingTable().incrementAndGet();
        }
    }

    public Peer getPeerOwner() {
        return peerOwner;
    }

    public StatisticRoutingTable getStats() {
        return stats;
    }


    public int getNumberOfGenerationTried() {
        return numberOfGenerationTried;
    }

    public void setNumberOfGenerationTried(int numberOfGenerationTried) {
        this.numberOfGenerationTried = numberOfGenerationTried;
    }



    public int size() {
        return size;
    }



    public synchronized void printDebug() {
        try {

            File f = new File(Main.routingTablesFolder+"/"+peerOwner.getIdStringHex()+"/RoutingTablePeerDebug.dat");
            File f2 = new File(Main.routingTablesFolder+"/"+peerOwner.getIdStringHex()+"/RoutingTablePeerHashDebug.dat");
            if (!f.exists()) {
                f.createNewFile();
            }
            if (!f2.exists()) {
                f2.createNewFile();
            }
            try(FileWriter fr = new FileWriter(f);
                BufferedWriter out = new BufferedWriter(fr);
                FileWriter fr2 = new FileWriter(f2);
                BufferedWriter out2 = new BufferedWriter(fr2)){
                out.write("Size: "+size+"\n");
                out2.write("Size: "+size+"\n");

                for (int i=0; i<EthConstants.ROUTING_TABLE_SIZE; i++) {
                    Bucket b = table[i];
                    b.printDebug(out);
                    b.printHashDebug(out2);
                    out.flush();
                    out2.flush();
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized void print() {
        try {
            File f = new File(Main.routingTablesFolder+"/"+peerOwner.getIdStringHex()+"/RoutingTablePeer.dat");

                if (!f.exists()) {
                    f.createNewFile();
                }


                try(FileWriter fr = new FileWriter(f);
                        BufferedWriter out = new BufferedWriter(fr)){
                    out.write("Owner Peer Id Hex: "+peerOwner.getIdStringHex()+"\n");
                    out.write("Owner Peer Hash: "+peerOwner.getHash256String()+"\n");
                    out.write("Size: "+size+"\n");
                    out.write("Number of findnode sent to generate the routing table: "+ stats.getNumSentGenerationFindnode().get() +"\n\n");

                    for (int i=0; i<EthConstants.ROUTING_TABLE_SIZE; i++) {
                        out.write("Bucket: "+i+"\n");
                        Bucket b = table[i];
                        out.write("size: "+b.size()+"\n");
                        b.print(out);
                        out.write("\n");
                        out.flush();
                    }

                }

                stats.print(peerOwner, size);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    //RETURN THE POSITION WHERE THE FIRST BIT DIFFERENT IS FOUND
    //IF THE BYTE ARRAYS ARE THE SAME -1 IS RETURNED
    //THE CHECK IS MADE FROM LEFT TO RIGHT
    public static int firstHashBitDifferent(Peer owner, Peer p) {
        String ownerHash = owner.getHash256String();
        String hash = p.getHash256String();

        for (int i = 0; i<EthConstants.ROUTING_TABLE_SIZE; i++) {
            if (ownerHash.charAt(i)!=hash.charAt(i)) {
                return i;
            }
        }
        return -1;

    }




}
