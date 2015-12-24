package io.github.giovibal.api.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;

import java.util.List;
import java.util.Set;

/**
 * Created by Giovanni Baleani on 24/12/2015.
 */
public class Backend extends AbstractVerticle {

    private PerfTimer perfTimer = new PerfTimer();

    @Override
    public void start() throws Exception {
        // Prepare BACKEND
//        JsonObject backendConf = new JsonObject()
//                .put("defaultHost", "192.168.0.1")
//                .put("defaultPort", 80)
//                ;

        JsonObject backendConf = config();

        String backendAddress = backendConf.getString("backend");

        HttpClientOptions backendOpt = new HttpClientOptions(backendConf);
        HttpClient backend = vertx.createHttpClient(backendOpt);

        vertx.eventBus().consumer("backend.test", (Message<JsonObject> h) -> {
            JsonObject body = h.body();

            String methodStr = body.getString("method");
            HttpMethod method = HttpMethod.valueOf(methodStr);

            String uri = body.getString("uri");

            byte[] frontendRequestBody = body.getBinary("body");

            HttpClientRequest backendRequest = backend.request(method, uri, backendResponse -> {
                String ra = h.replyAddress();
                MessageProducer<JsonObject> response = vertx.eventBus().sender(ra);

                JsonObject msgHeaders = new JsonObject();
                MultiMap headers = backendResponse.headers();
                Set<String> names = headers.names();
                for (String name : names) {
                    String value = headers.get(name);
                    msgHeaders.put(name, value);
                }


                JsonObject msg = new JsonObject();
                msg.put("statusCode", backendResponse.statusCode());
                msg.put("statusMessage", backendResponse.statusMessage());
                msg.put("chunked", true);
                msg.put("headers", msgHeaders);

                backendResponse.bodyHandler(backendResponseBody -> {
                    msg.put("body", backendResponseBody.getBytes());
                    response.send(msg);
                });

//                backendResponse.endHandler(event -> {
//
//                });

            });

            JsonObject headers = body.getJsonObject("headers");
            Set<String> names = headers.fieldNames();
            for (String name : names) {
                String value = headers.getString(name);
                backendRequest.putHeader(name, value);
            }

            if(frontendRequestBody==null || frontendRequestBody.length==0) {
                backendRequest.end();
            }
            else {
                backendRequest.end(Buffer.buffer(frontendRequestBody));
            }
        });
    }

}
