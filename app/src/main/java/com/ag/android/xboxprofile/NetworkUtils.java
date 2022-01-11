package com.ag.android.xboxprofile;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    public static Response getUrl(String urlSpec, Map<String, String> postData, Map<String, String> reqHeaders) throws IOException {
        reqHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        return getUrl(urlSpec, getPostDataString(postData), reqHeaders);
    }

    public static Response getUrl(String urlSpec, String postData, Map<String, String> reqHeaders) throws IOException {
        Log.d(TAG, "getting url: " + urlSpec);

        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Response response = new Response();

        try {
            connection.setInstanceFollowRedirects(false);

            if (reqHeaders != null) {
                for (String hName: reqHeaders.keySet()) {
                    connection.setRequestProperty(hName, reqHeaders.get(hName));
                }

                //Log.d(TAG, reqHeaders.toString());
            }


            if (postData != null) {
                connection.setRequestMethod("POST");

                OutputStream outRemote = new BufferedOutputStream(connection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outRemote, StandardCharsets.UTF_8));
                writer.write(postData);
                writer.flush();
                writer.close();
                outRemote.close();
            }

            if (connection.getResponseCode() >= 400) {
                Log.w(TAG, "response code: " + connection.getResponseCode());
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            response.setStatusCode(connection.getResponseCode());
            response.setHeaders(connection.getHeaderFields());

            InputStream in = connection.getInputStream();
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            int bytesRead;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                outBytes.write(buffer, 0, bytesRead);
            }
            outBytes.close();
            in.close();

            response.setData(outBytes.toByteArray());
            return response;

        } finally {
            connection.disconnect();
        }
    }

    public static Response getUrl(String urlSpec) throws IOException {
        return getUrl(urlSpec, (String)null, null);
    }


    private static String getPostDataString(Map<String, String> postDataParams) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : postDataParams.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }


    public static class Response {
        private int mStatusCode;
        private byte[] mData;
        private Map<String, List<String>> mHeaders;

        public Response() {
            mHeaders = new HashMap<>();
        }


        public int getStatusCode() {
            return mStatusCode;
        }

        public void setStatusCode(int statusCode) {
            mStatusCode = statusCode;
        }

        public byte[] getData() {
            return mData;
        }

        public void setData(byte[] data) {
            mData = data;
        }

        public Map<String, List<String>> getHeaders() {
            return mHeaders;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            mHeaders = headers;
        }

    }
}