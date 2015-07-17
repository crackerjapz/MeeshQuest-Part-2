package cmsc420.structure.pmquadtree;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Float;
import java.util.HashSet;

import cmsc420.exception.CityAlreadyMappedException;
import cmsc420.exception.CityOutOfBoundsException;
import cmsc420.exception.RoadAlreadyMappedException;
import cmsc420.exception.RoadOutOfBoundsException;
import cmsc420.geom.Circle2D;
import cmsc420.structure.City;
import cmsc420.utils.Canvas;
import cmsc420.utils.Lib;

/*
 * Clearly this section is inspired by the PRQuadTree from the canonical as 
 * well as the pseudocode from the spec.  Most of the Node implementation is from
 * the canonical with modifications to make it relevant. 
 */
public class PM3QuadTree {

	White SingletonWhiteNode = new White();

	/** root of the PM Quadtree */
	protected Node root;

	/** bounds of the spatial map */
	protected Point2D.Float spatialOrigin;

	/** width of the spatial map */
	protected int spatialWidth;

	/** height of the spatial map */
	protected int spatialHeight;

	/** used to keep track of cities within the spatial map */
	protected HashSet<String> cityNames;

	/** used to keep track of roads within the spatial map */
	protected HashSet<QEdge> roadList;

	/**used to keep track of isolated cities within the map */
	protected HashSet<String> isoCityNames;

	public PM3QuadTree(){
		root = SingletonWhiteNode;
		spatialOrigin = new Point2D.Float(0, 0);
		cityNames = new HashSet<String>();
		isoCityNames = new HashSet<String>();
		roadList = new HashSet<QEdge>();
	}
	//sets up the PMQuadTree
	public void setRange(int spatialWidth, int spatialHeight) {
		this.spatialWidth = spatialWidth;
		this.spatialHeight = spatialHeight;
	}

	//clears Structure
	public void clear() {
		root = SingletonWhiteNode;
		cityNames.clear();
		isoCityNames.clear();
	}

	/**
	 * Returns if the PM Quadtree contains a city with the given name.
	 * 
	 * @return true if the city is in the spatial map. false otherwise.
	 */
	public boolean contains(String name) {
		return cityNames.contains(name) || isoCityNames.contains(name);
	}

	//checks for empty structure
	public boolean isEmpty() {
		return (root == SingletonWhiteNode);
	}

	public boolean hasCites() {
		return (!cityNames.isEmpty() || !isoCityNames.isEmpty());
	}
	public boolean hasIsoCites() {
		return (!isoCityNames.isEmpty());
	}
	public boolean isInIso(City city) {
		String name = city.getName();
		return (isoCityNames.contains(name));
	}
	public boolean isInIso(String city) {
		//String name = city.getName();
		return (isoCityNames.contains(city));
	}
	/**
	 * Gets the root node of the PM Quadtree.
	 * 
	 * @return root node of the PM Quadtree
	 */
	public Node getRoot() {
		return root;
	}



	/**
	 * Returns if any part of a circle lies within a given rectangular bounds
	 * according to the rules of the PR Quadtree.
	 * 
	 * @param circle
	 *            circular region to be checked
	 * @param rect
	 *            rectangular bounds the point is being checked against
	 * @return true if the point lies within the rectangular bounds, false
	 *         otherwise
	 */
	public boolean intersects(Circle2D circle, Rectangle2D rect) {
		final double radiusSquared = circle.getRadius() * circle.getRadius();

		/* translate coordinates, placing circle at origin */
		final Rectangle2D.Double r = new Rectangle2D.Double(rect.getX()
				- circle.getCenterX(), rect.getY() - circle.getCenterY(), rect
				.getWidth(), rect.getHeight());

		if (r.getMaxX() < 0) {
			/* rectangle to left of circle center */
			if (r.getMaxY() < 0) {
				/* rectangle in lower left corner */
				return ((r.getMaxX() * r.getMaxX() + r.getMaxY() * r.getMaxY()) < radiusSquared);
			} else if (r.getMinY() > 0) {
				/* rectangle in upper left corner */
				return ((r.getMaxX() * r.getMaxX() + r.getMinY() * r.getMinY()) < radiusSquared);
			} else {
				/* rectangle due west of circle */
				return (Math.abs(r.getMaxX()) < circle.getRadius());
			}
		} else if (r.getMinX() > 0) {
			/* rectangle to right of circle center */
			if (r.getMaxY() < 0) {
				/* rectangle in lower right corner */
				return ((r.getMinX() * r.getMinX() + r.getMaxY() * r.getMaxY()) < radiusSquared);
			} else if (r.getMinY() > 0) {
				/* rectangle in upper right corner */
				return ((r.getMinX() * r.getMinX() + r.getMinY() * r.getMinY()) <= radiusSquared);
			} else {
				/* rectangle due east of circle */
				return (r.getMinX() <= circle.getRadius());
			}
		} else {
			/* rectangle on circle vertical centerline */
			if (r.getMaxY() < 0) {
				/* rectangle due south of circle */
				return (Math.abs(r.getMaxY()) < circle.getRadius());
			} else if (r.getMinY() > 0) {
				/* rectangle due north of circle */
				return (r.getMinY() <= circle.getRadius());
			} else {
				/* rectangle contains circle center point */
				return true;
			}
		}
	}
	public abstract class Node{
		/** Type flag for an empty PM Quadtree node */
		public static final int WHITE = 0;

		/** Type flag for a PM Quadtree leaf node */
		public static final int BLACK = 1;

		/** Type flag for a PM Quadtree internal node */
		public static final int GRAY = 2;

		/** type of PM Quadtree node (either empty, leaf, or internal) */
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
		 * Gets the type of the node (either empty, leaf, or internal).
		 * 
		 * @return type of the node
		 */
		public int getType() {
			return type;
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

		public abstract Node addRoad(QEdge road);


	}

	public class Black extends Node {
		HashSet <QEdge> roads = new HashSet<QEdge>();
		City city = null;

		public Black(){
			super(Node.BLACK);
		}

		public boolean hasCity(){
			//	if (city == null) return false;
			//return true;

			return (city == null) ? false : true;
		}


		public boolean hasCityRoads(){
			//	if (city == null) return false;
			//return true;

			return (city == null) ? false : true;
		}

		/**
		 * Gets the city contained by this node.
		 * 
		 * @return city contained by this node
		 */
		public City getCity() {
			return city;
		}

		@Override
		public Node add(City newCity, Float origin, int width, int height) {
			if (city == null || city.equals(newCity)) {
				/* node is empty, add city */
				city = newCity;
				return this;
			} else {
				/* node is full, partition node and then add city */
				Gray internalNode = new Gray(origin, width,
						height);
				internalNode.add(city, origin, width, height);
				internalNode.add(newCity, origin, width, height);
				for (QEdge road : roads){
					internalNode.addRoad(road);
				}
				return internalNode;
			}
		}

		@Override
		public Node addRoad(QEdge road) {
			if (roads.contains(road)){
				return this;
			}
			roads.add(road);
			return this;
		}

		public int getRoadsSize(){
			return roads.size();
		}
		
		public HashSet<QEdge> getRoads(){
			return roads;
		}
	}

	public class Gray extends Node{

		/** children nodes of this node */
		public Node[] children;

		/** rectangular quadrants of the children nodes */
		protected Rectangle2D.Float[] regions;

		/** origin of the rectangular bounds of this node */
		public Point2D.Float origin;

		/** origins of the rectangular bounds of each child node */
		protected Point2D.Float[] origins;

		/** width of the rectangular bounds of this node */
		public int width;

		/** height of the rectangular bounds of this node */
		public int height;

		/** half of the width of the rectangular bounds of this node */
		protected int halfWidth;

		/** half of the height of the rectangular bounds of this node */
		protected int halfHeight;
		public Gray(){
			super(Node.GRAY);
		}


		public Gray(Float origin, int width, int height) {
			super(Node.GRAY);

			this.origin = origin;

			children = new Node[4];
			for (int i = 0; i < 4; i++) {
				children[i] = SingletonWhiteNode;
			}

			this.width = width;
			this.height = height;

			halfWidth = width >> 1;
			halfHeight = height >> 1;

			origins = new Point2D.Float[4];
			origins[0] = new Point2D.Float(origin.x, origin.y + halfHeight);
			origins[1] = new Point2D.Float(origin.x + halfWidth, origin.y
					+ halfHeight);
			origins[2] = new Point2D.Float(origin.x, origin.y);
			origins[3] = new Point2D.Float(origin.x + halfWidth, origin.y);

			regions = new Rectangle2D.Float[4];
			int i = 0;
			while (i < 4) {
				regions[i] = new Rectangle2D.Float(origins[i].x, origins[i].y,
						halfWidth, halfHeight);
				i++;
			}

			/* add a cross to the drawing panel */
			if (Canvas.instance != null) {
				//canvas.addCross(getCenterX(), getCenterY(), halfWidth, Color.d);
				int cx = getCenterX();
				int cy = getCenterY();
				Canvas.instance.addLine(cx - halfWidth, cy, cx + halfWidth, cy, Color.GRAY);
				Canvas.instance.addLine(cx, cy - halfHeight, cx, cy + halfHeight, Color.GRAY);
			}
		}


		/**
		 * Gets the child node of this node according to which quadrant it falls
		 * in
		 * 
		 * @param quadrant
		 *            quadrant number (top left is 0, top right is 1, bottom
		 *            left is 2, bottom right is 3)
		 * @return child node
		 */
		public Node getChild(int quadrant) {
			if (quadrant < 0 || quadrant > 3) {
				throw new IllegalArgumentException();
			} else {
				return children[quadrant];
			}
		}

		/**
		 * Gets the rectangular region for the specified child node of this
		 * internal node.
		 * 
		 * @param quadrant
		 *            quadrant that child lies within
		 * @return rectangular region for this child node
		 */
		public Rectangle2D.Float getChildRegion(int quadrant) {
			if (quadrant < 0 || quadrant > 3) {
				throw new IllegalArgumentException();
			} else {
				return regions[quadrant];
			}
		}

		/**
		 * Gets the center X coordinate of this node's rectangular bounds.
		 * 
		 * @return center X coordinate of this node's rectangular bounds
		 */
		public int getCenterX() {
			return (int) origin.x + halfWidth;
		}

		/**
		 * Gets the center Y coordinate of this node's rectangular bounds.
		 * 
		 * @return center Y coordinate of this node's rectangular bounds
		 */
		public int getCenterY() {
			return (int) origin.y + halfHeight;
		}
		@Override
		public Node add(City city, Float origin, int width, int height) {
			final Point2D cityLocation = city.toPoint2D();
			for (int i = 0; i < 4; i++) {
				if (Lib.intersects(cityLocation, regions[i])) {
					children[i] = children[i].add(city, origins[i], halfWidth,
							halfHeight);
				}
			}
			return this;
		}


		@Override
		public Node addRoad(QEdge road) {
			final Line2D.Float line = road;
			for (int i = 0; i < 4; i++) {
				if (line.intersects(regions[i])) {
					children[i] = children[i].addRoad(road);
				}
			}
			return this;
		}

	}

	public class White extends Node{

		/**
		 * Constructs and initializes an empty node.
		 */
		public White() {
			super(Node.WHITE);
		}

		public Node add(City city, Point2D.Float origin, int width, int height) {
			Node blackNode = new Black();
			return blackNode.add(city, origin, width, height);
		}

		public Node addRoad(QEdge road) {
			Node blackNode = new Black();
			return blackNode.addRoad(road);
		}
		public Node remove(City city, Point2D.Float origin, int width,
				int height) {
			/* should never get here, nothing to remove */
			throw new IllegalArgumentException();
		}
	}

	//by compartmentalizing the city list to add the names in these functions, a single
	//add function can operate for both types of cities easily.
	public void add(City city) throws 
	CityAlreadyMappedException,
	CityOutOfBoundsException{

		if (cityNames.contains(city.getName()) || isoCityNames.contains(city.getName())) {
			/* city already mapped */
			throw new CityAlreadyMappedException();
		}

		/* check bounds */
		int x = (int) city.getX();
		int y = (int) city.getY();
		if (x < spatialOrigin.x || x >= spatialWidth || y < spatialOrigin.y
				|| y >= spatialHeight) {
			/* city out of bounds */
			throw new CityOutOfBoundsException();
		}

		/* insert city into PMQuadTree */
		//isoCityNames.add(city.getName());
		root = root.add(city, spatialOrigin, spatialWidth, spatialHeight);
	}


	public void addRoad(City start, City end) throws 
	RoadAlreadyMappedException,
	RoadOutOfBoundsException{

		QEdge insert = new QEdge(start, end);
		if (roadList.contains(insert)){
			throw new RoadAlreadyMappedException();
		}

		Rectangle2D.Float test = new Rectangle2D.Float(spatialOrigin.x, spatialOrigin.y,
				spatialWidth, spatialHeight);

		if (!insert.intersects(test)){
			throw new RoadOutOfBoundsException();
		}

		if (Lib.intersects(start.pt, test)){
			root = root.add(start, spatialOrigin, spatialWidth, spatialHeight);
			cityNames.add(start.getName());
			/* add city to canvas */
			Canvas.instance.addPoint(start.getName(), start.getX(), start.getY(),
					Color.BLACK);
		}
		if (Lib.intersects(end.pt, test)){
			root = root.add(end, spatialOrigin, spatialWidth, spatialHeight);
			cityNames.add(end.getName());
			/* add city to canvas */
			Canvas.instance.addPoint(end.getName(), end.getX(), end.getY(),
					Color.BLACK);
		}
	
		//must take care of adding cities for the roads here:
		QEdge road = new QEdge(start, end);
		root.addRoad(road);
		roadList.add(road);
	}
	public void addIso(String name) {
		isoCityNames.add(name);
	}
}
