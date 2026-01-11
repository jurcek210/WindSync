package si.um.feri.maprri.model;

public class WindmillMarker {
    public final double lon;
    public final double lat;
    public final boolean working;
    public final float windSpeed;
    public final String name;

    public float animationTime = 0f;

    public WindmillMarker(double lon, double lat, boolean working, float windSpeed, String name) {
        this.lon = lon;
        this.lat = lat;
        this.working = working;
        this.windSpeed = windSpeed;
        this.name = name;
    }
}
