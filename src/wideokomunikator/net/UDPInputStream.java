/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wideokomunikator.net;

import java.io.InputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPInputStream extends InputStream {

    private static final int PACKET_BUFFER_SIZE = 5000;

    DatagramSocket dsock = null;
    DatagramPacket dpack = null;

    byte[] ddata = new byte[PACKET_BUFFER_SIZE];
    int packSize = 0;
    int packIdx = 0;

    int value;

    /**
     * ******************** constructors *******************
     */
    /*
     *****************************************************************
     ***                ***
     ***  Name :  UDPInputStream                                 ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       Default constructor.                                ***
     ***                ***
     *****************************************************************
     */
    public UDPInputStream() {
    }

    /*
     *****************************************************************
     ***                ***
     ***  Name :  UDPInputStream                                 ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       Constructor.  Requires the address and port of the  ***
     ***   UDP socket to read from.                                ***
     ***                ***
     *****************************************************************
     */
    public UDPInputStream(String address, int port)
            throws UnknownHostException, SocketException {

        open(address, port);
    }

    /**
     * ********** opening and closing the stream ***********
     */
    /*
     *****************************************************************
     ***                ***
     ***  Name :  open                                             ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       The user may use this method to set the address and ***
     ***   port of the UDP socket to read from.                    ***
     ***                ***
     *****************************************************************
     */
    public void open(String address, int port)
            throws UnknownHostException, SocketException {

        dsock = new DatagramSocket(port, InetAddress.getByName(address));
    }

    /*
     *****************************************************************
     ***                ***
     ***  Name :  close                                       ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       Close the UDP socket and UDPInputStream.            ***
     ***                ***
     *****************************************************************
     */
    public void close() throws IOException {
        dsock.close();
        dsock = null;
        ddata = null;
        packSize = 0;
        packIdx = 0;
    }

    /**
     * **** reading, skipping and checking available data *****
     */
    /*
     *****************************************************************
     ***                ***
     ***  Name :  available                                 ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       Determines how many more values may be read before  ***
     ***   next blocking read.                                     ***
     ***                ***
     *****************************************************************
     */
    public int available() throws IOException {
        return packSize - packIdx;
    }

    /*
     *****************************************************************
     ***                ***
     ***  Name :  read()                                     ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       Reads the next value available.  Returns the value  ***
     ***   as an integer from 0 to 255.                            ***
     ***                ***
     *****************************************************************
     */
    public int read() throws IOException {
        System.out.println("read");
        if (packIdx == packSize) {
            receive();
        }

        value = ddata[packIdx] & 0xff;
        packIdx++;
        return value;
    }

    /*
     *****************************************************************
     ***                ***
     ***  Name :  read(byte[])                                 ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       Reads the next buff.length values into the input    ***
     ***   byte array, buff.                                       ***
     ***                ***
     *****************************************************************
     */
    public int read(byte[] buff) throws IOException {
        System.out.println("read buff");
        return read(buff, 0, buff.length);
    }

    /*
     *****************************************************************
     ***                ***
     ***  Name :  read(byte[], int, int)                         ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       Reads the next len values into the input byte array,***
     ***   buff, starting at offset off.                           ***
     ***                ***
     *****************************************************************
     */
    public int read(byte[] buff, int off, int len) throws IOException {
        System.out.println("read buff,off,len " + buff.length + " " + off + " " + len);
        if (packIdx == packSize) {
            receive();
        }

        int lenRemaining = len;

        while (available() < lenRemaining) {
            System.arraycopy(ddata,
                    packIdx,
                    buff,
                    off + (len - lenRemaining),
                    available());
            lenRemaining -= available();
            receive();
        }

        System.arraycopy(ddata,
                packIdx,
                buff,
                off + (len - lenRemaining),
                lenRemaining);
        packIdx += lenRemaining;
        System.out.println("len = " + len);
        return len;
    }

    /*
     *****************************************************************
     ***                ***
     ***  Name :  skip                                       ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       Skips over the next len values.                     ***
     ***                ***
     *****************************************************************
     */
    public long skip(long len) throws IOException {
        System.out.println("skip");
        if (packIdx == packSize) {
            receive();
        }

        long lenRemaining = len;

        while (available() < lenRemaining) {
            lenRemaining -= available();
            receive();
        }

        packIdx += (int) lenRemaining;
        return len;
    }

    /**
     * **************** receiving more data *****************
     */
    /*
     *****************************************************************
     ***                ***
     ***  Name :  receive                                    ***
     ***  By   :  U. Bergstrom   (Creare Inc., Hanover, NH)  ***
     ***  For  :  E-Scan            ***
     ***  Date :  October, 2001          ***
     ***                ***
     ***  Copyright 2001 Creare Inc.        ***
     ***  All Rights Reserved          ***
     ***                ***
     ***  Description :            ***
     ***       A blocking read to receive more data from the UDP   ***
     ***   socket.                                                 ***
     ***                ***
     *****************************************************************
     */
    private void receive() throws IOException {
        dpack = new DatagramPacket(ddata, PACKET_BUFFER_SIZE);
        dsock.receive(dpack);
        packIdx = 0;
        packSize = dpack.getLength();
        System.out.println("size = " + packSize);
    }

    /**
     * ******* marking and reseting are unsupported *******
     */
    public void mark(int readlimit) {
    }

    public void reset() throws IOException {
        throw new IOException("Marks are not supported by UDPInputStream.");
    }

    public boolean markSupported() {
        return false;
    }

}
