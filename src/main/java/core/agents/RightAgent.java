package core.agents;

import core.world.World;
import org.dyn4j.dynamics.Force;

/**
 * Represents an agent that chooses to go right all the time.
 */
public class RightAgent implements PlanningAgent {

  @Override
  public Force chooseAction(World state) {
    return state.getActions()[3];
  }
}
