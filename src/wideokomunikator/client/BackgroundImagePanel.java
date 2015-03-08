package wideokomunikator.client;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class BackgroundImagePanel extends JPanel{

    private BufferedImage img = new BufferedImage(600, 800, BufferedImage.TYPE_INT_ARGB);


    public BackgroundImagePanel(BufferedImage img) {
        this.img = img;
    }
    public void setImage(BufferedImage image){
        this.img = image;
    }

    public void paintComponent(Graphics g) {
        g.drawImage(img.getScaledInstance(1000, 600, Image.SCALE_SMOOTH), 0, 0, null);
    }

}
