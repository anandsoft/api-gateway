package io.github.giovibal.api.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

import java.util.Set;

/**
 * Created by giovibal on 24/12/15.
 */
public class Frontend extends AbstractVerticle {


    @Override
    public void start() throws Exception {
        // Prepare FRONTEND
//        JsonObject frontendConf = new JsonObject()
//                .put("port", 8080)
//                .put("compressionSupported", true)
//                .put("acceptBacklog", 1024)
//                .put("receiveBufferSize", 1024)
//                ;

        JsonObject frontendConf = config();

        String backendAddress = frontendConf.getString("backend");

        HttpServerOptions frontendOpt = new HttpServerOptions(frontendConf).setHandle100ContinueAutomatically(true);
        HttpServer frontend = vertx.createHttpServer(frontendOpt);

        frontend.requestHandler(request -> {

            HttpServerResponse response = request.response();

            HttpMethod method = request.method();
            String uri = request.uri();
            System.out.println(method+" "+uri);

            JsonObject msgHeaders = new JsonObject();
            MultiMap headers = request.headers();
            Set<String> names = headers.names();
            for (String name : names) {
                String value = headers.get(name);
                msgHeaders.put(name, value);
            }

            JsonObject msg = new JsonObject();
            msg.put("method", method.name());
            msg.put("uri", uri);
            msg.put("headers", msgHeaders);

            request.bodyHandler(body -> {
                msg.put("body", body.getBytes());

                MessageProducer<JsonObject> mp = vertx.eventBus().sender(backendAddress);
                mp.send(msg, (AsyncResult<Message<JsonObject>> replyHander) -> {
                    Message<JsonObject> reply = replyHander.result();
                    JsonObject backendResponse = reply.body();

                    int statusCode = backendResponse.getInteger("statusCode");
                    String statusMessage = backendResponse.getString("statusMessage");
                    boolean chinked = backendResponse.getBoolean("chunked");
                    JsonObject backendHeaders = backendResponse.getJsonObject("headers");
                    byte[] backendBody = backendResponse.getBinary("body");

                    response.setStatusCode(statusCode);
                    response.setStatusMessage(statusMessage);
                    response.setChunked(chinked);

                    Set<String> bnames = backendHeaders.fieldNames();
                    for (String bname : bnames) {
                        String bvalue = backendHeaders.getString(bname);
                        request.response().putHeader(bname, bvalue);
                    }

                    if (backendBody == null || backendBody.length==0) {
                        response.end();
                        System.out.println("response end.");
                    } else {
                        response.end(Buffer.buffer(backendBody));
                        System.out.println("response sent to client with content size: "+ backendBody.length+" "+ response.bytesWritten());
                    }
                });
            });

            response.closeHandler(event -> {
                System.out.println("response closed: "+ response.ended());
            });

        });

        frontend.listen();
    }


}
