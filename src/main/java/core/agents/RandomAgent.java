package core.agents;

import core.world.World;
import org.dyn4j.dynamics.Force;

/**
 * Represents an agent that chooses actions randomly.
 */
public class RandomAgent implements PlanningAgent {

  @Override
  public Force chooseAction(World state) {
    Force[] actions = state.getActions();
    return actions[(int) (Math.random() * actions.length)];
  }
}
