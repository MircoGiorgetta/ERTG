package com.ERTG.ERTG;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeersSet {

    private TreeNode peersTree;//INSIEME DI TUTTI I PEER "ORDINATI" SECONDO L'ID
    private TreeNode unbondedPeers;//INSIEME DEI PEER NON BONDED "ORDINATI" SECONDO L'ID
    private TreeNode bondedPeers;//INSIEME DEI PEER BONDED "ORDINATI" SECONDO L'ID
    private HashTreeNode hashedPeers;//INSIEME DI TUTTI I PEER "ORDINATI" SECONDO L'HASH DEL LORO ID
    private ConcurrentHashMap<String, Peer> peersMap;//INSIEME DI TUTTI I PEER CON CHIAVE IL LORO INDIRIZZO IP
    private ConcurrentHashMap<String, Peer> peersFromNewDiscovery;//INSIEME DI TUTTI I PEER TROVATI TRAMITE LA RICERCA DURANTE L'ESECUZIONE CORRENTE DEL PROGRAMMA
    private ConcurrentHashMap<String, Peer> peersDifferentIdButSameIpAndPort;//INSIEME DI TUTTI I CASI DI PEER G

    public PeersSet() {
        peersTree = new TreeNode("");
        unbondedPeers = new TreeNode("");
        bondedPeers = new TreeNode("");
        hashedPeers = new HashTreeNode("");
        peersMap = new ConcurrentHashMap<>();
        peersFromNewDiscovery = new ConcurrentHashMap<>();
        peersDifferentIdButSameIpAndPort = new ConcurrentHashMap<>();
    }

    public TreeNode getPeersTree() {
        return peersTree;
    }

    public TreeNode getBondedPeers() {
        return bondedPeers;
    }

    public ConcurrentHashMap<String, Peer> getPeersMap() {
        return peersMap;
    }

    public synchronized boolean add(Peer p) {

        boolean b = this.contains(p);//CONTROLLO SE IL PEER IN BASE AL SUO ID è GIà PRESENTE NELLA COLLLEZIONE
        if (!b) {

            Peer w = peersMap.put(p.getAddressPort(), p);//è POSSIBILE RICEVERE NODI CON UN ID DIVERSO TRA LORO MA CON INDIRIZZO IP E PORTA UGUALI TRA LORO
            //PERCIò PER MANTENERE LA COLLEZIONE COERENTE MANTENGO SOLO IL PRIMO NODO RICEVUTO
            if (w!=null) {
                peersMap.put(w.getAddressPort(), w);
                Peer p2 = peersDifferentIdButSameIpAndPort.putIfAbsent(p.getIdStringHex(), p);
                if (p2!=null) {
                    if (p2.getAddressPort()!=p.getAddressPort()) {
                        if (p2.addAnotherAddress(p.getAddress())) {
                            Main.stats.getNumSameIdButDifferentIPandPort().incrementAndGet();
                        }

                    }
                } else {
                    Main.stats.getNumDifferentIdButSameIPandPort().incrementAndGet();
                }


                return false;
            }
            hashedPeers.add(p);
            unbondedPeers.add(p);



            peersTree.add(p);

            return true;
        } else {//CONTROLLO IL PEER GIà PRESENTE SE HA UN INDIRIZZO ID E PORTA DIVERSI E AGGIORNO IL PEER
            Peer d = peersTree.getPeerWithIdStringBit(p.getIdString());
            if (d.getAddressPort()!=p.getAddressPort()) {
                if (d.addAnotherAddress(p.getAddress())) {
                    Main.stats.getNumSameIdButDifferentIPandPort().incrementAndGet();
                }
            }


        }


        return false;
    }

    public boolean addToNewDiscoveries(Peer p) {
        Peer o = peersFromNewDiscovery.putIfAbsent(p.getIdStringHex(), p);

        if (o!=null) {
           return false;
        }
        return true;
    }

    public boolean addToBonded(Peer p) {
        unbondedPeers.remove(p);
        return bondedPeers.add(p);
    }

    public boolean addToUnbonded(Peer p) {
        bondedPeers.remove(p);
        return unbondedPeers.add(p);
    }

    public Peer getPeerWithStringBit(String idStringBit) {
        if (idStringBit.length()!=512) {
            return null;
        }
        return peersTree.getPeerWithIdStringBit(idStringBit);
    }

    private synchronized boolean contains(Peer p) {
        return peersTree.contains(p);
    }


    public void printNewDiscoveries(String nameFile) {

        try {
            File f = new File(nameFile);

            if (!f.exists())
                f.createNewFile();

            JSONObject object = new JSONObject();
            JSONArray arr = new JSONArray();

            for (Peer p: peersFromNewDiscovery.values()) {
                p.printJson(arr);

            }

            object.put("peers", arr);


            try(FileWriter fr = new FileWriter(f)){

                object.write(fr);

            }

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }



    public void printPeersDifferentIdButSameIpAndPort(String nameFile) {
        try {
            File f = new File(nameFile);

            if (!f.exists())
                f.createNewFile();

            JSONObject object = new JSONObject();
            JSONArray arr = new JSONArray();

            for (Peer p: peersDifferentIdButSameIpAndPort.values()) {
                p.printJson(arr);

            }

            object.put("peers", arr);


            try(FileWriter fr = new FileWriter(f)){

                object.write(fr);

            }

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public void printAll(String filename) {
        peersTree.printAll(filename);
    }

    public void printAllJson(String nameFile) {
        peersTree.printAllJson(nameFile);
    }

    public void printAllJsonBondedPeers(String nameFile) {
        bondedPeers.printAllJson(nameFile);
    }

    public int getSize() {
        return peersTree.size();
    }

    public int getBondedSize() {
        return bondedPeers.size();
    }

    public int getUnbondedSize() {
        return unbondedPeers.size();
    }

    public int getHashedSize() {
        return hashedPeers.size();
    }

    public Peer getPeerByAddress(String addr) {
        return peersMap.get(addr);
    }

    public Peer getRandomPeer() {
        return peersTree.getRandomPeer();
    }

    public Peer getRandomBondedPeer() {
        return bondedPeers.getRandomPeer();
    }

    public Peer getRandomUnbondedPeer() {
        return unbondedPeers.getRandomPeer();
    }


    public Peer getPeerHashStartWith(String pattern) {
        return hashedPeers.getPeerHashStartWith(pattern);
    }


}
