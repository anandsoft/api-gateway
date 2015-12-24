package io.github.giovibal.api.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;

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



        HttpServer server = vertx.createHttpServer();

        // http://172.16.11.52:3000/api/2.0/config/graph?tenant=test.it
        HttpClientOptions opt = new HttpClientOptions()
                .setDefaultHost("172.16.11.52")
                .setDefaultPort(3000)
            ;

        HttpClient client = vertx.createHttpClient(opt);

        server.requestHandler(request -> {

            perfTimer.start();


//            // This handler gets called for each request that arrives on the server
//            HttpServerResponse response = request.response();
//            response.putHeader("content-type", "text/plain");
//
//            // Write to the response and end it
//            response.end("Hello World!");
            System.out.println(request.method()+" "+request.absoluteURI());



            HttpMethod method = request.method();
            String uri = request.uri();
            HttpClientRequest backendRequest = client.request(method, uri, backendResponse -> {

                // HEADERS
                HttpServerResponse response = request.response();
                MultiMap headers = backendResponse.headers();
                Set<String> names = headers.names();
                for (String name : names) {
                    List<String> values = headers.getAll(name);
                    response.putHeader(name, values);
                    System.out.println("HEADER fe << be: "+ name +" = "+ values);
                }

                // BODY
                response.setStatusCode(backendResponse.statusCode());
                response.setStatusMessage(backendResponse.statusMessage());
                backendResponse.handler(backendBuffer -> {
                    response.write(backendBuffer);
                });

                backendResponse.endHandler(event -> {
                    perfTimerBackend.checkpoint("backend time");
                    response.end();
                    perfTimer.checkpoint("total time");
                    perfTimer.printStats("gateway time", perfTimerBackend);
                });
            });

            // backend request headers
            MultiMap headers = request.headers();
            Set<String> names = headers.names();
            for (String name : names) {
                List<String> values = headers.getAll(name);
                backendRequest.putHeader(name, values);
                System.out.println("HEADER fe >> be: "+ name +" = "+ values);
            }

            // trigger backend request
            request.bodyHandler(requestBody -> {
                //requestBody
                perfTimerBackend.start();
                backendRequest.end(requestBody);
            });
        });

        server.listen(8080);
    }
}
