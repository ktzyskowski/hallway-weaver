package core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Shape;
import org.dyn4j.geometry.Vector2;

/**
 * Graphics2D renderer for dyn4j shape types. Only {@link Circle} and {@link Polygon} objects are supported.
 */
public final class Graphics2DRenderer {

  /**
   * Renders the given shape to the given graphics context using the given scale and color.
   *
   * @param g the graphics context
   * @param shape the shape to render
   * @param scale the scale to render the shape (pixels per dyn4j unit (typically meter)
   * @param color the color
   */
  public static void render(Graphics2D g, Shape shape, double scale, Color color) {
    if (shape == null) return;
    if (color == null) color = Color.RED;

    if (shape instanceof Circle) {
      Graphics2DRenderer.render(g, (Circle) shape, scale, color);
    } else if (shape instanceof Polygon) {
      Graphics2DRenderer.render(g, (Polygon) shape, scale, color);
    }
  }

  /**
   * Renders the given {@link Circle} to the given graphics context using the given scale and color.
   *
   * @param g the graphics context
   * @param circle the circle to render
   * @param scale the scale to render the shape (pixels per dyn4j unit (typically meter)
   * @param color the color
   */
  public static void render(Graphics2D g, Circle circle, double scale, Color color) {
    double radius = circle.getRadius();
    Vector2 center = circle.getCenter();

    double radius2 = 2.0 * radius;
    Ellipse2D.Double c = new Ellipse2D.Double(
        (center.x - radius) * scale,
        (center.y - radius) * scale,
        radius2 * scale,
        radius2 * scale);

    // fill the shape
    g.setColor(color);
    g.fill(c);

    // draw the outline
    g.setColor(getOutlineColor(color));
    g.draw(c);
  }

  /**
   * Renders the given {@link Polygon} to the given graphics context using the given scale and color.
   *
   * @param g the graphics context
   * @param polygon the polygon to render
   * @param scale the scale to render the shape (pixels per dyn4j unit (typically meter))
   * @param color the color
   */
  public static void render(Graphics2D g, Polygon polygon, double scale, Color color) {
    Vector2[] vertices = polygon.getVertices();
    int l = vertices.length;

    // create the awt polygon
    Path2D.Double p = new Path2D.Double();
    p.moveTo(vertices[0].x * scale, vertices[0].y * scale);
    for (int i = 1; i < l; i++) {
      p.lineTo(vertices[i].x * scale, vertices[i].y * scale);
    }
    p.closePath();

    // fill the shape
    g.setColor(color);
    g.fill(p);

    // draw the outline
    g.setColor(Graphics2DRenderer.getOutlineColor(color));
    g.draw(p);
  }

  /**
   * Returns the outline color for the given color.
   *
   * @param color the fill color
   * @return Color the outline color
   */
  private static Color getOutlineColor(Color color) {
    Color outlineColor = color.darker();
    return new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), color.getAlpha());
  }
}