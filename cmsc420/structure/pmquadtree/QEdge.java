package cmsc420.structure.pmquadtree;

import java.awt.geom.Line2D;

import cmsc420.structure.City;

public class QEdge extends Line2D.Float {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private City endpoint1;
	private City endpoint2;

	public QEdge(City city1, City city2){
		super(city1.pt, city2.pt);
		endpoint1 = city1;
		endpoint2 = city2;
	}
}
