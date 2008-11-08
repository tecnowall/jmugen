package org.lee.mugen.fight.section.elem;

import java.awt.Point;

import org.lee.mugen.util.BeanTools;

public class Bar extends SimpleElement {
	public static class Range {
		Point x = new Point();

		public Point getX() {
			return x;
		}

		public void setX(Point x) {
			this.x = x;
		}
		
	}
	
	Range range = new Range();

	public Range getRange() {
		return range;
	}

	public void setRange(Range range) {
		this.range = range;
	}
	
	@Override
	public void parse(Object root, String name, String value) {
		super.parse(root, name, value);
		if (name.equalsIgnoreCase("range.x")) {
			Point rangeX = (Point) BeanTools.getConvertersMap().get(Point.class).convert(value);
			getRange().setX(rangeX);
		}
		
	}


}
