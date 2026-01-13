package si.um.feri.maprri.raster.interaction;

import com.badlogic.gdx.math.Vector2;
import si.um.feri.maprri.model.WindmillMarker;
import si.um.feri.maprri.raster.utils.Constants;
import si.um.feri.maprri.raster.utils.MapRasterTiles;

import java.util.List;

public class WindmillClickHandler {

    private final List<WindmillMarker> windmills;
    private final int beginTileX;
    private final int beginTileY;
    private final int worldHeight;

    private static final float TOWER_WIDTH  = 28f;
    private static final float TOWER_HEIGHT = 70f;
    private static final float BLADE_RADIUS = 38f;

    public WindmillClickHandler(
        List<WindmillMarker> windmills,
        int beginTileX,
        int beginTileY,
        int worldHeight
    ) {
        this.windmills = windmills;
        this.beginTileX = beginTileX;
        this.beginTileY = beginTileY;
        this.worldHeight = worldHeight;
    }

    public WindmillMarker getClickedWindmill(float worldX, float worldY) {

        for (WindmillMarker w : windmills) {

            Vector2 p = MapRasterTiles.getPixelPosition(
                w.lat,
                w.lon,
                MapRasterTiles.TILE_SIZE,
                Constants.ZOOM,
                beginTileX,
                beginTileY,
                worldHeight
            );

            boolean inTower =
                worldX >= p.x - TOWER_WIDTH / 2f &&
                    worldX <= p.x + TOWER_WIDTH / 2f &&
                    worldY >= p.y - TOWER_HEIGHT &&
                    worldY <= p.y;

            float dx = worldX - p.x;
            float dy = worldY - p.y;

            boolean inBlades = dx * dx + dy * dy <= BLADE_RADIUS * BLADE_RADIUS;

            if (inTower || inBlades) {
                return w;
            }
        }

        return null;
    }
}
