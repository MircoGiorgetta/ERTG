package com.ERTG.ERTG;

import com.ERTG.crypto.ECKey;

public class ID {




    private String prvKey = "";
    private String nodeId = "";
    private String externalIp = "";
    private int port = -1;
    private ECKey pubKey=null;


    public ID(){

    }


    public String getPrvKey() {
        return prvKey;
    }

    public void setPrvKey(String prvKey) {
        this.prvKey = prvKey;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getExternalIp() {
        return externalIp;
    }

    public void setExternalIp(String externalIp) {
        this.externalIp = externalIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ECKey getPubKey() {
        return pubKey;
    }

    public void setPubKey(ECKey pubKey) {
        this.pubKey = pubKey;
    }


    @Override
    public boolean equals(Object obj) {
        if(!obj.getClass().isAssignableFrom(ID.class))
            return false;
        ID o = (ID) obj;

        return nodeId.equals(o.getNodeId());
    }

}
