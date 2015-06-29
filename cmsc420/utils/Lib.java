package cmsc420.utils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Lib {

	/**
	 * Returns if a point lies within a given rectangular bounds according to
	 * the rules of the PR Quadtree.
	 * 
	 * @param point
	 *            point to be checked
	 * @param rect
	 *            rectangular bounds the point is being checked against
	 * @return true if the point lies within the rectangular bounds, false
	 *         otherwise
	 */
	public static boolean intersects(Point2D point, Rectangle2D rect) {
		return (point.getX() >= rect.getMinX() && point.getX() < rect.getMaxX()
				&& point.getY() >= rect.getMinY() && point.getY() < rect
				.getMaxY());
	}
}
