package com.ERTG.ERTG;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NodeFinder implements Runnable {


    public LinkedBlockingQueue<Peer> enodeToGenerate = new LinkedBlockingQueue();






    @Override
    public void run() {


        while(!Main.terminate.get()) {
            if (Main.pause.get()) {
                try {
                    Thread.sleep(1000*10);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (!enodeToGenerate.isEmpty()) {

                Peer p = null;
                try {
                    p = enodeToGenerate.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (p==null) {
                    continue;
                }

                if (p.isBondable()) {
                    System.err.println("Number of Bonding try: "+p.getNumberOfBondingTried());

                    if (p.getNumberOfBondingTried()>5) {
                        p.setNumberOfBondingTried(0);
                        System.out.println("IL NODO "+p.getIdStringHex()+" NON ERA RAGGIUNGIBILE!!!");
                        Main.terminalStandardOutput.println("IL NODO "+p.getIdStringHex()+" NON ERA RAGGIUNGIBILE!!!");


                        synchronized (enodeToGenerate){
                            if(enodeToGenerate.isEmpty()) {
                                enodeToGenerate.notify();
                            }
                        }

                        continue;
                    }

                    Main.sendPing(p);
                    try {
                        enodeToGenerate.put(p);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (p.getState()!=PeerState.BONDED) {
                    try {
                        enodeToGenerate.put(p);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }



                RoutingTable r = Main.routingTableSet.get(p.getIdStringHex());

                for (int i=0; i<EthConstants.ROUTING_TABLE_SIZE; i++) {
                    String str = p.getFirstBitsHash(i);
                    if (str==null) {
                        continue;
                    }

                    if (i!=EthConstants.ROUTING_TABLE_SIZE-1) {
                        if (p.nextBitHash(str)==1) {//FACCIO IN MODO CHE L'i+1-ESIMO BIT SIA DIVERSO DA QUELLO DI p
                            //COSì DA EVITARE IL CASO IN CUI MI POSSA RESTITUIRE SUBITO COME PEER (CON I PRIMI i BIT UGUALI) LO STESSO PEER OWNER DELLA ROUTING TABLE
                            //ESEMPIO:
                            //OWNER = 11111 E peers CONTIENTE PURE 10000
                            //PRIMI 1 BIT UGUALI E RICEVO PEER 10000, QUINDI UNA FINDNODE MI RESTITUIRà PEER CON ALMENO IL 1 BIT UGUALE
                            //MA SE RICEVESSI DA peers SUBITO 11111, UNA FINDNODE MI RESTITUIREBBE PEER CON ALMENO (VICINO) I 5 BIT UGUALI O COMUNQUE VICINI A QUELLO
                            str=str+0;
                        } else {
                            str=str+1;
                        }
                    }



                    Peer peerTarget = Main.peers.getPeerHashStartWith(str);
                    if (peerTarget==null) {

                        continue;
                    }


                    MessageToWrite msg = new MessageToWrite();
                    msg.setType((byte)3).setPeer(p).setPeerTarget(peerTarget).setForGeneratePhase(true).setRoutingTable(r);
                    if(r.getNumberOfGenerationTried()!=0){
                        msg.setFrom(Main.extraHomeNode);
                    }
                    Main.udpListener.addMessageToWrite(msg);

                    p.setLastFindNodeMessageSentForGeneration(System.currentTimeMillis());

                    Main.terminalStandardOutput.println("PREPARO FINDNODE GENERAZIONE");//
                    System.out.println("PREPARO FINDNODE GENERAZIONE");//

                    if (peerTarget.equals(p)) {
                        break;
                    }


                }

                r.setNumberOfGenerationTried(1);
                ManagePhaseThread.lastGenerationStartTime.set(System.currentTimeMillis());



                synchronized (enodeToGenerate){
                    if(enodeToGenerate.isEmpty()) {
                        enodeToGenerate.notify();
                    }
                }


                continue;

            }


            if(Main.peers.getBondedSize()>0) {

                Peer p = Main.peers.getRandomBondedPeer();
                if (p!=null) {
                    if(p.getState()== PeerState.BONDED) {

                        Peer peerTarget = Main.peers.getRandomPeer();


                        MessageToWrite msg = new MessageToWrite();
                        msg.setType((byte)3).setPeer(p).setPeerTarget(peerTarget);
                        Main.udpListener.addMessageToWrite(msg);
                    }
                }

            } else {

                try {
                    synchronized (Main.peers.getBondedPeers()) {

                        while (Main.peers.getBondedSize()==0) {
                            Main.peers.getBondedPeers().wait();

                        }
                    }
                }catch (InterruptedException e){
                    e.printStackTrace();
                }


            }


        }
        Main.terminalStandardOutput.println("TERMINO NODE_FINDER");


    }
}
