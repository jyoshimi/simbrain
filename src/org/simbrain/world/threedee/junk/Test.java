package org.simbrain.world.threedee.junk;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.UIManager;

import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.CanvasHelper;
import org.simbrain.world.threedee.Moveable;
import org.simbrain.world.threedee.environment.Environment;
import org.simbrain.world.threedee.gui.FreeBirdView;
import org.simbrain.world.threedee.gui.KeyHandler;

public class Test {
    static Environment environment = new Environment();
    
    public static void main(String[] args)
    {
        Logger.getLogger("com.jme").setLevel(Level.OFF);
        Logger.getLogger("com.jmex").setLevel(Level.OFF);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        init("3D Demo");
        
        createView1("1", 512, 384);
//        createView1("2", 512, 384);
        createView4(512, 384);
        
        finish();
    }

    static JDesktopPane mainPanel = new JDesktopPane();
    static JFrame frame;
    static Shutdown shutdown = new Shutdown();
    
    private static void init(String title) {
        frame = new JFrame();
        
        frame.setTitle(title);
        mainPanel = new JDesktopPane();
        frame.getContentPane().add(mainPanel);
        frame.addWindowListener(shutdown);
    }
    
    private static AwtView createView1(String title, int width, int height) {
        Agent agent = new Agent("" + ++x);
        environment.add(agent);
        AwtView view = new AwtView(agent, environment, width, height);
        CanvasHelper canvas = new CanvasHelper(width, height, view);

        JFrame innerFrame = new JFrame("agent " + x);
        shutdown.frames.add(innerFrame);
                
        BorderLayout layout = new BorderLayout();
        innerFrame.getRootPane().setLayout(layout);
        
        innerFrame.getRootPane().add(canvas.getCanvas());
        
        KeyHandler handler = getHandler();
        
        agent.addInput(0, handler.input);
        
        innerFrame.addKeyListener(handler);
        innerFrame.setSize(width, height);
        
        return view;
    }
    
    private static void createView4(int width, int height) {
        FreeBirdView bird = new FreeBirdView();
        
        environment.addViewable(bird);
        
        AwtView view = new AwtView(bird, environment, width, height);
        CanvasHelper canvas = new CanvasHelper(width, height, view);

        JFrame innerFrame = new JFrame("bird");
        shutdown.frames.add(innerFrame);
                
        BorderLayout layout = new BorderLayout();
        innerFrame.getRootPane().setLayout(layout);
        
        innerFrame.getRootPane().add(canvas.getCanvas());
        
        KeyHandler handler = getHandler();
        
        bird.addInput(0, handler.input);
        
        innerFrame.addKeyListener(handler);
        innerFrame.setSize(width, height);
    }
    
    private static KeyHandler getHandler()
    {
        KeyHandler handler = new KeyHandler();
        
        handler.addBinding(KeyEvent.VK_LEFT, Moveable.Action.LEFT);
        handler.addBinding(KeyEvent.VK_RIGHT, Moveable.Action.RIGHT);
        handler.addBinding(KeyEvent.VK_UP, Moveable.Action.FORWARD);
        handler.addBinding(KeyEvent.VK_DOWN, Moveable.Action.BACKWARD);
        handler.addBinding(KeyEvent.VK_A, Moveable.Action.DOWN);
        handler.addBinding(KeyEvent.VK_Z, Moveable.Action.UP);
        handler.addBinding(KeyEvent.VK_U, Moveable.Action.RISE);
        handler.addBinding(KeyEvent.VK_J, Moveable.Action.FALL);
        
        return handler;
    }
    
    private static void finish() {
        frame.setSize(800, 600);
        frame.setState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
        for (JFrame frame : shutdown.frames) {
            frame.setVisible(true);
        }
    }
    
    static int x = 0;
    
    private static class Shutdown implements WindowListener {//extends WindowAdapter {
        List<JFrame> frames = new ArrayList<JFrame>();
        
        public void windowClosed(WindowEvent e) {
            for (JFrame frame : frames) {
                frame.dispose();
            }
        }

        public void windowActivated(WindowEvent e) {
            for (JFrame frame : frames) {
                frame.setVisible(true);
            }
        }

        public void windowClosing(WindowEvent e) {
            /* no implementation */
        }

        public void windowDeactivated(WindowEvent e) {
            /* no implementation */
        }

        public void windowDeiconified(WindowEvent e) {
            /* no implementation */
        }

        public void windowIconified(WindowEvent e) {
            /* no implementation */
        }

        public void windowOpened(WindowEvent e) {
            /* no implementation */
        }
    }
}