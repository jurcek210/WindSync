package si.um.feri.maprri.raster;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.ObjectMap;




import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import si.um.feri.maprri.api.WindmillApi;
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




    private WindmillRenderer windmillRenderer;


    private ZoomXY beginTile;

    private int tilesX;
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


    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        System.out.println(
            "windmill sheet exists = " +
                Gdx.files.internal("windmill/eolico_sheet.png").exists()
        );

        windmillApi = new WindmillApi("http://localhost:3001/api");
        windmillApi.fetchAll(list -> {
            windmills = list;
            windmillClickHandler = new WindmillClickHandler(
                windmills,
                beginTile.x,
                beginTile.y,
                tilesY * MapRasterTiles.TILE_SIZE
            );
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

            tiles = MapRasterTiles.getRasterTilesForBoundingBox(
                LAT_NORTH,
                LAT_SOUTH,
                LON_WEST,
                LON_EAST,
                Constants.ZOOM
            );

            beginTile = MapRasterTiles.getTileNumber(
                LAT_NORTH,
                LON_WEST,
                Constants.ZOOM
            );

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        windmillClickHandler = new WindmillClickHandler(
            windmills,
            beginTile.x,
            beginTile.y,
            tilesY * MapRasterTiles.TILE_SIZE
        );

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

        TiledMapTileLayer layer = new TiledMapTileLayer(
            tilesX,
            tilesY,
            MapRasterTiles.TILE_SIZE,
            MapRasterTiles.TILE_SIZE
        );

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
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        simulateBtn = new TextButton("SIMULATE", skin);
        simulateBtn.setSize(160, 40);
        simulateBtn.setPosition(
            Gdx.graphics.getWidth() - 160 - BUTTON_MARGIN,
            Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN - 40 - 10
        );
        simulateBtn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                if (simController != null) {
                    simController.toggle();
                    simulateBtn.setText(simController.isEnabled() ? "EXIT" : "SIMULATE");
                }
                return true;
            }
        });
        uiStage.addActor(simulateBtn);


        simController = new si.um.feri.maprri.raster.simulation.WindSimulationController(
            uiStage,
            skin,
            windmills,
            (WindmillMarker w) -> MapRasterTiles.getPixelPosition(
                w.lat, w.lon,
                MapRasterTiles.TILE_SIZE,
                Constants.ZOOM,
                beginTile.x, beginTile.y,
                tilesY * MapRasterTiles.TILE_SIZE
            ),
            () -> {
                updateAllOpenInfoWindows();
            }
        );


        Gdx.input.setInputProcessor(
            new com.badlogic.gdx.InputMultiplexer(
                uiStage,
                new GestureDetector(this)
            )
        );
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);

        handleInput();

        camera.position.x = Math.round(camera.position.x);
        camera.position.y = Math.round(camera.position.y);
        camera.update();

        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        if (showWindmills) {
            List<float[]> positions = new ArrayList<>();

            for (WindmillMarker w : windmills) {
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

            windmillRenderer.render(windmills, camera, positions);
        }



        drawToggleButton();
        drawSimSelectionRect();
        uiStage.act(Gdx.graphics.getDeltaTime());
        uiStage.draw();
    }

    private void drawToggleButton() {
        shapeRenderer.setProjectionMatrix(
            new com.badlogic.gdx.math.Matrix4()
                .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        );

        float x = Gdx.graphics.getWidth() - BUTTON_W - BUTTON_MARGIN;
        float y = Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(showWindmills ? Color.SKY : Color.DARK_GRAY);
        shapeRenderer.rect(x, y, BUTTON_W, BUTTON_H);
        shapeRenderer.end();
    }

    private void drawSimulateButton() {
        shapeRenderer.setProjectionMatrix(
            new com.badlogic.gdx.math.Matrix4()
                .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        );

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

            shapeRenderer.triangle(
                bx, by,
                bx + (float) Math.cos(rad + 0.15f) * bladeLen,
                by + (float) Math.sin(rad + 0.15f) * bladeLen,
                bx + (float) Math.cos(rad - 0.15f) * bladeLen,
                by + (float) Math.sin(rad - 0.15f) * bladeLen
            );
        }
    }



    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        Vector2 stageCoords = uiStage.screenToStageCoordinates(new Vector2(x, y));
        if (uiStage.hit(stageCoords.x, stageCoords.y, true) != null) {
            return true;
        }


        float screenY = Gdx.graphics.getHeight() - y;

        float bx = Gdx.graphics.getWidth() - BUTTON_W - BUTTON_MARGIN;
        float by = Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN;

        if (x >= bx && x <= bx + BUTTON_W && screenY >= by && screenY <= by + BUTTON_H) {
            showWindmills = !showWindmills;
            return true;
        }

        Vector3 worldClick = new Vector3(x, y, 0);
        camera.unproject(worldClick);


        if (simController != null && simController.isEnabled() && !simController.hasSelectedArea()) {
            Vector3 wc = new Vector3(x, y, 0);
            camera.unproject(wc);
            simController.handleMapClick(wc.x, wc.y);
            return true;
        }

        if (!showWindmills || windmillClickHandler == null) return false;


        WindmillMarker clicked =
            windmillClickHandler.getClickedWindmill(worldClick.x, worldClick.y);

        float simBx = Gdx.graphics.getWidth() - SIM_W - BUTTON_MARGIN;
        float simBy = Gdx.graphics.getHeight() - BUTTON_H - BUTTON_MARGIN - SIM_H - SIM_GAP;

        if (x >= simBx && x <= simBx + SIM_W && screenY >= simBy && screenY <= simBy + SIM_H) {
            if (simController != null) simController.toggle();
            return true;
        }

        if (clicked != null) {
            if (openWindowsByWindmill.containsKey(clicked)) {
                return true;
            }
            createAndShowWindmillWindow(clicked, (int) x, (int) y);
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
        for (WindmillMarker w : openWindowsByWindmill.keys()) {
            Window win = openWindowsByWindmill.get(w);
            if (win == null) continue;
            float px = win.getX();
            float py = win.getY();

            win.remove();
            createAndShowWindmillWindowAt(w, px, py);
        }
    }

    private void createAndShowWindmillWindowAt(WindmillMarker w, float stageX, float stageY) {
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
                return true;
            }
        });
        window.getTitleTable().add(closeBtn).padLeft(8f);

        fillWindmillWindow(window, w);
        window.pack();
        window.setKeepWithinStage(true);

        window.setPosition(stageX, stageY);

        uiStage.addActor(window);

        windmillWindows.add(window);
        openWindowsByWindmill.put(w, window);

        window.toFront();
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

    private void fillWindmillWindow(Window window, WindmillMarker w) {

        window.add("Name: ").left();
        window.add(w.name).left().row();

        window.add("Latitude: ").left();
        window.add(String.valueOf(w.lat)).left().row();

        window.add("Longitude: ").left();
        window.add(String.valueOf(w.lon)).left().row();

        window.add("Working: ").left();
        window.add(w.working ? "Yes" : "No").left().row();

        window.add("Wind speed: ").left();
        window.add(w.windSpeed + " m/s").left().row();

        float kWhPerHour = estimateEnergyKWhPerHour(w.windSpeed, w.working);
        float kWhPerDay = kWhPerHour * 24f;

        window.add("Est. energy (1h): ").left();
        window.add(String.format(java.util.Locale.US, "%.1f kWh", kWhPerHour)).left().row();

        window.add("Est. energy (24h): ").left();
        window.add(String.format(java.util.Locale.US, "%.1f kWh", kWhPerDay)).left().row();
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





    @Override public boolean tap(float x, float y, int count, int button) { return false; }
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float velocityX, float velocityY, int button) { return false; }
    @Override public boolean pan(float x, float y, float deltaX, float deltaY) { return false; }
    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }
    @Override public boolean zoom(float initialDistance, float distance) { return false; }
    @Override public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) { return false; }
    @Override public void pinchStop() {}

    private void handleInput() {
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
