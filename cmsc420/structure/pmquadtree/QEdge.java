package cmsc420.structure.pmquadtree;

import java.awt.geom.Line2D;

import cmsc420.structure.City;

public class QEdge extends Line2D.Float {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private City start;
	private City end;

	public QEdge(City city1, City city2){
		super(city1.pt, city2.pt);
		start = city1;
		end = city2;
	}
	
	public String getStartName(){
		return start.getName();
	}
	
	public String getEndName(){
		return end.getName();
	}
}
