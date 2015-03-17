package wideokomunikator.client;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.HashMap;

public class StreamBuffer<T> {

    private Integer current = -1;
    private T next = null;

    private HashMap<Integer, T> buffor = null;

    public StreamBuffer() {
        buffor = new HashMap<>();
    }

    public void setPacket(int ID, final T packet) {
        if (ID > current) {
            buffor.put(ID, packet);
        }
    }

    public T getPacket(int ID) {
        return buffor.get(ID);
    }

    public final T getNext() {
        T data;
        data = buffor.get(getNextID());
        current += 1;
        return data;
    }

    public T Next() {
        return next;
    }

    public int getCurrentID() {
        return current;
    }

    public int getNextID() {
        return current + 1;
    }

    public boolean isNext() {
        return (next = buffor.get(getNextID())) != null;
    }

    public void skip() {
        if (buffor.isEmpty()) {
            current += 1;
        } else {
            int min = Integer.MAX_VALUE;
            Iterator<Entry<Integer, T>> it = buffor.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Integer, T> item = it.next();
                if ((item.getKey()) < min) {
                    min = item.getKey();
                }
            }
            current = min - 1;
        }
    }

    public synchronized void lastAvaliable() {
        int max = Integer.MIN_VALUE;
        Iterator<Entry<Integer, T>> it = buffor.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, T>  item = it.next();
            if ((item.getKey()) > max) {
                max = item.getKey();
            }
        }
        current = max - 1;
        clean();
    }

    public void clean() {
        Iterator<Entry<Integer, T>> it = buffor.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, T> item = it.next();
            if (item.getKey() < current) {
                it.remove();
            }
        }
    }

    public int size() {
        return buffor.size();
    }

}
