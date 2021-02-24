package ru.netology.server.request;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {
    private final static int READ_AHEAD_LIMIT = 4096;
    private final static String GET = "GET";
    private final static String POST = "POST";
    private final static String HEADER_CONTENT_LENGTH = "Content-Length";

    private final String method;
    private final String urlPath;
    private final String path;
    // may be null
    private final Map<String, String> querryStringParams;
    private final Map<String, String> querryStringBodyParams;
    private final Map<String, String> headers;
    private final byte[] body;

    public Request(String method, String urlPath, String path,
                   Map<String, String> querryStringParams,
                   Map<String, String> querryStringBodyParams,
                   Map<String, String> headers,
                   byte[] body) {
        this.method = method;
        this.urlPath = urlPath;
        this.path = path;
        this.querryStringParams = querryStringParams;
        this.querryStringBodyParams = querryStringBodyParams;
        this.headers = headers;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getQuerryStringParams() {
        return querryStringParams;
    }

    public Map<String, String> getQuerryStringBodyParams() {
        return querryStringBodyParams;
    }

    public String getUrlPath() {
        return urlPath;
    }


    public static Request fromInputStream(InputStream in) throws IOException, URISyntaxException {
        in.mark(READ_AHEAD_LIMIT);
        byte[] buffer = new byte[READ_AHEAD_LIMIT];
        int read = in.read(buffer);

        //requestLine
        byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        if (requestLineEnd == -1) {
            // just close socket
            throw new IOException("ru.netology.server.request.Request is invalid");
        }

        String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            // just close socket
            throw new IOException("ru.netology.server.request.Request is invalid");
        }

        // method
        String method = requestLine[0];

        // path with querryParams
        String urlPath = requestLine[1];

        if (!urlPath.startsWith("/")) {
            // just close socket
            throw new IOException("ru.netology.server.request.Request is invalid");
        }

        // querryParams
        Map<String, String> queryStringParams = getQueryParamsFromUrl(urlPath);

        // path without querryParams
        String path = queryStringParams.isEmpty() ? urlPath : urlPath.substring(0, urlPath.indexOf("?"));

        //headers
        byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        int headersStart = requestLineEnd + requestLineDelimiter.length;

        int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);

        if (headersEnd == -1) {
            // just close socket
            throw new IOException("ru.netology.server.request.Request is invalid");
        }

        in.reset();
        in.skip(headersStart);

        byte[] headersBytes = in.readNBytes(headersEnd - headersStart);

        List<String> listOfHeaders = Arrays.asList(new String(headersBytes).split("\r\n"));
        Map<String, String> headers = new HashMap<>();

        listOfHeaders.forEach(headerLine -> {
            var i = headerLine.indexOf(":");
            var headerName = headerLine.substring(0, i);
            var headerValue = headerLine.substring(i + 2);
            headers.put(headerName, headerValue);
        });


        //Body
        byte[] body = null;
        Map<String, String> querryStringBodyParams = null;


        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);

            int bodyLength = in.available();

            if (bodyLength!=0) body = in.readNBytes(bodyLength);

            querryStringBodyParams = getQueryParamsFromBody(body);

            System.out.println(querryStringBodyParams);


//            String contentLength = headers.get(HEADER_CONTENT_LENGTH);
//
//            if (!(contentLength == null)) {
//                int length = Integer.parseInt(contentLength);
//                body = in.readNBytes(length);
//                querryStringBodyParams = getQueryParamsFromBody(body);
//            }
        }

        return new Request(method, urlPath, path, queryStringParams, querryStringBodyParams, headers, body);
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

//    private static Optional<String> extractHeader(Map<String, String> headers, String header) {
//        return headers.keySet().stream()
//                .filter(o -> o.startsWith(header))
//                .map(o -> o.substring(o.indexOf(" ")))
//                .map(String::trim)
//                .findFirst();
//    }

    public static Map<String, String> getQueryParamsFromUrl(String url) throws URISyntaxException {
        List<NameValuePair> params = URLEncodedUtils.parse(new URI(url), String.valueOf(StandardCharsets.UTF_8));

        Map<String, String> querryParams = new HashMap<>();

        if (!params.isEmpty()) {
            params.forEach(param -> querryParams.put(param.getName(), param.getValue()));
        }

        return querryParams;
    }

    public String getQuerryParam(String querryParamName) {
        return querryStringParams.get(querryParamName);
    }

    public static Map<String, String> getQueryParamsFromBody(byte[] body) {
        List<String> listOfHeaders = Arrays.asList(new String(body).split("\r\n"));

        Map<String, String> headers = new HashMap<>();

        listOfHeaders.forEach(headerLine -> {
            var i = headerLine.indexOf("=");
            var headerName = headerLine.substring(0, i);
            var headerValue = headerLine.substring(i + 1);
            headers.put(headerName, headerValue);
        });
        return headers;
    }
}


