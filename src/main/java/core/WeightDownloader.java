package core;

import core.agents.QLearningAgent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WeightDownloader {

  /**
   * Creates a new Q learning agent with weights stored in the given filename
   *
   * @param filename the weights filename
   * @return the Q agent
   */
  public static QLearningAgent load(String filename) {
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
      Map<String, Double> weights = new HashMap<>();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] pair = line.split(";");
        weights.put(pair[0], Double.parseDouble(pair[1]));
      }
      bufferedReader.close();
      return new QLearningAgent(0, 0, 0, 0, weights);
    } catch (FileNotFoundException exception) {
      return null;
    } catch (IOException ioException) {
      return null;
    }
  }

  /**
   * Saves the given Q agent's weights in a file with the given name.
   *
   * @param agent    the Q agent
   * @param filename the filename
   */
  public static void save(QLearningAgent agent, String filename) {
    try {
      PrintWriter out = new PrintWriter(filename);
      for (Entry<String, Double> pair : agent.weights.entrySet()) {
        out.printf("%s;%s%n", pair.getKey(), pair.getValue());
      }
    } catch (FileNotFoundException exception) {
      // do nothing
    }
  }

}
