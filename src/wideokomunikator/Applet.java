/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wideokomunikator;

import wideokomunikator.client.ConferenceView;
import javax.swing.JApplet;

/**
 *
 * @author Piotr
 */
public class Applet extends JApplet {

    /**
     * Initialization method that will be called after the applet is loaded into
     * the browser.
     */
    public void init() {
        new ConferenceView(800, 400);
    }

    // TODO overwrite start(), stop() and destroy() methods
}
