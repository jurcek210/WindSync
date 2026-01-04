package si.um.feri.maprri.model;

public class WindmillMarker {
    public final double lon;
    public final double lat;
    public final boolean status;
    public final String name;

    public WindmillMarker(double lon, double lat, boolean status, String name) {
        this.lon = lon;
        this.lat = lat;
        this.status = status;
        this.name = name;
    }
}
