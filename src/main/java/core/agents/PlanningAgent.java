package core.agents;

import core.world.World;
import org.dyn4j.dynamics.Force;

/**
 * Represents a planning agent that can choose actions to take in a world.
 */
public interface PlanningAgent {

  /**
   * Choose an action to take in the current state of the world.
   *
   * @param state the state of the world
   * @return the action to take
   */
  Force chooseAction(World state);

  /**
   * Perform any initialization before the agent can be used.
   */
  default void init() {
    // by default, do nothing
    System.out.println("No extra initialization");
  }
}
