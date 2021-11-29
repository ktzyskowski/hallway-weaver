import framework.SimulationBody;
import framework.SimulationFrame;
import java.util.ArrayList;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.PhysicsWorld;

import java.awt.*;

public final class HallwayWeaver extends SimulationFrame {

    public final static double WIDTH = 10;
    public final static double HEIGHT = 2;

    private SimulationBody agent;
    private SimulationBody endZone;
    private ArrayList<SimulationBody> adversaries;

    private Force action;

    public static void main(String[] args) {
        HallwayWeaver simulation = new HallwayWeaver("Hallway Weaver", 100.0);
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
        this.world.addBody(this.createRectangle(16,0.1,0,-1.6));
        this.world.addBody(this.createRectangle(16,0.1,0,1.6));
        this.world.addBody(this.createRectangle(0.1,3.1,-8,0));
        this.world.addBody(this.createRectangle(0.1,3.1,8,0));

        // add the main agent
        this.agent = this.createBall(1, 0, -5, 0);
        this.agent.setColor(Color.BLUE);
        this.world.addBody(this.agent);

        // add the adversaries
        this.adversaries = this.createAdversaries(50);
        this.adversaries.forEach(this.world::addBody);

        // add end zone
        endZone = this.createRectangle(0.1, 3.1, 7.9, 0);
        endZone.setColor(Color.GREEN);
        this.world.addBody(endZone);
    }

    @Override
    protected void preWorldUpdate() {

    }

    @Override
    protected void postWorldUpdate() {

    }

    private SimulationBody createRectangle(double width, double height, double xOffset, double yOffset) {
        SimulationBody wall = new SimulationBody(Color.BLACK);
        wall.addFixture(Geometry.createRectangle(width, height), 1, 0, 1);
        wall.translate(xOffset, yOffset);
        wall.setMass(MassType.INFINITE);
        return wall;
    }

    private SimulationBody createBall(double vx, double vy, double ox, double oy) {
        SimulationBody ball = new SimulationBody(Color.RED);
        BodyFixture fixture = ball.addFixture(Geometry.createCircle(0.05), 1.0, 0, 1);
        fixture.setRestitutionVelocity(0.0);
        ball.translate(ox, oy);
        ball.setLinearVelocity(vx, vy);
        ball.setAngularDamping(0.8);
        ball.setMass(MassType.NORMAL);
        return ball;
    }

    private ArrayList<SimulationBody> createAdversaries(int numAdversaries) {
        ArrayList<SimulationBody> adversaries = new ArrayList<>();
        for (int i = 0; i < numAdversaries; i++) {
            SimulationBody ball = this.createBall(0, 0, 0, 0);
            ball.setLinearVelocity(Vector2.create(2, Math.random() * Math.PI * 2));
            adversaries.add(ball);
        }
        return adversaries;
    }
}
