package si.um.feri.maprri.vector;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.Net;
import si.um.feri.maprri.api.WindmillApi;

public class VectorMap extends ApplicationAdapter {

    private VectorMapRenderer map;
    OrthographicCamera camera;
    ShapeRenderer shapeRenderer;

    float zoomSpeed = 0.02f;

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        map = new VectorMapRenderer(camera, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.update();

        WindmillApi api = new WindmillApi("http://localhost:3001/api");
        api.fetchAll(new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                System.out.println("API RESPONSE:");
                System.out.println(httpResponse.getResultAsString());
            }

            @Override
            public void failed(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void cancelled() {
            }
        });
    }

    @Override
    public void render() {
        handleInput();

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        map.shapeRenderer.setProjectionMatrix(camera.combined);
        map.render();
    }

    private void handleInput() {
        float moveSpeed = 5 / map.scale;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  map.offsetX -= moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) map.offsetX += moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    map.offsetY += moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  map.offsetY -= moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.A))     map.scale *= (1 + zoomSpeed);
        if (Gdx.input.isKeyPressed(Input.Keys.S))     map.scale *= (1 - zoomSpeed);
    }

    @Override
    public void resize(int width, int height) {
        map.viewport.update(width, height);
    }

    @Override
    public void dispose() {
        map.dispose();
        shapeRenderer.dispose();
    }
}
