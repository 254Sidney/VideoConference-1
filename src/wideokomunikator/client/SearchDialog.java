package wideokomunikator.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import wideokomunikator.User;

public abstract class SearchDialog extends JDialog{

    public SearchDialog() {
        initComponents();
        setVisible(true);
    }
    private void initComponents(){
        setTitle("Szukaj");        
        list = new JList(list_model = new DefaultListModel());
        search = new JTextField();
        search.setFont(new Font(search.getFont().getName(), Font.PLAIN, 20));
        JPanel panel = new JPanel();
        JScrollPane pane = new JScrollPane(panel);        
        pane.setMinimumSize(new Dimension(200, 400));
        pane.setBackground(Color.WHITE);
        panel.setBackground(Color.WHITE);
        GroupLayout l = new GroupLayout(getContentPane());
        getContentPane().setLayout(l);
        l.setAutoCreateGaps(true);
        l.setHorizontalGroup(l.createParallelGroup()
            .addComponent(search)
            .addComponent(pane));
        l.setVerticalGroup(l.createSequentialGroup()
            .addComponent(search,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE)
            .addComponent(pane));
        l = new GroupLayout(panel);
        panel.setLayout(l);
        l.setHorizontalGroup(l.createSequentialGroup().addComponent(list));
        l.setVerticalGroup(l.createSequentialGroup().addComponent(list));
        panel.setBorder(new EmptyBorder(-1, -1, -1, -1));
        list.setBorder(new BevelBorder(BevelBorder.LOWERED));
        list.setCellRenderer(new ListCellRenderer() {
            private final Color color = new Color(168, 221, 255);
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component p = (Component)value;
                p.setBackground(isSelected ? color : Color.WHITE);
                return p;
            }        
        });
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initAction();
        initMenu();
        setResizable(false);
        setAlwaysOnTop(true);
        pack();
        setLocationRelativeTo(null);
    }
    private void initMenu(){
        popup = new JPopupMenu("Menu");
        JMenuItem item;
        popup.add(item = new JMenuItem("Dodaj"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showUserAddDialog(((Friend)list.getSelectedValue()).getUser());
            }
        });
    }
    
    private void initAction(){
        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = search.getText();
                if(text!=null&&text.length()>0)
                    getUsers(search.getText());
                else{
                    setList(new Friend[]{});
                }
            }            
        });        
        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getButton()==MouseEvent.BUTTON3){
                    //popup.setLocation(e.getXOnScreen(), e.getYOnScreen());
                    //popup.setVisible(true);
                    list.setSelectedIndex(list.locationToIndex(e.getPoint()));
                    popup.show(list, e.getX(), e.getY());
                }else if (e.getButton()==MouseEvent.BUTTON1) {
                    if(e.getClickCount()==2){
                        showUserAddDialog(((Friend)list.getSelectedValue()).getUser());
                    }
                    
                }
            }            
        });
    }
    
    private void showUserAddDialog(User user){
        String options[] = new String[]{"Tak","Nie"};
        JPanel panel = new JPanel();
        Friend friend = new Friend(user);
        JLabel label = new JLabel("Dodać do znajomych użytkownika:");
        GroupLayout l = new GroupLayout(panel);
        panel.setLayout(l);
        l.setAutoCreateGaps(true);
        l.setHorizontalGroup(l.createParallelGroup().addComponent(label).addComponent(friend));
        l.setVerticalGroup(l.createSequentialGroup().addComponent(label).addComponent(friend));
        int option = JOptionPane.showOptionDialog(this,panel, 
                "Dodaj znajomego", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if(option == 0){
            addUser(user);
        }
    }
    
    
    public void setList(Friend[] list){
        if(list.length==2){
            for(Friend f:list){
                System.out.println(f.getUser());
            }
        }
        //System.out.println(list.length);
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
    }
    public abstract void getUsers(String username);
    
    
    public abstract void addUser(User user);
    
    private JList list;
    private JTextField search;    
    private JPopupMenu popup;
    private DefaultListModel<Friend> list_model;
}
