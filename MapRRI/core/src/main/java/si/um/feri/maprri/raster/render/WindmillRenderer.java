package si.um.feri.maprri.raster.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import si.um.feri.maprri.model.WindmillMarker;

import java.util.List;

public class WindmillRenderer {

    private final SpriteBatch batch;

    private final Animation<TextureRegion> windmillAnim;
    private final Animation<TextureRegion> fireAnim;

    private float fireTime = 0f;

    private static final float WINDMILL_SIZE = 128f;
    private static final float FIRE_SIZE = 48f;

    public WindmillRenderer() {

        batch = new SpriteBatch();


        Texture windmillSheet = new Texture("windmill/eolico_sheet.png");

        TextureRegion[][] windmillRegions =
            TextureRegion.split(windmillSheet, 256, 256);

        TextureRegion[] windmillFrames = new TextureRegion[12];

        int idx = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                windmillFrames[idx++] = windmillRegions[y][x];
            }
        }

        windmillAnim = new Animation<>(0.08f, windmillFrames);
        windmillAnim.setPlayMode(Animation.PlayMode.LOOP);

        Texture fireSheet = new Texture("fire/fire.png");

        fireSheet.setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );

        int cols = 7;
        int rows = 1;

        int frameW = fireSheet.getWidth() / cols;
        int frameH = fireSheet.getHeight();

        TextureRegion[][] fireRegions =
            TextureRegion.split(fireSheet, frameW, frameH);

        TextureRegion[] fireFrames = new TextureRegion[cols];

        idx = 0;
        for (int x = 0; x < cols; x++) {
            fireFrames[idx++] = fireRegions[0][x];
        }

        fireAnim = new Animation<>(0.12f, fireFrames);
        fireAnim.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void render(
        List<WindmillMarker> windmills,
        OrthographicCamera camera,
        List<float[]> positions
    ) {

        batch.setProjectionMatrix(camera.combined);

        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.begin();

        float delta = Gdx.graphics.getDeltaTime();
        fireTime += delta;

        for (int i = 0; i < windmills.size(); i++) {

            WindmillMarker w = windmills.get(i);

            float x = positions.get(i)[0];
            float y = positions.get(i)[1];

            if (w.state == WindmillMarker.State.BURNING) {

                w.burnTime += delta;


                if (w.burnTime < 1f) {
                    w.animationTime += delta * 20f;
                }

                else if (w.burnTime < 3f) {

                    float t = (w.burnTime - 1f) / 2f;
                    float speed = 20f * (1f - t);

                    w.animationTime += delta * speed;
                }

                else {
                    w.destroyed = true;
                }

            }
            else if (w.working && !w.destroyed) {
                w.animationTime += delta * w.windSpeed;
            }

            TextureRegion frame =
                windmillAnim.getKeyFrame(w.animationTime);

            batch.setColor(Color.WHITE);

            batch.draw(
                frame,
                x - WINDMILL_SIZE / 2f,
                y,
                WINDMILL_SIZE,
                WINDMILL_SIZE
            );


            if (w.state == WindmillMarker.State.BURNING) {

                TextureRegion fireFrame =
                    fireAnim.getKeyFrame(fireTime);

                float fireX = x - FIRE_SIZE / 2f;
                float fireY = y + WINDMILL_SIZE * 0.7f;

                batch.draw(
                    fireFrame,
                    fireX - 15,
                    fireY - 60,
                    FIRE_SIZE * 0.8f,
                    FIRE_SIZE * 0.8f
                );

                batch.draw(
                    fireFrame,
                    fireX - 10,
                    fireY - 17,
                    FIRE_SIZE * 0.5f,
                    FIRE_SIZE * 0.5f
                );
            }
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void dispose() {
        batch.dispose();
    }
}
