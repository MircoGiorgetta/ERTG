package com.ERTG.ERTG;

import com.ERTG.ERTG.Peer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;

public class Bucket {

    private LinkedList<Peer> bucket = new LinkedList<>();

    public Bucket() {

    }

    public synchronized boolean add(Peer p) {
        if (!bucket.contains(p)) {
            bucket.add(p);
            return true;
        }

        return false;
    }

    public synchronized int size() {
        return bucket.size();
    }

    public synchronized void print(BufferedWriter out) {
        try {
            for (Peer p : bucket) {
                out.write("ID HEX: "+p.getIdStringHex()+"\n");
                out.write("HASH HEX: "+p.getHash256StringHex()+"\n");
                out.write("HASH BIT: "+p.getHash256String()+"\n");
                out.write("----------------------------------------------------\n");

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void printDebug(BufferedWriter out) {
        try {
            for (Peer p : bucket) {
                out.write(p.getIdStringHex()+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void printHashDebug(BufferedWriter out) {
        try {
            for (Peer p : bucket) {
                out.write(p.getHash256StringHex()+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
