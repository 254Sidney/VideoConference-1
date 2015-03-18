package wideokomunikator.server.db;

import java.io.Serializable;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.derby.drda.NetworkServerControl;
import static wideokomunikator.exception.DatabaseException.ERROR_USER_EXIST;
import static wideokomunikator.exception.DatabaseException.ERROR_USER_NOT_EXIST_OR_WRONG_PASSWORD;
import wideokomunikator.server.PasswordSecurity;

public class Database implements Serializable{
    
    private transient Connection database_connection;
    private transient NetworkServerControl network_server;
    private transient static Database database = null;
    private transient static final int port = 1527;
    public static Database getInstance() {
        if(database == null){
            database = new Database();
        }
        return database;
    }

    public Database() {
        try {
            network_server = new NetworkServerControl(InetAddress.getByName("localhost"), port);
            network_server.start(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            initConnection();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            ex.printStackTrace();
        }       
    }

    private void initConnection() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        database_connection = DriverManager.getConnection("jdbc:derby://localhost:"+port+"/Wideokomunikator;create=true");
        Statement statement = database_connection.createStatement();
        if (!tableExists("USERS")) {
            statement.executeUpdate(""
                    + "CREATE TABLE USERS("
                    + "ID INT NOT NULL primary key GENERATED ALWAYS AS IDENTITY(START WITH 1, INCREMENT BY 1),"
                    + "EMAIL VARCHAR(50) NOT NULL UNIQUE ,"
                    + "FIRST_NAME VARCHAR(50) NOT NULL ,"
                    + "LAST_NAME VARCHAR(50) NOT NULL ,"
                    + "PASSWORD VARCHAR ("+PasswordSecurity.HASH_SIZE+") FOR BIT DATA NOT NULL"
                    + ")");
        }
        if(!tableExists("FRIENDS")){
            statement.executeUpdate(""
                    + "CREATE TABLE FRIENDS("
                    + "USER_ID INT NOT NULL,"
                    + "FRIEND_ID INT NOT NULL,"
                    + "FOREIGN KEY (USER_ID) REFERENCES USERS(ID),"
                    + "FOREIGN KEY (FRIEND_ID) REFERENCES USERS(ID)"
                    + ")");            
        }        
    }
    
    public wideokomunikator.User Sign (String email,String password)throws NullPointerException{
        return Sign(email, password.toCharArray());
    }
    public wideokomunikator.User Sign(String email,char[] password)throws NullPointerException{
        User u = null;
        try{
            u = new User(email);
        }catch(SQLException ex){
            return null;
        }
        try {
            if(PasswordSecurity.validatePassword(password, u.PASSWORD)){
                return (wideokomunikator.User)u;
            }else{
                return null;
            }
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }
    public wideokomunikator.User Register(String EMAIL,String FIRST_NAME,String LAST_NAME,char[] PASSWORD) throws NullPointerException{
        wideokomunikator.User u = null;
        try {
            u = new User(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        } catch (SQLException ex) {
            return null;
        }
        return u;
    }
    public wideokomunikator.User Register(String EMAIL,String FIRST_NAME,String LAST_NAME,String PASSWORD) throws NullPointerException{
        return Register(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD.toCharArray());
    }
    
    public boolean addFriend(int User_ID,int Friend_ID) throws SQLException{        
        
        PreparedStatement statement = database_connection.prepareStatement("SELECT COUNT(*) AS COUNT FROM FRIENDS WHERE USER_ID = ? AND FRIEND_ID = ?");
        statement.setInt(1, User_ID);
        statement.setInt(2, Friend_ID);
        ResultSet rs = statement.executeQuery();
        rs.next();
        int i = rs.getInt("COUNT");
        statement.close();
        System.out.println(i);
        if(i==0){            
            statement = database_connection.prepareStatement("INSERT INTO FRIENDS "
                    + "(USER_ID,FRIEND_ID) "
                    + "VALUES (?,?)");
            statement.setInt(1, User_ID);
            statement.setInt(2, Friend_ID);
            int update = statement.executeUpdate();
            if(update==1){
                return true;
            }   
            statement.close();
        }else{
            return false;
        }
            
        
        return false;
    }
    public boolean removeFriend(int user_id,int friend_id) throws SQLException{
        PreparedStatement statement = database_connection.prepareStatement("DELETE FROM FRIENDS WHERE USER_ID = ? AND FRIEND_ID = ?");
        statement.setInt(1, user_id);
        statement.setInt(2, friend_id);
        int delete = statement.executeUpdate();
        statement.close();
        if(delete==1){            
            return true;
        }else{
            return false;
        }
    }
    
    public wideokomunikator.User[] Search(String [] text,int user_id){
        ArrayList<wideokomunikator.User> users = new ArrayList<>();
        if(text.length>0){
            String SQL_Statement = "SELECT C1.ID,C1.EMAIL,C1.FIRST_NAME,C1.LAST_NAME, COUNT(C2.ID) AS CO FROM USERS AS C1 LEFT JOIN (";
            for(int i=0;i<text.length;i++){
                if(i>0){
                    SQL_Statement += " UNION ALL "; 
                }
                SQL_Statement += "SELECT * FROM USERS WHERE "
                        + "UPPER(FIRST_NAME) LIKE UPPER(?) OR "
                        + "UPPER(LAST_NAME) LIKE UPPER(?) OR "
                        + "UPPER(EMAIL) LIKE UPPER(?)";
            }

            SQL_Statement+=") AS C2 ON C2.ID = C1.ID "
                    + "GROUP BY C1.ID,C1.LAST_NAME,C1.FIRST_NAME,C1.EMAIL "
                    + "HAVING COUNT(C2.ID) > 0 "
                    + "ORDER BY CO DESC";
            try {
                PreparedStatement statement = database_connection.prepareStatement(SQL_Statement);
                int index = 0;
                for(int i=0;i<text.length;i++){                
                    statement.setString(index+1, "%"+text[i]+"%");
                    statement.setString(index+2, "%"+text[i]+"%");
                    statement.setString(index+3, "%"+text[i]+"%");
                    index+=3;
                }

                ResultSet rs = statement.executeQuery();
                while(rs.next()){
                    int ID = rs.getInt("ID");
                    String EMAIL = rs.getString("EMAIL");
                    String FIRST_NAME = rs.getString("FIRST_NAME");
                    String LAST_NAME = rs.getString("LAST_NAME");
                    if(ID != user_id)
                        users.add(new wideokomunikator.User(ID, EMAIL, FIRST_NAME, LAST_NAME));
                    //System.out.println(ID+" "+EMAIL+" "+FIRST_NAME+" "+LAST_NAME+" "+rs.getInt("CO"));
                }
                rs.close();
                statement.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return users.toArray(new wideokomunikator.User[users.size()]);
    }
    
    public wideokomunikator.User[] getFriends(int user_id){        
        ResultSet rs = null;
        ArrayList<wideokomunikator.User> users = new ArrayList<>();
        try {
            PreparedStatement statement;
            //statement = database_connection.prepareStatement("SELECT * FROM FRIENDS WHERE USER_ID = ?");
            statement = database_connection.prepareStatement("SELECT "
                    + "USERS.ID, "
                    + "USERS.EMAIL, "
                    + "USERS.FIRST_NAME, "
                    + "USERS.LAST_NAME FROM USERS "
                    + "INNER JOIN FRIENDS ON USERS.ID = FRIENDS.FRIEND_ID "
                    + "WHERE USER_ID = ?");
            statement.setInt(1, user_id);
            rs = statement.executeQuery();
            while(rs.next()){
                    wideokomunikator.User user = null;
                    int ID = rs.getInt("ID");
                    String EMAIL = rs.getString("EMAIL");
                    String FIRST_NAME = rs.getString("FIRST_NAME");
                    String LAST_NAME = rs.getString("LAST_NAME");
                    user = new wideokomunikator.User(ID, EMAIL, FIRST_NAME, LAST_NAME);
                    
                users.add(user);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }    
        return users.toArray(new wideokomunikator.User[users.size()]);
    }

    private boolean tableExists(String table) {
        int numRows = 0;
        try {
            DatabaseMetaData dbmd = database_connection.getMetaData();
            // Note the args to getTables are case-sensitive!
            ResultSet rs = dbmd.getTables(null, "APP", table.toUpperCase(), null);
            while (rs.next()) {
                ++numRows;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return numRows > 0;
    }
    
    public void close() throws Exception{
        database_connection.close();
        network_server.shutdown();        
    }

    public class User extends wideokomunikator.User implements Serializable{
        public transient byte[] PASSWORD;

        public User(String EMAIL, String FIRST_NAME, String LAST_NAME, char[] PASSWORD) throws SQLException,NullPointerException {
            byte[] BYTE_PASSWORD = null;
            try {
                BYTE_PASSWORD = PasswordSecurity.getHash(PASSWORD);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
            PreparedStatement statement = database_connection.prepareStatement("INSERT INTO USERS "
                    + "(EMAIL,FIRST_NAME,LAST_NAME,PASSWORD) "
                    + "VALUES (?,?,?,?)");
            statement.setString(1, EMAIL);
            statement.setString(2, FIRST_NAME);
            statement.setString(3, LAST_NAME);
            statement.setBytes(4, BYTE_PASSWORD);
            try{
            statement.executeUpdate();    
            this.ID = new User(EMAIL).ID;
            this.EMAIL = EMAIL;
            this.FIRST_NAME = FIRST_NAME;
            this.LAST_NAME = LAST_NAME;
            this.PASSWORD = BYTE_PASSWORD;
            }catch(java.sql.SQLIntegrityConstraintViolationException ex){
                throw new NullPointerException(ERROR_USER_EXIST);
            }
            statement.close();
        }       
        
        public User(String EMAIL, String FIRST_NAME, String LAST_NAME, String PASSWORD) throws SQLException,NullPointerException {
            this(EMAIL, FIRST_NAME, LAST_NAME,PASSWORD.toCharArray());
        }
        

        public User(String EMAIL) throws SQLException,NullPointerException{
            PreparedStatement statement = database_connection.prepareStatement("SELECT * FROM USERS WHERE EMAIL=?");
            statement.setString(1, EMAIL);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                this.ID = rs.getInt("ID");
                this.EMAIL = rs.getString("EMAIL");
                this.FIRST_NAME = rs.getString("FIRST_NAME");
                this.LAST_NAME = rs.getString("LAST_NAME");
                this.PASSWORD = rs.getBytes("PASSWORD");
            } else {
                throw new NullPointerException(ERROR_USER_NOT_EXIST_OR_WRONG_PASSWORD);
            }
            rs.close();
            statement.close();
        }

        @Override
        public String toString() {
            return ID+" "+EMAIL+" "+FIRST_NAME+" "+LAST_NAME;
        }
        
    }
}
