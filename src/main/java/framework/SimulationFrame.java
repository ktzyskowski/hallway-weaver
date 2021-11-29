/*
 * Copyright (c) 2010-2021 William Bittle  http://www.dyn4j.org/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions
 *     and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 *     and the following disclaimer in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of dyn4j nor the names of its contributors may be used to endorse or
 *     promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package framework;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

/**
 * A simple framework for building samples.
 * @version 4.2.0
 * @since 3.2.0
 */
public abstract class SimulationFrame extends JFrame {

    /** The conversion factor from nano to base */
    public static final double NANO_TO_BASE = 1.0e9;

    /** The canvas to draw to */
    protected final Canvas canvas = new Canvas();

    /** The dynamics engine */
    protected final World<SimulationBody> world = new World<>();

    /** True if the simulation is exited */
    private boolean stopped;

    /** The time stamp for the last iteration */
    private long last;

    /** Tracking for the step number when in manual stepping mode */
    private long stepNumber;

    /** Camera to track offset/scale of rendering */
    private final Camera camera = new Camera();

    /**
     * Constructs a new simulation frame (default resolution 800x600)
     *
     * @param name the frame name
     * @param scale the pixels per meter scale factor
     */
    public SimulationFrame(String name, double scale) {
        super(name);

        // setup the camera
        this.camera.setScale(scale);

        // setup the JFrame
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // add a window listener
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // before we stop the JVM stop the simulation
                stop();
                super.windowClosing(e);
            }
        });

        // create the size of the window
        Dimension size = new Dimension(1600, 1000);

        // setup canvas to paint on
        this.canvas.setPreferredSize(size);
        this.canvas.setMinimumSize(size);
        this.canvas.setMaximumSize(size);
        this.add(this.canvas);

        // make the JFrame not resizable
        this.setResizable(false);

        // size everything
        this.pack();

        this.canvas.requestFocus();

        // setup the world
        this.initializeWorld();
    }

    /**
     * Creates game objects and adds them to the world.
     */
    protected abstract void initializeWorld();

    /**
     * Start active rendering the simulation.
     * <p>
     * This should be called after the JFrame has been shown.
     */
    private void start() {
        // initialize the last update time
        this.last = System.nanoTime();
        // don't allow AWT to paint the canvas since we are
        this.canvas.setIgnoreRepaint(true);
        // enable double buffering (the JFrame has to be
        // visible before this can be done)
        this.canvas.createBufferStrategy(2);
        // run a separate thread to do active rendering
        // because we don't want to do it on the EDT
        Thread thread = new Thread() {
            public void run() {
                // perform an infinite loop stopped
                // render as fast as possible
                while (!isStopped()) {
                    gameLoop();
                    // you could add a Thread.yield(); or
                    // Thread.sleep(long) here to give the
                    // CPU some breathing room
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {}
                }
            }
        };
        // set the game loop thread to a daemon thread so that
        // it cannot stop the JVM from exiting
        thread.setDaemon(true);
        // start the game loop
        thread.start();
    }

    /**
     * The method calling the necessary methods to update
     * the game, graphics, and poll for input.
     */
    private void gameLoop() {
        // get the graphics object to render to
        Graphics2D g = (Graphics2D)this.canvas.getBufferStrategy().getDrawGraphics();

        // by default, set (0, 0) to be the center of the screen with the positive x axis
        // pointing right and the positive y axis pointing up
        this.transform(g);

        // reset the view
        this.clear(g);

        // get the current time
        long time = System.nanoTime();
        // get the elapsed time from the last iteration
        long diff = time - this.last;
        // set the last time
        this.last = time;
        // convert from nanoseconds to seconds
        double elapsedTime = (double) diff / NANO_TO_BASE;

        // render anything about the simulation (will render the World objects)
        AffineTransform tx = g.getTransform();
        g.translate(this.camera.getOffsetX(), this.camera.getOffsetY());
        this.render(g);
        g.setTransform(tx);

        // update the World
        this.world.update(elapsedTime);

        // dispose of the graphics object
        g.dispose();

        // blit/flip the buffer
        BufferStrategy strategy = this.canvas.getBufferStrategy();
        if (!strategy.contentsLost()) {
            strategy.show();
        }

        // Sync the display on some systems.
        // (on Linux, this fixes event queue problems)
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Performs any transformations to the graphics.
     * <p>
     * By default, this method puts the origin (0,0) in the center of the window
     * and points the positive y-axis pointing up.
     * @param g the graphics object to render to
     */
    protected void transform(Graphics2D g) {
        final int w = this.canvas.getWidth();
        final int h = this.canvas.getHeight();

        // before we render everything im going to flip the y axis and move the
        // origin to the center (instead of it being in the top left corner)
        AffineTransform yFlip = AffineTransform.getScaleInstance(1, -1);
        AffineTransform move = AffineTransform.getTranslateInstance(w / 2., -h / 2.);
        g.transform(yFlip);
        g.transform(move);
    }

    /**
     * Clears the previous frame.
     * @param g the graphics object to render to
     */
    protected void clear(Graphics2D g) {
        final int w = this.canvas.getWidth();
        final int h = this.canvas.getHeight();

        // lets draw over everything with a white background
        g.setColor(Color.WHITE);
        g.fillRect(-w / 2, -h / 2, w, h);
    }

    /**
     * Renders the example.
     * @param g the graphics object to render to
     */
    protected void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw all the objects in the world
        for (int i = 0; i < this.world.getBodyCount(); i++) {
            // get the object
            SimulationBody body = this.world.getBody(i);
            this.render(g, body);
        }
    }

    /**
     * Renders the body.
     * @param g the graphics object to render to
     * @param body the body to render
     */
    protected void render(Graphics2D g, SimulationBody body) {
        // if the object is selected, draw it magenta
        Color color = body.getColor();

        // draw the object
        body.render(g, this.camera.getScale(), color);
    }

    /**
     * Stops the simulation.
     */
    public synchronized void stop() {
        this.stopped = true;
    }

    /**
     * Returns true if the simulation is stopped.
     * @return boolean true if stopped
     */
    public boolean isStopped() {
        return this.stopped;
    }

    /**
     * Starts the simulation.
     */
    public void run() {
        // set the look and feel to the system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // show it
        this.setVisible(true);

        // start it
        this.start();
    }
}