package org.lee.mugen.fight.section;

import org.lee.mugen.fight.section.elem.PlayerFace;
import org.lee.mugen.fight.section.elem.Type;

public class Face {
	PlayerFace p1 = new PlayerFace();
	PlayerFace p2 = new PlayerFace();
	public PlayerFace getP1() {
		return p1;
	}
	public void setP1(PlayerFace p1) {
		this.p1 = p1;
	}
	public PlayerFace getP2() {
		return p2;
	}
	public void setP2(PlayerFace p2) {
		this.p2 = p2;
	}
	
	public void parse(String name, String value) {
		if (name.startsWith("p1.")) {
			p1.parse(Type.getNext(name), value);
		} else if (name.startsWith("p2.")) {
			p2.parse(Type.getNext(name), value);
		}
	}
}
