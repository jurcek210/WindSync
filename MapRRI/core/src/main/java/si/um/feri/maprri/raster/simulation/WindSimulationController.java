package si.um.feri.maprri.raster.simulation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.List;

import si.um.feri.maprri.model.WindmillMarker;

public class WindSimulationController {

    public interface WindmillWorldPosProvider {
        Vector2 getWorldPos(WindmillMarker w);
    }

    public interface OnSimulationDataChanged {
        void onWindmillsChanged();
    }

    private static final float CUT_OUT_WIND_MS = 25f;

    private boolean areaLocked = false;

    private final Stage uiStage;
    private final Skin skin;
    private final List<WindmillMarker> windmills;
    private final WindmillWorldPosProvider posProvider;
    private final OnSimulationDataChanged onChanged;

    private boolean enabled = false;
    private boolean selecting = false;

    private Vector2 start = null;
    private Vector2 end = null;
    private Rectangle rect = null;

    private float simulatedWind = 0f;
    private Window simWindow;

    private final ObjectMap<WindmillMarker, Float> originalWind = new ObjectMap<>();
    private final ObjectMap<WindmillMarker, Boolean> originalWorking = new ObjectMap<>();

    public WindSimulationController(
        Stage uiStage,
        Skin skin,
        List<WindmillMarker> windmills,
        WindmillWorldPosProvider posProvider,
        OnSimulationDataChanged onChanged
    ) {
        this.uiStage = uiStage;
        this.skin = skin;
        this.windmills = windmills;
        this.posProvider = posProvider;
        this.onChanged = onChanged;
    }
    public boolean isEnabled() { return enabled; }
    public boolean isSelecting() { return enabled && (selecting || start != null); }
    public Rectangle getRect() { return rect; }
    public Vector2 getStart() { return start; }
    public Vector2 getEndOrNull() { return end; }
    public float getSimulatedWind() { return simulatedWind; }

    public void start() {
        enabled = true;
        selecting = false;
        start = null;
        end = null;
        rect = null;
        simulatedWind = 0f;
        areaLocked = false;
        closeSimWindow();
    }

    public void exit() {
        restoreOriginals();
        enabled = false;
        selecting = false;
        start = null;
        end = null;
        rect = null;
        simulatedWind = 0f;
        areaLocked = false;
        closeSimWindow();
        onChanged.onWindmillsChanged();
    }

    public void toggle() {
        if (!enabled) start();
        else exit();
    }

    public boolean hasSelectedArea() {
        return rect != null && areaLocked;
    }


    public boolean handleMapClick(float worldX, float worldY) {
        if (!enabled) return false;
        if (areaLocked) return true;

        if (!selecting) {
            selecting = true;
            start = new Vector2(worldX, worldY);
            end = null;
            rect = null;
            return true;
        } else {
            end = new Vector2(worldX, worldY);
            selecting = false;

            float rx = Math.min(start.x, end.x);
            float ry = Math.min(start.y, end.y);
            float rw = Math.abs(start.x - end.x);
            float rh = Math.abs(start.y - end.y);
            rect = new Rectangle(rx, ry, rw, rh);
            areaLocked = true;

            openSimWindow();
            applyToWindmills();
            return true;
        }
    }

    public void setSimulatedWind(float v) {
        simulatedWind = Math.max(0f, v);
        applyToWindmills();
        onChanged.onWindmillsChanged();
    }

    private void applyToWindmills() {
        if (!enabled || rect == null) return;

        for (WindmillMarker w : windmills) {
            Vector2 p = posProvider.getWorldPos(w);
            boolean inside = rect.contains(p);

            if (inside) {
                if (!originalWind.containsKey(w)) originalWind.put(w, w.windSpeed);
                if (!originalWorking.containsKey(w)) originalWorking.put(w, w.working);

                w.windSpeed = simulatedWind;
                w.working = simulatedWind <= CUT_OUT_WIND_MS;
            } else {
                if (originalWind.containsKey(w)) w.windSpeed = originalWind.get(w);
                if (originalWorking.containsKey(w)) w.working = originalWorking.get(w);
            }
        }
    }

    private void restoreOriginals() {
        for (WindmillMarker w : originalWind.keys()) w.windSpeed = originalWind.get(w);
        for (WindmillMarker w : originalWorking.keys()) w.working = originalWorking.get(w);
        originalWind.clear();
        originalWorking.clear();
    }

    private void openSimWindow() {
        closeSimWindow();

        simWindow = new Window("Wind simulation", skin);
        simWindow.setMovable(true);
        simWindow.setResizable(false);
        simWindow.setKeepWithinStage(true);

        TextButton minus = new TextButton("-", skin);
        TextButton plus = new TextButton("+", skin);

        TextField speedField = new TextField(String.valueOf((int) simulatedWind), skin);
        speedField.setTextFieldFilter((tf, c) -> (c >= '0' && c <= '9'));
        speedField.setTextFieldListener((tf, c) -> {
            if (c == '\n' || c == '\r') {
                try {
                    float v = Float.parseFloat(tf.getText().trim());
                    setSimulatedWind(v);

                    tf.setText(String.valueOf((int) simulatedWind));

                    uiStage.setKeyboardFocus(null);
                } catch (Exception ignored) {}
            }
        });


        minus.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                setSimulatedWind(simulatedWind - 1f);
                speedField.setText(String.valueOf((int) simulatedWind));
                return true;
            }
        });

        plus.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                setSimulatedWind(simulatedWind + 1f);
                speedField.setText(String.valueOf((int) simulatedWind));
                return true;
            }
        });

        Table row = new Table(skin);
        row.add(minus).pad(5);
        row.add(speedField).width(80).pad(5);
        row.add(new Label("m/s", skin)).padRight(10);
        row.add(plus).pad(5);

        simWindow.add(new Label("Set wind speed:", skin)).row();
        simWindow.add(row).pad(10).row();

        simWindow.pack();
        simWindow.setPosition(20, Gdx.graphics.getHeight() - simWindow.getHeight() - 20);

        uiStage.addActor(simWindow);
        simWindow.toFront();
    }

    private void closeSimWindow() {
        if (simWindow != null) {
            simWindow.remove();
            simWindow = null;
        }
    }
}
