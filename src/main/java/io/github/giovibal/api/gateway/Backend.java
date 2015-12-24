package io.github.giovibal.api.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

/**
 * Created by Giovanni Baleani on 24/12/2015.
 */
public class Backend extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        JsonObject config = config();
        HttpClientOptions opt = new HttpClientOptions(config);
        HttpClient httpClient = vertx.createHttpClient(opt);
//        httpClient.
    }

}
