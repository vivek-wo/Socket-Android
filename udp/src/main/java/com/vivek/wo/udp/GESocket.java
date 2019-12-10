package com.vivek.wo.udp;

import android.os.ConditionVariable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class GESocket {
    private static final int LOCAL_PORT = 8168;
    private DatagramSocket mDatagramSocket = null;
    protected ArrayList<ProtocolPacket> mProtocolPacketList = new ArrayList<>();
    private ConditionVariable mReqCond = new ConditionVariable(false);

    private boolean isReceivedFinished = false;

    public GESocket() {
        try {
            mDatagramSocket = new DatagramSocket(LOCAL_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void setup() {
        if (mDatagramSocket != null) {
            new ReceivedThread().start();
            new SendThread().start();
        }
    }

    public void add(ProtocolPacket protocolPacket) {
        synchronized (mProtocolPacketList) {
            mProtocolPacketList.add(protocolPacket);
        }
        mReqCond.open();
    }

    public void send(DatagramPacket packet) {
        try {
            mDatagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*处理回包*/
    private void remove() {
        ProtocolPacket protocolPacket = null;
        synchronized (mProtocolPacketList) {
            for (ProtocolPacket o : mProtocolPacketList) {
                //TODO 处理回包
            }
            if (protocolPacket != null) {
                mProtocolPacketList.remove(protocolPacket);
            }
        }
    }

    //数据包处理
    private void parseReceivedPacket(InetAddress inetAddress, int port, byte[] receivedData, int
            length) {
    }

    class ReceivedThread extends Thread {
        private static final int RECEIVEPACKET_SIZE = 1024;/*接收包大小*/
        DatagramPacket packet;

        public ReceivedThread() {
            byte[] data = new byte[RECEIVEPACKET_SIZE];
            packet = new DatagramPacket(data, RECEIVEPACKET_SIZE);
        }

        boolean isLocalInetAddress() {
            //TODO 是否本地IP
            return false;
        }

        void printFormatHex(String ip, int port, byte[] receivedData) {
            //TODO 打印
        }

        void replyPacket(InetAddress inetAddress, int port, byte[] receivedData) {
            //TODO 回复确认包
            byte[] idData = new byte[4];
            DatagramPacket idPacket = new DatagramPacket(
                    idData, idData.length, inetAddress, port);
            send(idPacket);
        }

        @Override
        public void run() {
            while (!isReceivedFinished) {
                try {
                    mDatagramSocket.receive(packet);
                    int length = packet.getLength();
                    if (length <= 0) {
                        continue;
                    }
                    byte[] receivedData = new byte[length];
                    System.arraycopy(packet.getData(), 0, receivedData, 0,
                            length);
                    InetAddress inetAddress = packet.getAddress();
                    int port = packet.getPort();
                    // 判断是否是本机自身发出包
                    if (isLocalInetAddress()) {
                        continue;
                    }
                    //打印包
                    printFormatHex(inetAddress.toString(), port, receivedData);
                    // 发送回包
                    replyPacket(inetAddress, port, receivedData);
                    //处理回包
                    remove();
                    //解析包
                    parseReceivedPacket(inetAddress, port, receivedData, length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    class SendThread extends Thread {
        private ProtocolPacket protocolPacket;

        public SendThread() {

        }

        void printFormatHex(String ip, int port, byte[] receivedData) {
            //TODO 打印
        }

        private ProtocolPacket getNextProtocolPacket() {
            synchronized (mProtocolPacketList) {
                for (ProtocolPacket o : mProtocolPacketList) {
                    if (o == null) {
                        continue;
                    } else {
                        return o;
                    }
                }
            }
            return null;
        }

        @Override
        public void run() {
            while (true) {
                synchronized (mProtocolPacketList) {
                    protocolPacket = getNextProtocolPacket();
                    if (protocolPacket == null) {
                        mReqCond.close();
                    }
                }

                if (protocolPacket != null) {
                    if (protocolPacket.isValid()) {
                        try {
                            DatagramPacket packet = protocolPacket.getPacket();
                            mDatagramSocket.send(packet);
                            //
                            printFormatHex(packet.getAddress().toString(),
                                    packet.getPort(), packet.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (protocolPacket.isDisCarded()) {
                        synchronized (mProtocolPacketList) {
                            mProtocolPacketList.remove(protocolPacket);
                        }
                    }
                } else {
                    mReqCond.block();
                }
            }
        }
    }
}
