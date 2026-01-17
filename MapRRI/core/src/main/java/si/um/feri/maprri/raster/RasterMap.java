package si.um.feri.maprri.raster;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.Environment;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import si.um.feri.maprri.api.WindmillApi;
import si.um.feri.maprri.api.openweather.OpenWeatherApi;
import si.um.feri.maprri.model.WindmillMarker;
import si.um.feri.maprri.raster.interaction.WindmillClickHandler;
import si.um.feri.maprri.raster.render.WindmillRenderer;
import si.um.feri.maprri.raster.simulation.WindSimulationController;
import si.um.feri.maprri.raster.utils.Constants;
import si.um.feri.maprri.raster.utils.Geolocation;
import si.um.feri.maprri.raster.utils.MapRasterTiles;
import si.um.feri.maprri.raster.utils.ZoomXY;

public class RasterMap extends ApplicationAdapter implements GestureDetector.GestureListener {
    private ShapeRenderer shapeRenderer;
    private Vector3 touchPosition;
    private WindSimulationController simController;
    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;
    private OrthographicCamera camera;
    private List<WindmillMarker> windmills = new ArrayList<>();
    private WindmillApi windmillApi;
    private boolean showWindmills = true;
    private SpriteBatch batch;
    private Stage uiStage;
    private Skin skin;
    private WindmillClickHandler windmillClickHandler;
    private Array<Window> windmillWindows = new Array<>();
    private ObjectMap<WindmillMarker, Window> openWindowsByWindmill = new ObjectMap<>();
    private TextButton addBtn;
    private boolean addMode = false;
    private Window createWindmillWindow = null;
    private OpenWeatherApi openWeatherApi;
    private WindmillRenderer windmillRenderer;
    private Window sideCard;
    private ZoomXY beginTile;
    private int tilesX;
    private TextButton toggleWindmillsBtn;

    private int tilesY;
    private final float BUTTON_W = 160;
    private final float BUTTON_H = 40;
    private final float BUTTON_MARGIN = 20;
    private final Geolocation CENTER_GEOLOCATION = new Geolocation(46.557314, 15.637771);
    private final Geolocation MARKER_GEOLOCATION = new Geolocation(46.559070, 15.638100);
    private TextButton simulateBtn;
    private final float SIM_W = 160;
    private final float SIM_H = 40;
    private final float SIM_GAP = 10;
    private PerspectiveCamera detailCamera;
    private ModelBatch modelBatch;
    private Environment environment;
    private Model windmillModel;
    private ModelInstance windmillInstance;
    private boolean zooming = false;
    private float zoomStart;
    private float zoomTarget = 13f;
    private float zoomProgress = 0f;

    private float tiltStart = 0f;
    private float tiltTarget = 35f;


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
        boolean originalWorking;
        String originalName;
    }

    private enum ViewMode {
        MAP,
        DETAIL
    }

    private ViewMode viewMode = ViewMode.MAP;
    private WindmillMarker selectedWindmill = null;


    private ObjectMap<WindmillMarker, InfoRefs> infoRefsByWindmill = new ObjectMap<>();

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        System.out.println("windmill sheet exists = " + Gdx.files.internal("windmill/eolico_sheet.png").exists());
        windmillApi = new WindmillApi("http://localhost:3001/api");
        windmillApi.fetchAll(list -> {
            windmills.clear();
            windmills.addAll(list);
            windmillClickHandler = new WindmillClickHandler(windmills, beginTile.x, beginTile.y, tilesY * MapRasterTiles.TILE_SIZE);
        });
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
        camera.viewportWidth = Constants.VIEWPORT_WIDTH;
        camera.viewportHeight = Constants.VIEWPORT_HEIGHT;
        camera.zoom = 5f;
        Gdx.input.setInputProcessor(new GestureDetector(this));
        touchPosition = new Vector3();
        Texture[][] tiles;
        try {
            double LAT_NORTH = 46.88;
            double LAT_SOUTH = 45.42;
            double LON_WEST = 13.38;
            double LON_EAST = 16.60;
            tiles = MapRasterTiles.getRasterTilesForBoundingBox(LAT_NORTH, LAT_SOUTH, LON_WEST, LON_EAST, Constants.ZOOM);
            beginTile = MapRasterTiles.getTileNumber(LAT_NORTH, LON_WEST, Constants.ZOOM);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        windmillClickHandler = new WindmillClickHandler(windmills, beginTile.x, beginTile.y, tilesY * MapRasterTiles.TILE_SIZE);
        this.tilesX = tiles.length;
        this.tilesY = tiles[0].length;
        float worldWidth = tilesX * MapRasterTiles.TILE_SIZE;
        float worldHeight = tilesY * MapRasterTiles.TILE_SIZE;
        float zoomX = worldWidth / camera.viewportWidth;
        float zoomY = worldHeight / camera.viewportHeight;
        camera.zoom = Math.max(zoomX, zoomY);
        camera.position.set(worldWidth / 2f, worldHeight / 2f, 0);
        camera.update();
        camera.position.set(worldWidth / 2f, worldHeight * 0.8f, 0);
        camera.update();
        tiledMap = new TiledMap();
        MapLayers layers = tiledMap.getLayers();
        TiledMapTileLayer layer = new TiledMapTileLayer(tilesX, tilesY, MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE);
        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
                cell.setTile(new StaticTiledMapTile(new TextureRegion(tiles[x][y])));
                layer.setCell(x, tilesY - 1 - y, cell);
            }
        }
        windmillRenderer = new WindmillRenderer();
        layers.add(layer);
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
        uiStage = new Stage();
        skin = new Skin(Gdx.files.internal("flat-earth-ui.json"));

        openWeatherApi = new si.um.feri.maprri.api.openweather.OpenWeatherApi("fc630787973cb99d455abfba28768f83");
        addBtn = new TextButton("+", skin);
        addBtn.setSize(40, 40);
        addBtn.setPosition(Gdx.graphics.getWidth() - 40 - BUTTON_MARGIN, Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN - 40 - 10 - 40 - 10);

        addBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                addMode = !addMode;
                addBtn.setText(addMode ? "x" : "+");
                return true;
            }
        });


        uiStage.addActor(addBtn);
        simulateBtn = new TextButton("SIMULATE", skin);
        simulateBtn.setSize(160, 40);
        simulateBtn.setPosition(Gdx.graphics.getWidth() - 160 - BUTTON_MARGIN, Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN - 40 - 10);
        simulateBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                if (sideCard != null) {
                    sideCard.remove();
                    sideCard = null;
                }
                if (simController != null) {
                    simController.toggle();
                    simulateBtn.setText(simController.isEnabled() ? "EXIT" : "SIMULATE");
                }
                return true;
            }
        });
        uiStage.addActor(simulateBtn);
        simController = new si.um.feri.maprri.raster.simulation.WindSimulationController(uiStage, skin, windmills, (WindmillMarker w) -> MapRasterTiles.getPixelPosition(w.lat, w.lon, MapRasterTiles.TILE_SIZE, Constants.ZOOM, beginTile.x, beginTile.y, tilesY * MapRasterTiles.TILE_SIZE), () -> {
            updateAllOpenInfoWindows();
        });
        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputMultiplexer(uiStage, new GestureDetector(this)));
        modelBatch = new ModelBatch();

        detailCamera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        detailCamera.position.set(0f, 6f, 12f);
        detailCamera.lookAt(0f, 4f, 0f);
        detailCamera.near = 0.1f;
        detailCamera.far = 100f;
        detailCamera.update();
        ModelBuilder mb = new ModelBuilder();
        windmillModel = mb.createCylinder(
            0.4f, 6f, 0.4f, 20,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        windmillInstance = new ModelInstance(windmillModel);
        windmillInstance.transform.setToTranslation(0f, 3f, 0f);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

    }

    @Override
    public void render() {
        if (viewMode == ViewMode.MAP) {
            renderMap();
        } else {
            renderDetail();
        }

    }
    private void renderMap() {
        animateZoomAndTilt();

        ScreenUtils.clear(0, 0, 0, 1);
        handleInput();

        camera.position.x = Math.round(camera.position.x);
        camera.position.y = Math.round(camera.position.y);
        camera.update();

        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        if (showWindmills) {
            List<WindmillMarker> toRender = new ArrayList<>();

            if (selectedWindmill == null) {
                toRender.addAll(windmills);
            } else {
                toRender.add(selectedWindmill);
            }

            List<float[]> positions = new ArrayList<>();
            for (WindmillMarker w : toRender) {
                Vector2 p = MapRasterTiles.getPixelPosition(
                    w.lat,
                    w.lon,
                    MapRasterTiles.TILE_SIZE,
                    Constants.ZOOM,
                    beginTile.x,
                    beginTile.y,
                    tilesY * MapRasterTiles.TILE_SIZE
                );
                positions.add(new float[]{p.x, p.y});
            }

            windmillRenderer.render(toRender, camera, positions);
        }

        drawToggleButton();
        drawSimSelectionRect();
        uiStage.act(Gdx.graphics.getDeltaTime());
        uiStage.draw();
    }
    private void showSideWindmillCard(WindmillMarker w) {
        if (sideCard != null) {
            sideCard.remove();
        }

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



    private void renderDetail() {
        renderMap();

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        detailCamera.update();

        modelBatch.begin(detailCamera);
        modelBatch.render(windmillInstance, environment);
        modelBatch.end();
    }






    private void drawToggleButton() {
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        float x = Gdx.graphics.getWidth() - BUTTON_W - BUTTON_MARGIN;
        float y = Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(showWindmills ? Color.SKY : Color.DARK_GRAY);
        shapeRenderer.rect(x, y, BUTTON_W, BUTTON_H);
        shapeRenderer.end();
    }

    private void drawSimulateButton() {
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        float x = Gdx.graphics.getWidth() - SIM_W - BUTTON_MARGIN;
        float y = Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN - SIM_H - SIM_GAP;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(simController != null && simController.isEnabled() ? Color.ORANGE : Color.GRAY);
        shapeRenderer.rect(x, y, SIM_W, SIM_H);
        shapeRenderer.end();
    }

    private void drawWindmill(float x, float y, float size) {
        shapeRenderer.setColor(Color.SKY);
        float towerHeight = size * 1.6f;
        float towerWidth = size * 0.25f;
        shapeRenderer.rect(x - towerWidth / 2f, y - towerHeight, towerWidth, towerHeight);
        float hubRadius = size * 0.25f;
        shapeRenderer.circle(x, y, hubRadius);
        for (int i = 0; i < 3; i++) {
            float angle = i * 120f;
            float rad = (float) Math.toRadians(angle);
            float bladeLen = size;
            float bx = x + (float) Math.cos(rad) * hubRadius;
            float by = y + (float) Math.sin(rad) * hubRadius;
            shapeRenderer.triangle(bx, by, bx + (float) Math.cos(rad + 0.15f) * bladeLen, by + (float) Math.sin(rad + 0.15f) * bladeLen, bx + (float) Math.cos(rad - 0.15f) * bladeLen, by + (float) Math.sin(rad - 0.15f) * bladeLen);
        }
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {

        if (viewMode == ViewMode.DETAIL) {
            viewMode = ViewMode.MAP;
            selectedWindmill = null;
            return true;
        }

        Vector2 stageCoords = uiStage.screenToStageCoordinates(new Vector2(x, y));
        if (uiStage.hit(stageCoords.x, stageCoords.y, true) != null) return true;

        float screenY = Gdx.graphics.getHeight() - y;
        float bx = Gdx.graphics.getWidth() - BUTTON_W - BUTTON_MARGIN;
        float by = Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN;

        if (x >= bx && x <= bx + BUTTON_W && screenY >= by && screenY <= by + BUTTON_H) {
            showWindmills = !showWindmills;
            return true;
        }

        Vector3 worldClick = new Vector3(x, y, 0);
        camera.unproject(worldClick);

        if (addMode) {
            Geolocation geo = worldToLatLon(worldClick.x, worldClick.y);
            openCreateWindmillWindow(geo.lat, geo.lng, (int) x, (int) y);
            return true;
        }

        if (simController != null && simController.isEnabled() && !simController.hasSelectedArea()) {
            simController.handleMapClick(worldClick.x, worldClick.y);
            return true;
        }

        if (!showWindmills || windmillClickHandler == null) return false;

        WindmillMarker clicked =
            windmillClickHandler.getClickedWindmill(worldClick.x, worldClick.y);

        if (clicked != null) {
            selectedWindmill = clicked;


            zooming = true;
            zoomProgress = 0f;
            showSideWindmillCard(clicked);
            zoomStart = camera.zoom;
            return true;
        }


        return false;
    }



    private void drawSimSelectionRect() {
        if (simController == null || !simController.isEnabled()) return;
        if (simController.getStart() == null) return;
        Vector2 a = simController.getStart();
        Vector2 b = simController.getEndOrNull();
        if (b == null) {
            Vector3 tmp = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(tmp);
            b = new Vector2(tmp.x, tmp.y);
        }
        float rx = Math.min(a.x, b.x);
        float ry = Math.min(a.y, b.y);
        float rw = Math.abs(a.x - b.x);
        float rh = Math.abs(a.y - b.y);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(rx, ry, rw, rh);
        shapeRenderer.end();
    }

    private void updateAllOpenInfoWindows() {
        Array<WindmillMarker> keys = new Array<>();
        for (WindmillMarker w : openWindowsByWindmill.keys()) keys.add(w);
        for (WindmillMarker w : keys) {
            updateInfoWindowFor(w);
        }
    }

    private void animateZoomAndTilt() {
        if (!zooming || selectedWindmill == null) return;
        float focusOffsetX = camera.viewportWidth * camera.zoom * 0.25f;

        zoomProgress += Gdx.graphics.getDeltaTime();
        float t = Math.min(zoomProgress / 0.8f, 1f);
        t = t * t * (3f - 2f * t);

        camera.zoom = MathUtils.lerp(zoomStart, 1f / zoomTarget, t);

        Vector2 p = MapRasterTiles.getPixelPosition(
            selectedWindmill.lat,
            selectedWindmill.lon,
            MapRasterTiles.TILE_SIZE,
            Constants.ZOOM,
            beginTile.x,
            beginTile.y,
            tilesY * MapRasterTiles.TILE_SIZE
        );

        camera.position.x = MathUtils.lerp(camera.position.x, p.x + focusOffsetX, t);

        camera.position.y = MathUtils.lerp(camera.position.y, p.y, t);

        camera.up.set(0, MathUtils.cosDeg(tiltTarget * t), MathUtils.sinDeg(tiltTarget * t));
        camera.direction.set(0, 0, -1).nor();

        if (t >= 1f) {
            zooming = false;
        }
    }


    private void createAndShowWindmillWindow(WindmillMarker w, int screenX, int screenY) {
        Window window = new Window("Windmill info", skin);
        window.setMovable(true);
        window.setResizable(false);
        TextButton closeBtn = new TextButton("X", skin);
        closeBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                window.remove();
                windmillWindows.removeValue(window, true);
                openWindowsByWindmill.remove(w);
                infoRefsByWindmill.remove(w);
                return true;
            }
        });
        window.getTitleTable().add(closeBtn).padLeft(8f);
        fillWindmillWindow(window, w);
        window.pack();
        window.setKeepWithinStage(true);
        Vector2 stagePos = uiStage.screenToStageCoordinates(new Vector2(screenX, screenY));
        float margin = 10f;
        float x = stagePos.x + margin;
        float y = stagePos.y - window.getHeight() - margin;
        x = Math.max(0, Math.min(x, uiStage.getWidth() - window.getWidth()));
        y = Math.max(0, Math.min(y, uiStage.getHeight() - window.getHeight()));
        window.setPosition(x, y);
        uiStage.addActor(window);
        windmillWindows.add(window);
        openWindowsByWindmill.put(w, window);
        window.toFront();
    }

    private void createToggleWindmillsButton() {
        toggleWindmillsBtn = new TextButton("Windmills: ON", skin);
        toggleWindmillsBtn.setSize(BUTTON_W, BUTTON_H);
        toggleWindmillsBtn.setPosition(
            Gdx.graphics.getWidth() - BUTTON_W - BUTTON_MARGIN,
            Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN
        );

        toggleWindmillsBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                showWindmills = !showWindmills;
                toggleWindmillsBtn.setText(showWindmills ? "Windmills: ON" : "Windmills: OFF");
                return true;
            }
        });

        uiStage.addActor(toggleWindmillsBtn);
    }


    private void fillWindmillWindow(Window window, WindmillMarker w) {
        window.clear();
        window.defaults().pad(16).left();

        InfoRefs refs = new InfoRefs();
        infoRefsByWindmill.put(w, refs);

        Label title = new Label(w.name, skin);
        title.setFontScale(1.8f);
        title.setColor(Color.BLACK);
        window.add(title).colspan(2).padBottom(24).row();

        window.add("Latitude").left();
        window.add(String.valueOf(w.lat)).left().row();

        window.add("Longitude").left();
        window.add(String.valueOf(w.lon)).left().row();

        window.add("Working").left();
        refs.working = new Label(w.working ? "Yes" : "No", skin);
        window.add(refs.working).left().row();

        window.add("Wind speed").left();
        refs.windSpeed = new Label(String.format("%.2f m/s", w.windSpeed), skin);
        window.add(refs.windSpeed).left().row();

        float kWh1h = estimateEnergyKWhPerHour(w.windSpeed, w.working);
        float kWh24h = kWh1h * 24f;

        window.add("Energy (1h)").left();
        refs.kwh1h = new Label(String.format("%.1f kWh", kWh1h), skin);
        window.add(refs.kwh1h).left().row();

        window.add("Energy (24h)").left();
        refs.kwh24h = new Label(String.format("%.1f kWh", kWh24h), skin);
        window.add(refs.kwh24h).left().row();

        window.add().expandY().row();

        refs.editBtn = new TextButton("EDIT", skin);
        refs.deleteBtn = new TextButton("DELETE", skin);

        window.add(refs.editBtn).expandX().left().padTop(24);
        window.add(refs.deleteBtn).expandX().right().padTop(24);
    }


    private void setEditMode(WindmillMarker w, boolean editing) {
        InfoRefs refs = infoRefsByWindmill.get(w);
        if (refs == null) return;
        if (editing && !refs.editing) {
            refs.originalWorking = w.working;
            refs.originalName = w.name;
        }
        refs.editing = editing;
        if (refs.nameLabel != null) refs.nameLabel.setVisible(!editing);
        if (refs.nameField != null) {
            refs.nameField.setVisible(editing);
            refs.nameField.setDisabled(!editing);
            if (editing) {
                refs.nameField.setText(w.name);
                uiStage.setKeyboardFocus(refs.nameField);
                refs.nameField.selectAll();
            } else {
                uiStage.setKeyboardFocus(null);
            }
        }
        if (refs.editBtn != null) refs.editBtn.setText(editing ? "CONFIRM" : "EDIT");
        Window win = openWindowsByWindmill.get(w);
        if (win != null) win.pack();
    }

    private void deleteWindmillLocalAndServer(WindmillMarker w) {
        Window win = openWindowsByWindmill.get(w);
        if (win != null) {
            win.remove();
            windmillWindows.removeValue(win, true);
        }
        openWindowsByWindmill.remove(w);
        infoRefsByWindmill.remove(w);
        windmills.remove(w);
        windmillClickHandler = new WindmillClickHandler(windmills, beginTile.x, beginTile.y, tilesY * MapRasterTiles.TILE_SIZE);
        if (w.id != null) {
            windmillApi.deleteWindmill(w.id, () -> {
            });
        }
    }

    private void confirmEdit(WindmillMarker w) {
        InfoRefs refs = infoRefsByWindmill.get(w);
        if (refs == null) return;
        String newName = (refs.nameField != null) ? refs.nameField.getText().trim() : w.name;
        if (newName.isEmpty()) newName = w.name;
        boolean nameChanged = refs.originalName != null && !newName.equals(refs.originalName);
        boolean workingChanged = (w.working != refs.originalWorking);
        if (!nameChanged && !workingChanged) {
            setEditMode(w, false);
            return;
        }
        w.name = newName;
        if (refs.nameLabel != null) refs.nameLabel.setText(newName);
        if (w.id == null) {
            setEditMode(w, false);
            updateInfoWindowFor(w);
            return;
        }
        String oldId = w.id;
        windmillApi.deleteWindmill(oldId, () -> {
            windmillApi.createWindmill(w, created -> {
                w.id = created.id;
                windmillClickHandler = new WindmillClickHandler(windmills, beginTile.x, beginTile.y, tilesY * MapRasterTiles.TILE_SIZE);
                setEditMode(w, false);
                updateInfoWindowFor(w);
            });
        });
    }

    private void openCreateWindmillWindow(double lat, double lon, int screenX, int screenY) {
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
                final String name = nameField.getText().trim();
                if (name == ""){
                    return false;
                }
                openWeatherApi.fetchWindSpeed(lat, lon, windSpeed -> {
                    WindmillMarker marker = new WindmillMarker(null, lon, lat, active[0], windSpeed, name);
                    windmillApi.createWindmill(marker, created -> {
                        windmills.add(created);
                        windmillClickHandler = new WindmillClickHandler(windmills, beginTile.x, beginTile.y, tilesY * MapRasterTiles.TILE_SIZE);
                        w.remove();
                        if (createWindmillWindow == w) createWindmillWindow = null;
                        addMode = false;
                        addBtn.setText("+");
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
        w.setPosition(Math.max(0, Math.min(stagePos.x, uiStage.getWidth() - w.getWidth())), Math.max(0, Math.min(stagePos.y - w.getHeight(), uiStage.getHeight() - w.getHeight())));
        uiStage.addActor(w);
        w.toFront();
        uiStage.setKeyboardFocus(nameField);
    }

    private void updateInfoWindowFor(WindmillMarker w) {
        if (!openWindowsByWindmill.containsKey(w)) return;
        InfoRefs refs = infoRefsByWindmill.get(w);
        if (refs == null) return;
        refs.working.setText(w.working ? "Yes" : "No");
        refs.windSpeed.setText(String.format(java.util.Locale.US, "%.2f m/s", w.windSpeed));
        float kWhPerHour = estimateEnergyKWhPerHour(w.windSpeed, w.working);
        float kWhPerDay = kWhPerHour * 24f;
        refs.kwh1h.setText(String.format(java.util.Locale.US, "%.1f kWh", kWhPerHour));
        refs.kwh24h.setText(String.format(java.util.Locale.US, "%.1f kWh", kWhPerDay));
        openWindowsByWindmill.get(w).pack();
    }

    private float estimateEnergyKWhPerHour(float windSpeed, boolean working) {
        if (!working) return 0f;
        float v = Math.max(0f, windSpeed);
        float rho = 1.225f;
        float D = 80f;
        float A = (float) (Math.PI * Math.pow(D / 2f, 2));
        float Cp = 0.40f;
        float powerW = 0.5f * rho * A * Cp * v * v * v;
        float powerKW = powerW / 1000f;
        return powerKW;
    }

    private Geolocation worldToLatLon(float worldX, float worldY) {
        int tileSize = MapRasterTiles.TILE_SIZE;
        int z = Constants.ZOOM;
        double worldPixSize = (double) tileSize * (1 << z);
        double globalX = beginTile.x * tileSize + worldX;
        double globalY = beginTile.y * tileSize + (tilesY * tileSize - worldY);
        double lon = globalX / worldPixSize * 360.0 - 180.0;
        double n = Math.PI - (2.0 * Math.PI * globalY) / worldPixSize;
        double lat = Math.toDegrees(Math.atan(Math.sinh(n)));
        return new Geolocation(lat, lon);
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {
    }

    private void handleInput() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && selectedWindmill != null) {
            selectedWindmill = null;
            zooming = false;

            camera.up.set(0, 1, 0);
            camera.zoom = zoomStart;

            if (sideCard != null) {
                sideCard.remove();
                sideCard = null;
            }
        }



        float moveSpeed = 600 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.translate(-moveSpeed, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.translate(moveSpeed, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.translate(0, moveSpeed);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.translate(0, -moveSpeed);
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) camera.zoom -= 0.02f;
        if (Gdx.input.isKeyPressed(Input.Keys.E)) camera.zoom += 0.02f;
        camera.zoom = MathUtils.clamp(camera.zoom, 0.2f, 1.2f);
        float worldWidth = tilesX * MapRasterTiles.TILE_SIZE;
        float worldHeight = tilesY * MapRasterTiles.TILE_SIZE;
        float viewW = camera.viewportWidth * camera.zoom;
        float viewH = camera.viewportHeight * camera.zoom;
        if (viewW < worldWidth) {
            float halfW = viewW / 2f;
            camera.position.x = MathUtils.clamp(camera.position.x, halfW, worldWidth - halfW);
        } else {
            camera.position.x = worldWidth / 2f;
        }
        if (viewH < worldHeight) {
            float halfH = viewH / 2f;
            camera.position.y = MathUtils.clamp(camera.position.y, halfH, worldHeight - halfH);
        } else {
            camera.position.y = worldHeight / 2f;
        }
    }
}
