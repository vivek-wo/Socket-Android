package com.vivek.wo.udp;

import java.net.DatagramPacket;

public class ProtocolPacket {

    protected ProtocolPacket() {

    }

    /*是否废弃*/
    boolean isDisCarded() {
        return false;
    }

    /*是否有效*/
    boolean isValid() {
        return true;
    }

    DatagramPacket getPacket() {
        return null;
    }

}
