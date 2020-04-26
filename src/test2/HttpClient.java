package test2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.File;
import java.nio.file.Files;


import javax.xml.bind.DatatypeConverter;



/**
 * java -ea -cp jar/XDN-1.0.jar -Djava.util.logging.config.file=conf/logging.properties -Dlog4j.configuration=conf/log4j.properties -DgigapaxosConfig=conf/xdn.properties test.HttpClient
 */
public class HttpClient {
    private static final String USER_AGENT = "Mozilla/5.0";

    private static String TEST_URL = "http://localhost:8000";

    private static void get() throws IOException {
        URL obj = new URL(TEST_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();
        System.out.println("GET Response Code :: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ( (inputLine = in.readLine()) != null ) {
                response.append(inputLine);
            }
            in.close();

            // print result
            System.out.println(response.toString());
        } else {
            System.out.println("GET request not worked");
        }
    }

    private static void post() throws IOException {
        URL obj = new URL(TEST_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);

        File file_upload = new File("test3.wav");
        JSONObject json = new JSONObject();
        try {
            byte[] bytes = Files.readAllBytes(file_upload.toPath());
            json.put("File", DatatypeConverter.printBase64Binary(bytes));
            //json.put("value", "1");
            //json.put("id", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // For POST only - START
        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        os.write(json.toString().getBytes());
        os.flush();
        os.close();
        // For POST only - END

        int responseCode = con.getResponseCode();
        // System.out.println("POST Response Code :: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { //success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            // System.out.println(response.toString());
        } else {
            // System.out.println("POST request not worked");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length >= 1){
            TEST_URL = args[0];
        }

        for (int i =0; i<1100; i++) {
            long start = System.nanoTime();
            post();
            long elapsed = System.nanoTime() - start;
            System.out.println(elapsed/1000.0);
        }
    }
}
