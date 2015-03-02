package wideokomunikator.client;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class BackgroundImagePanel extends JPanel{

    private BufferedImage img;

    public BackgroundImagePanel(String img) throws IOException {
        this(ImageIO.read(BackgroundImagePanel.class.getResource("images/"+img)));
    }

    public BackgroundImagePanel(BufferedImage img) {
        this.img = img;
    }

    public void paintComponent(Graphics g) {
        g.drawImage(img.getScaledInstance(1000, 600, Image.SCALE_SMOOTH), 0, 0, null);
    }

}
