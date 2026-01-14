package si.um.feri.maprri.model;

public class WindmillMarker {
    public String id;
    public double lon;
    public double lat;
    public boolean working;
    public float windSpeed;
    public String name;

    public float animationTime = 0f;

    public WindmillMarker(String id, double lon, double lat, boolean working, float windSpeed, String name) {
        this.id = id;
        this.lon = lon;
        this.lat = lat;
        this.working = working;
        this.windSpeed = windSpeed;
        this.name = name;
    }
}
