package edu.umass.cs.xdn.deprecated;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.AppRequestParserBytes;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.ReconfigurableAppClientAsync;
import edu.umass.cs.reconfiguration.examples.AppRequest;
import edu.umass.cs.reconfiguration.examples.noopsimple.NoopApp;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class a temporary class used by XDNHttpServer to interact with
 * XDNAgentApp (a GigaPaxos App).
 */
@Deprecated
public class XDNAgentClient extends ReconfigurableAppClientAsync<Request> implements AppRequestParserBytes {

    static int received = 0;

    public XDNAgentClient() throws IOException {
            super();
    }

    /*
    @Override
    public Request getRequest(String stringified) throws RequestParseException {
        try {
            return NoopApp.staticGetRequest(stringified);
        } catch (RequestParseException | JSONException e) {
            System.out.println(stringified);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Set<IntegerPacketType> getRequestTypes() {
        return NoopApp.staticGetRequestTypes();
    }
     */

    @Override
    public Request getRequest(String s) throws RequestParseException {
        try {
            JSONObject json = new JSONObject(s);
            return new HttpActiveReplicaRequest(json);
        } catch (JSONException e) {
            throw new RequestParseException(e);
        }
    }

    private static HttpActiveReplicaPacketType[] types = HttpActiveReplicaPacketType.values();

    @Override
    public Set<IntegerPacketType> getRequestTypes() {
        return new HashSet<>(Arrays.asList(types));
    }

    /**
     * This implementation is wrong
     * Coordinate a request with the value
     * @return
     */
    public boolean execute(String val, String serviceName) {
        AppRequest request = new AppRequest(serviceName, val, AppRequest.PacketType.DEFAULT_APP_REQUEST, false);

        try {
            // coordinate request through GigaPaxos
            this.sendRequest(request
                    , new RequestCallback() {
                @Override
                public void handleResponse(Request response) {
                     System.out.println("Response received:"+response);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            // request coordination failed
            return false;
        }
        return true;
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        XDNAgentClient client = new XDNAgentClient();

        String testServiceName = PaxosConfig.getDefaultServiceName();
        Map<String, InetSocketAddress> servers = PaxosConfig.getActives();

        if (args.length > 0) {
            testServiceName = args[0];
        }

        Set<InetSocketAddress> initGroup = new HashSet<>();
        for(String name: servers.keySet()){
            initGroup.add(servers.get(name));
        }

        final int sent = 1;

        /*
        client.sendRequest(new CreateServiceName(testServiceName,
                        "0", initGroup),
                new RequestCallback() {
                    final long createTime = System.currentTimeMillis();
                    @Override
                    public void handleResponse(Request response) {
                        System.out.println("Response to create service name ="
                        + (response)
                        + " received in "
                        + (System.currentTimeMillis() - createTime)
                        + "ms");
                        received += 1;
                    }
                }
        );

        while (sent < received) {
            Thread.sleep(500);
        }
         */

        JSONObject json = new JSONObject();
        try {
            json.put("value", "1");
            json.put("id", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        client.execute(json.toString(), testServiceName);

        System.out.println("ServiceName:"+testServiceName);

        Thread.sleep(2000);

        System.out.println("Done.");
    }
}

