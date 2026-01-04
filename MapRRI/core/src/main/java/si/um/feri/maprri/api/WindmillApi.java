package si.um.feri.maprri.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;

public class WindmillApi {

    private final String baseUrl;

    public WindmillApi(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void fetchAll(Net.HttpResponseListener listener) {
        Net.HttpRequest req = new Net.HttpRequest(Net.HttpMethods.GET);
        req.setUrl(baseUrl + "/windmills");
        Gdx.net.sendHttpRequest(req, listener);
    }
}
