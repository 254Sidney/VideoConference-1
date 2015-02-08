/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wideokomunikator.server.conference;

/**
 *
 * @author Piotr
 */
public class Member {
    private byte[] stream = null; 
    public byte[] getStream(){
        return stream;
    }
    public void setStream(byte[] stream){
        this.stream = stream;
    }
}
