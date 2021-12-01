package core.agents;

import core.world.World;
import java.util.Arrays;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.DetectFilter;
import org.dyn4j.world.result.RaycastResult;

/**
 * A planning agent that learns the optimal action policy through approximate Q learning.
 */
public class QLearningAgent implements PlanningAgent {

  private final double alpha;    // learning rate
  private final double gamma;    // discount factor
  private final double epsilon;  // exploitation factor
  private final double episodes; // number of training episodes

  private static final int REWARD_NEUTRAL = -1;     // reward for surviving an action
  private static final int REWARD_WIN     =  1_000; // reward for reaching the goal state
  private static final int REWARD_LOSE    = -1_000; // reward for touching an obstacle

  private static final int    NUM_RAYS = 100; // number of radar rays
  private static final double LEN_RAYS = 5.0; // length of each radar ray

  private final double[] weights;

  /**
   * Creates a new Q learning agent from starting weights.
   *
   * @param alpha    the learning rate (between 0 and 1)
   * @param gamma    the discount factor (between 0 and 1)
   * @param epsilon  the exploitation factor (between 0 and 1); a larger number means exploit the policy more often
   * @param episodes the number of training episodes to conduct before evaluation
   * @param weights  the starting weights
   */
  public QLearningAgent(double alpha, double gamma, double epsilon, double episodes, double[] weights) {
    this.alpha = alpha;
    this.gamma = gamma;
    this.epsilon = epsilon;
    this.episodes = episodes;
    this.weights = weights;
  }

  /**
   * Creates a new Q learning agent.
   *
   * @param alpha    the learning rate (between 0 and 1)
   * @param gamma    the discount factor (between 0 and 1)
   * @param epsilon  the exploitation factor (between 0 and 1); a larger number means exploit the policy more often
   * @param episodes the number of training episodes to conduct before evaluation
   */
  public QLearningAgent(double alpha, double gamma, double epsilon, double episodes) {
    this(alpha, gamma, epsilon, episodes, new double[4 + NUM_RAYS]);
  }

  @Override
  public Force chooseAction(World state) {
    double[] features = this.extractFeatures(state);
    Force[] actions = state.getActions();

    Force bestAction = actions[0];
    double bestQValue = this.qValue(state, actions[0]);
    for (int index = 1; index < actions.length; index++) {
      double candidateQValue = this.qValue(state, actions[index]);
      if (candidateQValue > bestQValue) {
        bestQValue = candidateQValue;
        bestAction = actions[index];
      }
    }

    return bestAction;
  }

  @Override
  public void init() {
    System.out.println("Begin training");
    for (int episode = 1; episode <= this.episodes; episode++) {
      System.out.printf("Before episode %d%n", episode);
      this.train();
      System.out.printf("After episode %d%n", episode);
    }
    System.out.println("End training");
  }

  /**
   * Extracts an array of features from the given world state.
   *
   * The 1st index in the array is the x position of the player
   * The 2nd index is the y position of the player
   * The 3rd index is the x velocity of the player
   * The 4th index is the y velocity of the player
   * The remaining indices are the radar readings around the player
   *
   * @param state the world state
   * @return the feature representation
   */
  private double[] extractFeatures(World state) {
    double[] features = new double[4 + NUM_RAYS];

    // position and velocity
    Vector2 playerPosition = state.player.getWorldCenter();
    features[0] = playerPosition.x;
    features[1] = playerPosition.y;
    features[2] = state.player.getLinearVelocity().x;
    features[3] = state.player.getLinearVelocity().y;

    // radar readings
    for (int count = 0; count < NUM_RAYS; count++) {
      Ray ray = new Ray(playerPosition, 2 * Math.PI * count / NUM_RAYS);
      RaycastResult<Body, BodyFixture> result = state.raycastClosest(ray, LEN_RAYS, new DetectFilter<Body, BodyFixture>(true, true, null));
      if (result == null) {
        features[4 + count] = LEN_RAYS;
      } else {
        features[4 + count] = result.getRaycast().getDistance();
      }
    }

    return features;
  }

  /**
   * Computes the Q value of the given state and action pair.
   * @param state  the world state
   * @param action the action
   * @return the Q value
   */
  private double qValue(World state, Force action) {
    double[] features = this.extractFeatures(state);

    double qValue = 0.0;
    for (int index = 0; index < features.length; index++) {
      qValue += features[index] * weights[index];
    }

    return qValue;
  }

  /**
   * Performs one episode of training.
   */
  private void train() {
    World state = new World();
    while (!state.isTerminal()) {

      // choose an action, either random (exploration) or from our policy (exploitation)
      Force[] actions = state.getActions();
      Force action;
      if (Math.random() > this.epsilon) {
        action = actions[(int) (Math.random() * actions.length)];
      } else {
        World finalState = state; // <- need this to use lambdas
        action = Arrays.stream(actions).max((a1, a2) -> (int) (this.qValue(finalState, a1) - this.qValue(
            finalState, a2))).get();
      }

      // observe the changes and find the next state
      World nextState = state.generateNextState(action);

      // calculate the reward from the next state
      int reward = REWARD_NEUTRAL;
      if (nextState.isWin()) {
        reward = REWARD_WIN;
      } else if (nextState.isLose()) {
        reward = -REWARD_LOSE;
      }

      // update our weights to reflect a new and improved Q function
      this.update(state, action, nextState, reward);
      state = nextState;
    }
  }

  /**
   * Updates the internal weights after one observed sample taken during a training episode.
   *
   * @param state     the old state
   * @param action    the action performed
   * @param nextState the new state
   * @param reward    the reward received
   */
  private void update(World state, Force action, World nextState, int reward) {
    double[] features = this.extractFeatures(state);
    double sample = reward
        + (this.gamma * Arrays.stream(nextState.getActions())
        .map(nextAction -> this.qValue(nextState, nextAction)).max(Double::compareTo).get());
    double difference = sample - this.qValue(state, action);
    for (int index = 0; index < this.weights.length; index++) {
      weights[index] = weights[index] + alpha * difference * features[index];
    }
  }
}
