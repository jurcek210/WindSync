package si.um.feri.maprri.raster.Windmill;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.List;
import java.util.Locale;

import si.um.feri.maprri.api.WindmillApi;
import si.um.feri.maprri.api.openweather.OpenWeatherApi;
import si.um.feri.maprri.model.WindmillMarker;

public class WindmillDetailView {

    private final Stage uiStage;
    private final Skin skin;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final ModelBatch modelBatch;
    private final ModelInstance windmillInstance;
    private final Environment environment;

    private final WindmillApi windmillApi;
    private final OpenWeatherApi openWeatherApi;
    private final List<WindmillMarker> windmills;

    private final float BUTTON_W;
    private final float BUTTON_H;
    private final float BUTTON_MARGIN;

    private Window sideCard;
    private Window createWindmillWindow;

    private boolean exiting = false;
    private float exitProgress = 0f;
    private final float exitDuration = 0.5f;
    private float exitZoomFrom;

    private final ObjectMap<WindmillMarker, InfoRefs> infoRefsByWindmill = new ObjectMap<>();

    private static class InfoRefs {
        Label working;
        Label windSpeed;
        Label kwh1h;
        Label kwh24h;
        Label nameLabel;
        TextField nameField;
        TextButton editBtn;
        TextButton deleteBtn;
        boolean editing = false;
        String originalName;
    }

    public WindmillDetailView(
        Stage uiStage,
        Skin skin,
        ShapeRenderer shapeRenderer,
        OrthographicCamera camera,
        ModelBatch modelBatch,
        ModelInstance windmillInstance,
        Environment environment,
        WindmillApi windmillApi,
        OpenWeatherApi openWeatherApi,
        List<WindmillMarker> windmills,
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
        this.windmillApi = windmillApi;
        this.openWeatherApi = openWeatherApi;
        this.windmills = windmills;
        this.BUTTON_W = buttonW;
        this.BUTTON_H = buttonH;
        this.BUTTON_MARGIN = buttonMargin;
    }

    public void showSideCard(WindmillMarker w) {
        if (sideCard != null) sideCard.remove();

        Window window = new Window("Windmill info", skin);
        window.setMovable(false);
        window.setResizable(false);

        fillWindmillWindow(window, w);

        window.pack();
        window.setSize(420, Math.max(520, window.getHeight()));

        float x = Gdx.graphics.getWidth() - window.getWidth() - 200;
        float y = Gdx.graphics.getHeight() / 2f - window.getHeight() / 2f + 50;

        window.setPosition(x, y);
        uiStage.addActor(window);
        window.toFront();

        sideCard = window;
    }

    public void hideSideCard() {
        if (sideCard != null) {
            sideCard.remove();
            sideCard = null;
        }
    }

    public boolean isAnyEditActive() {
        for (InfoRefs refs : infoRefsByWindmill.values()) {
            if (refs.editing) return true;
        }
        return false;
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
            hideSideCard();
            onFinish.run();
        }
    }

    public void renderDetail3D() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(windmillInstance, environment);
        modelBatch.end();
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

    public void openCreateWindmillWindow(
        double lat,
        double lon,
        int screenX,
        int screenY,
        Runnable onCreated
    ) {
        if (createWindmillWindow != null) {
            createWindmillWindow.remove();
            createWindmillWindow = null;
        }

        Window w = new Window("New windmill", skin);
        createWindmillWindow = w;
        w.setMovable(true);
        w.setResizable(false);
        w.setKeepWithinStage(true);

        TextButton closeBtn = new TextButton("X", skin);
        closeBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                w.remove();
                if (createWindmillWindow == w) createWindmillWindow = null;
                return true;
            }
        });
        w.getTitleTable().add(closeBtn).padLeft(8f);

        w.add("Name: ").left();
        TextField nameField = new TextField("", skin);
        w.add(nameField).width(220).left().row();

        final boolean[] active = {true};
        w.add("Active: ").left();
        Label activeLabel = new Label("Yes", skin);
        activeLabel.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                active[0] = !active[0];
                activeLabel.setText(active[0] ? "Yes" : "No");
                return true;
            }
        });
        w.add(activeLabel).left().row();

        TextButton confirm = new TextButton("CONFIRM", skin);
        TextButton cancel = new TextButton("CANCEL", skin);

        cancel.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                w.remove();
                if (createWindmillWindow == w) createWindmillWindow = null;
                return true;
            }
        });

        confirm.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                String name = nameField.getText().trim();
                if (name.isEmpty()) return true;

                openWeatherApi.fetchWindSpeed(lat, lon, windSpeed -> {
                    WindmillMarker marker = new WindmillMarker(null, lon, lat, active[0], windSpeed, name);
                    windmillApi.createWindmill(marker, created -> {
                        windmills.add(created);
                        w.remove();
                        if (createWindmillWindow == w) createWindmillWindow = null;
                        onCreated.run();
                    });
                }, err -> {
                    err.printStackTrace();
                });

                return true;
            }
        });

        w.row();
        w.add(confirm).padTop(10).left();
        w.add(cancel).padTop(10).right();

        w.pack();
        Vector2 stagePos = uiStage.screenToStageCoordinates(new Vector2(screenX, screenY));
        w.setPosition(
            Math.max(0, Math.min(stagePos.x, uiStage.getWidth() - w.getWidth())),
            Math.max(0, Math.min(stagePos.y - w.getHeight(), uiStage.getHeight() - w.getHeight()))
        );

        uiStage.addActor(w);
        w.toFront();
        uiStage.setKeyboardFocus(nameField);
    }

    public void deleteWindmillLocalAndServer(
        WindmillMarker w,
        Runnable onDeleted
    ) {
        infoRefsByWindmill.remove(w);
        windmills.remove(w);

        if (sideCard != null) {
            sideCard.remove();
            sideCard = null;
        }

        if (w.id != null) {
            windmillApi.deleteWindmill(w.id, onDeleted);
        } else {
            onDeleted.run();
        }
    }

    private void confirmEdit(
        WindmillMarker w,
        Runnable onChanged
    ) {
        InfoRefs refs = infoRefsByWindmill.get(w);
        if (refs == null) return;

        String newName = refs.nameField.getText().trim();
        if (newName.isEmpty()) newName = w.name;

        boolean changed = !newName.equals(refs.originalName);

        w.name = newName;
        refs.nameLabel.setText(newName);

        if (!changed || w.id == null) {
            setEditMode(w, false);
            updateInfoWindowFor(w);
            onChanged.run();
            return;
        }

        String oldId = w.id;
        windmillApi.deleteWindmill(oldId, () ->
            windmillApi.createWindmill(w, created -> {
                w.id = created.id;
                setEditMode(w, false);
                updateInfoWindowFor(w);
                onChanged.run();
            })
        );
    }

    private void setEditMode(WindmillMarker w, boolean editing) {
        InfoRefs refs = infoRefsByWindmill.get(w);
        if (refs == null) return;

        if (editing && !refs.editing) {
            refs.originalName = w.name;
        }

        refs.editing = editing;

        refs.nameLabel.setVisible(!editing);
        refs.nameField.setVisible(editing);

        if (editing) {
            refs.nameField.setText(w.name);
            uiStage.setKeyboardFocus(refs.nameField);
            refs.nameField.selectAll();
        } else {
            uiStage.setKeyboardFocus(null);
        }

        refs.editBtn.setText(editing ? "CONFIRM" : "EDIT");

        if (sideCard != null) sideCard.pack();
    }

    private void fillWindmillWindow(Window window, WindmillMarker w) {
        window.clear();
        window.defaults().pad(16).left();

        InfoRefs refs = new InfoRefs();
        infoRefsByWindmill.put(w, refs);

        refs.nameLabel = new Label(w.name, skin);
        refs.nameLabel.setFontScale(1.8f);

        refs.nameField = new TextField(w.name, skin);
        refs.nameField.setVisible(false);

        window.add(refs.nameLabel).colspan(2).padBottom(12).row();
        window.add(refs.nameField).colspan(2).width(300).padBottom(12).row();

        window.add("Latitude");
        window.add(String.valueOf(w.lat)).row();

        window.add("Longitude");
        window.add(String.valueOf(w.lon)).row();

        window.add("Working");
        refs.working = new Label(w.working ? "Yes" : "No", skin);
        window.add(refs.working).row();

        window.add("Wind speed");
        refs.windSpeed = new Label("", skin);
        window.add(refs.windSpeed).row();

        float k1 = estimateEnergyKWhPerHour(w.windSpeed, w.working);
        float k24 = k1 * 24f;

        window.add("Energy (1h)");
        refs.kwh1h = new Label("", skin);
        window.add(refs.kwh1h).row();

        window.add("Energy (24h)");
        refs.kwh24h = new Label("", skin);
        window.add(refs.kwh24h).row();

        refs.editBtn = new TextButton("EDIT", skin);
        refs.deleteBtn = new TextButton("DELETE", skin);

        refs.editBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                if (!refs.editing) setEditMode(w, true);
                else confirmEdit(w, () -> {});
                return true;
            }
        });

        refs.deleteBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                deleteWindmillLocalAndServer(w, () -> {});
                return true;
            }
        });

        window.add().expandY().row();
        window.add(refs.editBtn).expandX().left().padTop(24);
        window.add(refs.deleteBtn).expandX().right().padTop(24);

        updateInfoWindowFor(w);
    }

    public void updateInfoWindowFor(WindmillMarker w) {
        InfoRefs refs = infoRefsByWindmill.get(w);
        if (refs == null) return;

        refs.working.setText(w.working ? "Yes" : "No");
        refs.windSpeed.setText(String.format(Locale.US, "%.2f m/s", w.windSpeed));

        float kWhPerHour = estimateEnergyKWhPerHour(w.windSpeed, w.working);
        float kWhPerDay = kWhPerHour * 24f;

        refs.kwh1h.setText(String.format(Locale.US, "%.1f kWh", kWhPerHour));
        refs.kwh24h.setText(String.format(Locale.US, "%.1f kWh", kWhPerDay));

        if (sideCard != null) sideCard.pack();
    }

    private float estimateEnergyKWhPerHour(float windSpeed, boolean working) {
        if (!working) return 0f;
        float v = Math.max(0f, windSpeed);
        float rho = 1.225f;
        float D = 80f;
        float A = (float) (Math.PI * Math.pow(D / 2f, 2));
        float Cp = 0.40f;
        float powerW = 0.5f * rho * A * Cp * v * v * v;
        return powerW / 1000f;
    }
}
