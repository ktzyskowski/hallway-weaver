import framework.SimulationBody;
import framework.SimulationFrame;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.world.PhysicsWorld;

import java.awt.*;

public final class HallwayWeaver extends SimulationFrame {

    public final static double WIDTH = 10;
    public final static double HEIGHT = 8;

    public static void main(String[] args) {
        HallwayWeaver simulation = new HallwayWeaver("Hallway Weaver", 200.0);
        simulation.run();
    }

    /**
     * Constructs a new simulation frame (default resolution 800x600)
     *
     * @param name  the frame name
     * @param scale the pixels per meter scale factor
     */
    public HallwayWeaver(String name, double scale) {
        super(name, scale);
    }

    @Override
    protected void initializeWorld() {
        this.world.setGravity(PhysicsWorld.ZERO_GRAVITY);

        // add world walls (and static obstacles)
        this.addWall(.1,4.1,-3,0);
        this.addWall(.1,4.1,3,0);
        this.addWall(6.1,.1,0,-2);
        this.addWall(6.1,.1,0,2);

        this.addBall();
    }

    private void addWall(double width, double height, double xOffset, double yOffset) {
        SimulationBody wall = new SimulationBody(Color.BLACK);
        wall.addFixture(Geometry.createRectangle(width, height), 1, 0, 1);
        wall.translate(xOffset, yOffset);
        wall.setMass(MassType.INFINITE);
        this.world.addBody(wall);
    }

    private void addBall() {
        SimulationBody ball = new SimulationBody(Color.RED);
        BodyFixture fixture = ball.addFixture(Geometry.createCircle(0.05), 1.0, 0, 1);
        fixture.setRestitutionVelocity(0.0);
        ball.translate(-0.25, 0.0);
        ball.setLinearVelocity(0.5, 0);
        ball.setAngularDamping(0.8);
        ball.setMass(MassType.NORMAL);
        this.world.addBody(ball);
    }
}
