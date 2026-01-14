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
    private String authToken;

    public WindmillApi(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    private void addAuth(Net.HttpRequest req) {
        if (authToken != null && !authToken.isEmpty()) {
            req.setHeader("Authorization", "Bearer " + authToken);
        }
        req.setHeader("Content-Type", "application/json");
    }

    public void fetchAll(Consumer<List<WindmillMarker>> onSuccess) {
        Net.HttpRequest req = new Net.HttpRequest(Net.HttpMethods.GET);
        req.setUrl(baseUrl + "/windmills");

        Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
            @Override public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String json = httpResponse.getResultAsString();
                JSONArray arr = new JSONArray(json);

                List<WindmillMarker> result = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    String id = o.optString("_id", null);

                    JSONObject loc = o.optJSONObject("location");
                    JSONArray coords = (loc != null) ? loc.optJSONArray("coordinates") : null;

                    double lon = coords != null ? coords.optDouble(0) : o.optDouble("lon");
                    double lat = coords != null ? coords.optDouble(1) : o.optDouble("lat");

                    boolean status = o.optBoolean("status", true);
                    float windSpeed = (float) o.optDouble("windSpeed", 0);
                    String name = o.optString("name", "");

                    result.add(new WindmillMarker(id, lon, lat, status, windSpeed, name));
                }

                Gdx.app.postRunnable(() -> onSuccess.accept(result));
            }

            @Override public void failed(Throwable t) { t.printStackTrace(); }
            @Override public void cancelled() {}
        });
    }

    public void deleteWindmill(String id, Runnable onSuccess) {
        Net.HttpRequest req = new Net.HttpRequest(Net.HttpMethods.DELETE);
        req.setUrl(baseUrl + "/windmills/" + id);
        addAuth(req);

        Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
            @Override public void handleHttpResponse(Net.HttpResponse httpResponse) {
                Gdx.app.postRunnable(onSuccess);
            }
            @Override public void failed(Throwable t) { t.printStackTrace(); }
            @Override public void cancelled() {}
        });
    }

    public void toggleStatus(String id, boolean status, Runnable onSuccess) {
        Net.HttpRequest req = new Net.HttpRequest(Net.HttpMethods.PUT);
        req.setUrl(baseUrl + "/windmills/" + id + "/toggle-status");
        addAuth(req);

        JSONObject body = new JSONObject();
        body.put("status", status);
        req.setContent(body.toString());

        Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
            @Override public void handleHttpResponse(Net.HttpResponse httpResponse) {
                Gdx.app.postRunnable(onSuccess);
            }
            @Override public void failed(Throwable t) { t.printStackTrace(); }
            @Override public void cancelled() {}
        });
    }

    public void createWindmill(WindmillMarker w, Consumer<WindmillMarker> onSuccess) {
        Net.HttpRequest req = new Net.HttpRequest(Net.HttpMethods.POST);
        req.setUrl(baseUrl + "/windmills");
        addAuth(req);

        JSONObject body = new JSONObject();
        body.put("name", w.name);
        body.put("status", w.working);
        body.put("windSpeed", w.windSpeed);

        JSONObject loc = new JSONObject();
        loc.put("type", "Point");
        loc.put("coordinates", new JSONArray().put(w.lon).put(w.lat));
        body.put("location", loc);

        req.setContent(body.toString());

        Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
            @Override public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String json = httpResponse.getResultAsString();
                JSONObject o = new JSONObject(json);
                String newId = o.optString("_id", null);

                WindmillMarker created = new WindmillMarker(
                    newId, w.lon, w.lat, w.working, w.windSpeed, w.name
                );

                Gdx.app.postRunnable(() -> onSuccess.accept(created));
            }
            @Override public void failed(Throwable t) { t.printStackTrace(); }
            @Override public void cancelled() {}
        });
    }
}
