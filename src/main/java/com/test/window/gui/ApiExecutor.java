package com.test.window.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Modern and robust API executor that accepts a pre-configured HttpClient
 * (for mTLS, SSL bypass, etc.) while supporting all payload types and auth.
 */
public class ApiExecutor {

    // ====================== AUTH & RESPONSE CLASSES ======================
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

        public String getType() { return type; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getToken() { return token; }
    }

    public static class Response {
        private final int statusCode;
        private final String body;
        private final long responseTimeMs;

        public Response(int statusCode, String body, long responseTimeMs) {
            this.statusCode = statusCode;
            this.body = body != null ? body : "";
            this.responseTimeMs = responseTimeMs;
        }

        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public long getResponseTimeMs() { return responseTimeMs; }
    }
    // =====================================================================

    /**
     * Main method used by RunApiTest – accepts a pre-built HttpClient
     * (with mTLS, SSL disabled, or normal HTTPS configured).
     */
    public Response execute(
            String method,
            String url,
            Map<String, Object> headers,
            Map<String, Object> params,
            String payload,
            String payloadType,
            Auth auth,
            CloseableHttpClient client) throws Exception {

        if (client == null) {
            throw new IllegalArgumentException("HttpClient must not be null");
        }

        String finalUrl = buildUrlWithParams(url, params);
        HttpRequestBase request = createRequest(method, finalUrl);

        // Add headers
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (v != null) request.addHeader(k, v.toString());
            });
        }

        // Add authentication
        if (auth != null) {
            if ("Basic Auth".equalsIgnoreCase(auth.getType())) {
                String creds = auth.getUsername() + ":" + auth.getPassword();
                String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
                request.addHeader("Authorization", "Basic " + encoded);
            } else if ("Bearer Token".equalsIgnoreCase(auth.getType())) {
                request.addHeader("Authorization", "Bearer " + auth.getToken());
            }
        }

        // Set payload
        if (payload != null && !payload.trim().isEmpty() && request instanceof HttpEntityEnclosingRequestBase) {
            HttpEntity entity = buildEntity(payload, payloadType);
            ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
        }

        long start = System.currentTimeMillis();
        try (CloseableHttpResponse httpResponse = client.execute(request)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity responseEntity = httpResponse.getEntity();
            String body = responseEntity != null ? EntityUtils.toString(responseEntity, StandardCharsets.UTF_8) : "";
            long time = System.currentTimeMillis() - start;
            return new Response(statusCode, body, time);
        }
    }

    private String buildUrlWithParams(String baseUrl, Map<String, Object> params) {
        if (params == null || params.isEmpty()) return baseUrl;

        StringBuilder url = new StringBuilder(baseUrl);
        boolean first = !baseUrl.contains("?");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() == null) continue;
            if (first) {
                url.append("?");
                first = false;
            } else {
                url.append("&");
            }
            url.append(urlEncode(entry.getKey()))
               .append("=")
               .append(urlEncode(entry.getValue().toString()));
        }
        return url.toString();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }

    private HttpRequestBase createRequest(String method, String url) {
        return switch (method.toUpperCase()) {
            case "GET" -> new HttpGet(url);
            case "POST" -> new HttpPost(url);
            case "PUT" -> new HttpPut(url);
            case "DELETE" -> new HttpDelete(url);
            case "PATCH" -> new HttpPatch(url);
            case "HEAD" -> new HttpHead(url);
            case "OPTIONS" -> new HttpOptions(url);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
    }

    private HttpEntity buildEntity(String payload, String payloadType) throws Exception {
        String type = payloadType != null ? payloadType.toLowerCase().trim() : "";

        return switch (type) {
            case "urlencoded", "form" -> {
                List<NameValuePair> pairs = new ArrayList<>();
                for (String pair : payload.split("&")) {
                    if (pair.isEmpty()) continue;
                    String[] kv = pair.split("=", 2);
                    String key = kv.length > 0 ? kv[0] : "";
                    String value = kv.length > 1 ? kv[1] : "";
                    pairs.add(new BasicNameValuePair(key, value));
                }
                yield new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
            }

            case "multipart", "formdata" -> {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                for (String pair : payload.split("&")) {
                    if (pair.isEmpty()) continue;
                    String[] kv = pair.split("=", 2);
                    String key = kv.length > 0 ? kv[0] : "";
                    String value = kv.length > 1 ? kv[1] : "";
                    builder.addTextBody(key, value, ContentType.TEXT_PLAIN);
                }
                yield builder.build();
            }

            default -> {
                ContentType contentType = switch (type) {
                    case "xml" -> ContentType.APPLICATION_XML;
                    case "text", "plain" -> ContentType.TEXT_PLAIN;
                    case "html" -> ContentType.TEXT_HTML;
                    default -> ContentType.APPLICATION_JSON;
                };
                yield new StringEntity(payload, contentType.withCharset(StandardCharsets.UTF_8));
            }
        };
    }

    // Optional: Pretty print JSON response
    public static String toPrettyJson(Response response) {
        if (response == null || response.getBody() == null || response.getBody().trim().isEmpty()) {
            return "";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(response.getBody(), Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            return response.getBody(); // Not JSON, return raw
        }
    }
}