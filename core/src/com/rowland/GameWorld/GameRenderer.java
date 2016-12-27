package com.rowland.GameWorld;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.magnetideas.loaders.ShaderLoader;
import com.magnetideas.parallax.ParallaxBackground;
import com.rowland.GameObjects.Yoyo;
import com.rowland.Screens.GameScreen;

public class GameRenderer {

    private OrthogonalTiledMapRenderer orthogonalTiledMapRenderer;
    private ShaderProgram blurShader;
    private SpriteBatch batch;
    private GameWorld world;

    public GameRenderer(GameWorld world) {
        this.world = world;
        orthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(world.map, 1 / 32f);
        batch = (SpriteBatch) orthogonalTiledMapRenderer.getBatch();
    }

    public void render(int[] array) {
        if (GameScreen.state == GameScreen.State.GAME_RUNNING)
            orthogonalTiledMapRenderer.render(array);
    }

    public void renderPlayer(float deltaTime) {
        if (GameScreen.state == GameScreen.State.GAME_RUNNING || GameScreen.state == GameScreen.State.GAME_LEVEL_END) {

            // Based on the Player state, get the animation frame
            if (world.getYoyo().visible) {
                TextureRegion frame = null;

                switch (world.getYoyo().getState()) {
                    case 0:
                        frame = GameScreen.pummaStill.getKeyFrame(world.getYoyo().stateTime);
                        break;

                    case 1:
                        frame = GameScreen.pummaWalk.getKeyFrame(world.getYoyo().stateTime);
                        break;

                    case 2:
                        frame = GameScreen.pummaJump.getKeyFrame(world.getYoyo().stateTime);
                        break;
                }

                // On the x-axis, draw the Player facing either right or left
                batch.begin();
                if (world.getYoyo().facesRight) {
                    batch.draw(frame, world.getYoyo().position.x, world.getYoyo().position.y, Yoyo.width, Yoyo.height);
                } else {
                    batch.draw(frame, world.getYoyo().position.x + Yoyo.width, world.getYoyo().position.y, -Yoyo.width, Yoyo.height);
                }
                batch.end();
            }
        }
    }

    public void renderBackground(ParallaxBackground parallaxBackground, OrthographicCamera camera) {
        batch.begin();
        parallaxBackground.draw(camera, batch);
        batch.end();
    }

    public void renderForeground(ParallaxBackground parallaxForeground, OrthographicCamera camera) {
        batch.begin();
        parallaxForeground.draw(camera, batch);
        batch.end();
    }

   /* public void renderPauseBackground(GameScreen gameScreen, OrthographicCamera camera) {


        TextureRegion  pauseTextureRegion = gameScreen.getScreenTexture();
        // gameScreen.setBackgroundTexture(pauseTextureRegion);
        //Texture pauseTexture = new Texture(Gdx.files.internal("data/yoyo_sprite.png"));
        //pauseTexture.flip(false, true);

   *//*     if (blurShader == null)
            blurShader = ShaderLoader.fromFile("VignetteVertexShader", "VignetteFragmentShader");
*//*

        blurShader = new ShaderProgram(VERT, FRAG);
        if (!blurShader.isCompiled()) {
            System.err.println(blurShader.getLog());
            System.exit(0);
        }
        if (blurShader.getLog().length() != 0)
            System.out.println(blurShader.getLog());

        batch.setShader(blurShader);

        //camera.setToOrtho(false, 32f, 18f);
        //batch.setProjectionMatrix(camera.combined);

        //bind the shader, then set the uniform, then unbind the shader
        blurShader.begin();
        blurShader.setUniformf("resolution", Gdx.graphics.getWidth() / 32, Gdx.graphics.getHeight() / 32);
        blurShader.end();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        gameScreen.getStage().getBatch().begin();
        gameScreen.getStage().getBatch().draw(pauseTextureRegion, 0, 0, Gdx.graphics.getWidth() / 32, Gdx.graphics.getHeight() / 32);
        gameScreen.getStage().getBatch().end();


        batch.setShader(SpriteBatch.createDefaultShader());
    }*/

    public void renderPauseBackground(GameScreen gameScreen, OrthographicCamera camera) {
        final float MAX_BLUR = 2f;
        final int FBO_SIZE_WIDTH = Gdx.graphics.getWidth();
        final int FBO_SIZE_HEIGHT = Gdx.graphics.getHeight();


        TextureRegion pauseTexture = gameScreen.getScreenTexture();
        pauseTexture.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        ShaderProgram.pedantic = false;

        if (blurShader == null)
            blurShader = ShaderLoader.fromFile("UniBlurVertexShader", "UniBlurFragmentShader");

        // setup uniforms for our shader
        blurShader.begin();
        blurShader.setUniformf("dir", 1f, 0f);
        blurShader.setUniformf("resolution", FBO_SIZE_WIDTH);
        blurShader.setUniformf("radius", MAX_BLUR);
        blurShader.end();

        FrameBuffer blurTargetA = new FrameBuffer(Pixmap.Format.RGBA8888, FBO_SIZE_WIDTH, FBO_SIZE_HEIGHT, false);
        FrameBuffer blurTargetB = new FrameBuffer(Pixmap.Format.RGBA8888, FBO_SIZE_WIDTH, FBO_SIZE_HEIGHT, false);
        TextureRegion fboRegion = new TextureRegion(blurTargetA.getColorBufferTexture());
        fboRegion.flip(false, true);

        SpriteBatch batch = new SpriteBatch();

        blurTargetA.begin();
        // before rendering, ensure we are using the default shader
        batch.setShader(SpriteBatch.createDefaultShader());
        // now we can start drawing...
        batch.begin();
        // draw our scene here
        batch.draw(pauseTexture, 0, 0);
        // finish rendering to the offscreen buffer
        batch.flush();
        // finish rendering to the offscreen buffer
        blurTargetA.end();
        // now let's start blurring the offscreen image
        batch.setShader(blurShader);


        blurTargetB.begin();
        // we want to render FBO target A into target B
        fboRegion.setTexture(blurTargetA.getColorBufferTexture());
        // draw the scene to target B with a horizontal blur effect
        batch.draw(fboRegion, 0, 0);
        // flush the batch before ending the FBO
        batch.flush();
        // finish rendering target B
        blurTargetB.end();


        fboRegion.setTexture(blurTargetB.getColorBufferTexture());
        gameScreen.setBackgroundTexture(fboRegion);
        // reset to default shader without blurs
        batch.setShader(SpriteBatch.createDefaultShader());
        // finally, end the batch since we have reached the end of the frame
        batch.end();
    }

    public OrthogonalTiledMapRenderer getOrthogonalTiledMapRenderer() {
        return orthogonalTiledMapRenderer;
    }

}
