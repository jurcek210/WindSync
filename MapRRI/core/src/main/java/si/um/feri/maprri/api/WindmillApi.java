package si.um.feri.maprri.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import org.json.JSONArray;
import org.json.JSONObject;
import si.um.feri.maprri.model.WindmillMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WindmillApi {

    private final String baseUrl;

    public WindmillApi(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void fetchAll(Consumer<List<WindmillMarker>> onSuccess) {
        Net.HttpRequest req = new Net.HttpRequest(Net.HttpMethods.GET);
        req.setUrl(baseUrl + "/windmills");

        Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                JSONArray arr = new JSONArray(httpResponse.getResultAsString());
                List<WindmillMarker> result = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    JSONObject loc = o.getJSONObject("location");
                    JSONArray c = loc.getJSONArray("coordinates");

                    double lon = c.getDouble(0);
                    double lat = c.getDouble(1);

                    boolean status = o.optBoolean("status", true);
                    String name = o.optString("name", "");

                    result.add(new WindmillMarker(lon, lat, status, name));
                }

                Gdx.app.postRunnable(() -> onSuccess.accept(result));
            }

            @Override public void failed(Throwable t) {}
            @Override public void cancelled() {}
        });
    }
}
