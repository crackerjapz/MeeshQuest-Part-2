package cmsc420.structure.prquadtree;

import java.awt.geom.Point2D;

import cmsc420.structure.City;

/**
 * Represents an empty leaf node of a PR Quadtree.
 */
public class EmptyNode extends Node {
	
	/** empty PR Quadtree node */
	public static EmptyNode instance = new EmptyNode();
	
	/**
	 * Constructs and initializes an empty node.
	 */
	public EmptyNode() {
		super(Node.EMPTY);
	}

	public Node add(City city, Point2D.Float origin, int width, int height) {
		Node leafNode = new LeafNode();
		return leafNode.add(city, origin, width, height);
	}

	public Node remove(City city, Point2D.Float origin, int width,
			int height) {
		/* should never get here, nothing to remove */
		throw new IllegalArgumentException();
	}
}
