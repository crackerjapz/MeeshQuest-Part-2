package cmsc420.structure.prquadtree;

import java.awt.geom.Point2D;

import cmsc420.structure.City;

/**
 * Node abstract class for a PR Quadtree. A node can either be an empty
 * node, a leaf node, or an internal node.
 */
public abstract class Node {
	/** Type flag for an empty PR Quadtree node */
	public static final int EMPTY = 0;

	/** Type flag for a PR Quadtree leaf node */
	public static final int LEAF = 1;

	/** Type flag for a PR Quadtree internal node */
	public static final int INTERNAL = 2;

	/** type of PR Quadtree node (either empty, leaf, or internal) */
	protected final int type;

	/**
	 * Constructor for abstract Node class.
	 * 
	 * @param type
	 *            type of the node (either empty, leaf, or internal)
	 */
	protected Node(final int type) {
		this.type = type;
	}

	/**
	 * Adds a city to the node. If an empty node, the node becomes a leaf
	 * node. If a leaf node already, the leaf node becomes an internal node
	 * and both cities are added to it. If an internal node, the city is
	 * added to the child whose quadrant the city is located within.
	 * 
	 * @param city
	 *            city to be added to the PR Quadtree
	 * @param origin
	 *            origin of the rectangular bounds of this node
	 * @param width
	 *            width of the rectangular bounds of this node
	 * @param height
	 *            height of the rectangular bounds of this node
	 * @return this node after the city has been added
	 */
	public abstract Node add(City city, Point2D.Float origin, int width,
			int height);

	/**
	 * Removes a city from the node. If this is a leaf node and the city is
	 * contained in it, the city is removed and the node becomes a leaf
	 * node. If this is an internal node, then the removal command is passed
	 * down to the child node whose quadrant the city falls in.
	 * 
	 * @param city
	 *            city to be removed
	 * @param origin
	 *            origin of the rectangular bounds of this node
	 * @param width
	 *            width of the rectangular bounds of this node
	 * @param height
	 *            height of the rectangular bounds of this node
	 * @return this node after the city has been removed
	 */
	public abstract Node remove(City city, Point2D.Float origin, int width,
			int height);

	/**
	 * Gets the type of the node (either empty, leaf, or internal).
	 * 
	 * @return type of the node
	 */
	public int getType() {
		return type;
	}
}

