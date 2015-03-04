/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wideokomunikator.audiovideo;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.IPacket;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Packet implements Serializable {

    private final byte[] Data;
    private final boolean KeyPacket;
    private final int UserID;
    private final int PacketId;

    public Packet(IPacket packet, int UserID) {
        this.Data = packet.getData().getByteArray(0, packet.getSize());
        this.KeyPacket = packet.isKeyPacket();
        this.UserID = UserID;
        this.PacketId = 0;
        packet.delete();

    }

    public Packet() {
        this.Data = null;
        this.KeyPacket = false;
        this.UserID = 0;
        this.PacketId = 0;
    }

    public Packet(byte[] Data, boolean KeyPacket, int UserID, int PacketId) {
        this.Data = Data;
        this.KeyPacket = KeyPacket;
        this.UserID = UserID;
        this.PacketId = PacketId;
    }

    public Packet(byte[] data) {
        this.UserID = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
        this.PacketId = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).order(ByteOrder.BIG_ENDIAN).getInt();
        this.KeyPacket = (ByteBuffer.wrap(Arrays.copyOfRange(data, 8, 9)).order(ByteOrder.BIG_ENDIAN).get() == (byte) 1) ? true : false;
        this.Data = Arrays.copyOfRange(data, 9, data.length);
    }

    public IPacket getIPacket() {
        IPacket ipacket = IPacket.make();
        IBuffer buff = IBuffer.make(ipacket, Data.length);
        buff.put(this.Data, 0, 0, Data.length);
        ipacket.setData(buff);
        ipacket.setKeyPacket(this.KeyPacket);
        return ipacket;
    }

    public byte[] getData() {
        return Data;
    }

    public int getUserID() {
        return UserID;
    }

    public boolean isKeyPacket() {
        return KeyPacket;
    }

    public int getPacketId() {
        return PacketId;
    }
    
    

    public byte[] getDataToSend(int PacketId) {
        byte[] counter = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(PacketId).array();
        byte[] userID = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(UserID).array();
        byte[] key = ByteBuffer.allocate(1).order(ByteOrder.BIG_ENDIAN).put(KeyPacket == true ? (byte) 1 : (byte) 0).array();
        //byte isKey = (byte)1;//;
        //System.out.println(isKey);
        byte[] message = new byte[Data.length + counter.length + userID.length + key.length];
        System.arraycopy(userID, 0, message, 0, 4);
        System.arraycopy(counter, 0, message, 4, 4);
        System.arraycopy(key, 0, message, 8, key.length);
        System.arraycopy(Data, 0, message, 9, Data.length);
        return message;
    }

}
