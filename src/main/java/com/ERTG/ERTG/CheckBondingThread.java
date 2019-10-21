package com.ERTG.ERTG;

import java.net.InetSocketAddress;

public class CheckBondingThread implements Runnable {

    InetSocketAddress indirizzo;

    public CheckBondingThread(InetSocketAddress indirizzo) {

        this.indirizzo=new InetSocketAddress(indirizzo.getAddress().getHostAddress(), indirizzo.getPort());
    }

    @Override
    public void run() {

        Peer p = Main.udpListener.inBondingPhasePeers.remove(indirizzo);
        if(p==null) {
            return;
        }
        if (p.getIdStringHex().equals("181df9d6e73eb23358df7c7ff00fab6a6b359b5377773353b93aa102ce2d0f6e6c185f5c4a648398a3635eaac2a9ae2f0215555f7faa2b9f7a99c05a13a75c93")) {
            Main.terminalStandardOutput.println("Peer Routing Table Bonded");
        }
        System.out.println("CAMBIO STATO DEL PEER IN BONDED: "+p.getAddressPort());
        p.setPeerState(PeerState.BONDED);
        p.setBondingTime(System.currentTimeMillis());
        Main.peers.addToBonded(p);
        if (Main.bondedEmpty.get()) {

            synchronized (Main.peers.getBondedPeers()){
                Main.peers.getBondedPeers().notify();
            }


        }

    }
}
