package si.um.feri.maprri.vector;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;

import si.um.feri.maprri.api.WindmillApi;
import si.um.feri.maprri.raster.RasterMapRenderer;

public class VectorMap extends ApplicationAdapter {

    private VectorMapRenderer map;
    private OrthographicCamera camera;
    private RasterMapRenderer raster;

    float zoomSpeed = 0.02f;

    @Override
    public void create() {
        camera = new OrthographicCamera(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        camera.position.set(0, 0, 0);
        camera.update();

        map = new VectorMapRenderer(
            camera,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        raster = new RasterMapRenderer();

        WindmillApi api = new WindmillApi("http://localhost:3001/api");
        api.fetchAll(windmills -> {
            map.setWindmills(windmills);
            System.out.println("Windmills loaded: " + windmills.size());
        });
    }

    @Override
    public void render() {
        handleInput();

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        raster.render(camera);
        map.render();
    }

    private void handleInput() {
        float speed = 10f * camera.zoom;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  camera.position.x -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camera.position.x += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    camera.position.y += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  camera.position.y -= speed;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.zoom *= 1.02f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.zoom /= 1.02f;

        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        map.viewport.update(width, height);
    }

    @Override
    public void dispose() {
        map.dispose();
        raster.dispose();
    }
}
