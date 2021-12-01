package core.world;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.NarrowphaseCollisionData;
import org.dyn4j.world.listener.CollisionListenerAdapter;

/**
 * Represents the hallway weaver simulation state.
 */
public class World extends org.dyn4j.world.World<Body> implements Serializable {

  // =====   Configuration   ===== //
  public static final double WORLD_HEIGHT = 60.0;
  public static final double WORLD_WIDTH = 360.0;
  public static final double TIME_PER_UPDATE = 1.0;
  public static final int OBSTACLE_COUNT = 100;
  public static final double OBSTACLE_SPEED = 15.0;

  public static final double FORCE_MAGNITUDE = 500.0;
  public static final Force FORCE_UP = new Force(0, FORCE_MAGNITUDE);
  public static final Force FORCE_DOWN = new Force(0, -FORCE_MAGNITUDE);
  public static final Force FORCE_LEFT = new Force(-FORCE_MAGNITUDE, 0);
  public static final Force FORCE_RIGHT = new Force(FORCE_MAGNITUDE, 0);
  public static final Force FORCE_NONE = new Force(0, 0);

  // =====   State Variables   ===== //
  public boolean won;
  public final Body player;
  public final Body goal;
  public final Map<Body, Boolean> obstacles;
  public final List<Body> walls;

  /**
   * Copies the given world state.
   *
   * @param player the player body info
   * @param obstacles the obstacle body info and collision info
   * @param won whether the goal has been touched by the player
   */
  private World(BodyInfo player, Map<BodyInfo, Boolean> obstacles, boolean won) {
    super();

    // zero gravity, since the simulation is top-down
    this.setGravity(ZERO_GRAVITY);

    // create the player
    this.player = player.toBody();
    this.player.addFixture(Geometry.createCircle(1.0), 1.0, 1.0, 1.0);
    this.player.setLinearDamping(2);
    this.player.setMass(MassType.NORMAL);
    this.addBody(this.player);

    // copy the obstacles
    this.obstacles = new HashMap<>();
    for (BodyInfo info : obstacles.keySet()) {
      Body obstacle = info.toBody();
      obstacle.addFixture(Geometry.createCircle(1.0), 1.0, 0.0, 1.0);
      obstacle.setMass(MassType.NORMAL);
      this.obstacles.put(obstacle, obstacles.get(info));
      this.addBody(obstacle);
    }

    // create the goal
    this.won = won;
    this.goal = new Body();
    goal.addFixture(Geometry.createRectangle(1, WORLD_HEIGHT), 1.0, 0.0, 0.0);
    goal.setMass(MassType.INFINITE);
    goal.translate(WORLD_WIDTH / 2.0, 0);
    this.addBody(goal);

    // create the walls
    this.walls = new ArrayList<>();
    Body topWall = new Body();
    topWall.addFixture(Geometry.createRectangle(WORLD_WIDTH, 1), 1.0, 0.0, 0.0);
    topWall.setMass(MassType.INFINITE);
    topWall.translate(0.0, WORLD_HEIGHT / 2.0);
    this.addBody(topWall);
    this.walls.add(topWall);
    Body bottomWall = new Body();
    bottomWall.addFixture(Geometry.createRectangle(WORLD_WIDTH, 1), 1.0, 0.0, 0.0);
    bottomWall.setMass(MassType.INFINITE);
    bottomWall.translate(0.0, -WORLD_HEIGHT / 2.0);
    this.addBody(bottomWall);
    this.walls.add(bottomWall);
    Body leftWall = new Body();
    leftWall.addFixture(Geometry.createRectangle(1, WORLD_HEIGHT), 1.0, 0.0, 0.0);
    leftWall.setMass(MassType.INFINITE);
    leftWall.translate(-WORLD_WIDTH / 2.0, 0);
    this.addBody(leftWall);
    this.walls.add(leftWall);

    // create the collision listeners
    this.addCollisionListener(new CollisionListener(this));
  }

  /**
   * Constructs a new, empty world state.
   */
  public World() {
    this(new BodyInfo(-10, 0, 0, 0),
        World.generateRandomObstacleInfo(),
        false);
  }

  /**
   * Return the body info of randomly generated obstacles.
   *
   * @return the body info of the obstacles
   */
  public static Map<BodyInfo, Boolean> generateRandomObstacleInfo() {
    Map<BodyInfo, Boolean> obstacles = new HashMap<>();
    for (int count = 0; count < OBSTACLE_COUNT; count++) {
      Vector2 velocity = new Vector2(Math.random() * 2 * Math.PI).multiply(OBSTACLE_SPEED);
      double positionX = Math.random() * WORLD_WIDTH - WORLD_WIDTH / 2 ;
      double positionY = Math.random() * WORLD_HEIGHT - WORLD_HEIGHT / 2 ;
      obstacles.put(new BodyInfo(positionX, positionY, velocity.x, velocity.y), false);
    }
    return obstacles;
  }

  /**
   * Checks if this state is a losing state.
   *
   * @return true if this state is a losing state; false otherwise
   */
  public boolean isLose() {
    for (Boolean collision : this.obstacles.values()) {
      if (collision)
        return true;
    }
    return false;
  }

  /**
   * Checks if this state is a winning state.
   *
   * @return true if the player has collided with the goal; false otherwise
   */
  public boolean isWin() {
    return this.won;
  }

  /**
   * Checks if this state is a terminal state.
   *
   * @return true if the player has won or lost; false otherwise
   */
  public boolean isTerminal() {
    return this.isWin() || this.isLose();
  }

  /**
   * Creates the successor state to this state after applying the given force to the player.
   *
   * @param action the action to perform to the player
   * @return the world after the action was performed.
   */
  public World generateNextState(Force action) {

    // clone the world
    Map<BodyInfo, Boolean> obstacles = new HashMap<>();
    for(Body obstacle : this.obstacles.keySet()) {
      obstacles.put(new BodyInfo(obstacle), this.obstacles.get(obstacle));
    }
    World nextState = new World(new BodyInfo(this.player), obstacles, this.won);

    // apply the update and update the world
    nextState.player.applyForce(action);
    nextState.update(TIME_PER_UPDATE);

    return nextState;
  }

  /**
   * Returns the list of legal actions the player can take in a timestep.
   *
   * @return the array of forces
   */
  public Force[] getActions() {
    return new Force[] { FORCE_UP, FORCE_DOWN, FORCE_LEFT, FORCE_RIGHT, FORCE_NONE };
  }

  /**
   * Custom collision listener to help when checking when simulation should end.
   */
  private static class CollisionListener extends CollisionListenerAdapter<Body, BodyFixture> {

    private final World world;

    public CollisionListener(World world) {
      this.world = world;
    }

    @Override
    public boolean collision(NarrowphaseCollisionData<Body, BodyFixture> narrowphaseCollisionData) {
      Body body1 = narrowphaseCollisionData.getBody1();
      Body body2 = narrowphaseCollisionData.getBody2();

      // check if we player and goal collided
      if (body1 == this.world.goal && body2 == this.world.player || body1 == this.world.player && body2 == this.world.goal) {
        this.world.won = true;
      }

      // check if the player collided with an obstacle
      if (body1 == this.world.player) {
        if (this.world.obstacles.containsKey(body2)) {
          this.world.obstacles.put(body2, true);
        }
      }
      if (body2 == this.world.player) {
        if (this.world.obstacles.containsKey(body1)) {
          this.world.obstacles.put(body1, true);
        }
      }

      return super.collision(narrowphaseCollisionData);
    }
  }

  /**
   * Represents a serialized version of a {@link Body} object;
   */
  private static class BodyInfo {

    private final double positionX;
    private final double positionY;
    private final double velocityX;
    private final double velocityY;

    /**
     * Serializes the given body.
     *
     * @param body the body
     */
    public BodyInfo(Body body) {
      this(body.getWorldCenter().x, body.getWorldCenter().y, body.getLinearVelocity().x, body.getLinearVelocity().y);
    }

    /**
     * Creates a new body info object.
     *
     * @param positionX the x position
     * @param positionY the y position
     * @param velocityX the x velocity
     * @param velocityY the y velocity
     */
    public BodyInfo(double positionX, double positionY, double velocityX, double velocityY) {
      this.positionX = positionX;
      this.positionY = positionY;
      this.velocityX = velocityX;
      this.velocityY = velocityY;
    }

    /**
     * Deserializes this body.
     *
     * @return the body
     */
    public Body toBody() {
      Body body = new Body();
      body.translate(this.positionX, this.positionY);
      body.setLinearVelocity(this.velocityX, this.velocityY);
      return body;
    }
  }
}
