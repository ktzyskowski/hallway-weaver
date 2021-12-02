package core.agents;

import core.world.World;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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

  private static final int    NUM_RAYS = 30;  // number of radar rays
  private static final double LEN_RAYS = 10.0; // length of each radar ray

  private final Map<String, Double> weights;

  /**
   * Creates a new Q learning agent from starting weights.
   *
   * @param alpha    the learning rate (between 0 and 1)
   * @param gamma    the discount factor (between 0 and 1)
   * @param epsilon  the exploitation factor (between 0 and 1); a larger number means exploit the policy more often
   * @param episodes the number of training episodes to conduct before evaluation
   * @param weights  the starting weights
   */
  public QLearningAgent(double alpha, double gamma, double epsilon, double episodes, Map<String, Double> weights) {
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
    this(alpha, gamma, epsilon, episodes, new HashMap<>());
  }

  @Override
  public Force chooseAction(World state) {
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
    for (int episode = 1; episode <= this.episodes; episode++) {
      System.out.printf("Start episode %d%n", episode);
      this.train();

      // print weights
//      for (String key : this.weights.keySet()) {
//        System.out.printf("%s: %f,", key, this.weights.get(key));
//        System.out.println();
//      }
    }
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
  private Map<String, Double> extractFeatures(World state, Force action) {
    Map<String, Double> features = new HashMap<>();

    // action
    if (action == World.FORCE_UP) {
      features.put("force.up", 1.0);
    } else if (action == World.FORCE_DOWN) {
      features.put("force.down", 1.0);
    } else if (action == World.FORCE_LEFT) {
      features.put("force.left", 1.0);
    } else if (action == World.FORCE_RIGHT) {
      features.put("force.right", 1.0);
    }

    // position and velocity
    Vector2 playerPosition = state.player.getWorldCenter();
    features.put(String.format("player.x.%d", (int) playerPosition.x), 1.0);
    features.put(String.format("player.vx.%d", (int) state.player.getLinearVelocity().x), 1.0);
    features.put(String.format("player.vy.%d", (int) state.player.getLinearVelocity().y), 1.0);

    // radar readings
    World nextState = state.generateNextState(action);
    for (int count = 0; count < NUM_RAYS; count++) {
      Ray ray = new Ray(playerPosition, 2 * Math.PI * count / NUM_RAYS);
      Ray nextRay = new Ray(nextState.player.getWorldCenter(), 2 * Math.PI * count / NUM_RAYS);
      RaycastResult<Body, BodyFixture> result = state.raycastClosest(ray, LEN_RAYS, new DetectFilter<>(true, true, null));
      RaycastResult<Body, BodyFixture> nextResult = nextState.raycastClosest(nextRay, LEN_RAYS, new DetectFilter<>(true, true, null));
      if (result == null) {
        features.put(String.format("ray.s.%d", count), 1.0);

        if (nextResult != null) { // next ray is not maxed out
          features.put(String.format("ray.t.%d", count), nextResult.getRaycast().getDistance() / LEN_RAYS - 1.0);
        }
        // no else, since both null means 0
      } else {
        features.put(String.format("ray.s.%d", count), result.getRaycast().getDistance() / LEN_RAYS);

        if (nextResult == null) { // next ray is maxed out
          features.put(String.format("ray.t.%d", count), result.getRaycast().getDistance() / LEN_RAYS - 1.0);
        } else { // neither are maxed out
          features.put(String.format("ray.t.%d", count), nextResult.getRaycast().getDistance() / LEN_RAYS - result.getRaycast().getDistance() / LEN_RAYS);
        }
      }
    }

    return features;
  }

  /**
   * Computes the Q value of the given state and action pair.
   *
   * @param state  the world state
   * @param action the action
   * @return the Q value
   */
  private double qValue(World state, Force action) {
    Map<String, Double> features = this.extractFeatures(state, action);

    double qValue = 0.0;
    for (Entry<String, Double> pair : features.entrySet()) {
      qValue += (pair.getValue() * this.weights.getOrDefault(pair.getKey(), 0.0));
    }

    //System.out.printf("Q: %f%n", qValue);
    return qValue;
  }

  /**
   * Performs one episode of training.
   */
  private void train() {
    World state = new World();
    int step = 0;
    while (!state.isTerminal()) {
      step++;

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

      // we want to reward going to the right towards the goal
      if (action == World.FORCE_RIGHT)
        reward *= -1;

      if (nextState.isWin()) {
        reward = REWARD_WIN;
      } else if (nextState.isLose()) {
        reward = -REWARD_LOSE;
      }

      // update our weights to reflect a new and improved Q function
      this.update(state, action, nextState, reward);
      state = nextState;
    }

    System.out.printf("> took %d samples%n", step);
    if (state.isWin()) {
      System.out.println("> won :)");
    } else {
      System.out.println("> lost :(");
    }
    System.out.printf("> final player.x: %d%n", (int) state.player.getWorldCenter().x);
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
    Map<String, Double> features = this.extractFeatures(state, action);
    double sample = reward
        + (this.gamma * Arrays.stream(nextState.getActions())
        .map(nextAction -> this.qValue(nextState, nextAction)).max(Double::compareTo).get());
    double difference = sample - this.qValue(state, action);
    for (String key : features.keySet()) {
      double updatedWeight = this.weights.getOrDefault(key, 0.0) + alpha * difference * features.get(key);
      this.weights.put(key, updatedWeight);
      //System.out.printf("%f ", updatedWeight);
    }
    //System.out.println();
  }
}
