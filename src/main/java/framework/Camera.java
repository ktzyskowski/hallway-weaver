package framework;

import java.awt.Point;

import lombok.Data;
import org.dyn4j.geometry.Vector2;

/**
 * Stores the zoom and panning state of the camera.
 */
@Data
public final class Camera {

    /** The scale (zoom) in pixels per meter */
    private double scale;

    /** The pan-x in pixels */
    private double offsetX;

    /** The pan-y in pixels */
    private double offsetY;

    /**
     * Returns world coordinates for the given point given the width/height of the viewport.
     *
     * @param width the viewport width (in pixels)
     * @param height the viewport height (in pixels)
     * @param p the point
     * @return Vector2 the world coordinates
     */
    public Vector2 toWorldCoordinates(double width, double height, Point p) {
        return new Vector2(
                (p.getX() - width * 0.5 - this.offsetX) / this.scale,
                -(p.getY() - height * 0.5 + this.offsetY) / this.scale);
    }
}