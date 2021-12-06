package core.world;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
  public static final int MILESTONE_DISTANCE = 10;
  public static final int NUM_STEPS = 3;
  public static final int OBSTACLE_COUNT = 60;
  public static final double OBSTACLE_SPEED = 10.0;

  public static final double FORCE_MAGNITUDE = 2_000.0;
  public static final Force FORCE_UP = new Force(0, FORCE_MAGNITUDE);
  public static final Force FORCE_DOWN = new Force(0, -FORCE_MAGNITUDE);
  public static final Force FORCE_LEFT = new Force(-FORCE_MAGNITUDE, 0);
  public static final Force FORCE_RIGHT = new Force(FORCE_MAGNITUDE, 0);
  public static final Force FORCE_NONE = new Force(0, 0);

  // =====   State Variables   ===== //
  public boolean won;
  public int score;
  public final Body player;
  public final Body goal;
  public final Map<Body, Boolean> obstacles;
  public final ArrayList<Body> walls;
  public final Set<Integer> milestones;

  /**
   * Copies the given world state.
   *
   * @param player    the player body info
   * @param obstacles the obstacle body info and collision info
   * @param won       whether the goal has been touched by the player
   * @param score     the score of the world
   */
  private World(BodyInfo player, Map<BodyInfo, Boolean> obstacles, boolean won, int score, Set<Integer> milestones) {
    super();

    // zero gravity, since the simulation is top-down
    this.setGravity(ZERO_GRAVITY);

    // clone the score
    this.score = score;

    // clone the milestones
    this.milestones = new HashSet<>(milestones);

    // create the player
    this.player = player.toBody();
    this.player.addFixture(Geometry.createCircle(1.0), 1.0, 0.0, 1.0);
    this.player.setLinearDamping(3);
    this.player.setMass(MassType.NORMAL);
    this.addBody(this.player);

    // copy the obstacles
    this.obstacles = new LinkedHashMap<>();
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
    this.addBody(this.goal);

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
    this(new BodyInfo(-WORLD_WIDTH / 2 + 10, 0, 0, 0),
        World.generateRandomObstacleInfo(new Vector2(-WORLD_WIDTH / 2 + 10, 0)),
        false, 0, new HashSet<>());
  }

  /**
   * Return the body info of randomly generated obstacles.
   *
   * @return the body info of the obstacles
   */
  public static Map<BodyInfo, Boolean> generateRandomObstacleInfo(Vector2 stayAwayPoint) {
    Map<BodyInfo, Boolean> obstacles = new HashMap<>();
    for (int count = 0; count < OBSTACLE_COUNT; count++) {
      Vector2 velocity = new Vector2(Math.random() * 2 * Math.PI).multiply(OBSTACLE_SPEED);

      double positionX = Math.random() * WORLD_WIDTH - WORLD_WIDTH / 2;
      double positionY = Math.random() * WORLD_HEIGHT - WORLD_HEIGHT / 2;

      // make sure obstacles don't appear near origin where player starts
      while (Math.sqrt((positionX - stayAwayPoint.x) * (positionX - stayAwayPoint.x) + (positionY - stayAwayPoint.y) * (positionY - stayAwayPoint.y)) < 10) {
        positionX = Math.random() * WORLD_WIDTH - WORLD_WIDTH / 2;
        positionY = Math.random() * WORLD_HEIGHT - WORLD_HEIGHT / 2;
      }

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

    World nextState = new World(new BodyInfo(this.player), obstacles, this.won, this.score, this.milestones);

    // apply the update and update the world
    nextState.player.applyForce(action);
    nextState.step(NUM_STEPS);

    // update the milestones if new ones were reached
    if (nextState.player.getWorldCenter().x > MILESTONE_DISTANCE - WORLD_WIDTH / 2) {
      nextState.milestones.add((int) (nextState.player.getWorldCenter().x) / MILESTONE_DISTANCE);
    }

    // update the score of the new word accordingly
    if (action == World.FORCE_LEFT) {
      nextState.score -= 1;
    } else if (action == World.FORCE_RIGHT) {
      nextState.score += 1;
    }

    if (nextState.isWin()) {
      nextState.score += 1_000;
    } else if (nextState.isLose()) {
      nextState.score -= 1_000;
    }

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
//      if (body1 == this.world.goal && body2 == this.world.player || body1 == this.world.player && body2 == this.world.goal) {
//        this.world.won = true;
//        System.out.println("! collided with goal !");
//        return super.collision(narrowphaseCollisionData);
//      }

      if (body1 == this.world.goal) {
        if (body2 == this.world.player) {
          this.world.won = true;
        }
      } else if (body2 == this.world.goal) {
        if (body1 == this.world.player) {
          this.world.won = true;
        }
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

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof World)) {
      return false;
    } else {
      World that = (World) o;

      boolean samePlayers = this.player.getWorldCenter().equals(that.player.getWorldCenter())
          && this.player.getLinearVelocity().equals(that.player.getLinearVelocity());

      boolean sameObstacles = true;
      Iterator<Body> thisObstacles = this.obstacles.keySet().iterator();
      Iterator<Body> thatObstacles = this.obstacles.keySet().iterator();
      while (thisObstacles.hasNext()) {
        Body thisObstacle = thisObstacles.next();
        Body thatObstacle = thatObstacles.next();
        if (!thisObstacle.getWorldCenter().equals(thatObstacle.getWorldCenter())
        || !thisObstacle.getLinearVelocity().equals(thatObstacle.getLinearVelocity())) {
          sameObstacles = false;
          break;
        }
      }

      boolean sameStatus = false;
      if (this.isWin() && !that.isWin()) {
        sameStatus = true;
      } else if (this.isLose() && that.isLose()) {
        sameStatus = true;
      } else if (!this.isTerminal() && !that.isTerminal()) {
        sameStatus = true;
      }

      return samePlayers && sameObstacles && sameStatus;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.won,
        this.player.getWorldCenter(),
        this.player.getLinearVelocity(),
        this.obstacles.hashCode());
  }
}
