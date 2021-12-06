package core.agents;

import java.util.HashMap;

import org.dyn4j.dynamics.Force;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import core.world.World;

public class mcts implements PlanningAgent {

    HashMap<World, Double> reward;
    HashMap<World, Integer> visited;
    HashMap<World, List<World>> children;
    double exploration_weight;

    public mcts() {
        this.reward = new HashMap<>();
        this.visited = new HashMap<>();
        this.children = new HashMap<>();
        this.exploration_weight = 1.0;
    }

    public Force chooseAction(World node) {
        if (node.isTerminal()) {
            throw new RuntimeException("Choose called on a terminal node");
        } else if (!this.children.containsKey(node)) {
            System.out.println("got here");
            return node.getRandomForce();
        } else {
            return this.getMaxNodeForce(node, this.getMaxScore(node));
        }
    }

    public Force getMaxNodeForce(World parentNode, World childNode) {

        if (parentNode.generateNextState(World.FORCE_UP).equals(childNode)) {
            return World.FORCE_UP;
        } else if (parentNode.generateNextState(World.FORCE_DOWN).equals(childNode)) {
            return World.FORCE_DOWN;
        } else if (parentNode.generateNextState(World.FORCE_LEFT).equals(childNode)) {
            return World.FORCE_LEFT;
        } else if (parentNode.generateNextState(World.FORCE_RIGHT).equals(childNode)) {
            return World.FORCE_RIGHT;
        } else if (parentNode.generateNextState(World.FORCE_NONE).equals(childNode)) {
            return World.FORCE_NONE;
        } else {
            throw new RuntimeException("The equals function isn't working.");
        }
    }

    // Returns the child node of the node passed in with the highest average reward
    // per visit
    // Used as a helper function for choose()
    public World getMaxScore(World node) {
        World maxNode = new World();
        double maxAvgReward = Integer.MIN_VALUE;
        double childAvgReward;
        for (World childNode : this.children.get(node)) {
            if (!this.visited.containsKey(childNode) || this.visited.get(childNode) == 0) {
                continue;
            } else {
                childAvgReward = this.reward.get(childNode) / this.visited.get(childNode);
                if (childAvgReward >= maxAvgReward) {
                    maxNode = childNode;
                    maxAvgReward = childAvgReward;
                }
            }
        }
        if (maxNode.equals(new World())) {
            return null;
        } else {
            return maxNode;
        }
    }

    // Make the tree one layer better, train for one iteration
    public void doRollout(World node) {
        List<World> path = this.select(node);
        World leaf = path.get(path.size() - 1);
        this.expand(leaf);
        double reward = this.simulate(leaf);
        this.backPropagate(path, reward);
    }

    // Find an unexplored descendent of node
    public List<World> select(World node) {
        List<World> path = new ArrayList<World>();
        while (true) {
            path.add(node);
            if (!this.children.containsKey(node) || (this.children.get(node)).size() == 0) {
                return path;
            }
            List<World> unexploreNodes = this.findUnexplored(node);
            if (unexploreNodes.size() > 0) {
                World n = unexploreNodes.remove(unexploreNodes.size() - 1);
                path.add(n);
                return path;
            }
            node = this.uctSelect(node);
        }
    }

    // Returns the list of nodes that aren't keys already
    // Serves as a helper for select
    public List<World> findUnexplored(World node) {
        List<World> unexplored = new ArrayList<World>(this.children.get(node));
        for (World key : this.children.keySet()) {
            if (this.children.get(node).contains(key)) {
                unexplored.remove(key);
            }
        }
        return unexplored;
    }

    // Update the children hash map with the children of the given node
    public void expand(World node) {
        if (this.children.containsKey(node)) {
            return;
        }
        this.children.put(node, node.findChildren());
    }

    // Returns reward of a random simulation to completion of the given node
    public double simulate(World node) {
        // while (true) {
        // if (node.isTerminal()) {
        // double reward = node.reward();
        // if (invertReward) {
        // return 1.0 - reward;
        // } else {
        // return reward;
        // }
        // }
        // node = node.findRandomChild();
        // invertReward = !invertReward;
        // }
        World curNode = node;
        for (int i = 0; i < 20; i++) {
            if (curNode.isTerminal()) {
                return curNode.reward();
            }
            curNode = curNode.findRandomChild();
        }
        System.out.println("distance reward: " + Double.toString(this.distanceCalc(node, curNode)));
        return this.distanceCalc(node, curNode);
    }

    public double distanceCalc(World startNode, World curNode) {
        double difference = curNode.player.getWorldCenter().x - startNode.player.getWorldCenter().x;
        double ratio = difference / ((World.WORLD_WIDTH / 2.0) - startNode.player.getWorldCenter().x);
        return ratio;
    }

    // Sending the reward back up the ancestors of the leaf
    public void backPropagate(List<World> path, double reward) {
        Collections.reverse(path);
        for (World node : path) {
            if (!this.visited.containsKey(node)) {
                this.visited.put(node, 1);
            }
            if (!this.reward.containsKey(node)) {
                this.reward.put(node, reward);
            }
            this.visited.put(node, this.visited.get(node) + 1);
            this.reward.put(node, this.reward.get(node) + reward);
            reward = 1.0 - reward;
        }
    }

    public World uctSelect(World node) {
        for (World childNode : this.children.get(node)) {
            if (!this.children.containsKey(childNode)) {
                throw new AssertionError("The children of the given node have not all been expanded.");
            }
        }
        Double logNVertex = Math.log(this.visited.get(node));

        return this.getMaxUCT(logNVertex, node);

    }

    public World getMaxUCT(Double logNVertex, World node) {
        World maxNode = new World();
        double maxUpperConfidence = Integer.MIN_VALUE;
        for (World childNode : this.children.get(node)) {
            double childConfidenceVal = (this.reward.get(childNode) / this.reward.get(childNode)) +
                    this.exploration_weight * Math.sqrt(logNVertex / this.visited.get(childNode));
            if (childConfidenceVal > maxUpperConfidence) {
                maxNode = childNode;
                maxUpperConfidence = childConfidenceVal;
            }
        }
        if (maxNode.equals(new World())) {
            return null;
        } else {
            return maxNode;
        }
    }

}