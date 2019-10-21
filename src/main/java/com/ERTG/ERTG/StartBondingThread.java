package com.ERTG.ERTG;


public class StartBondingThread implements Runnable {


    @Override
    public void run() {


        while(!Main.terminate.get()) {


            try {
                synchronized (Main.udpListener.toGenerateRoutingTable) {

                    while (!Main.udpListener.toGenerateRoutingTable.isEmpty()) {
                        Main.udpListener.toGenerateRoutingTable.wait();

                    }
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }


            if(Main.peers.getBondedSize()<DefaultConfigurationValues.DEFAULT_BONDED_PEERS_NUMBER_THRESHOLD[0]) {

                Peer p = Main.peers.getRandomUnbondedPeer();
                if (p!=null) {
                    if (p.isBondable()) {
                        Main.sendPing(p);
                        System.out.println("INIZIO FASE DI BONDING PER UN PEER: "+p.getAddress().toString());//

                    }
                }



            } else {


                try{

                    synchronized (Main.terminate){
                        while(!Main.terminate.get()){
                            Main.terminate.wait();
                        }
                    }

                }catch (InterruptedException e){
                    e.printStackTrace();
                }


            }

        }

        Main.terminalStandardOutput.println("TERMINO START_BONDING");

    }

}
