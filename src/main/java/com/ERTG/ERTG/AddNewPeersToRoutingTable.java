package com.ERTG.ERTG;

import com.ERTG.net.rlpx.NeighborsMessage;
import com.ERTG.net.rlpx.Node;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class AddNewPeersToRoutingTable implements Runnable {

    private NeighborsMessage message;
    private Peer owner;

    public AddNewPeersToRoutingTable(NeighborsMessage message, Peer p) {
        this.message=message;
        this.owner=p;
    }




    @Override
    public void run() {


        RoutingTable table = Main.routingTableSet.get(owner.getIdStringHex());
        table.getStats().getNumReceivedGenerationNeighbors().getAndIncrement();

        try{

            StringBuilder sb=new StringBuilder("");
            sb.append("*******************************************************\n");

            int c = 0;
            for(Node n : message.getNodes()){
                Peer p = new Peer(n.getId(),new InetSocketAddress(InetAddress.getByName(n.getHost()),n.getPort()));
                p.setTimestamp((int)(System.currentTimeMillis() / 1000));
                sb.append("NODO RICEVUTO: \n\tIDHEX: "+p.getIdStringHex()+"\n\tHASH256: "+p.getHash256String().substring(0,20)+"\n\tBIT UGUALI HASH: "+RoutingTable.firstHashBitDifferent(table.getPeerOwner(),p)+"\n");
                c++;
                if (Main.peers.add(p)) {
                    Main.stats.getNumAddedPeersDuringGeneration().incrementAndGet();
                }

                table.add(p);

            }
            Main.terminalStandardOutput.println("NODI CONTENUTI NEL MESSAGGIO: "+c);//

            table.getStats().getNumPeerReceivedDuringGeneration().addAndGet(c);
            table.getStats().addToMessagesReceived(sb.toString());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
