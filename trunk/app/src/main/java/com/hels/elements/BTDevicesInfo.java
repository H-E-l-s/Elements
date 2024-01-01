package com.hels.elements;

public class BTDevicesInfo {

    private String mac;
    private String name;
    private int type;

    public BTDevicesInfo(String mac, String name, int type) {
        this.mac = mac;
        this.name = name;
        this.type = type;
    }

    public void setName(String s) {
        name = s;
    }

    public String getName() {
        return name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
