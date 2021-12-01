package core.agents;

import core.world.World;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import org.dyn4j.dynamics.Force;

/**
 * A planning agent that chooses actions based off of what keys are being pressed.
 */
public class KeyboardAgent implements PlanningAgent, KeyListener {

  /** Keeps track of which keys are being pressed down */
  // in this order: [up, down, left, right]
  private final boolean[] pressed = new boolean[4];

  @Override
  public Force chooseAction(World state) {

    // choose first available action
    if (pressed[0]) {
      return state.getActions()[0];
    }
    else if (pressed[1]) {
      return state.getActions()[1];
    }
    else if (pressed[2]) {
      return state.getActions()[2];
    }
    else if (pressed[3]) {
      return state.getActions()[3];
    }
    return state.getActions()[4]; // do nothing
  }

  @Override
  public void keyTyped(KeyEvent e) {
    // do nothing;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_UP) {
      pressed[0] = true;
    }
    else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
      pressed[1] = true;
    }
    else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
      pressed[2] = true;
    }
    else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
      pressed[3] = true;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_UP) {
      pressed[0] = false;
    }
    else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
      pressed[1] = false;
    }
    else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
      pressed[2] = false;
    }
    else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
      pressed[3] = false;
    }
  }
}
