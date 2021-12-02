package core;

import core.agents.KeyboardAgent;
import core.agents.PlanningAgent;
import core.agents.QLearningAgent;
import core.agents.RandomAgent;

import core.agents.RightAgent;
import core.world.World;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;

/**
 * Renders and runs the world simulation
 */
public final class Simulation extends JFrame {

  /** The conversion factor from nano to base */
  public static final double NANO_TO_BASE = 1.0e9;

  /** The canvas to draw to */
  protected final Canvas canvas = new Canvas();

  /** The dynamics engine */
  public World world = new World();

  /** True if the simulation is exited */
  private boolean stopped;

  /** The time stamp for the last iteration */
  private long last;

  /** Camera to track offset/scale of rendering */
  private final double scale;

  /** Responsible for choosing the best action to take at any moment in time */
  private final PlanningAgent planningAgent;

  /**
   * Constructs a new simulation frame.
   *
   * @param name  the frame name
   * @param scale the pixels per meter scale factor
   */
  public Simulation(String name, double scale, PlanningAgent planningAgent) {
    super(name);

    // setup the agent
    this.planningAgent = planningAgent;

    // setup the camera
    this.scale = scale;

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
    Dimension size = new Dimension(940, 200);

    // setup canvas to paint on
    this.canvas.setPreferredSize(size);
    this.canvas.setMinimumSize(size);
    this.canvas.setMaximumSize(size);
    this.add(this.canvas);

    // make the JFrame not resizable
    this.setResizable(false);

    // size everything
    this.pack();
  }

  /**
   * Start active rendering the simulation.
   *
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
        while (!isStopped() && !world.isTerminal()) {
          gameLoop();
          // you could add a Thread.yield(); or
          // Thread.sleep(long) here to give the
          // CPU some breathing room
          try {
            Thread.sleep(20);
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
   * the simulation, graphics, and poll for input from the planning agent.
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
    this.render(g);
    g.setTransform(tx);

    // update the World
    this.world = this.world.generateNextState(this.planningAgent.chooseAction(this.world));

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
   *
   * By default, this method puts the origin (0,0) in the center of the window
   * and points the positive y-axis pointing up.
   *
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
   *
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
   * Renders the simulation.
   *
   * @param g the graphics object to render to
   */
  private void render(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // draw the walls and goal
    for (Body wall : this.world.walls) {
      this.render(g, wall, Color.BLACK);
    }
    this.render(g, this.world.goal, Color.GREEN);

    // draw the player
    this.render(g, this.world.player, Color.BLUE);

    // draw the obstacles
    for (Body obstacle : this.world.obstacles.keySet()) {
      this.render(g, obstacle, Color.RED);
    }
  }

  /**
   * Renders a body.
   *
   * @param g     the graphics context
   * @param body  the body to render
   * @param color the color of the body
   */
  private void render(Graphics2D g, Body body, Color color) {
    // save the original transform
    AffineTransform ot = g.getTransform();

    AffineTransform lt = new AffineTransform();
    lt.translate(body.getTransform().getTranslationX() * this.scale, body.getTransform().getTranslationY() * this.scale);
    lt.rotate(body.getTransform().getRotationAngle());
    g.transform(lt);

    // loop over all the body fixtures for this body
    for (BodyFixture fixture : body.getFixtures()) {
      Graphics2DRenderer.render(g, fixture.getShape(), this.scale, color);
    }

    // set the original transform
    g.setTransform(ot);
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

  /**
   * Runs the hallway weaver simulation.
   *
   * @param args the command line arguments (ignored)
   */
  public static void main(String[] args) {
    // create and prepare planning agent
    PlanningAgent agent = new QLearningAgent(0.05, 0.9, 0.4, 2500);
    //KeyboardAgent agent = new KeyboardAgent();

    agent.init(); // optional line if an agent needs to prepare itself before being used

    // run simulation with agent
    Simulation simulation = new Simulation("Hallway Weaver", 2.5, agent);
    //simulation.addKeyListener(agent);
    simulation.run();
  }
}