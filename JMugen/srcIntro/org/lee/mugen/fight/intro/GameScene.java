package org.lee.mugen.fight.intro;

import java.util.List;

import org.lee.mugen.core.Game;
import org.lee.mugen.core.renderer.game.intro.IntroRender;
import org.lee.mugen.renderer.GameWindow;
import org.lee.mugen.renderer.Renderable;

public class GameScene implements Game {

	@Override
	public void addRender(Renderable r) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void displayPendingScreeen() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void free() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Game getNext() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Renderable> getRenderables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(GameWindow container) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDebugAction(DebugAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reInit(GameWindow container) throws Exception {
		// TODO Auto-generated method stub
		
	}
	IntroRender introRender = new IntroRender();
	@Override
	public void render() throws Exception {
		introRender.render();
		
	}

	@Override
	public void renderDebugInfo() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(int delta) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
