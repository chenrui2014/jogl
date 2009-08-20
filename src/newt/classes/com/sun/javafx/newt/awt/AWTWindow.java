/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package com.sun.javafx.newt.awt;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.awt.event.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import com.sun.javafx.newt.Window;
import java.awt.Insets;
import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.*;

/** An implementation of the Newt Window class built using the
    AWT. This is provided for convenience of porting to platforms
    supporting Java SE. */

public class AWTWindow extends Window {

    public AWTWindow() {
        super();
        title = "AWT NewtWindow";
    }

    private Frame frame;
    private AWTCanvas canvas;
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    public void setTitle(final String title) {
        super.setTitle(title);
        runOnEDT(true, new Runnable() {
                public void run() {
                    if (frame != null) {
                        frame.setTitle(title);
                    }
                }
            });
    }

    protected void createNative(final Capabilities caps) {

        final AWTWindow awtWindow = this;

        runOnEDT(true, new Runnable() {
                public void run() {
                    frame = new Frame(getTitle());
                    frame.setUndecorated(isUndecorated());
                    frame.setLayout(new BorderLayout());
                    canvas = new AWTCanvas(caps);
                    Listener listener = new Listener(awtWindow);
                    canvas.addMouseListener(listener);
                    canvas.addMouseMotionListener(listener);
                    canvas.addKeyListener(listener);
                    canvas.addComponentListener(listener);
                    frame.add(canvas, BorderLayout.CENTER);
                    frame.setSize(width, height);
                    frame.setLocation(x, y);
                    frame.addComponentListener(new MoveListener(awtWindow));
                    frame.addWindowListener(new WindowEventListener(awtWindow));
                }
            });
    }

    protected void closeNative() {
        runOnEDT(true, new Runnable() {
                public void run() {
                    frame.dispose();
                    frame = null;
                }
            });
    }

    public boolean hasDeviceChanged() {
        boolean res = canvas.hasDeviceChanged();
        if(res) {
            config = canvas.getAWTGraphicsConfiguration();
            if (config == null) {
                throw new NativeWindowException("Error Device change null GraphicsConfiguration: "+this);
            }
            updateDeviceData();
        }
        return res;
    }

    public void setVisible(final boolean visible) {
        runOnEDT(true, new Runnable() {
                public void run() {
                    frame.setVisible(visible);
                }
            });

        config = canvas.getAWTGraphicsConfiguration();

        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }

        updateDeviceData();
    }

    private void updateDeviceData() {
        // propagate new info ..
        ((AWTScreen)getScreen()).setAWTGraphicsScreen((AWTGraphicsScreen)config.getScreen());
        ((AWTDisplay)getScreen().getDisplay()).setAWTGraphicsDevice((AWTGraphicsDevice)config.getScreen().getDevice());

        DisplayMode mode = ((AWTGraphicsDevice)config.getScreen().getDevice()).getGraphicsDevice().getDisplayMode();
        int w = mode.getWidth();
        int h = mode.getHeight();
        ((AWTScreen)screen).setScreenSize(w, h);
    }

    public void setSize(final int width, final int height) {
        this.width = width;
        this.height = height;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        }
        /** An AWT event on setSize() would bring us in a deadlock situation, hence invokeLater() */
        runOnEDT(false, new Runnable() {
                public void run() {
                    Insets insets = frame.getInsets();
                    frame.setSize(width + insets.left + insets.right,
                                  height + insets.top + insets.bottom);
                }
            });
    }

    public com.sun.javafx.newt.Insets getInsets() {
        final int insets[] = new int[] { 0, 0, 0, 0 };
        runOnEDT(true, new Runnable() {
                public void run() {
                    Insets frameInsets = frame.getInsets();
                    insets[0] = frameInsets.top;
                    insets[1] = frameInsets.left;
                    insets[2] = frameInsets.bottom;
                    insets[3] = frameInsets.right;
                }
            });
        return new com.sun.javafx.newt.
            Insets(insets[0],insets[1],insets[2],insets[3]);
    }

    public void setPosition(final int x, final int y) {
        this.x = x;
        this.y = y;
        if(!fullscreen) {
            nfs_x=x;
            nfs_y=y;
        }
        runOnEDT(true, new Runnable() {
                public void run() {
                    frame.setLocation(x, y);
                }
            });
    }

    public boolean setFullscreen(final boolean fullscreen) {
        if(this.fullscreen!=fullscreen) {
            final int x,y,w,h;
            this.fullscreen=fullscreen;
            if(fullscreen) {
                x = 0; y = 0;
                w = screen.getWidth();
                h = screen.getHeight();
            } else {
                x = nfs_x;
                y = nfs_y;
                w = nfs_width;
                h = nfs_height;
            }
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                System.err.println("AWTWindow fs: "+fullscreen+" "+x+"/"+y+" "+w+"x"+h);
            }
            /** An AWT event on setSize() would bring us in a deadlock situation, hence invokeLater() */
            runOnEDT(false, new Runnable() {
                    public void run() {
                        if(!frame.isDisplayable()) {
                            frame.setUndecorated(fullscreen);
                        } else {
                            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                                System.err.println("AWTWindow can't undecorate already created frame");
                            }
                        }
                        frame.setLocation(x, y);
                        frame.setSize(w, h);
                    }
                });
        }
        return true;
    }

    public Object getWrappedWindow() {
        return canvas;
    }

    protected void sendWindowEvent(int eventType) {
        super.sendWindowEvent(eventType);
    }

    protected void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        super.sendKeyEvent(eventType, modifiers, keyCode, keyChar);
    }

    protected void sendMouseEvent(int eventType, int modifiers,
                                  int x, int y, int button, int rotation) {
        super.sendMouseEvent(eventType, modifiers, x, y, button, rotation);
    }

    private static void runOnEDT(boolean wait, Runnable r) {
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            try {
                if(wait) {
                    EventQueue.invokeAndWait(r);
                } else {
                    EventQueue.invokeLater(r);
                }
            } catch (Exception e) {
                throw new NativeWindowException(e);
            }
        }
    }

    private static final int WINDOW_EVENT = 1;
    private static final int KEY_EVENT = 2;
    private static final int MOUSE_EVENT = 3;

    class MoveListener implements ComponentListener {
        private AWTWindow window;
        private AWTDisplay display;

        public MoveListener(AWTWindow w) {
            window = w;
            display = (AWTDisplay)window.getScreen().getDisplay();
        }

        public void componentResized(ComponentEvent e) {
        }

        public void componentMoved(ComponentEvent e) {
            x = frame.getX();
            y = frame.getY();
            display.enqueueEvent(window, com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_MOVED, null);
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }

    }

    class Listener implements ComponentListener, MouseListener, MouseMotionListener, KeyListener {
        private AWTWindow window;
        private AWTDisplay display;

        public Listener(AWTWindow w) {
            window = w;
            display = (AWTDisplay)window.getScreen().getDisplay();
        }

        public void componentResized(ComponentEvent e) {
            width = canvas.getWidth();
            height = canvas.getHeight();
            display.enqueueEvent(window, com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_RESIZED, null);
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
            // We ignore these as we synthesize them ourselves out of
            // mouse pressed and released events
        }

        public void mouseEntered(MouseEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_ENTERED, e);
        }

        public void mouseExited(MouseEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_EXITED, e);
        }

        public void mousePressed(MouseEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_PRESSED, e);
        }

        public void mouseReleased(MouseEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_RELEASED, e);
        }

        public void mouseMoved(MouseEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_MOVED, e);
        }

        public void mouseDragged(MouseEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_DRAGGED, e);
        }

        public void keyPressed(KeyEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.KeyEvent.EVENT_KEY_PRESSED, e);
        }

        public void keyReleased(KeyEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.KeyEvent.EVENT_KEY_RELEASED, e);
        }

        public void keyTyped(KeyEvent e)  {
            display.enqueueEvent(window, com.sun.javafx.newt.KeyEvent.EVENT_KEY_TYPED, e);
        }
    }

    class WindowEventListener implements WindowListener {
        private AWTWindow window;
        private AWTDisplay display;

        public WindowEventListener(AWTWindow w) {
            window = w;
            display = (AWTDisplay)window.getScreen().getDisplay();
        }

        public void windowActivated(WindowEvent e) {
        }
        public void windowClosed(WindowEvent e) {
        }
        public void windowClosing(WindowEvent e) {
            display.enqueueEvent(window, com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY, null);
        }
        public void windowDeactivated(WindowEvent e) {
        }
        public void windowDeiconified(WindowEvent e) {
        }
        public void windowIconified(WindowEvent e) {
        }
        public void windowOpened(WindowEvent e) {
        }
    }
}
