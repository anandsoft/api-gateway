package io.github.giovibal.api.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
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
        Vertx vertx = Vertx.vertx();
//        vertx.deployVerticle(Gateway.class.getName());

        JsonObject frontendConf = new JsonObject()
                .put("port", 8080)
                .put("compressionSupported", true)
                .put("tcpKeepAlive", true)
                .put("acceptBacklog", 1024)
                .put("receiveBufferSize", 1024)
                .put("backend", "backend.test")
                ;

        JsonObject backendConf = new JsonObject()
                .put("defaultHost", "192.168.0.1")
                .put("defaultPort", 80)
                .put("backend", "backend.test")
                ;

        vertx.deployVerticle(Backend.class.getName(), new DeploymentOptions().setConfig(backendConf));
        vertx.deployVerticle(Frontend.class.getName(), new DeploymentOptions().setConfig(frontendConf));
    }

    private PerfTimer perfTimer = new PerfTimer();
    private PerfTimer perfTimerBackend = new PerfTimer();

    @Override
    public void start() throws Exception {

        // Prepare BACKEND
        JsonObject backendConf = new JsonObject()
                .put("defaultHost", "192.168.0.1")
                .put("defaultPort", 80)
                ;
        HttpClientOptions backendOpt = new HttpClientOptions(backendConf);
        HttpClient backend = vertx.createHttpClient(backendOpt);


        // Prepare FRONTEND
        JsonObject frontendConf = new JsonObject()
                .put("port", 8080)
                .put("compressionSupported", true)
                .put("acceptBacklog", 1024)
                .put("receiveBufferSize", 1024)
                ;
        HttpServerOptions frontendOpt = new HttpServerOptions(frontendConf);
        HttpServer frontend = vertx.createHttpServer(frontendOpt);
        frontend.requestHandler(request -> {

            perfTimer.start();

            HttpMethod method = request.method();
            String uri = request.uri();
            System.out.println(method+" "+uri);

            HttpClientRequest backendRequest = backend.request(method, uri, backendResponse -> {
                HttpServerResponse response = request.response();

                copyHeadersFromBackendToFrontend(backendResponse, response);
                sendBackendRequestToFrontendRequest(backendResponse, response);
            });

            copyHeadersFromFrontendToBackend(request, backendRequest);
            sendFrontendRequestToBackendReqeust(request, backendRequest);
        });

        frontend.listen();
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

        from.handler(to::write);
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
        from.handler(to::write);
        from.endHandler(event -> {
            perfTimerBackend.start();
            to.end();
        });
    }
}
