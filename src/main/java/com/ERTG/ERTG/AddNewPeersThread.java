package com.ERTG.ERTG;

import com.ERTG.net.rlpx.NeighborsMessage;
import com.ERTG.net.rlpx.Node;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class AddNewPeersThread implements Runnable {

    private NeighborsMessage message;

    public AddNewPeersThread(NeighborsMessage message) {
        this.message=message;
    }

    @Override
    public void run() {
        try{

            int num = 0;

            for(Node n : message.getNodes()){
                Peer p = new Peer(n.getId(), new InetSocketAddress(InetAddress.getByName(n.getHost()),n.getPort()));
                p.setTimestamp((int)(System.currentTimeMillis() / 1000));

                if (Main.peers.add(p)) {
                    num=num+1;
                }
                Main.peers.addToNewDiscoveries(p);

            }
            Main.stats.getNumAddedPeersInLastExecution().addAndGet(num);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }
}
