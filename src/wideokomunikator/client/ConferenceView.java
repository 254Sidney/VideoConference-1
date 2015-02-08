package wideokomunikator.client;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.bytedeco.javacv.FrameGrabber;


public class ConferenceView extends JPanel implements MouseMotionListener,Runnable{
    
    
    private boolean full_screan = false;
    private Dimension window_size;
    private Point location;
    private BufferedImage image=null,bufor=null;
    private Thread thread;
    private boolean record = false;
    public ConferenceView(int Conference_Width_Size,int Conference_Height_Size){        
        window_size = new Dimension(Conference_Width_Size,Conference_Height_Size);
        initComponents();
        thread = new Thread(this);
                
    }

    @Override
    public void paint(Graphics g) {
        bufor = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g2 = bufor.getGraphics();
        Graphics2D g2d = (Graphics2D)g2;
        if (image != null)
          g2d.drawImage(image, 0, 0, getWidth(), getHeight(), this);
    
        g.drawImage(bufor, 0, 0,this);
    }
    
    private void initComponents(){
        location = new Point(100, 100);
        setLocation(location);
        addMouseMotionListener(this);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()==2){
                    setFullScrean(full_screan=!full_screan);
                }
            }
            
        });
        //setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(window_size);
        //setDefaultCloseOperation(EXIT_ON_CLOSE);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='\n'){                    
                    setFullScrean(full_screan=!full_screan);
                }
            }
            
        });        
        setVisible(true);
    }
    
    public void setImag(BufferedImage image){
        this.image = image;
        repaint();
    }
    
    public void setFullScrean(boolean value){
        full_screan=value;
        //dispose();
        //setUndecorated(value);
        if(value){
            setSize(Toolkit.getDefaultToolkit().getScreenSize());  
            setLocation(0, 0);
        }else{
            setSize(window_size);
            setLocation(location);
        }
        setVisible(true);        
    }
    public void start(){
        if (!thread.isAlive()) {
            thread.start();
        }
        record = true;
    }

    public void stop(){
        record = false;
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        //System.out.println(e.getXOnScreen()+" "+e.getX());
        //setLocation(e.getXOnScreen()-e.getX(), e.getYOnScreen()-e.getY());
    }


    @Override
    public void run() {
        boolean active = true;
        FrameGrabber grabber = null;   
        try {
            grabber = FrameGrabber.createDefault(0);
            grabber.setImageWidth(1200);
            grabber.setImageHeight(900);
            grabber.setFrameRate(60);
            grabber.start();

            System.out.println(grabber.getFormat());
        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        }            
        int frame = grabber.getFrameNumber();
        while(active){  
            if(record){
                try {
                    image = grabber.grab().getBufferedImage();
                    frame++;
                    grabber.setFrameNumber(frame);
                } catch (FrameGrabber.Exception ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                repaint();                
            }else{
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        try {
            grabber.stop();
        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        
    }
    
    
}

