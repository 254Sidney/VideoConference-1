package wideokomunikator.server.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import wideokomunikator.net.Frame;
import wideokomunikator.net.MESSAGE_TITLES;
import wideokomunikator.server.db.Database;
import wideokomunikator.User;
import static wideokomunikator.exception.DatabaseException.*;
import static wideokomunikator.net.MESSAGE_TYPE.*;

/**
 *
 * @author Piotr
 */
public class Response{

    private Frame frame = null;
    private OutputStream output;
    public Response(Frame frame,OutputStream output) throws IOException {
        this.output = output;
        switch(frame.getMESSAGE_TITLE()){
            case LOGIN:{this.frame = Login(frame);}break;
            case REGISTER:{this.frame = Register(frame);}break;
            case SEARCH:{this.frame = Search(frame);}break;
            case FRIENDS_ADD:{this.frame = AddFriend(frame);}break;
            case FRIENDS_LIST:{this.frame = FriendList(frame);}break;
            case FRIENDS_REMOVE:{this.frame = RemoveFriend(frame);}break;
        }
        send(this.frame);
    }
    private Frame Login(Frame frame){
        String email,password;
        String[] split = ((String)frame.getMESSAGE()).split("\n");
        email = split[0];
        password = split[1];
        User user = null;
        try{
            user = (wideokomunikator.User)(Database.getInstance().Sign(email, password));
        }catch(NullPointerException ex){
            if(ERROR_USER_NOT_EXIST.matches(ex.getMessage())){
                return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, ex.getMessage());    
            }
        }
        return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.LOGIN, user);        
    }
    
    private Frame Register(Frame frame){
        String email,password,firstname,lastname;
        String[] split = ((String)frame.getMESSAGE()).split("\n");
        email = split[0];
        firstname = split[1];
        lastname = split[2];
        password = split[3];
        User user = null;
        try{
            user = (wideokomunikator.User)(Database.getInstance().Register(email, firstname, lastname, password));
        }catch(NullPointerException ex){
            if(ERROR_USER_EXIST.matches(ex.getMessage())){ 
                return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, ex.getMessage());    
            }
        }
        return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.REGISTER, user);     
        
    }
    
    private Frame Search(Frame frame) throws IOException{
        String text = (String) frame.getMESSAGE();
        User[] u = Database.getInstance().Search(text.split("[ -]"),frame.getUSER_ID());
        for(int i=0;i<u.length;i++){
            send(new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), frame.getMESSAGE_TITLE(), u[i]));
        }
        return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.FINISH, u.length);    
        
    }
    
    private Frame AddFriend(Frame frame){
        Frame f = null;
        int id = (int) frame.getMESSAGE();
        
        try {
            if (Database.getInstance().addFriend(frame.getUSER_ID(), id)){
                f = new Frame(RESPONSE, 
                        frame.getUSER_ID(), 
                        frame.getMESSAGE_ID(), 
                        MESSAGE_TITLES.OK, 
                        null);
            }else{
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
                ex.getMessage());
        }
        return f;
    }
    private Frame FriendList(Frame frame) throws IOException{        
        User[] u = Database.getInstance().getFriends(frame.getUSER_ID());
        for(int i=0;i<u.length;i++){
            send(new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), frame.getMESSAGE_TITLE(), u[i]));
        }
        return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.FINISH, u.length);   
    }
    
    private Frame RemoveFriend(Frame frame){
        int user_id = (int) frame.getMESSAGE();
        try {
            if(Database.getInstance().removeFriend(frame.getUSER_ID(),user_id)){
                return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.OK, null);
            }else{  
                return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, "Bład podczas usuwania znajomego");
            }
        } catch (SQLException ex) {
            return new Frame(RESPONSE, frame.getUSER_ID(), frame.getMESSAGE_ID(), MESSAGE_TITLES.ERROR, ex.getMessage());
        }
        
    }
    
    public void send(Frame frame) throws IOException{
        ObjectOutputStream oos = new ObjectOutputStream(output);
        oos.writeObject(frame);
    }
    
    
}
