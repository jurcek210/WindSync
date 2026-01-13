package si.um.feri.maprri.raster.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import si.um.feri.maprri.model.WindmillMarker;

import java.util.List;

public class WindmillRenderer {

    private final SpriteBatch batch;
    private final Animation<TextureRegion> animation;

    private static final float WINDMILL_SIZE = 128f;

    public WindmillRenderer() {
        batch = new SpriteBatch();

        Texture sheet = new Texture("windmill/eolico_sheet.png");
        TextureRegion[][] regions = TextureRegion.split(sheet, 256, 256);

        TextureRegion[] frames = new TextureRegion[12];
        int idx = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                frames[idx++] = regions[y][x];
            }
        }

        animation = new Animation<>(0.08f, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void render(
        List<WindmillMarker> windmills,
        OrthographicCamera camera,
        List<float[]> positions
    ) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float delta = Gdx.graphics.getDeltaTime();

        for (int i = 0; i < windmills.size(); i++) {
            WindmillMarker w = windmills.get(i);

            float x = positions.get(i)[0];
            float y = positions.get(i)[1];

            if (w.working) {
                w.animationTime += delta * w.windSpeed;
                batch.setColor(Color.WHITE);
            } else {
                batch.setColor(1f, 0f, 0f, 1f);
            }

            TextureRegion frame = animation.getKeyFrame(w.animationTime);

            batch.draw(
                frame,
                x - WINDMILL_SIZE / 2f,
                y,
                WINDMILL_SIZE,
                WINDMILL_SIZE
            );
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void dispose() {
        batch.dispose();
    }
}
