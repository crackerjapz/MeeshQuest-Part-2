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
	
	public boolean equals(final Object obj){
		if (obj == this)
			return true;
		if (obj != null && (obj.getClass().equals(this.getClass()))) {
			QEdge c = (QEdge) obj;
			return (start.getName().equals(c.start.getName()) && 
					end.getName().equals(c.end.getName()));
		}
		return false;
	}
	
	public int hashCode() {
		int hash = 11;
		hash = 37 * hash + start.hashCode();
		hash = 37 * hash + end.hashCode();
		return hash;
	}
}
