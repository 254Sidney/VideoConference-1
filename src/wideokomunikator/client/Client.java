package wideokomunikator.client;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import wideokomunikator.User;
import static wideokomunikator.exception.DatabaseException.*;
import wideokomunikator.main;
import wideokomunikator.net.MESSAGE_TITLES;
import wideokomunikator.net.MESSAGE_TYPE;

public class Client extends JFrame {

    private ClientConnection connection;
    private User user = null;
    private ArrayList<Friend> friends;
    private Point location;
    private Dimension window_size;
    private final String serverAdress;
    private final int serverPort;

    public Client(String serverAdress, int serverPort) {
        friends = new ArrayList<Friend>();
        this.serverAdress = serverAdress;
        this.serverPort = serverPort;
        initConnection();
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ex) {
            showErrorDialog(ex.toString());
        }
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (panel_user != null) {
                    panel_user.view.close();
                }
            }
        });
        setSize(1000, 600);
        setMinimumSize(new Dimension(800, 400));
        try {
            panel_login = new Login("wilczynskipio@gmail.com", "komutator2");
            panel_register = new Register();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        setInTheMiddle();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPanel(panel_login);
    }

    private void initConnection() {
        try {
            connection = new ClientConnection(serverAdress, serverPort);
            connection.start();
        } catch (IOException ex) {
            showErrorDialog("Brak połączenia z serwerem:" + serverAdress + " " + serverPort);
            System.exit(0);
        }
    }

    private void setPanel(Container panel) {
        setContentPane(panel);
        getContentPane().revalidate();
        getContentPane().repaint();

    }

    public void setFullScreanView(boolean value, ConferenceView component) {
        dispose();
        setUndecorated(value);
        panel_user.menubar.setVisible(!value);
        if (value) {
            window_size = getSize();
            location = getLocation();
            setPanel(panel_user.view);
            setSize(Toolkit.getDefaultToolkit().getScreenSize());
            setLocation(0, 0);
        } else {
            setPanel(panel_user);
            setSize(window_size);
            setLocation(location);
            panel_user.splitpane.setRightComponent(panel_user.view);
        }
        setVisible(true);
    }

    private void setInTheMiddle() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((d.width - getWidth()) / 2, (d.height - getHeight()) / 2);

    }

    private void setUser(User user) {
        this.user = user;
        setTitle(user.getFIRST_NAME() + " " + user.getLAST_NAME() + " (" + user.getEMAIL() + ")");
        panel_user = new UserPanel();
        getFriends();
    }

    private void getFriends() {
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
                while (frame.getMESSAGE_TITLE() != MESSAGE_TITLES.FINISH && time > 0) {
                    wideokomunikator.net.Frame f = null;
                    time = wideokomunikator.net.Frame.WAIT_TIME;
                    while ((f = connection.getFrame(index)) == null && time > 0) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                        }
                        time -= 10;
                    }
                    if (f == null) {
                        showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                        break;
                    }
                    frame = f;
                    if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.FRIENDS_LIST) {
                        User user = (User) frame.getMESSAGE();
                        list.add(new Friend(user));
                    } else if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR) {
                        showErrorDialog(frame.getMESSAGE());
                    } else if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.FINISH) {
                        break;
                    }
                }
                Collections.sort(list);
                friends = list;
                panel_user.setFriends(friends.toArray(new Friend[friends.size()]));

            }
        }).start();

    }

    private void showErrorDialog(Object error) {
        JOptionPane.showMessageDialog(this, error, "Błąd", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessageDialog(Object message) {
        JOptionPane.showMessageDialog(this, message, "Wiadomość", JOptionPane.INFORMATION_MESSAGE);
    }

    private Login panel_login;
    private Register panel_register;
    private UserPanel panel_user;

    private class UserPanel extends JPanel {

        public UserPanel() {
            initComponents();
        }

        private void initComponents() {
            friends_panel = new JList<Friend>(list_model = new DefaultListModel<Friend>());
            friends_panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
            friends_panel.setCellRenderer(new ListCellRenderer<Friend>() {
                private final Color color = new Color(168, 221, 255);

                @Override
                public Component getListCellRendererComponent(JList<? extends Friend> list, Friend value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component p = (Component) value;
                    p.setBackground(isSelected ? color : Color.WHITE);
                    return p;
                }
            });
            friends_panel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            initPopup();
            friends_panel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        int index = friends_panel.locationToIndex(e.getPoint());
                        boolean isSelected = friends_panel.isSelectedIndex(index);
                        if (!isSelected) {
                            int[] ind = new int[1];
                            ind[0] = index;
                            friends_panel.setSelectedIndices(ind);
                        }
                        popup.show(friends_panel, e.getX(), e.getY());
                    }
                }
            });
            JScrollPane scroll_pane = new JScrollPane(friends_panel);
            view = new ConferenceView() {
                @Override
                public void setFullScrean(boolean value) {
                    setFullScreanView(value, this);
                }
            };
            splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, scroll_pane, view);
            GroupLayout l = new GroupLayout(this);
            setLayout(l);
            scroll_pane.setMinimumSize(new Dimension(256, 400));
            view.setMinimumSize(new Dimension(512, 512));
            l.setHorizontalGroup(l.createSequentialGroup().addComponent(splitpane));
            l.setVerticalGroup(l.createSequentialGroup().addComponent(splitpane));

            splitpane.setDividerSize(10);
            initMenu();
            connection.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    wideokomunikator.net.Frame frame = null;
                    if (evt.getNewValue() instanceof wideokomunikator.net.Frame) {
                        frame = (wideokomunikator.net.Frame) evt.getNewValue();
                        if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.CONFERENCE_INIT) {
                            int i = JOptionPane.showConfirmDialog(null, "Właśnie zostałeś dodany do konferencji, czy chcesz dołaczyć?", "Nowa Konferencja", JOptionPane.YES_NO_OPTION);
                            if (i == JOptionPane.OK_OPTION) {
                                int port = (int) frame.getMESSAGE();
                                view.initConnection(serverAdress, port, user.getID());
                            }
                        }
                    }
                }
            });
        }

        private void initMenu() {
            menubar = new JMenuBar();
            setJMenuBar(menubar);
            JMenu menu;
            JMenuItem item;
            menubar.add(menu = new JMenu("Program"));
            menu.add(item = new JMenuItem("Zamknij"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            menubar.add(menu = new JMenu("Znajomi"));
            menu.add(item = new JMenuItem("Szukaj"));
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    search = new SearchDialog() {
                        private SwingWorker<Friend[], Friend> worker;

                        @Override
                        public void getUsers(final String username) {
                            if (worker != null) {
                                worker.cancel(true);
                            }

                            worker = new SwingWorker<Friend[], Friend>() {
                                ArrayList<Friend> list = new ArrayList<Friend>();

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

                                    while (!this.isDone()) {
                                        int time = wideokomunikator.net.Frame.WAIT_TIME;
                                        while ((f = connection.getFrame(index)) == null && time > 0) {
                                            time -= 10;
                                            Thread.sleep(10);
                                        };

                                        if (f == null) {
                                            showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                                            break;
                                        }
                                        if (f.getMESSAGE_TITLE() == MESSAGE_TITLES.FINISH) {
                                            break;
                                        } else if (f.getMESSAGE_TITLE() == MESSAGE_TITLES.SEARCH) {
                                            User u = (User) f.getMESSAGE();
                                            Friend friend = new Friend(u);
                                            list.add(friend);
                                            publish(friend);
                                        } else if (f.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR) {
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
                                    if (!this.isCancelled()) {
                                        try {
                                            list = (Friend[]) this.get();
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
                        public void addUser(final User friend) {
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
                                    while ((f = connection.getFrame(index)) == null && time > 0) {
                                        try {
                                            Thread.sleep(10);
                                            time -= 10;
                                        } catch (InterruptedException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                    if (f == null) {
                                        showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                                        return;
                                    }

                                    if (f.getMESSAGE_TITLE() == MESSAGE_TITLES.OK) {
                                        getFriends();

                                    } else if (f.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR) {
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
            //menubar.add(menu = new JMenu("Narzędzia"));

        }

        private void initPopup() {
            popup = new JPopupMenu("Menu");
            JMenuItem item;
            popup.add(item = new JMenuItem("Rozpocznij Rozmowe"));

            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            String users = "";
                            for (int i = 0; i < friends_panel.getSelectedValuesList().size(); i++) {
                                int user_id = friends_panel.getSelectedValuesList().get(i).getUser().getID();
                                if (i == 0) {
                                    users += user_id;
                                } else {
                                    users += "\n" + user_id;
                                }
                            };
                            wideokomunikator.net.Frame frame = new wideokomunikator.net.Frame(
                                    MESSAGE_TYPE.REQUEST,
                                    user.getID(),
                                    connection.getNextFrameId(),
                                    MESSAGE_TITLES.CONFERENCE_INIT, users
                            );
                            connection.sendFrame(frame);
                            int index = frame.getMESSAGE_ID();
                            int time = wideokomunikator.net.Frame.WAIT_TIME;
                            while ((frame = connection.getFrame(index)) == null && time > 0) {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException ex) {
                                }
                                time -= 10;
                            }
                            if (frame == null) {
                                showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                                return;
                            } else if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.OK) {

                                try {
                                    int port = (int) frame.getMESSAGE();
                                    view.initConnection(serverAdress, port, user.getID());

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    showErrorDialog(ex.getLocalizedMessage());
                                }
                            } else if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR) {
                                System.out.println("Błąd " + frame.getMESSAGE());
                                showErrorDialog(frame.getMESSAGE());
                            }
                        }
                    }).start();
                }
            });
            popup.add(item = new JMenuItem("Usuń"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            User u = ((Friend) friends_panel.getSelectedValue()).getUser();
                            if (u == null) {
                                return;
                            }
                            wideokomunikator.net.Frame frame = new wideokomunikator.net.Frame(
                                    MESSAGE_TYPE.REQUEST,
                                    user.getID(),
                                    connection.getNextFrameId(),
                                    MESSAGE_TITLES.FRIENDS_REMOVE,
                                    u.getID());
                            connection.sendFrame(frame);
                            int index = frame.getMESSAGE_ID();
                            int time = wideokomunikator.net.Frame.WAIT_TIME;
                            while ((frame = connection.getFrame(index)) == null && time > 0) {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException ex) {
                                }
                                time -= 10;
                            }
                            if (frame == null) {
                                showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                                return;
                            } else if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.OK) {
                                getFriends();
                            } else if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR) {
                                showErrorDialog(frame.getMESSAGE());
                            }
                        }
                    }).start();
                }
            });
        }

        private void setFriends(Friend[] list) {
            ArrayList<Friend> l = new ArrayList<Friend>(Arrays.asList(list));
            Collections.sort(l);
            list_model.removeAllElements();
            for (Friend f : l) {
                list_model.addElement(f);
            }
        }

        public ConferenceView getView() {
            return view;
        }
        private SearchDialog search;
        private JSplitPane splitpane;
        private JList<Friend> friends_panel;
        private DefaultListModel<Friend> list_model;
        private ConferenceView view;
        private JMenuBar menubar;
        private JPopupMenu popup;
    }

    private class Login extends BackgroundImagePanel {

        private final int COLUMN_SIZE = 20;
        private Font font;

        public Login() throws IOException {
            this("", "");
        }

        public Login(String login) throws IOException {
            this(login, "");
        }

        public Login(String login, String password) throws IOException {
            super(ImageIO.read(main.class.getResource("images/background.png")));
            this.email = new JTextField(login);
            this.password = new JPasswordField(password);
            initComponents();
        }

        private void initComponents() {
            JPanel panel = new JPanel();
            this.setLayout(new GridBagLayout());
            this.add(panel);
            panel.setOpaque(false);
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

        private void login(final String email, final char[] password) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    button_login.setEnabled(false);
                    wideokomunikator.net.Frame f = new wideokomunikator.net.Frame(
                            wideokomunikator.net.MESSAGE_TYPE.REQUEST,
                            0,
                            connection.getNextFrameId(),
                            MESSAGE_TITLES.LOGIN, email + "\n" + new String(password));
                    connection.sendFrame(f);
                    int index = f.getMESSAGE_ID();
                    wideokomunikator.net.Frame frame;
                    int time = wideokomunikator.net.Frame.WAIT_TIME;
                    while ((frame = connection.getFrame(index)) == null && time > 0) {
                        try {
                            Thread.sleep(10);
                            time -= 10;
                        } catch (InterruptedException ex) {
                            showErrorDialog(ex.toString());
                        }
                    }
                    button_login.setEnabled(true);
                    if (frame == null) {
                        showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                        return;
                    }
                    User user = null;
                    if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.LOGIN) {
                        try {
                            user = (User) frame.getMESSAGE();
                        } catch (Exception e) {
                            showErrorDialog(e.toString());
                        }
                        if (user != null) {
                            setUser(user);
                            setPanel(panel_user);
                            panel_login.clear();
                        }
                    } else if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR) {
                        String message = (String) frame.getMESSAGE();
                        if (ERROR_USER_NOT_EXIST.matches(message)) {
                            showErrorDialog("Błędne login lub hasło");
                        } else {
                            showErrorDialog(message);
                        }
                    }
                }
            }).start();
        }

        private void login() {
            login(email.getText(), password.getPassword());
        }

        private void initActions() {
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

        public void clear() {
            email.setText("");
            password.setText("");
        }
        private JTextField email;
        private JPasswordField password;
        private JButton button_login;
        private JLabel register;
    }

    private class Register extends BackgroundImagePanel {

        private final int COLUMN_SIZE = 20;
        private Font font;

        public Register() throws IOException {
            super(ImageIO.read(main.class.getResource("images/background.png")));
            initComponents();
        }

        private void initComponents() {
            JPanel panel = new JPanel();
            this.setLayout(new GridBagLayout());
            this.add(panel);
            panel.setOpaque(false);
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

        private void initActions() {
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

        private void register(final String email, final String firstname, final String lastname, final char[] password) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    button_register.setEnabled(false);
                    wideokomunikator.net.Frame f = new wideokomunikator.net.Frame(
                            wideokomunikator.net.MESSAGE_TYPE.REQUEST,
                            0,
                            connection.getNextFrameId(),
                            MESSAGE_TITLES.REGISTER,
                            email + "\n" + firstname + "\n" + lastname + "\n" + new String(password));
                    connection.sendFrame(f);
                    int index = f.getMESSAGE_ID();
                    wideokomunikator.net.Frame frame;
                    int time = wideokomunikator.net.Frame.WAIT_TIME;
                    while ((frame = connection.getFrame(index)) == null && time > 0) {
                        try {
                            Thread.sleep(10);
                            time -= 10;
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (frame == null) {
                        showErrorDialog("Przekroczono limit czasu oczekiwania na odpowiedź!");
                        return;
                    }
                    User user = null;
                    try {
                        user = (User) frame.getMESSAGE();
                    } catch (Exception e) {
                    }
                    if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.REGISTER) {
                        if (user != null) {
                            setUser(user);
                            setPanel(panel_user);
                            panel_register.clear();
                        }
                    } else if (frame.getMESSAGE_TITLE() == MESSAGE_TITLES.ERROR) {
                        String message = (String) frame.getMESSAGE();
                        if (ERROR_USER_EXIST.matches(message)) {
                            showErrorDialog("Konto o podanym emailu już istnieje");
                        } else {
                            showErrorDialog(message);
                        }
                    }
                    button_register.setEnabled(true);
                }
            }).start();
        }

        public void clear() {
            email.setText("");
            firstname.setText("");
            lastname.setText("");
            password.setText("");
            password_repeated.setText("");
        }
        private JTextField firstname, lastname, email;
        private JPasswordField password, password_repeated;
        private JButton button_register;
        private JLabel login;
    }

}
