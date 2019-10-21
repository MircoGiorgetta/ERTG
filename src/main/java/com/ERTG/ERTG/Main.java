package com.ERTG.ERTG;

import com.ERTG.util.ByteUtil;
import org.json.JSONArray;

import com.ERTG.crypto.ECKey;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.spongycastle.util.encoders.Hex;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ERTG.util.ByteUtil.hexStringToBytes;
import static com.ERTG.util.ByteUtil.toHexString;

public class Main {

    public static AtomicBoolean pause = new AtomicBoolean(false);


    public static AtomicBoolean startBondingPreferancePhase = new AtomicBoolean(true);
    public static AtomicBoolean startFindNodePreferencePhase = new AtomicBoolean(false);
    public static AtomicBoolean terminate = new AtomicBoolean(false);

    public static AtomicBoolean bondedEmpty = new AtomicBoolean(true);//Determina se l'insieme dei peer bonded sia ancora vuoto

    public static StatisticInfo stats = new StatisticInfo();

    public static PeersSet peers = new PeersSet();


    /**
     * HASH MAP CON CHIAVE L'ID IN FORMATO HEX DEL PROPRIETARIO DELLA ROUTING TABLE
     * */
    public static ConcurrentHashMap<String,RoutingTable> routingTableSet = new ConcurrentHashMap<>();

    public static ConcurrentLinkedQueue<Peer> retryGenerationPeers = new ConcurrentLinkedQueue<>();

    public static UdpListener udpListener = null;
    public static NodeFinder nodeFinder = new NodeFinder();

    public static ID homeNode = null;

    public static ID extraHomeNode = null;


    public static PrintStream terminalStandardOutput;

    public static String inputNameFile;
    public static File routingTablesFolder;



    public static void main(String[] args) {

        terminalStandardOutput = System.out;

        File o = new File("out"+System.currentTimeMillis());
        if(!o.exists())
            try
            {
                o.createNewFile();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(o);
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        System.setOut(new PrintStream(out));





        File ee = new File("err"+System.currentTimeMillis());
        if(!ee.exists())
            try
            {
                ee.createNewFile();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        FileOutputStream err = null;
        try
        {
            err = new FileOutputStream(ee);
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        System.setErr(new PrintStream(err));



        createAndRetrieveConfigFile();

        if(args.length>0) {
            inputNameFile = args[0];

        }

        createRoutingTablesFolder();

        execJarBasicCrawler();

        loadPeersFromBasicCrawlerFile();

        loadHomeNodeInfo();

        restorePeer();

        udpListener = new UdpListener();


        addBootnodes();


        Thread thread = new Thread(udpListener);
        thread.start();
        Thread nodeFinderThread = new Thread(nodeFinder);
        nodeFinderThread.start();
        Thread startBonding = new Thread(new StartBondingThread());
        startBonding.start();
        Thread coordinator = new Thread(new ManagePhaseThread());
        coordinator.start();


        try {

            synchronized (terminate) {

                while (!terminate.get()) {
                    terminate.wait();

                }
            }
        }catch(InterruptedException e) {
            e.printStackTrace();
        }


        thread.interrupt();
        nodeFinderThread.interrupt();
        startBonding.interrupt();
        coordinator.interrupt();
        Main.udpListener.pongsToWrite.clear();
        Main.udpListener.pingsToWrite.clear();
        Main.udpListener.findNodeToWrite.clear();

        System.out.println("salvo i peer su file");
        peers.printAllJsonBondedPeers("BondedPeers.json");
        peers.printAll("Peers.dat");
        peers.printAllJson("Peers.json");
        peers.printPeersDifferentIdButSameIpAndPort("PeersDifferentIdButSameIpAndPort.json");
        peers.printNewDiscoveries("RetrievedPeersDuringDiscovery.json");


        stats.getPeerSize().set(peers.getSize());
        stats.getBondedPeerSize().set(peers.getBondedSize());
        stats.getUnbondedPeerSize().set(peers.getUnbondedSize());
        stats.getHashedPeerSize().set(peers.getHashedSize());
        stats.print();


        for(RoutingTable r : routingTableSet.values()) {
            r.print();
            r.printDebug();//
        }


        System.out.println("Fine");
        Main.terminalStandardOutput.println("TERMINO MAIN");


    }




    public static void sendPingFromOtherID(Peer p, ID id){



        MessageToWrite msg = new MessageToWrite();
        msg.setType((byte)1).setPeer(p);
        if(id!=null) {
           msg.setFrom(id);
        }
        udpListener.addMessageToWrite(msg);
    }

    public static void sendPing(Peer p){
        sendPingFromOtherID(p,null);

    }



    private static void initializeDefaultValues() {
        DefaultConfigurationValues.DEFAULT_VALUES_SET.put("DEFAULT_BONDING_PREFERENCE_TIME", DefaultConfigurationValues.DEFAULT_BONDING_PREFERENCE_TIME);
        DefaultConfigurationValues.DEFAULT_VALUES_SET.put("DEFAULT_FINDNODE_PREFERENCE_TIME", DefaultConfigurationValues.DEFAULT_FINDNODE_PREFERENCE_TIME);
        DefaultConfigurationValues.DEFAULT_VALUES_SET.put("DEFAULT_BALANCED_PREFERENCE_TIME", DefaultConfigurationValues.DEFAULT_BALANCED_PREFERENCE_TIME);
        DefaultConfigurationValues.DEFAULT_VALUES_SET.put("DEFAULT_BASIC_CRAWLER_REFRESH_TIME", DefaultConfigurationValues.DEFAULT_BASIC_CRAWLER_REFRESH_TIME);
        DefaultConfigurationValues.DEFAULT_VALUES_SET.put("DEFAULT_PORT", DefaultConfigurationValues.DEFAULT_PORT);
        DefaultConfigurationValues.DEFAULT_VALUES_SET.put("DEFAULT_BONDED_PEERS_NUMBER_THRESHOLD", DefaultConfigurationValues.DEFAULT_BONDED_PEERS_NUMBER_THRESHOLD);

    }

    private static void createAndRetrieveConfigFile() {

        initializeDefaultValues();

        File configFile = new File("Config.properties");

        if (!configFile.exists() || !configFile.isFile()) {

            try(FileWriter fr = new FileWriter(configFile);
                BufferedWriter out = new BufferedWriter(fr)){
                configFile.createNewFile();

                for (String str: DefaultConfigurationValues.DEFAULT_VALUES_SET.keySet()) {
                    out.write(str+" = "+(DefaultConfigurationValues.DEFAULT_VALUES_SET.get(str))[0]+"\n");
                }


            } catch (IOException e) {
                e.printStackTrace();
            }


        } else {

            try(FileReader fr = new FileReader(configFile);
                BufferedReader in = new BufferedReader(fr)){

                String line;

                int conta = 0;

                while ((line = in.readLine()) != null) {
                    String[] field = line.split(" = ");
                    if (field.length==2) {
                        int[] a = DefaultConfigurationValues.DEFAULT_VALUES_SET.get(field[0]);
                        if (a!=null) {
                            try {
                                a[0] = Integer.parseInt(field[1]);
                            } catch (NumberFormatException e) {
                                System.err.println("VALORE "+field[0]+" DI Config.properties MALFORMATTATO");
                                System.out.println("VALORE "+field[0]+" DI Config.properties MALFORMATTATO");
                            }
                        } else {
                            System.err.println("VALORE "+field[0]+" DI Config.properties MALFORMATTATO");
                            System.out.println("VALORE "+field[0]+" DI Config.properties MALFORMATTATO");
                        }

                    } else {
                        System.err.println("VALORE NUMERO "+conta+" DI Config.properties MALFORMATTATO");
                        System.out.println("VALORE NUMERO "+conta+" DI Config.properties MALFORMATTATO");
                    }
                    conta++;
                }


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * SE NON è PRESENTE UN FILE ExternalPeers O è PIù VECCHIO DI UN GIORNO ESEGUE IL BasicCrawler.jar
     * */
    private static void execJarBasicCrawler() {
        File f = new File("ExternalPeers");
        try {

            if (f.exists() && f.isFile()) {
                BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                LocalDateTime date = attr.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime now = LocalDateTime.now();
                now = now.minusHours(DefaultConfigurationValues.DEFAULT_BASIC_CRAWLER_REFRESH_TIME[0]);
                if (date.isAfter(now)) {
                    return;
            //    System.out.println();
                }
            }
            Process process = Runtime.getRuntime().exec("java -jar BasicCrawler.jar ExternalPeers", null);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static boolean checkEnodeFormatString(String enode) {
        if (enode!=null) {
            if (enode.startsWith("enode://")) {
                String id = enode.substring(8,136);

                if (isHexadecimal(id)) {
                    if (enode.charAt(136)=='@') {
                        String ipAndPort = enode.substring(137);
                        String[] arr = ipAndPort.split(":");
                        if (arr.length==2) {
                            String ip = arr[0];
                            if (isValidIp(ip)) {
                                String portStr = arr[1];
                                boolean res = false;
                                int port = 0;
                                try {
                                    port = Integer.parseInt(portStr);
                                    res = true;
                                } catch (NumberFormatException e) {

                                }
                                if (res) {
                                    if (port>=0 && port<=65535) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }




    public static boolean isHexadecimal(String id) {

        String HEXADECIMAL_PATTERN = "^[a-fA-F0-9]+$";

        Pattern pattern = Pattern.compile(HEXADECIMAL_PATTERN);

        Matcher matcher = pattern.matcher(id);

        return matcher.matches();
    }

    public static boolean isValidIp(String ip) {
        String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);

        Matcher matcher = pattern.matcher(ip);

        return matcher.matches();
    }


    public static Peer enodeFormatToPeer(String str) {
        Peer p = null;
        try {
            if (str!=null) {
                String str2 = str.replaceFirst("enode://","");
                String[] id = str2.split("@");
                String[] ip = (id[1]).split(":");
                p = new Peer(ByteUtil.hexStringToBytes(id[0]), new InetSocketAddress(InetAddress.getByName(ip[0]),Integer.parseInt(ip[1])));
                p.setTimestamp((int)(System.currentTimeMillis() / 1000));
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return p;

    }

    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }


    public static File createRoutingTablesFolder() {
        File dir = new File("RoutingTables");
        routingTablesFolder=dir;
        if (dir.exists()) {
            deleteDirectory(dir);
        }
        if (dir.mkdirs()) {
            return dir;
        }
        return null;

    }

    public static boolean createFolderTable(String idHex) {
        File dir = new File(Main.routingTablesFolder.getPath()+"/"+idHex);
        if (dir.exists()) {
            deleteDirectory(dir);
        }

        return dir.mkdirs();

    }

    public static void initializePeerToGenerateRoutingTable(String enodeUrl) {
        if (!Main.checkEnodeFormatString(enodeUrl)) {
            Main.terminalStandardOutput.println("Malformatted enode URL!!!!!!");
            return;
        }



        String idHex = enodeUrl.substring(8,136);
        byte[] idByte = ByteUtil.hexStringToBytes(idHex);
        BigInteger bi1 = new BigInteger(1, idByte);
        String idString = String.format("%512s", bi1.toString(2));
        idString = idString.replace(' ', '0');

        Peer p = Main.peers.getPeerWithStringBit(idString);
        if (p==null) {
            p = Main.enodeFormatToPeer(enodeUrl);
            Main.peers.add(p);
        }

        createFolderTable(p.getIdStringHex());


        if (p.isBondable()) {
            Main.sendPing(p);
        }


        try {
            Main.routingTableSet.putIfAbsent(p.getIdStringHex(), new RoutingTable(p));
            Main.nodeFinder.enodeToGenerate.put(p);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }



    public static void retrievePeerToGenerateRoutingTable() {
        if (Main.inputNameFile==null) {
            return;
        }
        File ownerPeerFile = new File(Main.inputNameFile);

        if (ownerPeerFile.exists() && ownerPeerFile.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(ownerPeerFile))) {
                String line;

                while ((line = br.readLine()) != null) {

                    initializePeerToGenerateRoutingTable(line);

                }


            } catch (IOException | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }

        }
        ManagePhaseThread.lastGenerationStartTime.set(System.currentTimeMillis());
    }

    public static ID createNewID(){

        ID node = new ID();

        ECKey key = new ECKey();

        String newPrvKey = Hex.toHexString(key.getPrivKeyBytes());

        ECKey newPubKey = ECKey.fromPrivate(Hex.decode(newPrvKey));

        node.setNodeId(toHexString(newPubKey.getNodeId()));
        node.setPubKey(newPubKey);
        node.setPrvKey(newPrvKey);
        node.setExternalIp(homeNode.getExternalIp());
        node.setPort(homeNode.getPort());



        return node;
    }

    public static void retryGenerationEmptyTableWithNewID() {

        extraHomeNode=createNewID();

        for(RoutingTable r : routingTableSet.values()){
            if (r.size()==0) {

                Peer tableOwner = r.getPeerOwner();

                tableOwner.resetBonding();

                if (tableOwner.isBondable()) {
                    Main.sendPingFromOtherID(tableOwner,extraHomeNode);
                }


                try {
                    Main.retryGenerationPeers.add(tableOwner);
                    Main.nodeFinder.enodeToGenerate.put(tableOwner);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

        ManagePhaseThread.lastGenerationStartTime.set(System.currentTimeMillis());

    }



    private static void addBootnodes() {

        String[] idAndAddress;
        String id;
        String[] ipAndPort;
        String ip;
        String port;
        InetAddress addr;
        Peer p;


        for(String s : EthConstants.BOOTNODE) {
            try {
                idAndAddress = s.split("@");
                id = idAndAddress[0];
                ipAndPort = (idAndAddress[1]).split(":");
                ip = ipAndPort[0];
                port = ipAndPort[1];

                addr = InetAddress.getByName(ip);
                p = new Peer(hexStringToBytes(id), new InetSocketAddress(addr,Integer.parseInt(port)));


                p.setTimestamp((int)(System.currentTimeMillis() / 1000));
                if (!peers.add(p)) {
                    Main.stats.getNumAddedPeersFromPreviousExecution().decrementAndGet();
                }
                Main.stats.getNumBootnode().incrementAndGet();


                if (p.isBondable()) {
                    sendPing(p);
                }


            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }


    }


    private static void restorePeersDifferentIdButSameIpAndPort() {
        File peersFile = new File("PeersDifferentIdButSameIpAndPort.json");

        if (peersFile.exists() && peersFile.isFile()) {

            try {
                JSONTokener tokener = new JSONTokener(new FileReader(peersFile));
                JSONObject object = new JSONObject(tokener);
                JSONArray arr = (JSONArray) object.get("peers");
                for (Object obj : arr) {
                    Peer p = createPeerFromJson((JSONObject) obj);

                    peers.add(p);

                }


            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

    private static void restorePeer() {
        File peersFile = new File("Peers.json");
        int num = 0;

        if (peersFile.exists() && peersFile.isFile()) {

            try {
                JSONTokener tokener = new JSONTokener(new FileReader(peersFile));
                JSONObject object = new JSONObject(tokener);
                JSONArray arr = (JSONArray) object.get("peers");
                for (Object obj : arr) {
                    Peer p = createPeerFromJson((JSONObject) obj);

                    if (peers.add(p)) {
                        num++;
                    }
                    if (p.getState()==PeerState.BONDED) {
                        peers.addToBonded(p);
                    }

                }


            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        Main.stats.getNumAddedPeersFromPreviousExecution().addAndGet(num);

        restorePeersDifferentIdButSameIpAndPort();

    }


    private static Peer createPeerFromJson(JSONObject obj1) {
        Peer p = null;

        try {
            p = new Peer(hexStringToBytes(obj1.getString("id")),new InetSocketAddress(InetAddress.getByName(obj1.getString("ip")), obj1.getInt("port")));

            p.setTimestamp((int)(System.currentTimeMillis() / 1000));
            JSONArray additionalAddressesArr = obj1.getJSONArray("additionalAddresses");
            for (Object o : additionalAddressesArr) {
                String s = (String) o;
                s = s.replace("/", "");
                String[] strr = s.split(":");
                InetSocketAddress a = new InetSocketAddress(InetAddress.getByName(strr[0]),Integer.parseInt(strr[1]));
                if (p.addAnotherAddress(a)) {
                    Main.stats.getNumSameIdButDifferentIPandPort().incrementAndGet();
                }

            }

            long bondingTime = obj1.getLong("bondingTime");
            if (System.currentTimeMillis() - bondingTime < 1000 * 60 * 60 * 12) {//IL BONDING VALE PER 12 ORE
                p.setBondingTime(bondingTime);
                p.setPeerState(PeerState.BONDED);
            } else {
                p.setTimestamp((int)(System.currentTimeMillis() / 1000));
            }


        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return p;



    }



    private static void loadPeersFromBasicCrawlerFile() {
        File externalPeers = new File("ExternalPeers");

        if (externalPeers.exists() && externalPeers.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(externalPeers))) {
                String line;
                String line2;
                while ((line = br.readLine()) != null) {
                    line2 = line.replaceFirst("admin.addPeer[(]\"enode://","");
                    String[] id = line2.split("@");
                    String[] ip = (id[1]).split(":");
                    String port = (ip[1]).replaceFirst("\"[)]","");
                    Peer p = new Peer(ByteUtil.hexStringToBytes(id[0]), new InetSocketAddress(InetAddress.getByName(ip[0]),Integer.parseInt(port)));
                    p.setTimestamp((int)(System.currentTimeMillis() / 1000));

                    if(peers.add(p)) {
                        Main.stats.getNumAddedPeersFromBasicCrawler().incrementAndGet();
                    }

                }
                System.out.println("Caricati peer esterni da file");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



/**
 * CREA IL FILE HomeNode.json RAPPRESENTANTE IL NOSTRO NODO CON VARIE SUE INFORMAZIONI
 *
 * IL FILE VIENE CREATO NUOVO AD OGNI AVVIO
 * */
    private static void loadHomeNodeInfo(){


        File homeNodeFile = new File("HomeNode.json");

        homeNode = new ID();


        if (!homeNodeFile.exists()) {


            ECKey key = new ECKey();//GenerateNodeIdRandomly

            String newPrvKey = Hex.toHexString(key.getPrivKeyBytes());

            ECKey newPubKey = ECKey.fromPrivate(Hex.decode(newPrvKey));

            JSONObject homeNodeInfo = new JSONObject();
            homeNodeInfo.put("nodeId", toHexString(newPubKey.getNodeId()));
            homeNodeInfo.put("privateKey", newPrvKey);

            String Ip = "";
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {

                Ip = in.readLine();
                if (Ip == null || Ip.trim().isEmpty()) {
                    throw new IOException("Invalid address: '" + Ip + "'");
                }
                try {
                    InetAddress.getByName(Ip);
                } catch (Exception e) {
                    throw new IOException("Invalid address: '" + Ip + "'");
                }
            } catch (IOException e) {

                e.printStackTrace();
            }

            homeNodeInfo.put("ip", Ip);
            homeNodeInfo.put("port", DefaultConfigurationValues.DEFAULT_PORT[0]);

            try (FileWriter file = new FileWriter(homeNodeFile)) {

                homeNodeInfo.write(file);

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("File saved!");//
        }

        try (FileReader rd = new FileReader(homeNodeFile)) {
            JSONTokener tokener = new JSONTokener(rd);
            JSONObject object = new JSONObject(tokener);

            homeNode.setNodeId(object.getString("nodeId"));
            homeNode.setPrvKey(object.getString("privateKey"));
            homeNode.setExternalIp(object.getString("ip"));
            homeNode.setPort(object.getInt("port"));
            homeNode.setPubKey(ECKey.fromPrivate(Hex.decode(homeNode.getPrvKey())));


        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Dati Caricati");//

    }
}

