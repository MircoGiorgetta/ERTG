package com.ERTG.ERTG;

import com.ERTG.net.rlpx.Message;

import java.net.InetSocketAddress;

public class MessageToWrite {

    private byte type;
    private InetSocketAddress address=null;
    private Peer peer=null;
    private Message message;
    private Peer target;
    private boolean forGeneratePhase = false;
    private RoutingTable table=null;
    private ID from = Main.homeNode;


    public MessageToWrite() {

    }



    public ID getFrom() {
        return from;
    }

    public MessageToWrite setFrom(ID from) {
        this.from = from;
        return this;
    }


    public byte getType() {
        return type;
    }

    public MessageToWrite setType(byte type) {
        this.type = type;
        return this;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public MessageToWrite setAddress(InetSocketAddress address) {
        this.address = address;
        return this;
    }

    public Peer getPeer() {
        return peer;
    }

    public MessageToWrite setPeer(Peer peer) {
        this.peer = peer;
        return this;
    }

    public Message getMessage() {
        return message;
    }

    public MessageToWrite setMessage(Message message) {
        this.message = message;
        return this;
    }

    public Peer getPeerTarget() { return target; }

    public MessageToWrite setPeerTarget(Peer target) {
        this.target = target;
        return this;
    }

    public boolean isForGeneratePhase() {
        return forGeneratePhase;
    }

    public MessageToWrite setForGeneratePhase(boolean forGeneratePhase) {
        this.forGeneratePhase = forGeneratePhase;
        return this;
    }


    public RoutingTable getRoutingTable() {
        return table;
    }

    public MessageToWrite setRoutingTable(RoutingTable table) {
        this.table = table;
        return this;
    }

}
