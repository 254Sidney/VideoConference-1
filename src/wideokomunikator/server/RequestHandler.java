package wideokomunikator.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import javax.net.ssl.SSLSocket;
import wideokomunikator.net.Frame;
import wideokomunikator.server.net.Response;

public class RequestHandler extends Thread {

    private SSLSocket client;
    private OutputStream client_out;
    private InputStream client_in;
    private boolean client_active = true;
    private Frame frame = null;
    private wideokomunikator.User client_id;
    private int frame_id = 1;

    public RequestHandler(SSLSocket client) throws IOException {
        this.client = client;
        //open client streams
        client_in = client.getInputStream();
        client_out = client.getOutputStream();
    }

    public int getNextFrameId(){
        return frame_id+2;
    }
    private RequestHandler get() {
        return this;
    }

    public int ConferenceInit(int[] Users_IDs) {
        wideokomunikator.server.conference.Server server = null;
        try {
            server = new wideokomunikator.server.conference.Server(Users_IDs);
            server.start();
            Server.conferences.add(server);
            return server.getAdress();
        } catch (SocketException ex) {
            ex.printStackTrace();
            if (server != null) {
                server.stopServer();
                server = null;
            }
            return 0;
        }
    }

    public void setUser(wideokomunikator.User user) {
        this.client_id = user;
    }

    public wideokomunikator.User getUser() {
        return client_id;
    }

    public boolean isAvailable(int id) {
        for (RequestHandler request : Server.clientsThreads) {
            wideokomunikator.User user = request.client_id;
            if (user != null) {
                if (user.getID() == id) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (client_active) {
                    try {
                        Frame f = new Frame(client_in);
                        Response r = new Response(f, client_out, get());
                    } catch (ClassNotFoundException | IOException ex) {
                        client_active = false;
                        System.out.println("Client "+getUser()+" "+ex.getMessage());
                    }
                }
                close();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (client_active) {
                    if (frame == null) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    } else {
                        try {
                            frame.send(client_out);
                            client_out.flush();
                            frame = null;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            client_active = false;
                        }
                    }
                }
                close();
            }
        }).start();
    }

    private void close() {
        if (!client.isClosed()) {
            try {
                client_in.close();
                client_out.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Server.clientsActivity.put(this.client_id.getID(), false);
        Server.clientsThreads.remove(this);
    }

    public void sendFrame(Frame frame) {
        frame_id = getNextFrameId();
        this.frame = frame;
    }    
}
