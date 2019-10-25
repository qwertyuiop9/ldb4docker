package com.andreavendrame.ldb4docker.editor;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = "/test")
public class RestTest {

    private static final boolean SHOW_REQUEST_OUTPUT = false;
    private static final String GET_REQUEST = "GET";
    private static final String POST_REQUEST = "POST";

    private List<MyRequest> testRequests = new LinkedList<>();

    @RequestMapping(value = "/executeRequestsList")
    private void executeRequestList() {

        loadRequestList();  // Loading the requested HTTP request list

        for (MyRequest request : testRequests) {
            executeRequest(request);
        }

    }

    @RequestMapping(value = "/execute")
    private void executeRequest(MyRequest httpRequest) {

        URL url = null;
        try {
            url = new URL(httpRequest.requestBody);
        } catch (MalformedURLException e) {
            System.out.format("Richiesta %s fallita. Causa URL MAL FORMATO...\n", httpRequest);
            e.printStackTrace();
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
        } catch (IOException e) {
            System.out.format("Richiesta %s fallita. Causa ERRORE APERTURA CONNESSIONE...\n", httpRequest);
            e.printStackTrace();
        }
        try {
            switch (httpRequest.requestMethod) {
                case GET_REQUEST:
                    Objects.requireNonNull(connection).setRequestMethod(GET_REQUEST);
                    break;
                case POST_REQUEST:
                    Objects.requireNonNull(connection).setRequestMethod(POST_REQUEST);
                    break;
                default:
                    Objects.requireNonNull(connection).setRequestMethod(GET_REQUEST);
                    break;
            }

        } catch (ProtocolException e) {
            System.out.format("Richiesta %s fallita. Causa REQUEST METHOD errato...\n", httpRequest);
            e.printStackTrace();
        }
        int responseCode = -1;
        try {
            responseCode = Objects.requireNonNull(connection).getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.format("Richiesta: %s terminata con codice %d\n", httpRequest.requestBody, responseCode);
    }

    private void loadRequestList() {

        // Creating builder controls and initializing it
        this.testRequests.add(new MyRequest("http://localhost:8081/editor/directedControls?controlName=container&arityIn=1&arityOut=0&active=true", POST_REQUEST));
        this.testRequests.add(new MyRequest("http://localhost:8081/editor/directedControls?controlName=network&arityIn=0&arityOut=2&active=true", POST_REQUEST));
        this.testRequests.add(new MyRequest("http://localhost:8081/editor/directedControls?controlName=volume&arityIn=0&arityOut=2&active=true", POST_REQUEST));
        this.testRequests.add(new MyRequest("http://localhost:8081/editor/directedSignatures", POST_REQUEST));
        this.testRequests.add(new MyRequest("http://localhost:8081/editor/start-editing", POST_REQUEST));
        // Editing the builder by adding ports, names, links ecc...
        this.testRequests.add(new MyRequest("http://localhost:8081/editor/roots", POST_REQUEST));
        this.testRequests.add(new MyRequest("http://localhost:8081/editor/nodes?controlName=container&parentType=root&parentName=0:r", POST_REQUEST));
        //this.testRequests.add(new MyRequest("", ""));
    }

    private static class MyRequest {

        final String requestBody;
        final String requestMethod;

        MyRequest(String requestBody, String requestMethod) {
            this.requestBody = requestBody;
            this.requestMethod = requestMethod;
        }
    }

}
