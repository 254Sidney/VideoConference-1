
package wideokomunikator.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class Frame implements Serializable{
    protected final MESSAGE_TYPE MESSAGE_TYPE;
    protected final int USER_ID;
    protected final int MESSAGE_ID;
    protected final MESSAGE_TITLES MESSAGE_TITLE;
    protected final Object MESSAGE;
    public static final int WAIT_TIME = 60000;

    public Frame() {
        this.MESSAGE_TYPE = null;
        this.USER_ID = 0;
        this.MESSAGE_ID = 0;
        this.MESSAGE_TITLE = null;
        this.MESSAGE = null;
    }

    public Frame(MESSAGE_TYPE MESSAGE_TYPE, int USER_ID, int MESSAGE_ID, MESSAGE_TITLES MESSAGE_TITLE, Object MESSAGE) {
        this.MESSAGE_TYPE = MESSAGE_TYPE;
        this.USER_ID = USER_ID;
        this.MESSAGE_ID = MESSAGE_ID;
        this.MESSAGE_TITLE = MESSAGE_TITLE;
        this.MESSAGE = MESSAGE;
    }
    public Frame(InputStream input) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(input);
        Frame f = (Frame)ois.readObject();
        this.MESSAGE = f.MESSAGE;
        this.MESSAGE_ID = f.MESSAGE_ID;
        this.MESSAGE_TITLE = f.MESSAGE_TITLE;
        this.MESSAGE_TYPE = f.MESSAGE_TYPE;
        this.USER_ID = f.USER_ID;
    }    
    
    public void send(OutputStream output) throws IOException{
        ObjectOutputStream oos = new ObjectOutputStream(output);
        oos.writeObject(this);
        oos.flush();
    }

    @Override
    public String toString() {
        return "MESSAGE ID \t= "+MESSAGE_ID+
                "\nMESSAGE_TYPE \t= "+MESSAGE_TYPE+
                "\nMESSAGE_TITLE \t= "+MESSAGE_TITLE+
                "\nMESSAGE \t= "+MESSAGE;
    }

    public MESSAGE_TITLES getMESSAGE_TITLE() {
        return MESSAGE_TITLE;
    }

    public Object getMESSAGE() {
        return MESSAGE;
    }

    public int getUSER_ID() {
        return USER_ID;
    }

    public int getMESSAGE_ID() {
        return MESSAGE_ID;
    }

    public MESSAGE_TYPE getMESSAGE_TYPE() {
        return MESSAGE_TYPE;
    }
    
}