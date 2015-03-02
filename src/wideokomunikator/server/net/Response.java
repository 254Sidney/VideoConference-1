package wideokomunikator.server.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import wideokomunikator.net.Frame;
import wideokomunikator.net.MESSAGE_TITLES;
import wideokomunikator.server.db.Database;
import wideokomunikator.User;
import static wideokomunikator.exception.DatabaseException.*;
import static wideokomunikator.net.MESSAGE_TYPE.*;
import wideokomunikator.server.RequestHandler;

/**
 *
 * @author Piotr
 */
public class Response {

    private Frame frame = null;
    private OutputStream output;
    private RequestHandler request;

    public Response(Frame frame, OutputStream output, RequestHandler request) throws IOException {
        this.request = request;
        this.output = output;
        //System.out.println("Receiving from"+frame.getMESSAGE_ID()+" \n"+frame);
        switch (frame.getMESSAGE_TITLE()) {
            case LOGIN: {
                this.frame = Login(frame);
            }
            break;
            case REGISTER: {
                this.frame = Register(frame);
            }
            break;
            case SEARCH: {
                this.frame = Search(frame);
            }
            break;
            case FRIENDS_ADD: {
                this.frame = AddFriend(frame);
            }
            break;
            case FRIENDS_LIST: {
                this.frame = FriendList(frame);
            }
            break;
            case FRIENDS_REMOVE: {
                this.frame = RemoveFriend(frame);
            }
            break;
            case CONFERENCE_INIT: {
                this.frame = ConferenceInit(frame);
            }
            break;
        }
        send(this.frame);
    }

    private Frame Login(Frame frame) {
        String email, password;
        String[] split = ((String) frame.getMESSAGE()).split("\n");
        email = split[0];
        password = split[1];
        User user = null;
        try {
            user = (wideokomunikator.User) (Database.getInstance().Sign(email, password));
        } catch (NullPointerException ex) {
            if (ERROR_USER_NOT_EXIST.matches(ex.getMessage())) {
                return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, ex.toString());
            }
        }
        request.setUser(user);
        System.out.println(user);
        return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.LOGIN, user);
    }

    private Frame Register(Frame frame) {
        String email, password, firstname, lastname;
        String[] split = ((String) frame.getMESSAGE()).split("\n");
        email = split[0];
        firstname = split[1];
        lastname = split[2];
        password = split[3];
        User user = null;
        try {
            user = (wideokomunikator.User) (Database.getInstance().Register(email, firstname, lastname, password));
        } catch (NullPointerException ex) {
            if (ERROR_USER_EXIST.matches(ex.getMessage())) {
                return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, ex.toString());
            }
        }
        return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.REGISTER, user);

    }

    private Frame Search(Frame frame) throws IOException {
        String text = (String) frame.getMESSAGE();
        User[] u = Database.getInstance().Search(text.split("[ -]"), frame.getUSER_ID());
        for (int i = 0; i < u.length; i++) {
            send(new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), frame.getMESSAGE_TITLE(), u[i]));
        }
        return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.FINISH, u.length);

    }

    private Frame AddFriend(Frame frame) {
        Frame f = null;
        int id = (int) frame.getMESSAGE();

        try {
            if (Database.getInstance().addFriend(frame.getUSER_ID(), id)) {
                f = new Frame(RESPONSE,
                        frame.getUSER_ID(),
                        frame.getMESSAGE_ID(),
                        MESSAGE_TITLES.OK,
                        null);
            } else {
                f = new Frame(RESPONSE,
                        frame.getUSER_ID(),
                        frame.getMESSAGE_ID(),
                        MESSAGE_TITLES.ERROR,
                        "Wystąpił bład podczas dodawania znajomego do bazy danych, spróbuj ponownie później");

            }

        } catch (SQLException ex) {
            f = new Frame(RESPONSE,
                    frame.getUSER_ID(),
                    frame.getMESSAGE_ID(),
                    MESSAGE_TITLES.ERROR,
                    ex.toString());
        }
        return f;
    }

    private Frame FriendList(Frame frame) throws IOException {
        User[] u = Database.getInstance().getFriends(frame.getUSER_ID());
        for (int i = 0; i < u.length; i++) {
            send(new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), frame.getMESSAGE_TITLE(), u[i]));
        }
        return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.FINISH, u.length);
    }

    private Frame RemoveFriend(Frame frame) {
        int user_id = (int) frame.getMESSAGE();
        try {
            if (Database.getInstance().removeFriend(frame.getUSER_ID(), user_id)) {
                return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.OK, null);
            } else {
                return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, "Bład podczas usuwania znajomego");
            }
        } catch (SQLException ex) {
            return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, ex.toString());
        }

    }

    private Frame ConferenceInit(Frame frame) {
        try {
            String[] split = ((String) frame.getMESSAGE()).split("\n");
            int users_id[] = new int[split.length + 1];
            users_id[0] = frame.getUSER_ID();
            for (int i = 1; i < users_id.length; i++) {
                users_id[i] = Integer.parseInt(split[i-1]);
            }
            int adres = request.ConferenceInit(users_id);

            
            for (int i = 1; i < users_id.length; i++) {
                for (int j = 0; j < wideokomunikator.server.Server.clients.size(); j++) {
                    RequestHandler req = wideokomunikator.server.Server.clients.get(j);
                    if (req.getUser().getID() == users_id[i]) {
                        req.sendFrame(new Frame(REQUEST, frame.getUSER_ID(), req.getNextFrameId(), MESSAGE_TITLES.CONFERENCE_INIT, adres));
                    }
                }
            }
            return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.OK, adres);
        } catch (Exception ex) {
            return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, ex.toString());
        }
    }

    public void send(Frame frame) throws IOException {
        //System.out.println("Sending to"+frame.getMESSAGE_ID()+" \n"+frame);
        ObjectOutputStream oos = new ObjectOutputStream(output);
        oos.writeObject(frame);
        oos.flush();
        output.flush();
    }
}
