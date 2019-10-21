package com.ERTG.ERTG;


import com.ERTG.crypto.HashUtil;
import com.ERTG.util.ByteUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.*;


public class Peer {

    private byte[] id;
    private String idString;
    private String idStringHex;
    private InetSocketAddress addr;
    private String addrPortString;
    private long bondingTime;

    private long lastFindNodeMessageSentForGeneration;

    private int port;
    private int timestamp;
    private PeerState state;

    private byte[] hash256;
    private String hash256String;

    private long bondingTryTime;//TIMESTAMP DELL'ULTIMA PROVA DI ISTITUZIONE DEL BONDING
    private boolean triedBonding; //PER CONTROLLARE SE ALMENO UNA VOLTA è STATO PROVATO IL BONDING

    private int numberOfBondingTried = 0;

    Set addressSet = Collections.synchronizedSet(new HashSet());//è POSSIBILE RICEVERE NODI CON UN ID DIVERSO TRA LORO MA CON INDIRIZZO IP E PORTA UGUALI TRA LORO


    public Peer(byte[] id, InetSocketAddress addr){
        addressSet.add(addr);

        this.id = id;
        hash256 = HashUtil.sha3(id);

        BigInteger bi1 = new BigInteger(1, id);
        idString = String.format("%512s", bi1.toString(2));
        idString = idString.replace(' ', '0');

        idStringHex = ByteUtil.toHexString(id);

        BigInteger bi2 = new BigInteger(1, hash256);
        hash256String = String.format("%256s", bi2.toString(2));
        hash256String = hash256String.replace(' ', '0');

        triedBonding=false;


        this.addr = addr;
        this.port = addr.getPort();
        this.addrPortString = addr.getAddress().getHostAddress()+":"+port;
        state=PeerState.INITIAL;
    }

    public synchronized boolean isBondable() {
        boolean res = (state==PeerState.INITIAL && (!triedBonding || System.currentTimeMillis()-bondingTryTime>1000*EthConstants.BONDING_RETRY_INTERVAL));
        if (res) {
            triedBonding = true;
            bondingTryTime = System.currentTimeMillis();
            numberOfBondingTried++;
        }
        return res;
    }

    public synchronized void resetBonding() {
        triedBonding = false;
        bondingTryTime = 0;
        numberOfBondingTried = 0;
        bondingTime=0;
        state=PeerState.INITIAL;
    }

    public boolean addAnotherAddress(InetSocketAddress addr2) {
        return addressSet.add(addr2);
    }

    public Set getAdditionalAddresses() {
        return addressSet;
    }

    public String getAddressPort() {
        return addrPortString;
    }

    public String getIdString() {
        return idString;
    }

    public byte[] getHash256() {
        return hash256;
    }

    public String getHash256String() {
        return hash256String;
    }

    public String getHash256StringHex() {
        return ByteUtil.toHexString(hash256);
    }


    public long getLastFindNodeMessageSentForGeneration() {
        return lastFindNodeMessageSentForGeneration;
    }

    public void setLastFindNodeMessageSentForGeneration(long lastFindNodeMessageSentForGeneration) {
        this.lastFindNodeMessageSentForGeneration = lastFindNodeMessageSentForGeneration;
    }

    public PeerState getState() {
        return state;
    }

    public void setPeerState(PeerState state) {
        if(state == this.state)
            return;
        this.state = state;
    }


    public byte[] getId() {
        return id;
    }

    public String getIdStringHex() {
        return idStringHex;
    }


    public InetSocketAddress getAddress() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return timestamp;
    }

    void setPort(int port) {
        this.port = port;
    }

    public PeerState getPeerState() {
        return state;
    }

    public int getNumberOfBondingTried() {
        return numberOfBondingTried;
    }

    public void setNumberOfBondingTried(int numberOfBondingTried) {
        this.numberOfBondingTried = numberOfBondingTried;
    }

    public void incrementNumberOfBondingTried() {
        numberOfBondingTried++;
    }

    @Override
    public boolean equals(Object obj) {
        if(!obj.getClass().isAssignableFrom(Peer.class))
            return false;
        Peer o = (Peer) obj;

        return idStringHex.equals(o.getIdStringHex());
    }

    @Override
    public String toString() {
        return idStringHex+"@"+addrPortString+" "+state+" "+bondingTime+" "+this.otherAdressesToString()+"\n";
    }


    public String otherAdressesToString() {
        StringBuilder sb=new StringBuilder("[");
        synchronized (addressSet) {
            Iterator i = addressSet.iterator();
            while(i.hasNext()) {
                InetSocketAddress a = (InetSocketAddress) i.next();
                sb.append(a.toString());
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("]");

        }
        return sb.toString();


    }

    public void printJson(JSONArray arr) {
        JSONObject object = new JSONObject();
        object.put("id", idStringHex);
        object.put("ip", addr.getAddress().getHostAddress());
        object.put("port", port);
        object.put("state", state);
        object.put("bondingTime", bondingTime);

        object.put("additionalAddresses", addressSet);

        arr.put(object);
    }


    //RITORNA I PRIMI num BIT DELL'HASH
    public String getFirstBitsHash(int num) {
        if (num==0) {
            return "";
        }
        if (num>0 && num<=EthConstants.ROUTING_TABLE_SIZE) {
            return hash256String.substring(0,num);//LA SOTTOSTRINGA PARTE DAL CARATTERE 0 FINO AL CARATTERE num-1
        }
        return null;
    }


    public byte nextBit(String startPattern) {

        if (this.idString.startsWith(startPattern + "1"))
            return 1;
        else
            return 0;
    }


    public byte nextBitHash(String startPattern) {

        if (this.hash256String.startsWith(startPattern + "1"))
            return 1;
        else
            return 0;
    }


    public long getBondingTime() {
        return bondingTime;
    }


    public void setBondingTime(long bondingTime) {
        this.bondingTime = bondingTime;
    }

}

