package si.um.feri.maprri.raster.utils;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MapRasterTiles {

    static String mapServiceUrl = "https://maps.geoapify.com/v1/tile/";
    static String token = "?&apiKey=" + Keys.GEOAPIFY;
    static String tilesetId = "positron";
    static String format = "@2x.png";

    public static final int TILE_SIZE = 512;

    public static Texture getRasterTile(int zoom, int x, int y) throws IOException {
        URL url = new URL(mapServiceUrl + tilesetId + "/" + zoom + "/" + x + "/" + y + format + token);
        ByteArrayOutputStream bis = fetchTile(url);
        return getTexture(bis.toByteArray());
    }

    public static Texture getRasterTile(String zoomXY) throws IOException {
        URL url = new URL(mapServiceUrl + tilesetId + "/" + zoomXY + format + token);
        ByteArrayOutputStream bis = fetchTile(url);
        return getTexture(bis.toByteArray());
    }

    public static Texture getRasterTile(ZoomXY zoomXY) throws IOException {
        URL url = new URL(mapServiceUrl + tilesetId + "/" + zoomXY.toString() + format + token);
        ByteArrayOutputStream bis = fetchTile(url);
        return getTexture(bis.toByteArray());
    }

    public static Texture[] getRasterTileZone(ZoomXY zoomXY, int size) throws IOException {
        Texture[] array = new Texture[size * size];
        int[] factorY = new int[size * size];
        int[] factorX = new int[size * size];

        int value = (size - 1) / -2;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                factorY[i * size + j] = value;
                factorX[i + j * size] = value;
            }
            value++;
        }

        for (int i = 0; i < size * size; i++) {
            array[i] = getRasterTile(
                zoomXY.zoom,
                zoomXY.x + factorX[i],
                zoomXY.y + factorY[i]
            );
        }
        return array;
    }

    public static ByteArrayOutputStream fetchTile(URL url) throws IOException {
        ByteArrayOutputStream bis = new ByteArrayOutputStream();
        InputStream is = url.openStream();
        byte[] bytebuff = new byte[4096];
        int n;
        while ((n = is.read(bytebuff)) > 0) {
            bis.write(bytebuff, 0, n);
        }
        is.close();
        return bis;
    }

    public static Texture getTexture(byte[] array) {
        return new Texture(new Pixmap(array, 0, array.length));
    }

    public static ZoomXY getTileNumber(double lat, double lon, int zoom) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor(
            (1 - Math.log(Math.tan(Math.toRadians(lat)) +
                1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom)
        );

        if (xtile < 0) xtile = 0;
        if (xtile >= (1 << zoom)) xtile = (1 << zoom) - 1;
        if (ytile < 0) ytile = 0;
        if (ytile >= (1 << zoom)) ytile = (1 << zoom) - 1;

        return new ZoomXY(zoom, xtile, ytile);
    }

    public static double tile2long(int tileNumberX, int zoom) {
        return (tileNumberX / Math.pow(2, zoom) * 360 - 180);
    }

    public static double tile2lat(int tileNumberY, int zoom) {
        double n = Math.PI - 2 * Math.PI * tileNumberY / Math.pow(2, zoom);
        return (180 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));
    }

    public static double[] project(double lat, double lng, int tileSize) {
        double siny = Math.sin((lat * Math.PI) / 180);
        siny = Math.min(Math.max(siny, -0.9999), 0.9999);

        return new double[]{
            tileSize * (0.5 + lng / 360),
            tileSize * (0.5 - Math.log((1 + siny) / (1 - siny)) / (4 * Math.PI))
        };
    }

    public static Vector2 getPixelPosition(
        double lat,
        double lng,
        int tileSize,
        int zoom,
        int beginTileX,
        int beginTileY,
        int height
    ) {
        double[] world = project(lat, lng, tileSize);
        double scale = Math.pow(2, zoom);

        return new Vector2(
            (float)(Math.floor(world[0] * scale) - beginTileX * tileSize),
            height - (float)(Math.floor(world[1] * scale) - beginTileY * tileSize - 1)
        );
    }

    public static Vector2 getPixelPosition(
        double lat,
        double lng,
        int beginTileX,
        int beginTileY,
        int worldHeight
    ) {
        double[] world = project(lat, lng, TILE_SIZE);
        double scale = Math.pow(2, Constants.ZOOM);

        return new Vector2(
            (float)(Math.floor(world[0] * scale) - beginTileX * TILE_SIZE),
            worldHeight - (float)(Math.floor(world[1] * scale) - beginTileY * TILE_SIZE - 1)
        );
    }

    public static Vector2 getPixelPosition(
        double lat,
        double lng,
        int beginTileX,
        int beginTileY
    ) {
        double[] world = project(lat, lng, TILE_SIZE);
        double scale = Math.pow(2, Constants.ZOOM);

        return new Vector2(
            (float)(Math.floor(world[0] * scale) - beginTileX * TILE_SIZE),
            (float)(Math.floor(world[1] * scale) - beginTileY * TILE_SIZE)
        );
    }

    public static Texture[][] getRasterTilesForBoundingBox(
        double latNorth,
        double latSouth,
        double lonWest,
        double lonEast,
        int zoom
    ) throws IOException {

        ZoomXY topLeft = getTileNumber(latNorth, lonWest, zoom);
        ZoomXY bottomRight = getTileNumber(latSouth, lonEast, zoom);

        int width = bottomRight.x - topLeft.x + 1;
        int height = bottomRight.y - topLeft.y + 1;

        Texture[][] tiles = new Texture[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = getCachedOrFetchTile(
                    zoom,
                    topLeft.x + x,
                    topLeft.y + y
                );
            }
        }

        return tiles;
    }

    public static Geolocation[][] fetchPath(Geolocation[] geolocations) {
        double[][] coords = new double[geolocations.length][2];
        for (int i = 0; i < geolocations.length; i++) {
            coords[i] = new double[]{geolocations[i].lat, geolocations[i].lng};
        }

        try {
            return getRouteFromCoordinates(coords);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Geolocation[][] getRouteFromCoordinates(double[][] coordinates) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < coordinates.length; i++) {
            sb.append(coordinates[i][0]).append(",").append(coordinates[i][1]);
            if (i < coordinates.length - 1) sb.append("|");
        }

        URL url = new URL(
            "https://api.geoapify.com/v1/routing?waypoints=" +
                sb + "&mode=drive&apiKey=" + Keys.GEOAPIFY
        );

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
            new InputStreamReader(con.getInputStream())
        );

        String line;
        StringBuilder response = new StringBuilder();
        while ((line = in.readLine()) != null) response.append(line);
        in.close();

        JSONArray coords = new JSONObject(response.toString())
            .getJSONArray("features")
            .getJSONObject(0)
            .getJSONObject("geometry")
            .getJSONArray("coordinates");

        Geolocation[][] result = new Geolocation[coords.length()][];

        for (int i = 0; i < coords.length(); i++) {
            JSONArray seg = coords.getJSONArray(i);
            Geolocation[] g = new Geolocation[seg.length()];
            for (int j = 0; j < seg.length(); j++) {
                JSONArray c = seg.getJSONArray(j);
                g[j] = new Geolocation(c.getDouble(1), c.getDouble(0));
            }
            result[i] = g;
        }

        return result;
    }
// to je za ono nalogo da se ne ponovno nalagajo ploščice
    private static Texture getCachedOrFetchTile(int zoom, int x, int y) throws IOException {
        String path = "tiles/z" + zoom + "_x" + x + "_y" + y + ".png";
        FileHandle file = Gdx.files.local(path);

        if (file.exists()) {
            return new Texture(file);
        }

        URL url = new URL(
            mapServiceUrl + tilesetId + "/" + zoom + "/" + x + "/" + y + format + token
        );

        ByteArrayOutputStream bis = fetchTile(url);

        file.parent().mkdirs();
        file.writeBytes(bis.toByteArray(), false);

        return new Texture(file);
    }
}
