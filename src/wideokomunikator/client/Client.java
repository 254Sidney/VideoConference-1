package wideokomunikator.client;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import wideokomunikator.User;
import static wideokomunikator.exception.DatabaseException.*;
import wideokomunikator.net.MESSAGE_TITLES;
import wideokomunikator.net.MESSAGE_TYPE;

public class Client extends JFrame{
    private ClientConnection connection;
    private User user = null;
    private ArrayList<Friend> friends;
    private Point location;
    private Dimension window_size;
    public Client() {
        friends = new ArrayList<Friend>();
        //friends.add(new Friend(new User(1, "piotr-wilczynski@gazeta.pl", "Piotr", "Wilczyński")));
        friends.add(new Friend(new User(20, "andrzej-wilczyński@gmail.com", "Andrzej", "Wilczyński")));
        friends.add(new Friend(new User(21, "jan_kowalski@wp.pl", "Jan", "Kowalski")));
        friends.add(new Friend(new User(22, "knowak@o2.pl", "Karol", "Nowak")));
        friends.add(new Friend(new User(23, "malysza@interia.pl", "Adam", "Małysz")));
        friends.add(new Friend(new User(24, "pudzian@gmail.com", "Mariusz", "Pudzianowski")));
        initConnection();
        initComponents();
    }
    
    private void initComponents(){
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        setSize(1000, 600);
        setMinimumSize(new Dimension(800, 400));
        panel_login = new Login();
        panel_register = new Register();
        setInTheMiddle();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPanel(panel_login);
        //setUser(new User(1, "wilczynskipio@gmail.com", "Piotr", "Wilczyński"));
        //setPanel(panel_user);
    }
    
    private void initConnection(){
        try {        
            //connection = new ClientConnection("192.168.56.101",5000);
            connection = new ClientConnection("localhost",5000);
            connection.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void setPanel(Container panel){
        setContentPane(panel);
        getContentPane().revalidate(); 
        getContentPane().repaint();
        
    }
    
    public void setFullScreanView(boolean value,ConferenceView component){
        dispose();
        setUndecorated(value);
        if(value){
            window_size = getSize();
            location = getLocation();
            setPanel(panel_user.view);
            setSize(Toolkit.getDefaultToolkit().getScreenSize());  
            setLocation(0, 0);
        }else{
            setPanel(panel_user);
            setSize(window_size);
            setLocation(location);
            panel_user.splitpane.setRightComponent(panel_user.view);
        }
        setVisible(true);        
    }
    
    private void setInTheMiddle(){
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((d.width-getWidth())/2, (d.height-getHeight())/2);
        
    }
    
    private void setUser(User user){
        this.user = user;
        setTitle(user.getFIRST_NAME()+" "+user.getLAST_NAME()+" ("+user.getEMAIL()+")");
        panel_user = new UserPanel();
        getFriends();
        //panel_user.view.start();
    }
    
    private void getFriends(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Friend> list = new ArrayList<>();
                wideokomunikator.net.Frame frame = new wideokomunikator.net.Frame(
                        MESSAGE_TYPE.REQUEST, 
                        user.getID(), 
                        connection.getNextFrameId(), 
                        MESSAGE_TITLES.FRIENDS_LIST, null);
                connection.sendFrame(frame);
                int index = frame.getMESSAGE_ID();
                int time = wideokomunikator.net.Frame.WAIT_TIME;
                while(frame.getMESSAGE_TITLE()!=MESSAGE_TITLES.FINISH&&time>0){
                    wideokomunikator.net.Frame f = null;
                    time = wideokomunikator.net.Frame.WAIT_TIME;
                    while((f = connection.getFrame(index))==null&&time>0){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {}
                        time-=10;
                    }
                    if(f==null){
                        showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                        break;                        
                    }
                    frame = f;
                    if(frame.getMESSAGE_TITLE()==MESSAGE_TITLES.FRIENDS_LIST){
                        User user = (User) frame.getMESSAGE();
                        list.add(new Friend(user));
                    }else if(frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR){
                        showErrorDialog(frame.getMESSAGE());
                    }else if(frame.getMESSAGE_TITLE() == MESSAGE_TITLES.FINISH){
                        break;                        
                    }
                }                
                Collections.sort(list);
                friends = list;
                panel_user.setFriends(friends.toArray(new Friend[friends.size()]));

            }
        }).start();
        
        
    }
    
    private void showErrorDialog(Object error){
        JOptionPane.showMessageDialog(this, error, "Błąd", JOptionPane.ERROR_MESSAGE);
    }
    private void showMessageDialog(Object message){
        JOptionPane.showMessageDialog(this, message, "Wiadomość", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private Login panel_login;
    private Register panel_register;
    private UserPanel panel_user;
    
    private class UserPanel extends JPanel{

        public UserPanel() {
            initComponents();
        }
        private void initComponents(){
            friends_panel = new JList(list_model = new DefaultListModel<Friend>());
            friends_panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
            friends_panel.setCellRenderer(new CellRenderer());
            initPopup();
            friends_panel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if(e.getButton()==MouseEvent.BUTTON3){                        
                        friends_panel.setSelectedIndex(friends_panel.locationToIndex(e.getPoint()));
                        popup.show(friends_panel, e.getX(), e.getY());
                    }
                }            
            });
            JScrollPane scroll_pane = new JScrollPane(friends_panel);
            view = new ConferenceView(600, 600){
                @Override
                public void setFullScrean(boolean value) {
                    setFullScreanView(value, this);
                }                
            };
            splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,true, scroll_pane, view);
            GroupLayout l = new GroupLayout(this);
            setLayout(l);
            scroll_pane.setMinimumSize(new Dimension(256, 400));
            view.setMinimumSize(new Dimension(512, 512));
            l.setHorizontalGroup(l.createSequentialGroup().addComponent(splitpane));
            l.setVerticalGroup(l.createSequentialGroup().addComponent(splitpane));
            
            splitpane.setDividerSize(10);
            initMenu();
            
        }
        
        private void initMenu(){
            menubar = new JMenuBar();
            setJMenuBar(menubar);
            JMenu menu;
            JMenuItem item;
            menubar.add(menu = new JMenu("Program"));
            menubar.add(menu = new JMenu("Znajomi"));
            menu.add(item = new JMenuItem("Szukaj"));
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    search = new SearchDialog(){
                        private SwingWorker<Friend[],Friend> worker;
                        private ArrayList<Friend> searchlist;
                        @Override
                        public void getUsers(final String username) {
                            if(worker!=null){
                                worker.cancel(true);
                            }
                            
                            ArrayList<Friend> list = new ArrayList<>();
                            worker = new SwingWorker<Friend[],Friend>() {

                                @Override
                                protected Friend[] doInBackground() throws Exception {
                                    wideokomunikator.net.Frame f = new wideokomunikator.net.Frame(
                                            MESSAGE_TYPE.REQUEST, 
                                            user.getID(),
                                            connection.getNextFrameId(),
                                            MESSAGE_TITLES.SEARCH,
                                            username);
                                    connection.sendFrame(f);
                                    int index = f.getMESSAGE_ID();
                                    
                                    while(!this.isDone()){
                                        int time = wideokomunikator.net.Frame.WAIT_TIME;
                                        while((f=connection.getFrame(index))==null&&time>0){
                                            time-=10;
                                            Thread.sleep(10);
                                        };
                                        
                                        if(f==null){
                                            showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                                            break;
                                        }
                                        if(f.getMESSAGE_TITLE()==MESSAGE_TITLES.FINISH){
                                            break;
                                        }else if(f.getMESSAGE_TITLE() == MESSAGE_TITLES.SEARCH){
                                            User u = (User) f.getMESSAGE();
                                            Friend friend = new Friend(u);
                                            list.add(friend);
                                            publish(friend);
                                        }else if(f.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR){
                                            String error = (String) f.getMESSAGE();
                                            showErrorDialog(error);
                                        }                                        
                                    }
                                    return list.toArray(new Friend[list.size()]);
                                }
                                @Override
                                protected void process(java.util.List<Friend> chunks) {
                                    search.setList(chunks.toArray(new Friend[chunks.size()]));
                                }
                                @Override
                                protected void done() {
                                    Friend[] list = new Friend[]{};
                                    if(!this.isCancelled()){
                                        try {
                                            list =  (Friend[]) this.get();
                                        } catch (InterruptedException | ExecutionException ex) {
                                            ex.printStackTrace();
                                        }
                                        search.setList(list);
                                    }
                                }
                                
                            };
                            worker.execute();
                        }

                        @Override
                        public void addUser(User friend) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    wideokomunikator.net.Frame f = new wideokomunikator.net.Frame(
                                                   MESSAGE_TYPE.REQUEST, 
                                                   user.getID(),
                                                   connection.getNextFrameId(),
                                                   MESSAGE_TITLES.FRIENDS_ADD,
                                                   friend.getID());
                                    connection.sendFrame(f);
                                    int index = f.getMESSAGE_ID();
                                    int time = wideokomunikator.net.Frame.WAIT_TIME;
                                    while((f=connection.getFrame(index))==null&&time>0){                                        
                                        try {
                                            Thread.sleep(10);
                                            time-=10;
                                        } catch (InterruptedException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                    if(f==null){
                                        showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                                        return;
                                    }
                                    
                                    if(f.getMESSAGE_TITLE() == MESSAGE_TITLES.OK){
                                        getFriends();     
                                        
                                    }else if(f.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR){
                                        String error = (String) f.getMESSAGE();
                                        showErrorDialog(error);
                                    }                                
                                }
                            }).start();
                            
                        }
                        
                    };
                }
            });
            menubar.add(menu = new JMenu("Konferencje"));
            menubar.add(menu = new JMenu("Widok"));
            menubar.add(menu = new JMenu("Narzędzia"));
            
        }
        
        private void initPopup(){
            popup = new JPopupMenu("Menu");
            JMenuItem item;
            popup.add(item = new JMenuItem("Rozpocznij Rozmowe"));
            popup.add(item = new JMenuItem("Usuń"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            User u = ((Friend)friends_panel.getSelectedValue()).getUser();
                            wideokomunikator.net.Frame frame = new wideokomunikator.net.Frame(
                                    MESSAGE_TYPE.REQUEST,
                                    user.getID(), 
                                    connection.getNextFrameId(), 
                                    MESSAGE_TITLES.FRIENDS_REMOVE, 
                                    u.getID());
                            connection.sendFrame(frame);
                            int index = frame.getMESSAGE_ID();
                            int time = wideokomunikator.net.Frame.WAIT_TIME;
                            while((frame = connection.getFrame(index))==null && time>0){
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException ex) {}
                                time-=10;                            
                            }
                            if(frame == null){
                                showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                                return;                        
                            }else if(frame.getMESSAGE_TITLE() == MESSAGE_TITLES.OK){
                                getFriends();
                            }else if(frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR){
                                showErrorDialog(frame.getMESSAGE());
                            }
                        }                       
                    }).start();
                }
            });
        }
        private void setFriends(Friend[] list){        
        ArrayList<Friend> l = new ArrayList(Arrays.asList(list));
        for(int i = 0;i<list_model.size();i++){
            Friend f = list_model.get(i);
            if(!l.contains(f)){
                list_model.removeElement(f);
                i--;
            }else{
                l.remove(f);
            }
        }
        for(Friend f:l){
            int index = 0;
            if(list_model.size()>0){
                for(int i = 0;i<list_model.size();i++){
                    if(f.compareTo(list_model.get(i))<0){
                        index++;
                    }else{
                        list_model.add(index,f);                    
                        break;
                    }
                }
            }else{
                list_model.addElement(f);      
                
            }
        }
            friends_panel.setListData(friends.toArray());
        }

        public ConferenceView getView() {
            return view;
        }
        private SearchDialog search;
        private JSplitPane splitpane;
        private JList friends_panel;
        private DefaultListModel<Friend> list_model;
        private ConferenceView view;
        private JMenuBar menubar;
        private JPopupMenu popup;
    }
    
    private class Login extends JPanel{       
        private final int COLUMN_SIZE = 20;
        private Font font;
        public Login() {
            this("", "");
        }
        public Login(String login) {
            this(login, "");
        }
        
        public Login(String login,String password) {
            this.email = new JTextField(login);
            this.password = new JPasswordField(password);   
            initComponents();
        }
        
        private void initComponents(){
            JPanel panel = new JPanel();
            this.setLayout(new GridBagLayout());
            this.add(panel);
            JLabel label_email = new JLabel("Email");
            font = label_email.getFont();
            font = new Font(font.getName(), font.getStyle(), 24);
            JLabel label_password = new JLabel("Hasło");
            button_login = new JButton("Zaloguj");
            register = new JLabel("Utwórz konto");
            register.setCursor(new Cursor(Cursor.HAND_CURSOR));
            register.setForeground(Color.BLUE);
            email.setColumns(COLUMN_SIZE);
            password.setColumns(COLUMN_SIZE);
            label_email.setFont(font);
            label_password.setFont(font);
            email.setFont(font);
            password.setFont(font);
            button_login.setFont(font);
            register.setFont(font);    
            
            panel.add(email);
            panel.add(password);
            GroupLayout l = new GroupLayout(panel);
            panel.setLayout(l);
            l.setAutoCreateContainerGaps(true);
            l.setHorizontalGroup(l.createParallelGroup()
                .addComponent(label_email)
                .addComponent(email)
                .addComponent(label_password)
                .addComponent(password)
                .addGroup(l.createSequentialGroup()
                    .addComponent(button_login)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(register)));
            l.setVerticalGroup(l.createSequentialGroup()
                .addComponent(label_email)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(email)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(label_password)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(password)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(l.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(button_login)
                    .addComponent(register)));
            initActions();
        }
        
        private void login(String email,char[] password){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    button_login.setEnabled(false);
                    wideokomunikator.net.Frame f = new wideokomunikator.net.Frame(
                        wideokomunikator.net.MESSAGE_TYPE.REQUEST,
                        0,
                        connection.getNextFrameId(),
                        MESSAGE_TITLES.LOGIN, email+"\n"+new String(password));
                    connection.sendFrame(f);   
                    int index = f.getMESSAGE_ID();
                    wideokomunikator.net.Frame frame;
                    int time = wideokomunikator.net.Frame.WAIT_TIME;
                    while((frame=connection.getFrame(index))==null&&time>0){
                        try {
                            Thread.sleep(10);
                            time-=10;
                        } catch (InterruptedException ex) {}
                    }
                    if(frame==null){
                        showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                        return;
                    }
                    User user = null;
                    try{
                        user = (User) frame.getMESSAGE();
                    }catch(Exception e){}
                    if(frame.getMESSAGE_TITLE()==MESSAGE_TITLES.LOGIN){
                        if(user != null){
                            setUser(user);
                            setPanel(panel_user); 
                            panel_login.clear();
                        }
                    }else if(frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR){
                        String message = (String) frame.getMESSAGE();
                        if(ERROR_USER_NOT_EXIST.matches(message)){
                            showErrorDialog("Błędne login lub hasło");
                        }else{
                            showErrorDialog(message);
                        }
                    }
                    button_login.setEnabled(true);
                }
            }).start();
        }
        private void login(){
            login(email.getText(), password.getPassword());            
        }
        private void initActions(){
            register.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setPanel(panel_register);
                }
                
            });
            button_login.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    login();
                }
            });
            email.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    login();
                }
            });
            password.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    login();
                }
            });
        }
        public void clear(){
            email.setText("");
            password.setText("");
        }
        private JTextField email;
        private JPasswordField password;
        private JButton button_login;
        private JLabel register;
    }
    
    private class Register extends JPanel{
        private final int COLUMN_SIZE = 20;
        private Font font;

        public Register() {
            initComponents();
        }
        
        private void initComponents(){            
            JPanel panel = new JPanel();
            this.setLayout(new GridBagLayout());
            this.add(panel);            
            firstname = new JTextField(COLUMN_SIZE);
            lastname = new JTextField(COLUMN_SIZE);
            email = new JTextField(COLUMN_SIZE);
            password = new JPasswordField(COLUMN_SIZE);
            password_repeated = new JPasswordField(COLUMN_SIZE);
            JLabel label_firstname = new JLabel("Imię");
            JLabel label_lastname = new JLabel("Nazwisko");
            JLabel label_email = new JLabel("E-mail");
            JLabel label_password = new JLabel("Hasło");
            JLabel label_password_repeated = new JLabel("Wprowadź hasło ponownie");
            button_register = new JButton("Utwórz konto");
            login = new JLabel("Cofnij");
            font = label_firstname.getFont();
            font = new Font(font.getName(), font.getStyle(), 24);
            firstname.setFont(font);
            lastname.setFont(font);
            email.setFont(font);
            password.setFont(font);
            password_repeated.setFont(font);
            button_register.setFont(font);
            login.setFont(font);
            label_firstname.setFont(font);
            label_lastname.setFont(font);
            label_email.setFont(font);
            label_password.setFont(font);
            label_password_repeated.setFont(font);
            
            login.setForeground(Color.BLUE);
            login.setCursor(new Cursor(Cursor.HAND_CURSOR));
            login.setBorder(new EmptyBorder(0, 20, 0, 20));
            
            GroupLayout l = new GroupLayout(panel);
            panel.setLayout(l);
            l.setHorizontalGroup(l.createParallelGroup()
                .addComponent(label_firstname)
                .addComponent(firstname)
                .addComponent(label_lastname)
                .addComponent(lastname)
                .addComponent(label_email)
                .addComponent(email)
                .addComponent(label_password)
                .addComponent(password)
                .addComponent(label_password_repeated)
                .addComponent(password_repeated)
                .addGroup(l.createSequentialGroup()
                    .addComponent(button_register)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(login)));
            
            l.setVerticalGroup(l.createSequentialGroup()
                .addComponent(label_firstname)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(firstname)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(label_lastname)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lastname)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(label_email)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(email)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(label_password)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(password)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(label_password_repeated)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(password_repeated)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(l.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(button_register)
                    .addComponent(login)));
            initActions();
        }
        
        private void initActions(){
            login.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    setPanel(panel_login);
                }                
            });
            button_register.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    register(email.getText(), firstname.getText(), lastname.getText(), password.getPassword());
                }
            });
        }
        private void register(String email,String firstname,String lastname,char[] password){
            new Thread(new Runnable() {

                @Override
                public void run() {
                    button_register.setEnabled(false);
                    wideokomunikator.net.Frame f = new wideokomunikator.net.Frame(
                        wideokomunikator.net.MESSAGE_TYPE.REQUEST,
                        0, 
                        connection.getNextFrameId(), 
                        MESSAGE_TITLES.REGISTER, 
                        email+"\n"+firstname+"\n"+lastname+"\n"+new String(password));
                    connection.sendFrame(f); 
                    int index = f.getMESSAGE_ID();
                    wideokomunikator.net.Frame frame;
                    int time = wideokomunikator.net.Frame.WAIT_TIME;
                    while((frame=connection.getFrame(index))==null&&time>0){
                        try {
                            Thread.sleep(10);
                            time-=10;
                        } catch (InterruptedException ex) {}
                    }
                    if(frame==null){
                        showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                        return;
                    }
                    User user = null;
                    try{
                    user = (User) frame.getMESSAGE();
                    }catch(Exception e){}
                    if(frame.getMESSAGE_TITLE()==MESSAGE_TITLES.REGISTER){
                        if(user != null){
                            setUser(user);
                            setPanel(panel_user);      
                            panel_register.clear();
                        }
                    }else if(frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR){
                        String message = (String) frame.getMESSAGE();
                        if(ERROR_USER_EXIST.matches(message)){
                            showErrorDialog("Konto o podanym emailu już istnieje");
                        }else{
                            showErrorDialog(message);                            
                        }
                    }
                    button_register.setEnabled(true);
                }
            }).start();              
        }
        public void clear(){
            email.setText("");
            firstname.setText("");
            lastname.setText("");
            password.setText("");
            password_repeated.setText("");
        }
        private JTextField firstname,lastname,email;
        private JPasswordField password,password_repeated;
        private JButton button_register;
        private JLabel login;
    }
    
    
    private class CellRenderer extends DefaultListCellRenderer{
        private final Color color = new Color(168, 221, 255);
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component p = (Component)value;
            p.setBackground(isSelected ? color : Color.WHITE);
            return p;
        }        
    }
}
