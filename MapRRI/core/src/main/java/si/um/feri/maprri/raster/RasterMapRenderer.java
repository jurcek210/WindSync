package si.um.feri.maprri.raster;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class RasterMapRenderer {

    private final Texture mapTexture;
    private final SpriteBatch batch;

    public RasterMapRenderer() {
        batch = new SpriteBatch();
        mapTexture = new Texture("slovenia_static.png");
    }

    public void render(OrthographicCamera camera) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        batch.draw(
            mapTexture,
            -mapTexture.getWidth() / 2f,
            -mapTexture.getHeight() / 2f
        );

        batch.end();
    }

    public void dispose() {
        mapTexture.dispose();
        batch.dispose();
    }
}
