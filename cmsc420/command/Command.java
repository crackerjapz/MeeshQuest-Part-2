/**
 * @(#)Command.java        1.1 
 * 
 * 2014/09/09
 *
 * @author Ruofei Du, Ben Zoller (University of Maryland, College Park), 2014
 * 
 * All rights reserved. Permission is granted for use and modification in CMSC420 
 * at the University of Maryland.
 */
package cmsc420.command;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import cmsc420.utils.*;
import cmsc420.utils.Shape2DDistanceCalculator;
import cmsc420.utils.Canvas;
import cmsc420.drawing.*;
import cmsc420.exception.CityAlreadyMappedException;
import cmsc420.exception.CityOutOfBoundsException;
import cmsc420.geom.*;
import cmsc420.structure.*;
import cmsc420.structure.pmquadtree.PM3QuadTree;
import cmsc420.structure.pmquadtree.PM3QuadTree.Node;
import cmsc420.structure.pmquadtree.PM3QuadTree.Black;
import cmsc420.structure.pmquadtree.PM3QuadTree.Gray;
//import cmsc420.structure.prquadtree.PRQuadtree;
//import cmsc420.structure.prquadtree.Node;
//import cmsc420.structure.prquadtree.LeafNode;
//import cmsc420.structure.prquadtree.InternalNode;


/**
 * Processes each command in the MeeshQuest program. Takes in an XML command
 * node, processes the node, and outputs the results.
 * 
 * @author Ben Zoller
 * @version 2.0, 23 Jan 2007
 */
public class Command {
	/** output DOM Document tree */
	protected Document results;

	/** root node of results document */
	protected Element resultsNode;

	/**
	 * stores created cities sorted by their names (used with listCities command)
	 */
	protected final TreeMap<String, City> citiesByName = new TreeMap<String, City>(new Comparator<String>() {

		@Override
		public int compare(String o1, String o2) {
			return o2.compareTo(o1);
		}
		
	});

	/**
	 * stores created cities sorted by their locations (used with listCities command)
	 */
	protected final TreeSet<City> citiesByLocation = new TreeSet<City>(
			new CityLocationComparator());

	/** stores mapped cities in a spatial data structure */
	//protected final PRQuadtree prQuadtree = new PRQuadtree();
	
	protected final PM3QuadTree pmQuadtree = new PM3QuadTree();

	/** spatial width and height of the PR Quadtree */
	protected int spatialWidth, spatialHeight;

	/**
	 * Set the DOM Document tree to send the of processed commands to.
	 * 
	 * Creates the root results node.
	 * 
	 * @param results
	 *            DOM Document tree
	 */
	public void setResults(Document results) {
		this.results = results;
		resultsNode = results.createElement("results");
		results.appendChild(resultsNode);
	}

	/**
	 * Creates a command result element. Initializes the command name.
	 * 
	 * @param node
	 *            the command node to be processed
	 * @return the results node for the command
	 */
	private Element getCommandNode(final Element node) {
		final Element commandNode = results.createElement("command");
		commandNode.setAttribute("name", node.getNodeName());
		return commandNode;
	}

	/**
	 * Processes an integer attribute for a command. Appends the parameter to
	 * the parameters node of the results. Should not throw a number format
	 * exception if the attribute has been defined to be an integer in the
	 * schema and the XML has been validated beforehand.
	 * 
	 * @param commandNode
	 *            node containing information about the command
	 * @param attributeName
	 *            integer attribute to be processed
	 * @param parametersNode
	 *            node to append parameter information to
	 * @return integer attribute value
	 */
	private int processIntegerAttribute(final Element commandNode,
			final String attributeName, final Element parametersNode) {
		final String value = commandNode.getAttribute(attributeName);

		if (parametersNode != null) {
			/* add the parameters to results */
			final Element attributeNode = results.createElement(attributeName);
			attributeNode.setAttribute("value", value);
			parametersNode.appendChild(attributeNode);
		}

		/* return the integer value */
		return Integer.parseInt(value);
	}

	/**
	 * Processes a string attribute for a command. Appends the parameter to the
	 * parameters node of the results.
	 * 
	 * @param commandNode
	 *            node containing information about the command
	 * @param attributeName
	 *            string attribute to be processed
	 * @param parametersNode
	 *            node to append parameter information to
	 * @return string attribute value
	 */
	private String processStringAttribute(final Element commandNode,
			final String attributeName, final Element parametersNode) {
		final String value = commandNode.getAttribute(attributeName);

		if (parametersNode != null) {
			/* add parameters to results */
			final Element attributeNode = results.createElement(attributeName);
			attributeNode.setAttribute("value", value);
			parametersNode.appendChild(attributeNode);
		}

		/* return the string value */
		return value;
	}

	/**
	 * Reports that the requested command could not be performed because of an
	 * error. Appends information about the error to the results.
	 * 
	 * @param type
	 *            type of error that occurred
	 * @param command
	 *            command node being processed
	 * @param parameters
	 *            parameters of command
	 */
	private void addErrorNode(final String type, final Element command,
			final Element parameters) {
		final Element error = results.createElement("error");
		error.setAttribute("type", type);
		error.appendChild(command);
		error.appendChild(parameters);
		resultsNode.appendChild(error);
	}

	/**
	 * Reports that a command was successfully performed. Appends the report to
	 * the results.
	 * 
	 * @param command
	 *            command not being processed
	 * @param parameters
	 *            parameters used by the command
	 * @param output
	 *            any details to be reported about the command processed
	 */
	private void addSuccessNode(final Element command,
			final Element parameters, final Element output) {
		final Element success = results.createElement("success");
		success.appendChild(command);
		success.appendChild(parameters);
		success.appendChild(output);
		resultsNode.appendChild(success);
	}

	/**
	 * Processes the commands node (root of all commands). Gets the spatial
	 * width and height of the map and send the data to the appropriate data
	 * structures.
	 * 
	 * @param node
	 *            commands node to be processed
	 */
	public void processCommands(final Element node) {
		spatialWidth = Integer.parseInt(node.getAttribute("spatialWidth"));
		spatialHeight = Integer.parseInt(node.getAttribute("spatialHeight"));

		/* initialize canvas */
		Canvas.instance.setFrameSize(spatialWidth, spatialHeight);
		/* add a rectangle to show where the bounds of the map are located */
		Canvas.instance.addRectangle(0, 0, (spatialWidth > spatialHeight) ? spatialWidth : spatialHeight, 
				(spatialWidth > spatialHeight) ? spatialWidth : spatialHeight, Color.WHITE, true);
		Canvas.instance.addRectangle(0, 0, spatialWidth, spatialHeight, Color.BLACK,
				false);

		/* set PR Quadtree range */
		pmQuadtree.setRange(spatialWidth, spatialHeight);
	}

	/**
	 * Processes a createCity command. Creates a city in the dictionary (Note:
	 * does not map the city). An error occurs if a city with that name or
	 * location is already in the dictionary.
	 * 
	 * @param node
	 *            createCity node to be processed
	 */
	public void processCreateCity(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int radius = processIntegerAttribute(node, "radius",
				parametersNode);
		final String color = processStringAttribute(node, "color",
				parametersNode);

		/* create the city */
		final City city = new City(name, x, y, radius, color);

		if (citiesByLocation.contains(city)) {
			addErrorNode("duplicateCityCoordinates", commandNode,
					parametersNode);
		} else if (citiesByName.containsKey(name)) {
			addErrorNode("duplicateCityName", commandNode, parametersNode);
		} else {
			final Element outputNode = results.createElement("output");

			/* add city to dictionary */
			citiesByName.put(name, city);
			citiesByLocation.add(city);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}
//This one isn't being processed until Part 3
	/**
	 * Processes a deleteCity command. Deletes a city from the dictionary. An
	 * error occurs if the city does not exist or is currently mapped.
	 * 
	 * @param node
	 *            deleteCity node being processed
	 */
	/*public void processDeleteCity(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final String name = processStringAttribute(node, "name", parametersNode);

		if (!citiesByName.containsKey(name)) {
			/* city with name does not exist * /
			addErrorNode("cityDoesNotExist", commandNode, parametersNode);
		} else {
			/* delete city * /
			final Element outputNode = results.createElement("output");
			final City deletedCity = citiesByName.get(name);

			if (pmQuadtree.contains(name)) {
				/* city is mapped * /
				pmQuadtree.remove(deletedCity);
				addCityNode(outputNode, "cityUnmapped", deletedCity);
			}

			citiesByName.remove(name);
			citiesByLocation.remove(deletedCity);

			/* add success node to results * /
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}*/

	/**
	 * Clears all the data structures do there are not cities or roads in
	 * existence in the dictionary or on the map.
	 * 
	 * @param node
	 *            clearAll node to be processed
	 */
	public void processClearAll(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		/* clear data structures */
		citiesByName.clear();
		citiesByLocation.clear();
		pmQuadtree.clear();

		/* clear canvas */
		Canvas.instance.clear();
		/* add a rectangle to show where the bounds of the map are located */
		Canvas.instance.addRectangle(0, 0, spatialWidth, spatialHeight, Color.BLACK,
				false);

		/* add success node to results */
		addSuccessNode(commandNode, parametersNode, outputNode);
	}

	/**
	 * Lists all the cities, either by name or by location.
	 * 
	 * @param node
	 *            listCities node to be processed
	 */
	public void processListCities(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final String sortBy = processStringAttribute(node, "sortBy",
				parametersNode);

		if (citiesByName.isEmpty()) {
			addErrorNode("noCitiesToList", commandNode, parametersNode);
		} else {
			final Element outputNode = results.createElement("output");
			final Element cityListNode = results.createElement("cityList");

			Collection<City> cityCollection = null;
			if (sortBy.equals("name")) {
				cityCollection = citiesByName.values();
			} else if (sortBy.equals("coordinate")) {
				cityCollection = citiesByLocation;
			} else {
				/* XML validator failed */
				System.exit(-1);
			}

			for (City c : cityCollection) {
				addCityNode(cityListNode, c);
			}
			outputNode.appendChild(cityListNode);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	/**
	 * Creates a city node containing information about a city. Appends the city
	 * node to the passed in node.
	 * 
	 * @param node
	 *            node which the city node will be appended to
	 * @param cityNodeName
	 *            name of city node
	 * @param city
	 *            city which the city node will describe
	 */
	private void addCityNode(final Element node, final String cityNodeName,
			final City city) {
		final Element cityNode = results.createElement(cityNodeName);
		cityNode.setAttribute("name", city.getName());
		cityNode.setAttribute("x", Integer.toString((int) city.getX()));
		cityNode.setAttribute("y", Integer.toString((int) city.getY()));
		cityNode.setAttribute("radius", Integer
				.toString((int) city.getRadius()));
		cityNode.setAttribute("color", city.getColor());
		node.appendChild(cityNode);
	}

	/**
	 * Creates a city node containing information about a city. Appends the city
	 * node to the passed in node.
	 * 
	 * @param node
	 *            node which the city node will be appended to
	 * @param city
	 *            city which the city node will describe
	 */
	private void addCityNode(final Element node, final City city) {
		addCityNode(node, "city", city);
	}

	/**
	 * Maps a city to the spatial map.
	 * 
	 * @param node
	 *            mapCity command node to be processed
	 */
	public void processMapCity(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);

		final Element outputNode = results.createElement("output");

		if (!citiesByName.containsKey(name)) {
			addErrorNode("nameNotInDictionary", commandNode, parametersNode);
		} else if (pmQuadtree.contains(name)) {
			addErrorNode("cityAlreadyMapped", commandNode, parametersNode);
		} else {
			City city = citiesByName.get(name);
			try {
				/* insert city into PR Quadtree */
				pmQuadtree.add(city);

				/* add city to canvas */
				Canvas.instance.addPoint(city.getName(), city.getX(), city.getY(),
						Color.BLACK);

				/* add success node to results */
				addSuccessNode(commandNode, parametersNode, outputNode);
			} catch (CityAlreadyMappedException e) {
				addErrorNode("cityAlreadyMapped", commandNode, parametersNode);
			} catch (CityOutOfBoundsException e) {
				addErrorNode("cityOutOfBounds", commandNode, parametersNode);
			}
		}
	}

	//This one isn't being worked until part 3
	/**
	 * Removes a city from the spatial map.
	 * 
	 * @param node
	 *            unmapCity command node to be processed
	 */
	/*public void processUnmapCity(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);

		final Element outputNode = results.createElement("output");

		if (!citiesByName.containsKey(name)) {
			addErrorNode("nameNotInDictionary", commandNode, parametersNode);
		} else if (!pmQuadtree.contains(name)) {
			addErrorNode("cityNotMapped", commandNode, parametersNode);
		} else {
			City city = citiesByName.get(name);

			/* unmap the city in the PR Quadtree * /
			pmQuadtree.remove(city);

			/* remove city from canvas * /
			Canvas.instance.removePoint(city.getName(), city.getX(), city.getY(),
					Color.BLACK);

			/* add success node to results * /
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}*/

	/**
	 * Processes a saveMap command. Saves the graphical map to a given file.
	 * 
	 * @param node
	 *            saveMap command to be processed
	 * @throws IOException
	 *             problem accessing the image file
	 */
	public void processSaveMap(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);

		final Element outputNode = results.createElement("output");

		/* save canvas to '<name>.png' */
		Canvas.instance.save(name);

		/* add success node to results */
		addSuccessNode(commandNode, parametersNode, outputNode);
	}

	/**
	 * Prints out the structure of the PR Quadtree in a human-readable format.
	 * 
	 * @param node
	 *            printPRQuadtree command to be processed
	 */
	public void processPrintPRQuadtree(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		if (pmQuadtree.isEmpty()) {
			/* empty PR Quadtree */
			addErrorNode("mapIsEmpty", commandNode, parametersNode);
		} else {
			/* print PR Quadtree */
			final Element quadtreeNode = results.createElement("quadtree");
			printPRQuadtreeHelper(pmQuadtree.getRoot(), quadtreeNode);

			outputNode.appendChild(quadtreeNode);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	/**
	 * Traverses each node of the PR Quadtree.
	 * 
	 * @param currentNode
	 *            PR Quadtree node being printed
	 * @param xmlNode
	 *            XML node representing the current PR Quadtree node
	 */
	private void printPRQuadtreeHelper(final Node currentNode,
			final Element xmlNode) {
		if (currentNode.getType() == Node.WHITE) {
			Element white = results.createElement("white");
			xmlNode.appendChild(white);
		} else {
			if (currentNode.getType() == Node.BLACK) {
				/* leaf node */
				final Black currentLeaf = (Black) currentNode;
				final Element black = results.createElement("black");
				black.setAttribute("name", currentLeaf.getCity().getName());
				black.setAttribute("x", Integer.toString((int) currentLeaf
						.getCity().getX()));
				black.setAttribute("y", Integer.toString((int) currentLeaf
						.getCity().getY()));
				xmlNode.appendChild(black);
			} else {
				/* internal node */
				final Gray currentInternal = (Gray) currentNode;
				final Element gray = results.createElement("gray");
				gray.setAttribute("x", Integer.toString((int) currentInternal
						.getCenterX()));
				gray.setAttribute("y", Integer.toString((int) currentInternal
						.getCenterY()));
				for (int i = 0; i < 4; i++) {
					printPRQuadtreeHelper(currentInternal.getChild(i), gray);
				}
				xmlNode.appendChild(gray);
			}
		}
	}

	/**
	 * Finds the mapped cities within the range of a given point.
	 * 
	 * @param node
	 *            rangeCities command to be processed
	 * @throws IOException
	 */
	public void processRangeCities(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		final TreeSet<City> citiesInRange = new TreeSet<City>(
				new CityNameComparator());

		/* extract values from command */
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int radius = processIntegerAttribute(node, "radius",
				parametersNode);

		String pathFile = "";
		if (node.getAttribute("saveMap").compareTo("") != 0) {
			pathFile = processStringAttribute(node, "saveMap", parametersNode);
		}
		/* get cities within range */
		final Point2D.Double point = new Point2D.Double(x, y);
		rangeCitiesHelper(point, radius, pmQuadtree.getRoot(), citiesInRange);

		/* print out cities within range */
		if (citiesInRange.isEmpty()) {
			addErrorNode("noCitiesExistInRange", commandNode, parametersNode);
		} else {
			/* get city list */
			final Element cityListNode = results.createElement("cityList");
			for (City city : citiesInRange) {
				addCityNode(cityListNode, city);
			}
			outputNode.appendChild(cityListNode);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);

			if (pathFile.compareTo("") != 0) {
				/* save canvas to file with range circle */
				if(radius != 0) {
					Canvas.instance.addCircle(x, y, radius, Color.BLUE, false);
				}
				Canvas.instance.save(pathFile);
				if(radius != 0) {
					Canvas.instance.removeCircle(x, y, radius, Color.BLUE, false);
				}
			}
		}
	}

	/**
	 * Determines if any cities within the PR Quadtree not are within the radius
	 * of a given point.
	 * 
	 * @param point
	 *            point from which the cities are measured
	 * @param radius
	 *            radius from which the given points are measured
	 * @param node
	 *            PR Quadtree node being examined
	 * @param citiesInRange
	 *            a list of cities found to be in range
	 */
	private void rangeCitiesHelper(final Point2D.Double point,
			final int radius, final Node node, final TreeSet<City> citiesInRange) {
		if (node.getType() == Node.BLACK) {
			final Black leaf = (Black) node;
			final double distance = point.distance(leaf.getCity().toPoint2D());
			if (distance <= radius) {
				/* city is in range */
				final City city = leaf.getCity();
				citiesInRange.add(city);
			}
		} else if (node.getType() == Node.GRAY) {
			/* check each quadrant of internal node */
			final Gray internal = (Gray) node;

			final Circle2D.Double circle = new Circle2D.Double(point, radius);
			for (int i = 0; i < 4; i++) {
				if (pmQuadtree.intersects(circle, internal.getChildRegion(i))) {
					rangeCitiesHelper(point, radius, internal.getChild(i),
							citiesInRange);
				}
			}
		}
	}

	/**
	 * Finds the nearest city to a given point.
	 * 
	 * @param node
	 *            nearestCity command being processed
	 */
	public void processNearestCity(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		/* extract attribute values from command */
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);

		final Point2D.Float point = new Point2D.Float(x, y);
		
		if (citiesByName.size() <= 0) {
			addErrorNode("mapIsEmpty", commandNode, parametersNode);
			return;
		}

		//final PriorityQueue<NearestCity> nearCities = new PriorityQueue<NearestCity>(
		//		citiesByName.size());

		if (pmQuadtree.getRoot().getType() == Node.WHITE) {
			addErrorNode("mapIsEmpty", commandNode, parametersNode);
		} else {

			//
			//nearCities.add(new NearestCity(null, Double.POSITIVE_INFINITY));
			//

			//nearestCityHelper(prQuadtree.getRoot(), point, nearCities);
			//NearestCity nearestCity = nearCities.remove();
			City n = nearestCityHelper2(pmQuadtree.getRoot(), point);
			//addCityNode(outputNode, nearestCity.getCity());
			addCityNode(outputNode, n);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	/**
	 * 2/25/2011
	 * @param root
	 * @param point
	 */
	private City nearestCityHelper2(Node root, Point2D.Float point) {
		PriorityQueue<QuadrantDistance> q = new PriorityQueue<QuadrantDistance>();
		Node currNode = root;
		while (currNode.getType() != Node.BLACK) {
			Gray g = (Gray) currNode;
			for (int i = 0; i < 4; i++) {
				Node kid = g.children[i];
				if (kid.getType() != Node.WHITE) {
					q.add(new QuadrantDistance(kid, point));
				}
			}
			currNode = q.remove().quadtreeNode;
		}
		
		return ((Black) currNode).getCity();
	}
	
	class QuadrantDistance implements Comparable<QuadrantDistance> {
		public Node quadtreeNode;
		private double distance;
		
		public QuadrantDistance(Node node, Point2D.Float pt) {
			quadtreeNode = node;
			if (node.getType() == Node.GRAY) {
				Gray gray = (Gray) node;
				distance = Shape2DDistanceCalculator.distance(pt, 
						new Rectangle2D.Float(gray.origin.x, gray.origin.y, gray.width, gray.height));
			} else if (node.getType() == Node.BLACK) {
				Black leaf = (Black) node;
				distance = pt.distance(leaf.getCity().pt);
			} else {
				throw new IllegalArgumentException("Only leaf or internal node can be passed in");
			}
		}
		
		public int compareTo(QuadrantDistance qd) {
			if (distance < qd.distance) {
				return -1;
			} else if (distance > qd.distance) {
				return 1;
			} else {
				if (quadtreeNode.getType() != qd.quadtreeNode.getType()) {
					if (quadtreeNode.getType() == Node.GRAY) {
						return -1;
					} else {
						return 1;
					}
				} else if (quadtreeNode.getType() == Node.BLACK) {
					// both are leaves
					return ((Black) qd.quadtreeNode).getCity().getName().compareTo(
							((Black) quadtreeNode).getCity().getName());
				} else {
					// both are internals
					return 0;
				}
			}
		}
	}

//	/**
//	 * Examines the distance from each city in a PR Quadtree node from the given
//	 * point.
//	 * 
//	 * @param node
//	 *            PR Quadtree node being examined
//	 * @param point
//	 *            point
//	 * @param nearCities
//	 *            priority queue of cities organized by how close they are to
//	 *            the point
//	 */
//	private void nearestCityHelper(Node node, Point2D.Float point,
//			PriorityQueue<NearestCity> nearCities) {
//		if (node.getType() == Node.LEAF) {
//			LeafNode leaf = (LeafNode) node;
//			NearestCity nearCity = new NearestCity(leaf.getCity(), point
//					.distance(leaf.getCity().toPoint2D()));
//			if (nearCity.compareTo(nearCities.peek()) < 0) {
//				nearCities.add(nearCity);
//			}
//		} else if (node.getType() == Node.INTERNAL) {
//			InternalNode internal = (InternalNode) node;
//			TreeSet<NearestQuadrant> nearestQuadrants = new TreeSet<NearestQuadrant>();
//			for (int i = 0; i < 4; i++) {
//				nearestQuadrants.add(new NearestQuadrant(Shape2DDistanceCalculator.distance(point, internal
//						.getChildRegion(i)), i));
//			}
//			
//			for (NearestQuadrant nearQuadrant : nearestQuadrants) {
//				final int i = nearQuadrant.getQuadrant(); 
//				
//				if (Shape2DDistanceCalculator.distance(point, internal
//						.getChildRegion(i)) <= nearCities.peek().getDistance()) {
//
//					nearestCityHelper(internal.getChild(i), point, nearCities);
//				}
//			}
//		}
//	}
//	
//	private class NearestQuadrant implements Comparable<NearestQuadrant> {
//
//		private double distance;
//		
//		private int quadrant;
//		
//		public NearestQuadrant(double distance, int quadrant) {
//			this.distance = distance;
//			this.quadrant = quadrant;
//		}
//
//		public int getQuadrant() {
//			return quadrant;
//		}
//
//		public int compareTo(NearestQuadrant o) {
//			if (distance < o.distance) {
//				return -1;
//			} else if (distance > o.distance) {
//				return 1;
//			} else {
//				if (quadrant < o.quadrant) {
//					return -1;
//				} else if (quadrant > o.quadrant) {
//					return 1;
//				} else {
//					return 0;
//				}
//			}
//		}
//		
//	}
//
//	/**
//	 * Used with the nearestCity command. Each NearestCity contains a city and
//	 * the city's distance from a give point. A NearestCity is less than another
//	 * if it's distance is smaller than the other's.
//	 * 
//	 * @author Ben Zoller
//	 * @version 1.0
//	 */
//	private class NearestCity implements Comparable<NearestCity> {
//		/** city */
//		private final City city;
//
//		/** city's distance to a point */
//		private final double distance;
//
//		/**
//		 * Constructs a city and it's distance from a point.
//		 * 
//		 * @param city
//		 *            city
//		 * @param distance
//		 *            distance from a point
//		 */
//		private NearestCity(final City city, final double distance) {
//			this.city = city;
//			this.distance = distance;
//		}
//
//		/**
//		 * Gets the city
//		 * 
//		 * @return city
//		 */
//		private City getCity() {
//			return city;
//		}
//
//		/**
//		 * Compares one city to another based on their distances.
//		 * 
//		 * @param otherNearCity
//		 *            other city
//		 * @return distance comparison results
//		 */
//		public int compareTo(final NearestCity otherNearCity) {
//			if (distance < otherNearCity.distance) {
//				return -1;
//			} else if (distance > otherNearCity.distance) {
//				return 1;
//			} else {
//				return city.getName().compareTo(otherNearCity.city.getName());
//			}
//		}
//
//		/**
//		 * Gets the distance
//		 * 
//		 * @return distance
//		 */
//		public double getDistance() {
//			return distance;
//		}
//	}
}
