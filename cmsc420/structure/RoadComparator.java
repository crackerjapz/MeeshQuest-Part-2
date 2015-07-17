package cmsc420.structure ;


import java.util.Comparator;

import cmsc420.structure.pmquadtree.QEdge;


public class RoadComparator implements Comparator<QEdge>{


	@Override
	public int compare(final QEdge c1, final QEdge c2) {
		if (c1.getStartName().equals(c2.getStartName())){
			return c2.getEndName().compareTo(c1.getEndName());
		}
		return c2.getStartName().compareTo(c1.getStartName());
	}

}