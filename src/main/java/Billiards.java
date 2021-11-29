import java.awt.Color;

import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import framework.SimulationBody;
import framework.SimulationFrame;
import org.dyn4j.world.World;

/**
 * A simple scene of two billiard balls colliding with one another
 * and a wall.
 * <p>
 * Primarily used to illustrate the computation of the mass and size
 * of the balls.  See the {@link Billiards#initializeWorld()} method.
 * @author William Bittle
 * @version 4.2.0
 * @since 3.2.0
 */
public final class Billiards extends SimulationFrame {
    /** The serial version id */
    private static final long serialVersionUID = -8518496343422955267L;

    /**
     * Default constructor.
     */
    public Billiards() {
        super("Billiards", 250.0);
    }

    /* (non-Javadoc)
     * @see org.dyn4j.samples.SimulationFrame#initializeWorld()
     */
    @Override
    protected void initializeWorld() {
        // no gravity on a top-down view of a billiards game
        this.world.setGravity(World.ZERO_GRAVITY);

        // create all your bodies/joints

        final double edgeDepth = 0.29 / 2.0;
        final double tableWidth = 1.83;
        final double tableHeight = 1.12;

        final double halfTableWidth = tableWidth / 2.0;
        final double halfTableHeight = tableHeight / 2.0;
        final double halfEdgeDepth = edgeDepth / 2.0;

        // 2.25 in diameter = 0.028575 m radius
        final double ballRadius = 0.028575;

        // 0.126 oz/in^3 = 217.97925 kg/m^3
        final double ballDensity = 217.97925;

        final double ballFriction = 0;//0.08;
        final double ballRestitution = 1;//0.9;

        SimulationBody wallRight = new SimulationBody(new Color(150, 75, 0));
        BodyFixture fixture = wallRight.addFixture(Geometry.createRectangle(edgeDepth, tableHeight), 1.0, 0.4, 0.3);
        fixture.setRestitutionVelocity(0.0);
        wallRight.translate(halfTableWidth - halfEdgeDepth, 0);
        wallRight.setMass(MassType.INFINITE);
        world.addBody(wallRight);

        SimulationBody wallLeft = new SimulationBody(new Color(150, 75, 0));
        fixture = wallLeft.addFixture(Geometry.createRectangle(edgeDepth, tableHeight), 1.0, 0.4, 0.3);
        fixture.setRestitutionVelocity(0.0);
        wallLeft.translate(-halfTableWidth + halfEdgeDepth, 0);
        wallLeft.setMass(MassType.INFINITE);
        world.addBody(wallLeft);

        SimulationBody wallTop = new SimulationBody(new Color(150, 75, 0));
        fixture = wallTop.addFixture(Geometry.createRectangle(tableWidth, edgeDepth), 1.0, 0.4, 0.3);
        fixture.setRestitutionVelocity(0.0);
        wallTop.translate(0, halfTableHeight - halfEdgeDepth);
        wallTop.setMass(MassType.INFINITE);
        world.addBody(wallTop);

        SimulationBody wallBottom = new SimulationBody(new Color(150, 75, 0));
        fixture = wallBottom.addFixture(Geometry.createRectangle(tableWidth, edgeDepth), 1.0, 0.4, 0.3);
        fixture.setRestitutionVelocity(0.0);
        wallBottom.translate(0, -halfTableHeight + halfEdgeDepth);
        wallBottom.setMass(MassType.INFINITE);
        world.addBody(wallBottom);

        SimulationBody cueBall = new SimulationBody(new Color(255, 255, 255));
        fixture = cueBall.addFixture(Geometry.createCircle(ballRadius), ballDensity, ballFriction, ballRestitution);
        fixture.setRestitutionVelocity(0.0);
        cueBall.translate(-0.25, 0.0);
        cueBall.setLinearVelocity(2.0, 0.0);
        //cueBall.setLinearDamping(0.3);
        cueBall.setAngularDamping(0.8);
        cueBall.setMass(MassType.NORMAL);
        this.world.addBody(cueBall);

        // billiard colors
        Color[] colors = new Color[] {
                // solid
                new Color(255, 215, 0),
                new Color(0, 0, 255),
                new Color(255, 0, 0),
                new Color(75, 0, 130),
                new Color(255, 69, 0),
                new Color(34, 139, 34),
                new Color(128, 0, 0),
                new Color(0, 0, 0),

                // striped (just do a lighter color)
                new Color(255, 215, 0).darker(),
                new Color(0, 0, 255).darker(),
                new Color(255, 0, 0).darker(),
                new Color(75, 0, 130).brighter(),
                new Color(255, 69, 0).darker(),
                new Color(34, 139, 34).brighter(),
                new Color(128, 0, 0).brighter(),
                new Color(0, 0, 0).brighter()
        };

        final int rackSize = 5;
        final double sx = 0.45;
        final double sy = 0.0;

        // 5 columns
        int n = 0;
        for (int i = 0; i < rackSize; i++) {
            double x = sx - (ballRadius * 2.0 * (double)i);
            double columnHeight = ballRadius * 2.0 * (rackSize - i);
            double csy = columnHeight / 2.0;
            // 5 - i rows
            for (int j = 0; j < rackSize - i; j++) {
                double y = sy + csy - (ballRadius * 2.0 * j);

                SimulationBody ball = new SimulationBody(colors[n]);
                fixture = ball.addFixture(Geometry.createCircle(ballRadius), ballDensity, ballFriction, ballRestitution);
                fixture.setRestitutionVelocity(0.0);
                ball.translate(x, y);
                ball.setLinearDamping(0.4);
                ball.setAngularDamping(0.8);
                ball.setMass(MassType.NORMAL);
                this.world.addBody(ball);

                n++;
            }
        }
    }

    /**
     * Entry point for the example application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Billiards simulation = new Billiards();
        simulation.run();
    }
}