package si.um.feri.maprri.vector;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.maprri.model.WindmillMarker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class VectorMapRenderer {

    private float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

    public final ShapeRenderer shapeRenderer;

    private final List<float[]> roads = new ArrayList<>();
    private final List<float[]> buildings = new ArrayList<>();
    private final List<String> buildingNames = new ArrayList<>();

    private List<WindmillMarker> windmills = new ArrayList<>();

    public OrthographicCamera camera;
    public Viewport viewport;

    public float scale = 0.0016f;
    public float offsetX = 0f, offsetY = 0f;

    public VectorMapRenderer(OrthographicCamera camera, float screenWidth, float screenHeight) {
        shapeRenderer = new ShapeRenderer();
        this.camera = camera;
        viewport = new FitViewport(screenWidth, screenHeight, camera);
        viewport.apply();

        loadGeoJSON("obcine.geojson");
    }

    private void loadGeoJSON(String filename) {
        try {
            InputStream is = Gdx.files.internal(filename).read();
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            JSONObject geojson = new JSONObject(text);
            JSONArray features = geojson.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                JSONObject geometry = feature.getJSONObject("geometry");

                String name = properties.optString("name", "");
                String type = geometry.getString("type");

                if (type.equals("LineString")) {
                    roads.add(parseCoordinates(geometry.getJSONArray("coordinates")));
                } else if (type.equals("Polygon")) {
                    buildings.add(parsePolygon(geometry.getJSONArray("coordinates")));
                    buildingNames.add(name);
                } else if (type.equals("MultiPolygon")) {
                    JSONArray polys = geometry.getJSONArray("coordinates");
                    for (int j = 0; j < polys.length(); j++) {
                        buildings.add(parsePolygon(polys.getJSONArray(j)));
                        buildingNames.add(name);
                    }
                }
            }

            offsetX = (minX + maxX) / 2f;
            offsetY = (minY + maxY) / 2f;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[] parseCoordinates(JSONArray coords) {
        float[] verts = new float[coords.length() * 2];
        for (int i = 0; i < coords.length(); i++) {
            JSONArray c = coords.getJSONArray(i);
            Vector2 p = lonLatToMeters(c.getDouble(0), c.getDouble(1));
            verts[i * 2] = p.x;
            verts[i * 2 + 1] = p.y;

            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }
        return verts;
    }

    private float[] parsePolygon(JSONArray polyCoords) {
        return parseCoordinates(polyCoords.getJSONArray(0));
    }

    public void setWindmills(List<WindmillMarker> windmills) {
        this.windmills = windmills;
    }

    public static Vector2 lonLatToMeters(double lon, double lat) {
        double x = lon * 20037508.34 / 180.0;
        double y = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        y = y * 20037508.34 / 180.0;
        return new Vector2((float) x, (float) y);
    }

    public void render() {

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < buildings.size(); i++) {
            String n = buildingNames.get(i);
            if (
                "Ljubljana".equalsIgnoreCase(n) ||
                    "Maribor".equalsIgnoreCase(n) ||
                    n.toLowerCase().contains("koper") ||
                    "Celje".equalsIgnoreCase(n)
            ) {
                shapeRenderer.setColor(Color.SKY);
                drawFilledPolygon(buildings.get(i));
            }
        }

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for (int i = 0; i < buildings.size(); i++) {
            String n = buildingNames.get(i);
            if (
                "Ljubljana".equalsIgnoreCase(n) ||
                    "Maribor".equalsIgnoreCase(n) ||
                    n.toLowerCase().contains("koper") ||
                    "Celje".equalsIgnoreCase(n)
            ) {
                shapeRenderer.setColor(Color.SKY);
            } else {
                shapeRenderer.setColor(Color.LIGHT_GRAY);
            }
            drawPolygon(buildings.get(i));
        }

        shapeRenderer.setColor(Color.DARK_GRAY);
        for (float[] verts : roads) {
            drawLineString(verts);
        }

        shapeRenderer.setColor(Color.RED);
        for (WindmillMarker w : windmills) {
            Vector2 p = lonLatToMeters(w.lon, w.lat);
            float x = (p.x - offsetX) * scale;
            float y = (p.y - offsetY) * scale;

            shapeRenderer.circle(x, y + 6, 6);
            shapeRenderer.triangle(
                x - 4, y + 4,
                x + 4, y + 4,
                x, y - 6
            );
        }


        shapeRenderer.end();
    }

    private void drawFilledPolygon(float[] verts) {
        for (int i = 1; i < verts.length / 2 - 1; i++) {
            shapeRenderer.triangle(
                (verts[0] - offsetX) * scale,
                (verts[1] - offsetY) * scale,
                (verts[i * 2] - offsetX) * scale,
                (verts[i * 2 + 1] - offsetY) * scale,
                (verts[(i + 1) * 2] - offsetX) * scale,
                (verts[(i + 1) * 2 + 1] - offsetY) * scale
            );
        }
    }

    private void drawPolygon(float[] verts) {
        for (int i = 0; i < verts.length - 2; i += 2) {
            float x1 = (verts[i] - offsetX) * scale;
            float y1 = (verts[i + 1] - offsetY) * scale;
            float x2 = (verts[i + 2] - offsetX) * scale;
            float y2 = (verts[i + 3] - offsetY) * scale;
            shapeRenderer.line(x1, y1, x2, y2);
        }
    }

    private void drawLineString(float[] verts) {
        for (int i = 0; i < verts.length - 2; i += 2) {
            float x1 = (verts[i] - offsetX) * scale;
            float y1 = (verts[i + 1] - offsetY) * scale;
            float x2 = (verts[i + 2] - offsetX) * scale;
            float y2 = (verts[i + 3] - offsetY) * scale;
            shapeRenderer.line(x1, y1, x2, y2);
        }
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
