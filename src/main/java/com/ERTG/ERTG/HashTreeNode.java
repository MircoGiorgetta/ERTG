package com.ERTG.ERTG;

import java.util.ArrayList;
import java.util.List;

public class HashTreeNode {

    public int bucketSize=0;

    public static int MAX_KADEMLIA_K = 1;

    //SE IL BIT è UGUALE A 1 VA A SINISTRA
    HashTreeNode left;

    //SE IL BIT è UGUALE A 0 VA A DESTRA
    HashTreeNode right;

    String name;

    List<Peer> peers;


    public HashTreeNode(String name) {

        this.name = name;
        peers = new ArrayList<>();
    }

    public synchronized int size() {
        return bucketSize;
    }



    public synchronized boolean add(Peer peer) {
        if (peer == null) throw new Error("Not a leaf");

        boolean res = false;
        if ( peers == null){

            if (peer.nextBitHash(name) == 1) {
                res = left.add(peer);
            } else {
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
            this.splitBucket();
        }
        return res;
    }


    public void splitBucket() {
        left = new HashTreeNode(name + "1");
        right = new HashTreeNode(name + "0");

        for (Peer id : peers) {
            if (id.nextBitHash(name) == 1) {
                left.add(id);
            } else {
                right.add(id);
            }
        }

        this.peers = null;
    }


    public synchronized boolean contains(Peer peer) {
        if (peer == null) throw new Error("Not a leaf");

        if ( peers == null){

            if (peer.nextBitHash(name) == 1) {
                return left.contains(peer);
            } else {
                return right.contains(peer);
            }

        }

        return peers.contains(peer);

    }

    private Peer getFirstPeerAvailable() {

        if (right == null && left == null) {
            if (peers.size()>0) {
                return peers.get(0);
            } else {
                return null;
            }
        }


        Peer p=null;
        if (left!=null) {
            p=left.getFirstPeerAvailable();
        }
        if (p==null) {
            p=right.getFirstPeerAvailable();
        }
        return p;



    }



    public synchronized Peer getPeerHashStartWith(String pattern) {


        if (pattern.length()> EthConstants.ROUTING_TABLE_SIZE) {
            return null;
        }

        if (pattern.length()==0) {
            if (peers!=null) {
                if (peers.size()>0) {
                    return peers.get(0);
                }
            } else {
                return this.getFirstPeerAvailable();
            }

            return null;

        }

        if (peers!=null) {
            String prefix = name+pattern;
            for (Peer p : peers) {
                if (p.getHash256String().startsWith(prefix)) {
                    return p;
                }
            }
            return null;
        }

        char c = pattern.charAt(0);
        String pattern2;
        if (c=='1') {
            pattern2 = pattern.replaceFirst("1","");
            return left.getPeerHashStartWith(pattern2);
        } else if (c=='0') {
            pattern2 = pattern.replaceFirst("0","");
            return right.getPeerHashStartWith(pattern2);
        }
        return null;



    }


}
