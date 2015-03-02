package wideokomunikator.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StreamBuffer <T> {

    private int current = -1;

    private HashMap<Integer, T> buffor = null;

    public StreamBuffer() {
        buffor = new HashMap<Integer, T>();
    }

    public void setPacket(int ID, T packet) {
        if (ID > current) {
            buffor.put(ID, packet);
        }
    }

    public T getPacket(int ID) {
        return buffor.getOrDefault(ID, null);
    }

    public synchronized T getNext() {
        T data = buffor.get(getNextID());
        current += 1;
        clean();
        return data;
    }

    public synchronized int getNextID() {
        return current + 1;
    }

    public synchronized boolean isNext() {
        return buffor.get(getNextID()) != null;
    }
    
    private synchronized void clean(){
        Iterator it = buffor.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pair = (Map.Entry)it.next();
            if(((int)pair.getKey())<current-1){
                it.remove();
            }else if(((int)pair.getKey())>=current){
                return;
            }
        }
    }
    
}
