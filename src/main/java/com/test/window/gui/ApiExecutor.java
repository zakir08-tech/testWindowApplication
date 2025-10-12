package com.test.window.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiExecutor {

    public static class Auth {
        private final String type;
        private final String username;
        private final String password;
        private final String token;

        public Auth(String type, String username, String password, String token) {
            this.type = type;
            this.username = username;
            this.password = password;
            this.token = token;
        }

        public String getType() {
            return type;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getToken() {
            return token;
        }
    }

    public static class Response {
        private final int statusCode;
        private final String body;

        public Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }
    }

    public Response executeTest(
            String method,
            String url,
            HashMap<String, Object> headers,
            HashMap<String, Object> params,
            String payload,
            String payloadType,
            HashMap<String, Object> modifyPayload,
            Auth auth,
            boolean sslValidation) throws Exception {

        System.out.println("Debug: Inside executeTest - Payload received: " + payload);
        return executeRequest(method, url, headers, params, payload, payloadType, auth, sslValidation);
    }

    private List<NameValuePair> parseUrlEncodedString(String encodedPayload) throws Exception {
        List<NameValuePair> params = new ArrayList<>();
        if (encodedPayload == null || encodedPayload.trim().isEmpty()) {
            return params;
        }
        String[] pairs = encodedPayload.split("&");
        for (String pairStr : pairs) {
            if (pairStr.isEmpty()) continue;
            int idx = pairStr.indexOf('=');
            if (idx == -1) {
                params.add(new BasicNameValuePair(URLDecoder.decode(pairStr, "UTF-8"), ""));
            } else {
                String key = URLDecoder.decode(pairStr.substring(0, idx), "UTF-8");
                String value = URLDecoder.decode(pairStr.substring(idx + 1), "UTF-8");
                params.add(new BasicNameValuePair(key, value));
            }
        }
        return params;
    }

    public Response executeRequest(
            String method,
            String url,
            HashMap<String, Object> headers,
            HashMap<String, Object> params,
            String payload,
            String payloadType,
            Auth auth,
            boolean sslValidation) throws Exception {

        System.out.println("Debug: Inside executeRequest - Payload being sent: " + payload);

        CloseableHttpClient client;
        if (!sslValidation) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }}, new SecureRandom());
            client = HttpClients.custom().setSSLContext(sslContext).build();
        } else {
            client = HttpClients.createDefault();
        }

        StringBuilder urlWithParams = new StringBuilder(url);
        if (params != null && !params.isEmpty()) {
            urlWithParams.append("?");
            for (Map.Entry<String, Object> param : params.entrySet()) {
                String paramValue = param.getValue() != null ? param.getValue().toString() : "";
                urlWithParams.append(param.getKey()).append("=").append(paramValue).append("&");
            }
            if (urlWithParams.length() > 0) {
                urlWithParams.deleteCharAt(urlWithParams.length() - 1);
            }
        }

        HttpRequestBase request;
        switch (method.toUpperCase()) {
            case "GET":
                request = new HttpGet(urlWithParams.toString());
                break;
            case "POST":
                request = new HttpPost(urlWithParams.toString());
                break;
            case "PUT":
                request = new HttpPut(urlWithParams.toString());
                break;
            case "DELETE":
                request = new HttpDelete(urlWithParams.toString());
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        if (headers != null) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String headerValue = header.getValue() != null ? header.getValue().toString() : "";
                request.addHeader(header.getKey(), headerValue);
            }
        }

        if (auth != null && auth.getType() != null) {
            if (auth.getType().equalsIgnoreCase("Basic Auth")) {
                String credentials = auth.getUsername() + ":" + auth.getPassword();
                String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
                request.addHeader("Authorization", "Basic " + encodedCredentials);
            } else if (auth.getType().equalsIgnoreCase("Bearer Token")) {
                request.addHeader("Authorization", "Bearer " + auth.getToken());
            }
        }

        if (payload != null && !payload.trim().isEmpty() && (request instanceof HttpEntityEnclosingRequestBase)) {
            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
            String lowerPayloadType = payloadType != null ? payloadType.toLowerCase() : "";
            if ("urlencoded".equals(lowerPayloadType)) {
                List<NameValuePair> formParams = parseUrlEncodedString(payload);
                entityRequest.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
                System.out.println("Debug: Set URL-encoded form entity with params: " + formParams);
            } else if ("formdata".equals(lowerPayloadType) || "multipart".equals(lowerPayloadType)) {
                List<NameValuePair> formParams = parseUrlEncodedString(payload);
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                for (NameValuePair nv : formParams) {
                    builder.addTextBody(nv.getName(), nv.getValue(), ContentType.TEXT_PLAIN);
                }
                entityRequest.setEntity(builder.build());
                System.out.println("Debug: Set multipart form entity with params: " + formParams);
            } else {
                ContentType contentType;
                switch (lowerPayloadType) {
                    case "json":
                        contentType = ContentType.APPLICATION_JSON;
                        break;
                    case "xml":
                        contentType = ContentType.APPLICATION_XML;
                        break;
                    case "text":
                        contentType = ContentType.TEXT_PLAIN;
                        break;
                    default:
                        contentType = ContentType.APPLICATION_JSON;
                        break;
                }
                entityRequest.setEntity(new StringEntity(payload, contentType));
                System.out.println("Debug: Set string entity with Content-Type: " + contentType + " for payload: " + payload);
            }
        } else {
            System.out.println("Debug: No payload set in HTTP request (payload is null or empty, or method is not POST/PUT)");
        }

        try (CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity) : "";
            return new Response(statusCode, responseBody);
        } finally {
            client.close();
        }
    }

    public static String toPrettyJson(Response response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Object json = mapper.readValue(response.getBody(), Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            return response.getBody();
        }
    }
}