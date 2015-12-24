package io.github.giovibal.api.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Set;

/**
 * Created by Giovanni Baleani on 24/12/2015.
 */
public class Gateway extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(Gateway.class.getName());
    }

    private PerfTimer perfTimer = new PerfTimer();
    private PerfTimer perfTimerBackend = new PerfTimer();

    @Override
    public void start() throws Exception {

        String backendHost = "192.168.0.1";
        int backendPort = 80;

        // Prepare BACKEND
        JsonObject backendConf = new JsonObject()
                .put("defaultHost", backendHost)
                .put("defaultPort", backendPort)
                ;
        HttpClientOptions opt = new HttpClientOptions(backendConf)
//                .setSsl(true)
//                .setDefaultHost(backendHost)
//                .setDefaultPort(backendPort)
            ;

        HttpClient client = vertx.createHttpClient(opt);

        // Prepare FRONTEND
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(request -> {

            perfTimer.start();
            System.out.println(request.method()+" "+request.absoluteURI());

            HttpMethod method = request.method();
            String uri = request.uri();
            HttpClientRequest backendRequest = client.request(method, uri, backendResponse -> {
                HttpServerResponse response = request.response();
                copyHeadersFromBackendToFrontend(backendResponse, response);
                sendBackendRequestToFrontendRequest(backendResponse, response);
            });

            copyHeadersFromFrontendToBackend(request, backendRequest);
            sendFrontendRequestToBackendReqeust(request, backendRequest);
        });

        server.listen(8080);
    }

    // response: backend ---> frontend
    private void copyHeadersFromBackendToFrontend(HttpClientResponse from, HttpServerResponse to) {
        MultiMap headers = from.headers();
        Set<String> names = headers.names();
        for (String name : names) {
            List<String> values = headers.getAll(name);
            to.putHeader(name, values);
//            System.out.println("HEADER fe << be: "+ name +" = "+ values);
        }
    }
    private void sendBackendRequestToFrontendRequest(HttpClientResponse from, HttpServerResponse to) {
        to.setStatusCode(from.statusCode());
        to.setStatusMessage(from.statusMessage());
        to.setChunked(true);
        from.handler(backendBuffer -> {
            to.write(backendBuffer);
        });

        from.endHandler(event -> {
//            perfTimerBackend.checkpoint("backend time");
            perfTimerBackend.stop();
            to.end();
//            perfTimer.checkpoint("total time");
            perfTimer.stop();
            perfTimer.printStats("gateway time", perfTimerBackend);
        });
    }

    // request: frontend ---> backend
    private void copyHeadersFromFrontendToBackend(HttpServerRequest from, HttpClientRequest to) {
        MultiMap headers = from.headers();
        Set<String> names = headers.names();
        for (String name : names) {
            List<String> values = headers.getAll(name);
            to.putHeader(name, values);
//            System.out.println("HEADER fe >> be: "+ name +" = "+ values);
        }
    }
    private void sendFrontendRequestToBackendReqeust(HttpServerRequest from, HttpClientRequest to) {
        from.handler(frontendRequestedData -> {
            to.write(frontendRequestedData);
        });
        from.endHandler(event -> {
            perfTimerBackend.start();
            to.end();
        });
    }
}
