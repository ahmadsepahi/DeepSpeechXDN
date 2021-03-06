package test;

import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.examples.AppRequest;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ReplicableClientRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

/**
 * java -ea -cp jar/XDN-1.0.jar -Djava.util.logging.config.file=conf/logging.properties -Dlog4j.configuration=conf/log4j.properties -DgigapaxosConfig=conf/xdn.properties test.ExecuteServices
 */
public class ExecuteServices {
    static int received = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        XDNAgentClient client = new XDNAgentClient();
        String testServiceName = "xdn-demo-app"+ XDNConfig.xdnServiceDecimal+"Alvin";

        Integer addValue = 1;
        final int total = 1;
        int id = (new Random()).nextInt();

        // System.out.println("Start testing... ");
        for (int i=0; i<total; i++) {
            int sent = 1;
            HttpActiveReplicaRequest req = new HttpActiveReplicaRequest(HttpActiveReplicaPacketType.EXECUTE,
                    testServiceName,
                    id++,
                    addValue.toString(),
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
