package wideokomunikator.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import wideokomunikator.User;

public class Friend extends JPanel implements Comparable<Friend>{
    private JLabel icon = null;
    private final User user;
    public Friend(User user) {
        this.user = user;
        initComponents();
    }

    public Friend(User user,Image icon) {
        this.user = user;
        this.icon = new JLabel(new ImageIcon(icon.getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
    }

    public User getUser() {
        return user;
    }
    
    private void initComponents(){
        JLabel name = new JLabel(user.getFIRST_NAME()+" "+user.getLAST_NAME());
        JLabel email = new JLabel(user.getEMAIL());
        //icon.setMinimumSize(new Dimension(50, 50));
        name.setFont(new Font(name.getFont().getName(), Font.PLAIN, 16));
        email.setForeground(Color.GRAY);
        email.setFont(new Font(email.getFont().getName(), Font.PLAIN, 12));
        if(icon==null){
            try {
                getDefaultIcon();
            } catch (IOException ex) {}
        }
        GroupLayout l = new GroupLayout(this);
        setLayout(l);
        l.setAutoCreateGaps(true);
        l.setHorizontalGroup(l.createSequentialGroup()
            .addComponent(icon)
            .addGroup(l.createParallelGroup()
                .addComponent(name)
                .addComponent(email)));
        l.setVerticalGroup(l.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addComponent(icon)
            .addGroup(l.createSequentialGroup()
                .addComponent(name)
                .addComponent(email)));
                
        
    }
    private void getDefaultIcon() throws IOException{        
        Image im = ImageIO.read(getClass().getResource("/wideokomunikator/images/user_icon.png")).getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        icon = new JLabel(new ImageIcon(im));
    }

    @Override
    public int compareTo(Friend o) {
        return user.compareTo(o.getUser());
    }
    
    public boolean equals(Object obj) {    
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof Friend) {
            User user = ((Friend)obj).getUser();
            return this.getUser().equals(user);
        }
        return false;
    }
    
    
}
