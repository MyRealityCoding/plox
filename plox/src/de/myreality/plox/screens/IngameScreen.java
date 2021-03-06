package de.myreality.plox.screens;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenEquations;
import aurelienribon.tweenengine.TweenManager;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;

import de.myreality.plox.GameContext;
import de.myreality.plox.GameObject;
import de.myreality.plox.GameObjectFactory;
import de.myreality.plox.GameObjectListener;
import de.myreality.plox.GameObjectType;
import de.myreality.plox.Planet;
import de.myreality.plox.Player;
import de.myreality.plox.PlayerScore;
import de.myreality.plox.PloxGame;
import de.myreality.plox.PowerUp;
import de.myreality.plox.PowerUpStrategy;
import de.myreality.plox.Resources;
import de.myreality.plox.Scoreable;
import de.myreality.plox.Shot;
import de.myreality.plox.ai.EnemyController;
import de.myreality.plox.ai.RotationStrategy;
import de.myreality.plox.google.AchievementManager;
import de.myreality.plox.google.GoogleInterface;
import de.myreality.plox.graphics.ParticleRenderer;
import de.myreality.plox.input.IngameControls;
import de.myreality.plox.tweens.GameObjectTween;
import de.myreality.plox.tweens.SpriteTween;
import de.myreality.plox.ui.LifeBar;
import de.myreality.plox.ui.PopupManager;
import de.myreality.plox.ui.ScoreLabel;

public class IngameScreen implements Screen, GameContext {

	private Planet planet;

	private IngameControls controls;

	private OrthographicCamera camera;

	private SpriteBatch batch;

	private List<GameObject> objects;

	private GameObjectFactory objectFactory;

	private GameObject player;

	private Sprite background;

	private EnemyController controller;

	private CollisionHandler collisionHandler;

	private PloxGame game;

	private ParticleRenderer particleRenderer;

	private TweenManager tweenManager;

	private Label pointLabel;
	
	private Scoreable playerScore;

	private Sprite gameOver;

	private boolean over;
	
	private PopupManager popupManager;
	
	private AchievementManager achievementManager;

	private boolean fadeActivated;

	public IngameScreen(PloxGame game) {
		this.game = game;
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.141176471f, 0.188235294f, 0.278431373f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));

		
		if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
			gameover();
		}

		if (Gdx.app.getType().equals(ApplicationType.Desktop) && !over) {

			GameObject player = getPlayer();
			final int speed = (int) (580 * delta);

			if (Gdx.input.isKeyPressed(Keys.W)) {
				player.setY(player.getY() - speed);
			} else if (Gdx.input.isKeyPressed(Keys.S)) {
				player.setY(player.getY() + speed);
			}

			if (Gdx.input.isKeyPressed(Keys.A)) {
				player.setX(player.getX() - speed);
			} else if (Gdx.input.isKeyPressed(Keys.D)) {
				player.setX(player.getX() + speed);
			}
		}

		if (over && !fadeActivated) {
			fadeActivated = true;

			for (GameObject o : objects) {
				Tween.to(o, GameObjectTween.ALPHA, 2f).target(0.3f).ease(TweenEquations.easeInOutQuad)
						.start(tweenManager);
			}
			((ScoreLabel)pointLabel).reset();
			pointLabel.setFontScale(4);
			pointLabel.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 3);
			controls.clear();
			controls.addActor(pointLabel);
			Tween.to(planet, GameObjectTween.ALPHA, 2f).target(0.3f).ease(TweenEquations.easeOutQuad).start(tweenManager);

			Tween.to(gameOver, SpriteTween.ALPHA, 0.6f).target(1.0f)
					.ease(TweenEquations.easeInOutQuad).start(tweenManager);
			Tween.to(gameOver, SpriteTween.BOUNCE, 0.6f)
					.target(Gdx.graphics.getHeight() / 3f
							- gameOver.getHeight() / 2f)
					.ease(TweenEquations.easeOutBounce).start(tweenManager);
		}

		tweenManager.update(delta);
		controls.act(delta);
		
		if (!over) {
			controller.update(delta);
		} else {
			pointLabel.setColor(Color.WHITE);
			pointLabel.setPosition(Gdx.graphics.getWidth() / 2 - pointLabel.getPrefWidth() / 2f, Gdx.graphics.getHeight() / 3);
		}

		camera.update();

		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		background.setBounds(0, 0, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		background.draw(batch);

		planet.draw(batch);

		for (GameObject o : objects) {

			if (!fadeActivated) {
				o.update(delta);
			}

			for (GameObject other : objects) {
				if (o.collidesWidth(other) && !fadeActivated) {
					collisionHandler.collide(o, other);
				}
			}

			if (!ScreenUtils.isOutOfScreen(o)) {
				o.draw(batch);
			} else {
				remove(o);
			}

			if (o.getCurrentLife() < 1 && !fadeActivated) {

				remove(o);
				// Spawn powerups here
				for (PowerUpStrategy s : o.getPowerUps()) {
					GameObject powerUp = objectFactory.createPowerUp(o.getCenterX(), o.getCenterY(), s);
					powerUp.addStrategy(new RotationStrategy());
					add(powerUp);
				}
			}
		}

		particleRenderer.render(batch, delta);

		if (fadeActivated) {
			gameOver.draw(batch);
		}

		batch.end();
		controls.draw();

		if (!over && player.isDead() || planet.isDead()) {
			gameover();
		}
	}
	
	@Override
	public PopupManager getPopupManager() {
		return popupManager;
	}

	@Override
	public void resize(int width, int height) {
		if (controls != null) {
			controls.setViewport(width, height);
		} else {
			controls = new IngameControls(width, height, this, game);
			
			LabelStyle labelStyle = new LabelStyle();
			labelStyle.font = Resources.get(Resources.BITMAP_FONT_REGULAR, BitmapFont.class);
			labelStyle.fontColor = new Color(1f, 1f, 1f, 1f);
			pointLabel = new ScoreLabel(playerScore, tweenManager, labelStyle);
			pointLabel.setPosition(80, Gdx.graphics.getHeight() - pointLabel.getHeight() - 80);
			
			controls.addActor(pointLabel);
			
			LifeBar bar = new LifeBar(player, tweenManager);
			bar.setBounds(Gdx.graphics.getWidth() / 3, Gdx.graphics.getHeight() - Gdx.graphics.getHeight() / 6f, Gdx.graphics.getWidth() / 3, Gdx.graphics.getHeight() / 8);
			controls.addActor(bar);
			pointLabel.setFontScale(2f);
			Gdx.input.setInputProcessor(controls);
			Gdx.input.setCatchBackKey(true);
			LabelStyle style = new LabelStyle();
			style.font = Resources.get(Resources.BITMAP_FONT_REGULAR, BitmapFont.class);
			style.fontColor = Color.valueOf("ffffff");
			popupManager = new PopupManager(controls, tweenManager, style);
		}

		camera.setToOrtho(true, width, height);

	}
	
	public boolean isOver() {
		return over;
	}
	
	public AchievementManager getAchievementManager() {
		return achievementManager;
	}

	@Override
	public void show() {

		tweenManager = new TweenManager();
		objects = new CopyOnWriteArrayList<GameObject>();
		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		controller = new EnemyController(this, tweenManager);
		background = new Sprite(Resources.get(Resources.BACKGROUND_INGAME, Texture.class));
		objectFactory = new GameObjectFactory();
		collisionHandler = new CollisionHandler(this);
		particleRenderer = new ParticleRenderer();
		playerScore = new PlayerScore();
		achievementManager = new AchievementManager(game.getGoogle());
		playerScore.addListener(achievementManager);
		float centerX = Gdx.graphics.getWidth() / 2f;
		float centerY = Gdx.graphics.getHeight() / 2f;
		gameOver = new Sprite(Resources.get(Resources.GAMEOVER, Texture.class));
		gameOver.flip(false, true);
		float scaleFactor = Gdx.graphics.getWidth() / 800f;
		gameOver.setBounds(0, 0, gameOver.getWidth() * scaleFactor,
				gameOver.getHeight() * scaleFactor);
		gameOver.setPosition(Gdx.graphics.getWidth() / 2 - gameOver.getWidth()
				/ 2, 0);
		gameOver.setColor(gameOver.getColor().r, gameOver.getColor().g,
				gameOver.getColor().b, 0f);

		player = objectFactory.createPlayer(0, 0);
		player.setX(centerX - player.getWidth() / 2f);
		player.setY(player.getHeight() / 2f);
		player.addListener(particleRenderer);
		objects.add(player);
		
		for (GameObjectListener l : player.getListeners()) {
			l.onAdd(player);
		}

		planet = objectFactory.createPlanet(Math.round(centerX),
				Math.round(centerY), tweenManager);
		
	}

	@Override
	public Player getPlayer() {
		return (Player) player;
	}
	
	@Override
	public Scoreable getPlayerScore() {
		return playerScore;
	}

	@Override
	public Planet getPlanet() {
		return planet;
	}

	public GameObjectFactory getFactory() {
		return objectFactory;
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		controls.dispose();
	}

	@Override
	public void add(GameObject object) {
		objects.add(object);
		object.addListener(particleRenderer);
		for (GameObjectListener l : object.getListeners()) {
			l.onAdd(object);
		}
	}

	@Override
	public void remove(GameObject object) {
		objects.remove(object);
		for (GameObjectListener l : object.getListeners()) {
			l.onRemove(object);
		}
	}
	
	@Override
	public ParticleRenderer getParticleRenderer() {
		return particleRenderer;
	}

	public void gameover() {
		if (!over) {
			GoogleInterface google = game.getGoogle();
			google.submitScore(playerScore.getScore());
			over = true;
			particleRenderer.clear();
		}
	}

	private class CollisionHandler {
		
		private GameContext context;
		
		public CollisionHandler(GameContext context) {
			this.context = context;
		}

		public void collide(GameObject a, GameObject b) {

			if (a.getType().equals(GameObjectType.SHOT)
					&& !b.getType().equals(GameObjectType.SHOT)
					&& !b.getType().equals(GameObjectType.POWERUP)) {

				if (!b.getType().equals(GameObjectType.PLAYER)) {
					
					Shot shot = (Shot)a;					
					b.damage(shot.getDamage(), shot);
					b.setIced(1.5f);
					
					Sound sound = Resources.get(Resources.SOUND_IMPACT, Sound.class);
					sound.play(0.4f, (float)(1.0f + Math.random() * 0.4), (float)(1.0f + Math.random() * 0.4));
					
					if (b.isDead()) {		
						sound = Resources.get(Resources.SOUND_EXPLODE, Sound.class);
						sound.play(1f, (float)(0.3f + Math.random() * 0.6), (float)(1.0f + Math.random() * 0.4));
						playerScore.addScore(50);
						popupManager.popup(b.getCenterX(), b.getCenterY(), "50");						
					} else {
						playerScore.addScore(10);
						popupManager.popup(b.getCenterX(), b.getCenterY(), "10");
					}
					remove(a);
				}
			} else if (a.getType().equals(GameObjectType.POWERUP)
					&& b.getType().equals(GameObjectType.PLAYER)) {
				PowerUp powerUp = (PowerUp)a;
				powerUp.onCollect(context);
				Sound sound = Resources.get(Resources.SOUND_POWERUP, Sound.class);
				sound.play(1f, (float)(0.3f + Math.random() * 0.6), (float)(0.5f + Math.random() * 0.4));
				
				if (powerUp.isUseable()) {
					b.addPowerUp(powerUp.getStrategy());
				}
				
				remove(powerUp);
			}

			if (a.getType().equals(GameObjectType.ALIEN)
					&& b.getType().equals(GameObjectType.PLAYER)) {

				
				if (!b.isIndestructable()) {
					
					b.damage(100, a);
					a.kill();
					
					tweenManager.killTarget(b);
					float padding = 5;
					Tween.to(b, GameObjectTween.SHAKE_X, 0.02f)
			        .target(b.getX() + padding)
			        .ease(TweenEquations.easeInOutQuad)
			        .repeatYoyo(20, 0).start(tweenManager);
					
					Tween.to(b, GameObjectTween.SHAKE_Y, 0.01f)
			        .target(b.getY() + padding)
			        .ease(TweenEquations.easeInOutQuad)
			        .repeatYoyo(10, 0).start(tweenManager);
				} else {
					a.damage((int) (b.getMaxLife() / 2f), b);
					playerScore.addScore(10);
					popupManager.popup(b.getCenterX(), b.getCenterY(), "10");
				}
				
				if (a.isDead()) {
					Sound sound = Resources.get(Resources.SOUND_EXPLODE, Sound.class);
					sound.play(1f, (float)(0.3f + Math.random() * 0.6), (float)(1.0f + Math.random() * 0.4));
				} else {
					Sound sound = Resources.get(Resources.SOUND_IMPACT, Sound.class);
					sound.play(1f, (float)(0.3f + Math.random() * 0.6), (float)(1.0f + Math.random() * 0.4));
				}
			}
		}
	}

}
