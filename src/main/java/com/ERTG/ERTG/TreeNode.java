package com.ERTG.ERTG;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TreeNode {


    public int bucketSize=0;

    public static int MAX_KADEMLIA_K = 7;

    //SE IL BIT è UGUALE A 1 VA A SINISTRA
    TreeNode left;

    //SE IL BIT è UGUALE A 0 VA A DESTRA
    TreeNode right;

    String name;

    List<Peer> peers;


    public TreeNode(String name) {

        this.name = name;
        peers = new ArrayList<>();
    }

    public synchronized int size() {
        return bucketSize;
    }

    //RITORNA TRUE SE L'ELEMENTO NON C'ERA ED è STATO INSERITO
    public synchronized boolean add(Peer peer) {
        if (peer == null) throw new Error("Not a leaf");
        boolean res = false;
        if ( peers == null){

            if (peer.nextBit(name) == 1) {
                res = left.add(peer);
            }
            else {
                res = right.add(peer);
            }
            if (res) {
                bucketSize++;
            }
            return res;

        }


        boolean b = peers.contains(peer);
        if (!b) {
            peers.add(peer);
            bucketSize++;
            res = true;

        }

        if (peers.size() > MAX_KADEMLIA_K) {
            splitBucket();
        }
        return res;
    }

    //RITORNA TRUE SE IL PEER ERA PRESENTE ED è STATO RIMOSSO
    public synchronized boolean remove(Peer peer) {
        if (peer == null) throw new Error("Not a leaf");
        boolean res = false;
        if ( peers == null){

            if (peer.nextBit(name) == 1) {
                res = left.remove(peer);
            } else {
                res = right.remove(peer);
            }
            if (res) {
                bucketSize--;
            }
            return res;


        }

        res = peers.remove(peer);
        if (res) {
            bucketSize--;
        }
        return res;


    }


    public synchronized boolean contains(Peer peer) {
        if (peer == null) throw new Error("Not a leaf");

        if ( peers == null){

            if (peer.nextBit(name) == 1) {
                return left.contains(peer);
            } else {
                return right.contains(peer);
            }

        }

        return peers.contains(peer);

    }

    public synchronized Peer getPeerWithIdStringBit(String idStringBit) {

        if (idStringBit == null) throw new Error("Not a leaf");

        if ( peers == null){
            if (idStringBit.startsWith(name + "1")) {
                return left.getPeerWithIdStringBit(idStringBit);
            } else {
                return right.getPeerWithIdStringBit(idStringBit);
            }
        }

        for (Peer p : peers) {
            if (p.getIdString().equals(idStringBit)) {
                return p;
            }
        }
        return null;










    }


    public void splitBucket() {
        left = new TreeNode(name + "1");
        right = new TreeNode(name + "0");

        for (Peer id : peers) {
            if (id.nextBit(name) == 1) {
                left.add(id);
            } else {
                right.add(id);
            }
        }

        this.peers = null;
    }


    public synchronized Peer getRandomPeer() {
        Random rand = new Random(System.currentTimeMillis());
        if (size()==0) {
            return null;
        }

        int n = rand.nextInt(size());
        if (n<0 || n>=bucketSize) {
            return null;
        }

        return this.searchForRandomPeer(n);


    }

    private Peer searchForRandomPeer(int n) {
        if (left != null && 0 <= n && n < left.bucketSize) {
            return left.searchForRandomPeer(n);
        } else if (right != null && left.bucketSize <= n && n < left.bucketSize+right.bucketSize) {
            return right.searchForRandomPeer(n-left.bucketSize);
        } else if (right == null && left == null) {
            return peers.get(n);
        }
        return null;
    }

    public synchronized void printAll(String nameFile) {
        try {
            File f = new File(nameFile);

            if (!f.exists())
                f.createNewFile();

            try(FileWriter fr = new FileWriter(f);
                BufferedWriter out = new BufferedWriter(fr)){
                out.write(size()+"\n");
                print(out);

            }

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    private void print(BufferedWriter out) {
        if ( peers == null){

            left.print(out);
            right.print(out);

        } else {
            try {
                for(Peer p : peers) {
                    out.write(p.toString());

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }



    public synchronized void printAllJson(String nameFile) {
        try {
            File f = new File(nameFile);

            if (!f.exists())
                f.createNewFile();

            JSONObject object = new JSONObject();
            JSONArray arr = new JSONArray();


            printJson(arr);

            object.put("peers", arr);
            try (FileWriter file = new FileWriter(f)) {
                object.write(file);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void printJson(JSONArray arr) {
        if ( peers == null){


            left.printJson(arr);
            right.printJson(arr);

        } else {
            for(Peer p : peers) {
                p.printJson(arr);

            }


        }


    }


    public TreeNode left() {
        return left;
    }

    public TreeNode right() {
        return right;
    }


    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(name).append("\n");

        if (peers == null) return sb.toString();

        for (Peer id : peers)
            sb.append(id.getIdString()).append("\n");

        return sb.toString();
    }




    public String getName(){
        return name;
    }


}
