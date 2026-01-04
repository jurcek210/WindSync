package si.um.feri.maprri.raster.utils;

public class MapProjection {

    public static final int TILE_SIZE = 256;

    public static float lonToWorldX(double lon, int zoom) {
        double scale = (1 << zoom) * TILE_SIZE;
        return (float) ((lon + 180.0) / 360.0 * scale);
    }

    public static float latToWorldY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        double mercN = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
        double scale = (1 << zoom) * TILE_SIZE;
        return (float) ((1.0 - mercN / Math.PI) / 2.0 * scale);
    }
}
