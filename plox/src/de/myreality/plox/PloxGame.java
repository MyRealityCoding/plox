package de.myreality.plox;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;

import de.myreality.plox.google.GoogleInterface;
import de.myreality.plox.screens.MenuScreen;
import de.myreality.plox.tweens.ActorTween;
import de.myreality.plox.tweens.GameObjectTween;
import de.myreality.plox.tweens.SpriteTween;

public class PloxGame extends Game {
	
	private GoogleInterface google;
	
	public PloxGame(GoogleInterface google) {
		this.google = google;
		google.login();
	}
	
	public GoogleInterface getGoogle() {
		return google;
	}

	@Override
	public void create() {
		Resources.load();
		
		Tween.registerAccessor(Sprite.class, new SpriteTween());
		Tween.registerAccessor(Actor.class, new ActorTween());
		Tween.registerAccessor(GameObject.class, new GameObjectTween());

		// Start the music
		Music music = Resources.get(Resources.MUSIC_THEME, Music.class);
		music.setLooping(true);
		music.setVolume(0.2f);
		music.play();
		
		setScreen(new MenuScreen(this));
	}

	@Override
	public void dispose() {
		super.dispose();
		Resources.dispose();
		google.logout();
	}

	@Override
	public void resume() {
		super.resume();
		Resources.load();
	}
	
	
	
}
