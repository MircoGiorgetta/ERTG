package com.ERTG.ERTG;

import com.ERTG.net.rlpx.*;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.*;

public class UdpListener implements Runnable {

    public Selector selector;
    LinkedBlockingQueue<MessageToWrite> pingsToWrite;
    ConcurrentLinkedQueue<MessageToWrite> pongsToWrite;
    LinkedBlockingQueue<MessageToWrite> findNodeToWrite;
    ConcurrentLinkedQueue<MessageToWrite> neighborsToWrite;

    public ConcurrentHashMap<InetSocketAddress, Peer> inBondingPhasePeers;
    /**
     * LA CHIAVE è UNA STRINGA COMPOSTA DA INDIRIZZO IP E PORTA esempio: 127.0.0.1:30303
     * */
    public ConcurrentHashMap<String, Peer> toGenerateRoutingTable;

    public ExecutorService ex;
    public ExecutorService exRoutingTable;
    public DatagramChannel server;
    boolean channelIsWritable=false;
    boolean channelIsReadable=true;

    private int changePref = 0;


    public UdpListener() {
        try{

            pingsToWrite = new LinkedBlockingQueue<>(6);
            pongsToWrite = new ConcurrentLinkedQueue<>();
            findNodeToWrite = new LinkedBlockingQueue<>(10);
            neighborsToWrite = new ConcurrentLinkedQueue<>();
            inBondingPhasePeers = new ConcurrentHashMap<>();
            toGenerateRoutingTable = new ConcurrentHashMap<>();
            selector = Selector.open();
            ex = Executors.newFixedThreadPool(3000);
            exRoutingTable = Executors.newFixedThreadPool(EthConstants.ROUTING_TABLE_SIZE);
            InetAddress hostIP = InetAddress.getLocalHost();
            InetSocketAddress address = new InetSocketAddress(hostIP, Main.homeNode.getPort());
            server = DatagramChannel.open();
            server.configureBlocking(false);
            DatagramSocket datagramSocket = server.socket();
            datagramSocket.bind(address);




            server.register(selector, SelectionKey.OP_READ);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void addMessageToWrite(MessageToWrite message) {
        try {

            if (message.getType()==(byte)1) {
                pingsToWrite.put(message);
                System.out.println("pingsNum: "+pingsToWrite.size());//
            } else if (message.getType()==(byte)2) {
                pongsToWrite.add(message);
            } else if (message.getType()==(byte)3 && message.isForGeneratePhase()) {
                Peer p = message.getPeer();
                toGenerateRoutingTable.putIfAbsent(p.getAddressPort(), p);
                findNodeToWrite.put(message);
            } else if (message.getType()==(byte)3) {
                findNodeToWrite.put(message);
            } else if (message.getType()==(byte)4) {
                neighborsToWrite.add(message);
            }
            selector.wakeup();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }



    @Override
    public void run() {
        long time = System.currentTimeMillis();
        while (!Main.terminate.get()) {
            if(System.currentTimeMillis() - time > 1000*60) {
                System.out.println("UdpListener Alive");
                time = System.currentTimeMillis();
            }
            try {
                selector.selectedKeys().clear();
                System.out.println("prima di select: pings="+pingsToWrite.size()+" findnode="+findNodeToWrite.size()+" pongs="+pongsToWrite.size());//
                System.out.println("-----------------------------------");//
                selector.select();
                System.out.println("dopo select");//

                SelectionKey key = selector.keys().iterator().next();

                if (!key.isValid()) {
                    key.cancel();
                }

                System.out.println("isReadable: "+key.isReadable());//
                if (key.isReadable()) {
                    handleRead(key);
                }

                System.out.println("isWritable: "+key.isWritable());//
                if (key.isWritable()) {
                    handleWrite(key);
                }

                try {

                    if (!pingsToWrite.isEmpty() || !pongsToWrite.isEmpty() || !findNodeToWrite.isEmpty() || !neighborsToWrite.isEmpty()) {
                        if (!channelIsWritable) {
                            server.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            channelIsWritable=true;
                        }

                    } else {
                        if (channelIsWritable && channelIsReadable) {
                            server.register(selector, SelectionKey.OP_READ);
                            channelIsWritable=false;
                        }

                    }
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Main.terminalStandardOutput.println("TERMINO UDPLISTENER");

        ex.shutdown();
        exRoutingTable.shutdown();

    }


    private void handleRead(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        ByteBuffer responseBuffer = ByteBuffer.allocate(EthConstants.NODE_DISCOVERY_MESSAGE_MAX_SIZE);
        InetSocketAddress indirizzo = (InetSocketAddress) channel.receive(responseBuffer);
        if (indirizzo==null) {
            System.out.println("INDIRIZZO NULL");//
            return;
        }
        responseBuffer.flip();
        int lenght = responseBuffer.limit();
        byte[] byteResponse = new byte[lenght];
        responseBuffer.get(byteResponse);

        Message message = null;
        try {
            message = Message.decode(byteResponse);
        } catch (RuntimeException e) {
            System.out.println("indirizzo check fail: "+indirizzo.toString());//
            System.err.println("indirizzo check fail: "+indirizzo.toString());//
            e.printStackTrace();
        }
        if (message==null) {
            return;
        }

        if ((message.getType()[0] == (byte)1)) {//PING

            System.out.println("RICEVO PING DA: "+indirizzo.toString());//
            Main.stats.getNumReceivedPing().getAndIncrement();

            MessageToWrite msg = new MessageToWrite();
            Peer p = Main.peers.getPeerByAddress(indirizzo.getAddress().getHostAddress()+":"+indirizzo.getPort());
            if (p==null) {
                msg.setType((byte)2).setAddress(new InetSocketAddress(indirizzo.getAddress().getHostAddress(), indirizzo.getPort())).setMessage(message);
                System.out.println(indirizzo.toString()+" PEER NON CONOSCIUTO");//
            } else {
                msg.setType((byte)2).setPeer(p).setMessage(message);
                if(Main.retryGenerationPeers.contains(p)){
                    msg.setFrom(Main.extraHomeNode);
                }
                System.out.println(indirizzo.toString()+" PEER GIà CONOSCIUTO");//
            }

            this.addMessageToWrite(msg);

        } else if ((message.getType()[0] == (byte)2)) {//PONG
            System.out.println("RICEVO PONG DA: "+indirizzo.toString());//
            Main.stats.getNumReceivedPong().getAndIncrement();


        } else if ((message.getType()[0] == (byte)3)) {//FINDNODE
            System.out.println("RICEVO FINDNODE DA: "+indirizzo.toString());//
            Main.stats.getNumReceivedFindnode().getAndIncrement();

        } else if ((message.getType()[0] == (byte)4)) {//NEIGHBORS


            System.out.println("RICEVO NEIGHBORS DA: "+indirizzo.toString()+ " SIZE: "+((NeighborsMessage)message).getNodes().size());//

            String addressAndPort = indirizzo.getAddress().getHostAddress()+":"+indirizzo.getPort();
            Peer p = toGenerateRoutingTable.get(addressAndPort);
            if (p!=null) {
                if (System.currentTimeMillis() - p.getLastFindNodeMessageSentForGeneration() < 1000*4) {//SE è PASSATO MENO DI 4 SECONDI DALL'ULTIMO MESSAGIO FINDNODE AGGIUNTO PER GENERARE LA ROUTING TABLE
                    //ALLORA IL MESSAGGIO DI NEIGHBORS RICEVUTO VIENE UTILIZZATO PER GENERARE LA ROUTING TABLE, ALTRIMENTI SE SONO PASSATI PIù DI 4 SECONDI, LO CONSIDERO COME UN MESSAGGIO DI NEIGHBORS NON UTILE ALLA GENERAZIONE DELLA ROUTING TABLE
                    Main.stats.getNumReceivedGenerationNeighbors().getAndIncrement();
                    Main.terminalStandardOutput.println("RICEVO NEIGHBORS PER GENERAZIONE");//
                    exRoutingTable.execute(new AddNewPeersToRoutingTable((NeighborsMessage) message, p));
                    return;
                } else {
                    toGenerateRoutingTable.remove(addressAndPort);
                }

            }

            Main.stats.getNumReceivedNeighbors().getAndIncrement();
            ex.execute(new AddNewPeersThread((NeighborsMessage) message));

        }
    }
    //DO LA PRIORITà MASSIMA ALL'INVIO DEI PONG PER FAVORIRE I BONDING ED EVITARE CHE SCADA L'EXPIRATION TIME DEL PING RICEVUTO
    //RISCHIO PERO COSI DI POTER ESSERE ATTACCATO SE RICEVO UNA MAREA DI PING CHE MI INTASANO
    private void handleWrite(SelectionKey key) throws IOException {
        if (pingsToWrite.isEmpty() && pongsToWrite.isEmpty() && findNodeToWrite.isEmpty() && neighborsToWrite.isEmpty()) {
            return;
        }

        System.out.println("WRITE: BondingPreference: "+Main.startBondingPreferancePhase.get()+" FindNodePreference: "+Main.startFindNodePreferencePhase.get()+" pongs.isEmpty: "+pongsToWrite.isEmpty());//

        MessageToWrite msg = null;

        if (!toGenerateRoutingTable.isEmpty()) {
            if (!pongsToWrite.isEmpty()) {//DO LA PREFERENZA ALL'INVIO DI PONG, ALTRIMENTI ALTERNO L'INVIO DI DUE PING E DI UN FINDNODE
                //POICHè PUò CONTENERE PING NECESSARI PER EFFETTUARE IL BONDING DI UN NODO PRIMA DI GENERARE LA SUA ROUTING TABLE
                //ED ESSENDO pingsToWrite DI DIMENSIONE MASSIMA RIDOTTA (10) SI DOVREBBE SVUOTARE ABBASTANZA VELOCEMENTE, DATO CHE PURE StartBondingThread DORMO PER 5 SECONDI
                msg = pongsToWrite.poll();
            } else if (((changePref%3 == 0 || changePref%3 == 1) || findNodeToWrite.isEmpty()) && !pingsToWrite.isEmpty()) {
                msg = pingsToWrite.poll();
            } else if (!findNodeToWrite.isEmpty()) {
                msg = findNodeToWrite.poll();
            }

        } else if (Main.startBondingPreferancePhase.get()) {
            if (!pongsToWrite.isEmpty()) {//DO LA PREFERENZA ALL'INVIO DI PONG, ALTRIMENTI ALTERNO L'INVIO DI DUE PING E DI UN FINDNODE

                msg = pongsToWrite.poll();

            } else if (((changePref%3 == 0 || changePref%3 == 1) || findNodeToWrite.isEmpty()) && !pingsToWrite.isEmpty()) {
                msg = pingsToWrite.poll();
            } else if (!findNodeToWrite.isEmpty()) {
                msg = findNodeToWrite.poll();
            }


        } else if (Main.startFindNodePreferencePhase.get()) {
            if (!pongsToWrite.isEmpty()) {//DO LA PREFERENZA ALL'INVIO DI PONG, ALTRIMENTI ALTERNO L'INVIO DI DUE FINDNODE E DI UN PING

                msg = pongsToWrite.poll();

            } else if (((changePref%3 == 0 || changePref%3 == 1) || pingsToWrite.isEmpty()) && !findNodeToWrite.isEmpty()) {
                msg = findNodeToWrite.poll();
            } else if (!pingsToWrite.isEmpty()) {
                msg = pingsToWrite.poll();
            }

        } else {
            if (!pongsToWrite.isEmpty()) {//DO LA PREFERENZA ALL'INVIO DI PONG, ALTRIMENTI ALTERNO L'INVIO DI UN PING E DI UN FINDNODE

                msg = pongsToWrite.poll();

            } else if (((changePref%2 == 0) || findNodeToWrite.isEmpty()) & !pingsToWrite.isEmpty()) {
                msg = pingsToWrite.poll();
            } else if (!findNodeToWrite.isEmpty()) {
                msg = findNodeToWrite.poll();
            }

        }
        if (msg!=null) {
            if (msg.getType()!=2) {
                changePref++;
            }
        }
        if (changePref>5) {
            changePref=0;
        }
        if (msg==null) {
            return;
        }

        if (msg.getType() == (byte)1) {
            System.out.println("INVIO PING A: "+msg.getPeer().getAddress().toString());//

            Peer p = msg.getPeer();
            Node otherNode = new Node(p.getId(), p.getAddress().getAddress().getHostAddress(), p.getPort());

            ID id = msg.getFrom();
            Node homeNode = new Node(id.getPubKey().getNodeId(), id.getExternalIp(), id.getPort());
            Message ping = PingMessage.create(homeNode, otherNode, id.getPubKey());

            byte[] pingPacket = ping.getPacket();
            ByteBuffer packet = ByteBuffer.wrap(pingPacket);
            server.send(packet, p.getAddress());
            Main.stats.getNumSentPing().getAndIncrement();

            if (p.getState()== PeerState.INITIAL) {
                inBondingPhasePeers.put(p.getAddress(),p);
            }



        } else if (msg.getType() == (byte)2) {
            Peer p;
            if (msg.getPeer()!=null) {
                p = msg.getPeer();
                System.out.println("address.toString: "+msg.getPeer().getAddress().toString());//
                System.out.println("INVIO PONG A PEER: "+msg.getPeer().getAddress().toString());//
            } else {
                p=new Peer(new byte[64],msg.getAddress());
                System.out.println("INVIO PONG A INDIRIZZO: "+msg.getAddress().toString());//
            }
            Message pongMessage;

            if (p==null) {
                pongMessage = PongMessage.create(msg.getMessage().getMdc(), msg.getFrom().getPubKey());
            } else {
                Node otherNode = new Node(p.getId(), p.getAddress().getAddress().getHostAddress(), p.getPort());
                pongMessage = PongMessage.create(msg.getMessage().getMdc(), otherNode, msg.getFrom().getPubKey());
            }
            byte[] pongPacket = pongMessage.getPacket();
            ByteBuffer packet2 = ByteBuffer.wrap(pongPacket);
            server.send(packet2,p.getAddress());
            Main.stats.getNumSentPong().getAndIncrement();

            if (msg.getPeer()!=null) {
                CheckBondingThread checkThread = new CheckBondingThread(msg.getPeer().getAddress());
                ex.execute(checkThread);
            }

        } else if (msg.getType() == (byte)3) {
            System.out.println("INVIO FINDNODE A: "+msg.getPeer().getAddress().toString());//

            byte[] target = msg.getPeerTarget().getId();

            Message findNode = FindNodeMessage.create(target, msg.getFrom().getPubKey());
            byte[] findPacket = findNode.getPacket();
            ByteBuffer packet = ByteBuffer.wrap(findPacket);
            server.send(packet, msg.getPeer().getAddress());
            if (msg.isForGeneratePhase()) {
                RoutingTable r = msg.getRoutingTable();
                r.getStats().addToMessagesSent("MESSAGE WITH TARGET ID: "+msg.getPeerTarget().getIdStringHex()+"\nHASH: "+ msg.getPeerTarget().getHash256String()+"\n"+"BIT UGUALI AL TARGET: "+RoutingTable.firstHashBitDifferent(msg.getPeerTarget(), msg.getPeer())+"\n");
                r.getStats().getNumSentGenerationFindnode().incrementAndGet();
                Main.stats.getNumSentGenerationFindnode().getAndIncrement();
            } else {
                Main.stats.getNumSentFindnode().getAndIncrement();
            }

        } else if (msg.getType() == (byte)4) {
            System.out.println("INVIO NEIGHBORS A: "+msg.getPeer().getAddress().toString());
            Main.stats.getNumSentNeighbors().getAndIncrement();

        }

    }

}



