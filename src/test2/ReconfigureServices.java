package test2;

import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ReplicableClientRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.nio.file.Files;

/**
 * java -ea -cp jar/XDN-1.0.jar -Djava.util.logging.config.file=conf/logging.properties -Dlog4j.configuration=conf/log4j.properties -DgigapaxosConfig=conf/xdn.properties test.ReconfigurableServices
 */
public class ReconfigureServices {
    static int received = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        XDNAgentClient client = new XDNAgentClient();

        String testServiceName = "deepspeech_orig"+ XDNConfig.xdnServiceDecimal+"Alvin";

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
        final int total = 100;
        int id = 0;

        for (int i=0; i<1000; i++) {
            int sent = 1;
            HttpActiveReplicaRequest req = new HttpActiveReplicaRequest(HttpActiveReplicaPacketType.EXECUTE,
                    testServiceName,
                    id++,
                    json.toString(),
                    true,
                    false,
                    0
            );
            // AppRequest request = new AppRequest(testServiceName, json.toString(), AppRequest.PacketType.DEFAULT_APP_REQUEST, false);
            // System.out.println("About to send "+i+"th request.");

            long start = System.nanoTime();
            try {
                // coordinate request through GigaPaxos
                client.sendRequest(ReplicableClientRequest.wrap(req)
                        , new RequestCallback() {
                            @Override
                            public void handleResponse(Request response) {
                                System.out.println((System.nanoTime()-start)/1000.0);
                                received = 1;
                            }
                        });

            } catch (IOException e) {
                e.printStackTrace();
                // request coordination failed

            }
            while (received < sent ) {
                Thread.sleep(500);
            }
            received = 0;
        }

        client.close();

    }
}
