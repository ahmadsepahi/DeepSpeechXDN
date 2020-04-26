package test2;

import okhttp3.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.io.File;
import java.nio.file.Files;

import javax.xml.bind.DatatypeConverter;


public class HttpClientTest {

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static void main(String[] args) throws IOException, JSONException {

        OkHttpClient httpClient = new OkHttpClient();

        String url = "http://localhost:8000";

        JSONObject json = new JSONObject();
        File file_upload = new File("test3.wav");
        byte[] bytes = Files.readAllBytes(file_upload.toPath());
        json.put("File", DatatypeConverter.printBase64Binary(bytes));
        //json.put("value", "1");

        System.out.println("JSON is "+json);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = httpClient.newCall(req).execute();
        System.out.println("Response:"+response.body().string());

        /*
        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        try(OutputStream os = con.getOutputStream()) {
            byte[] input = json.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
            os.flush();
        }

        int responseCode = con.getResponseCode();

        System.out.println("Response code:"+responseCode);
        */
    }
}
