package si.um.feri.maprri.api.openweather;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import org.json.JSONObject;

import java.util.Locale;
import java.util.function.Consumer;

public class OpenWeatherApi {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private final String apiKey;

    public OpenWeatherApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public void fetchWindSpeed(double lat, double lon, Consumer<Float> onSuccess, Consumer<Throwable> onError) {
        String url = String.format(
            Locale.US,
            "%s?lat=%.6f&lon=%.6f&appid=%s&units=metric",
            BASE_URL, lat, lon, apiKey
        );

        Net.HttpRequest req = new Net.HttpRequest(Net.HttpMethods.GET);
        req.setUrl(url);
        req.setHeader("Accept", "application/json");

        Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    String json = httpResponse.getResultAsString();
                    JSONObject o = new JSONObject(json);
                    float wind = (float) o.getJSONObject("wind").optDouble("speed", 0.0);

                    Gdx.app.postRunnable(() -> onSuccess.accept(wind));
                } catch (Throwable t) {
                    if (onError != null) Gdx.app.postRunnable(() -> onError.accept(t));
                }
            }

            @Override public void failed(Throwable t) {
                if (onError != null) Gdx.app.postRunnable(() -> onError.accept(t));
            }

            @Override public void cancelled() {}
        });
    }
}

