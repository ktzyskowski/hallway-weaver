import java.util.HashMap;

import framework.SimulationBody;
import framework.SimulationFrame;
import org.dyn4j.dynamics.Force;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.dyn4j.world.World;
//Change this import to be the custom written world class

public class mcts {

    HashMap<mctsWorld, Double> reward;
    HashMap<mctsWorld, Integer> visited;
    HashMap<mctsWorld, List<mctsWorld>> children;
    double exploration_weight;

    public mcts() {
        this.reward = new HashMap<>();
        this.visited = new HashMap<>();
        this.children = new HashMap<>();
        this.exploration_weight = 1.0;
    }

    public mctsWorld choose(mctsWorld node) {
        if (node.isTerminal()) {
            throw new RuntimeException("Choose called on a terminal node");
        } else if (!this.children.containsKey(node)) {
            return node.findRandomChild();
        } else {
            return this.getMaxScore(node);
        }
    }

    // Returns the child node of the node passed in with the highest average reward per visit
    // Used as a helper function for choose()
    public mctsWorld getMaxScore(mctsWorld node) {
        mctsWorld maxNode = new mctsWorld();
        double maxAvgReward = Integer.MIN_VALUE;
        double childAvgReward;
        for (mctsWorld childNode : this.children.get(node)) {
            if (this.visited.get(childNode) == 0) {
                continue;
            } else {
                childAvgReward = this.reward.get(childNode) / this.visited.get(childNode);
                if (childAvgReward >= maxAvgReward) {
                    maxNode = childNode;
                    maxAvgReward = childAvgReward;
                }
            }
        }
        if (maxNode.equals(new mctsWorld())) {
            return null;
        } else {
            return maxNode;
        }
    }

    // Make the tree one layer better, train for one iteration
    public void doRollout(mctsWorld node) {
        List<mctsWorld> path = this.select(node);
        mctsWorld leaf = path.get(path.size() - 1);
        this.expand(leaf);
        double reward = this.simulate(leaf);
        this.backPropagate(path, reward);
    }
   
    // Find an unexplored descendent of node
    public List<mctsWorld> select(mctsWorld node) {
        List<mctsWorld> path = new ArrayList<mctsWorld>();
        while (true) {
            path.add(node);
            if (!this.children.containsKey(node) || !((this.children.get(node)).size() == 0)) {
                return path;
            }
            List<mctsWorld> unexploreNodes = this.findUnexplored(node);
            if (unexploreNodes.size() > 0) {
                mctsWorld n = unexploreNodes.remove(unexploreNodes.size() - 1);
                path.add(n);
                return path;
            }
            node = this.uctSelect(node);
        }
    }

    // Returns the list of nodes that aren't keys already
    // Serves as a helper for select
    public List<mctsWorld> findUnexplored(mctsWorld node) {
        List<mctsWorld> unexplored = new ArrayList<mctsWorld>(this.children.get(node));
        for (mctsWorld key : this.children.keySet()) {
            if (this.children.get(node).contains(key)) {
                unexplored.remove(key);
            }
        }
        return unexplored;
    }

    // Update the children hash map with the children of the given node
    public void expand(mctsWorld node) {
        if (this.children.containsKey(node)) {
            return;
        }
        this.children.put(node, node.findChildren());
    }

    // Returns reward of a random simulation to completion of the given node
    public double simulate(mctsWorld node) {
        boolean invertReward = true;
        while (true) {
            if (node.isTerminal()) {
                double reward = node.reward();
                if (invertReward) {
                    return 1.0 - reward;
                } else {
                    return reward;
                }
            }
            node = node.findRandomChild();
            invertReward = !invertReward;
        }
    }

    // Sending the reward back up the ancestors of the leaf
    public void backPropagate(List<mctsWorld> path, double reward) {
        Collections.reverse(path);
        for(mctsWorld node : path) {
            this.visited.put(node, this.visited.get(node) + 1);
            this.reward.put(node, this.reward.get(node) + reward);
            reward = 1.0 - reward;
        }
    }


    public mctsWorld uctSelect(mctsWorld node) {
        for(mctsWorld childNode : this.children.get(node)) {
            if(!this.children.containsKey(childNode)) {
                throw new AssertionError("The children of the given node have not all been expanded.");
            }
        }
        Double logNVertex = Math.log(this.visited.get(node));

        return getMaxUCT(logNVertex, node);

    }

    public mctsWorld getMaxUCT(Double logNVertex, mctsWorld node) {
        mctsWorld maxNode = new mctsWorld();
        double maxUpperConfidence = Integer.MIN_VALUE;
        for(mctsWorld childNode : this.children.get(node)) {
            double childConfidenceVal = (this.reward.get(childNode) / this.reward.get(childNode)) + 
            this.exploration_weight * Math.sqrt(logNVertex / this.visited.get(childNode)); 
            if(childConfidenceVal > maxUpperConfidence) {
                maxNode = childNode;
                maxUpperConfidence = childConfidenceVal;
            }
        }
        if(maxNode.equals(new mctsWorld())) {
            return null;
        }
        else {
            return maxNode;
        }
    }

}

class mctsWorld extends World {

    private Random rand;

    public mctsWorld() {
        super();
        this.rand = new Random();
    }

    public List<mctsWorld> findChildren() {
        Force[] actions = this.getActions();
        List<mctsWorld> childStates = new ArrayList<mctsWorld>();
        if (this.isTerminal()) {
            return null;
        }
        for (Force action : actions) {
            childStates.add(this.generateNextState(action));
        }
        return childStates;
    }

    public mctsWorld findRandomChild() {
        Force[] actions = this.getActions();
        if (this.isTerminal()) {
            return null;
        }
        return this.generateNextState(actions[rand.nextInt(actions.length)]);
    }

    public double reward() {
        if(!this.isTerminal()) {
            throw new RuntimeException("Reward called on nonterminal board");
        }
        else if(this.isLose()) {
            return 0.0;
        }
        else if (this.isWin()) {
            return 1.0;
        }
        else {
            throw new Exception("None of the conditionals were met.")
        }
    }
}