package cmsc420.structure.prquadtree;

import java.awt.geom.Point2D;

import cmsc420.structure.City;

/**
 * Represents a leaf node of a PR Quadtree.
 */
public class LeafNode extends Node {
	/** city contained within this leaf node */
	protected City city;

	/**
	 * Constructs and initializes a leaf node.
	 */
	public LeafNode() {
		super(Node.LEAF);
	}

	/**
	 * Gets the city contained by this node.
	 * 
	 * @return city contained by this node
	 */
	public City getCity() {
		return city;
	}

	public Node add(City newCity, Point2D.Float origin, int width,
			int height) {
		if (city == null) {
			/* node is empty, add city */
			city = newCity;
			return this;
		} else {
			/* node is full, partition node and then add city */
			InternalNode internalNode = new InternalNode(origin, width,
					height);
			internalNode.add(city, origin, width, height);
			internalNode.add(newCity, origin, width, height);
			return internalNode;
		}
	}

	public Node remove(City city, Point2D.Float origin, int width,
			int height) {
		if (this.city != city) {
			/* city not here */
			throw new IllegalArgumentException();
		} else {
			/* remove city, node becomes empty */
			this.city = null;
			return EmptyNode.instance;
		}
	}
}