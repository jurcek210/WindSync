package si.um.feri.maprri.raster.Windmill;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import si.um.feri.maprri.model.WindmillMarker;

public class WindmillDetailView {

    private final Stage uiStage;
    private final Skin skin;
    private final ShapeRenderer shapeRenderer;
    private final ModelBatch modelBatch;
    private final ModelInstance windmillInstance;
    private final Environment environment;
    private final OrthographicCamera camera;

    private Window sideCard;

    private boolean exiting = false;
    private float exitProgress = 0f;
    private float exitDuration = 0.5f;
    private float exitZoomFrom;

    private final float BUTTON_W;
    private final float BUTTON_H;
    private final float BUTTON_MARGIN;

    public WindmillDetailView(
        Stage uiStage,
        Skin skin,
        ShapeRenderer shapeRenderer,
        OrthographicCamera camera,
        ModelBatch modelBatch,
        ModelInstance windmillInstance,
        Environment environment,
        float buttonW,
        float buttonH,
        float buttonMargin
    ) {
        this.uiStage = uiStage;
        this.skin = skin;
        this.shapeRenderer = shapeRenderer;
        this.camera = camera;
        this.modelBatch = modelBatch;
        this.windmillInstance = windmillInstance;
        this.environment = environment;
        this.BUTTON_W = buttonW;
        this.BUTTON_H = buttonH;
        this.BUTTON_MARGIN = buttonMargin;
    }

    public void showSideCard(WindmillMarker w, java.util.function.BiConsumer<Window, WindmillMarker> fillFn) {
        if (sideCard != null) sideCard.remove();

        Window window = new Window("Windmill info", skin);
        window.setMovable(false);
        window.setResizable(false);

        fillFn.accept(window, w);

        window.pack();
        window.setSize(420, Math.max(520, window.getHeight()));

        float x = Gdx.graphics.getWidth() - window.getWidth() - 200;
        float y = Gdx.graphics.getHeight() / 2f - window.getHeight() / 2f + 50;

        window.setPosition(x, y);
        uiStage.addActor(window);
        window.toFront();

        sideCard = window;
    }

    public void renderDetail() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(windmillInstance, environment);
        modelBatch.end();
    }

    public void animateExit(float zoomStart, float tiltTarget, Runnable onFinish) {
        if (!exiting) {
            exiting = true;
            exitProgress = 0f;
            exitZoomFrom = camera.zoom;
        }

        exitProgress += Gdx.graphics.getDeltaTime();
        float t = Math.min(exitProgress / exitDuration, 1f);
        t = t * t * (3f - 2f * t);

        camera.zoom = MathUtils.lerp(exitZoomFrom, zoomStart, t);
        camera.up.set(0, MathUtils.cosDeg(tiltTarget * (1f - t)), MathUtils.sinDeg(tiltTarget * (1f - t)));

        if (sideCard != null) sideCard.getColor().a = 1f - t;

        if (t >= 1f) {
            exiting = false;
            camera.up.set(0, 1, 0);
            camera.zoom = zoomStart;

            if (sideCard != null) {
                sideCard.remove();
                sideCard = null;
            }

            onFinish.run();
        }
    }

    public void drawToggleButton(boolean active) {
        shapeRenderer.setProjectionMatrix(
            new com.badlogic.gdx.math.Matrix4()
                .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        );

        float x = Gdx.graphics.getWidth() - BUTTON_W - BUTTON_MARGIN;
        float y = Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(active ? Color.SKY : Color.DARK_GRAY);
        shapeRenderer.rect(x, y, BUTTON_W, BUTTON_H);
        shapeRenderer.end();
    }
}
