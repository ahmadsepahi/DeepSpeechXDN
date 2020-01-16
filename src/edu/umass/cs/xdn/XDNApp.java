package edu.umass.cs.xdn;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.AppRequestParserBytes;
import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.paxosutil.LargeCheckpointer;
import edu.umass.cs.nio.JSONPacket;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.examples.AbstractReconfigurablePaxosApp;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.interfaces.Reconfigurable;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import edu.umass.cs.xdn.docker.DockerKeys;
import edu.umass.cs.xdn.docker.DockerContainer;
import edu.umass.cs.xdn.util.ProcessResult;
import edu.umass.cs.xdn.util.ProcessRuntime;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *  XDNApp is an umbrella application.
 *
 */
public class XDNApp extends AbstractReconfigurablePaxosApp<String>
        implements Replicable, Reconfigurable, AppRequestParserBytes {

    // private String myID;

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    // private static String containerUrl;

    // used to propagate coordinated result to applications
    private static OkHttpClient httpClient;

    /**
     * A map of app name to containerizedApps
     * FIXME: use Service interface rather than DockerService if we decide to switch to another container
     */
    private Map<String, DockerContainer> containerizedApps;

    /**
     * A map of service name to container app name
     */
    private Map<String, String> serviceNames;

    private Set<String> runningApps;

    private String gatewayIPAddress;

    private enum XDNAppKeys {
        APP,
        SERVICE_NAME
    }

    /**
     * 
     */
    public XDNApp() {
        httpClient = new OkHttpClient();

        gatewayIPAddress = "localhost";
        if (System.getProperty("gateway") != null)
            // TODO: we can also get this from docker command directly
            gatewayIPAddress = System.getProperty("gateway");

        // containerUrl = "http://" + cAddr + XDNConfig.xdnRoute;

        // System.out.println("Container URL is:"+containerUrl);

        containerizedApps = new HashMap<>();
        // avoid throwing an exception when bootup
        containerizedApps.put(PaxosConfig.getDefaultServiceName(),
                new DockerContainer(PaxosConfig.getDefaultServiceName(),
                        null, -1, null));
        // FIXME change HashSet to a sorted list to track resource usage
        runningApps = new HashSet<>();

        serviceNames = new HashMap<>();

        List<String> whoCommand = new ArrayList<>();
        whoCommand.add("whoami");
        ProcessResult result = null;
        try {
            result = ProcessRuntime.executeCommand(whoCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        assert (result != null);

        if ( !result.getResult().equals("root") && XDNConfig.largeCheckPointerEnabled ) {
            // if largeCheckPointerEnabled is enabled but the program is not running with root privilege, log a severe error and exit, because checkpoint won't work.
            System.out.println("LargeCheckpointer is enabled, must run with root privilege.");
            System.exit(1);
        }

        // customized checkpoint directory does not work for criu restore,
        // use the default directory (/var/lib/docker/containers)
        /*
        File checkpointFolder = new File(XDNConfig.checkpointDir);
        if (!checkpointFolder.exists()) {
            boolean created = checkpointFolder.mkdir();
            if (!created)
                System.out.println(this+" failed to create checkpoint folder!");
        }
        */
    }

    private String getContainerUrl(String addr) {
        return "http://" + addr + XDNConfig.xdnRoute;
    }

    @Override
    public boolean execute(Request request,
                           boolean doNotReplyToClient) {
        if (request instanceof HttpActiveReplicaRequest) {
            HttpActiveReplicaRequest r = (HttpActiveReplicaRequest) request;
            String name = r.getServiceName();
            String containerUrl = null;
            if (serviceNames.containsKey(name) && containerizedApps.containsKey(serviceNames.get(name)))
                containerUrl = getContainerUrl(containerizedApps.get(serviceNames.get(name)).getAddr());

            if (containerUrl == null)
                return false;

            if ( HttpActiveReplicaPacketType.EXECUTE.equals(r.getRequestType()) ) {
                RequestBody body = RequestBody.create(JSON, request.toString());
                okhttp3.Request req = new okhttp3.Request.Builder()
                        .url(containerUrl)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(req).execute()) {
                    // System.out.println("Received response from nodejs app:"+response);
                    // System.out.println("Content:"+response.body().string());
                    ((HttpActiveReplicaRequest) request).setResponse(response.body() != null?
                            response.body().string():
                            "");
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            /*
            else if (HttpActiveReplicaPacketType.SNAPSHOT.equals(r.getRequestType())) {
                String name = r.getServiceName();
                List<String> command = getCheckpointCreateCommand(name);
                return run(command);
            } else if (HttpActiveReplicaPacketType.RECOVER.equals(r.getRequestType())) {
                String name = r.getServiceName();
                List<String> command = getRestoreCommand(name);
                return run(command);
            }*/

        }
        return false;
    }

    @Override
    public String checkpoint(String name) {
        if (name.equals(PaxosConfig.getDefaultServiceName())){
            // do nothing for the default app
            return "";
        }
        // handle checkpoint for XDNApp's default user name, which represents a unique device, e.g., XDNApp0_AlvinRouter
        else if (name.startsWith(PaxosConfig.getDefaultServiceName())){
            /*
              Checkpoint XDN Agent state of this specific node: serviceNames and containerizedApps.
              Though containerizedApps can be retrieved from docker command, it's better to keep a copy by XDN itself.
             */
            JSONObject xdnState = new JSONObject();
            JSONObject apps = new JSONObject();
            JSONObject services = new JSONObject();
            try {
                for (String appName: containerizedApps.keySet()){
                        apps.put(appName, containerizedApps.get(appName).toJSONObject());
                }
                xdnState.put(XDNAppKeys.APP.toString(), apps);

                for (String serviceName: serviceNames.keySet()) {
                    services.put(serviceName, serviceNames.get(serviceName));
                }
                xdnState.put(XDNAppKeys.SERVICE_NAME.toString(), services);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return xdnState.toString();
        }


        // Now let's handle app state and user state event
        if (XDNConfig.largeCheckPointerEnabled) {
            // use {@link LargeCheckpointer} to checkpoint
            String appName = name.split(XDNConfig.xdnServiceDecimal)[0];

            List<String> checkpointListCommand = getCheckpointListCommand(appName);
            ProcessResult result = null;
            try {
                result = ProcessRuntime.executeCommand(checkpointListCommand);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            assert (result != null);

            // checkpoint exists
            boolean exists = result.getResult().contains(appName);
            if (exists){
                // remove the previous checkpoint
                List<String> checkpointRemoveCommand = getCheckpointRemoveCommand(appName);

                if (! run(checkpointRemoveCommand) ){
                    // checkpoint has not been removed successfully
                    return null;
                }
            }

            List<String> checkpointCreateCommand = getCheckpointCreateCommand(appName, true);
            if (!run(checkpointCreateCommand))
                // checkpoint container failed
                return null;
            // assert(cp.exists());

            // Note: this only works with root privilege
            String image = XDNConfig.defaultCheckpointDir+containerizedApps.get(appName).getID()+"/checkpoints/"+appName;
            List<String> tarCommand = getTarCommand(appName+".tar.gz", image, XDNConfig.checkpointDir);
            assert(run(tarCommand));
            File cp = new File(XDNConfig.checkpointDir+appName+".tar.gz");

            return LargeCheckpointer.createCheckpointHandle(cp.getAbsolutePath());

        } else {
            // send checkpoint request to the underlying app
            String containerUrl = null;
            if (serviceNames.containsKey(name) && containerizedApps.containsKey(serviceNames.get(name)))
                containerUrl = getContainerUrl(containerizedApps.get(serviceNames.get(name)).getAddr());

            if (containerUrl == null)
                return null;

            // send checkpoint request to the underlying app
            JSONObject json = new JSONObject();
            try {
                json.put(HttpActiveReplicaRequest.Keys.NAME.toString(), name);
                json.put(JSONPacket.PACKET_TYPE.toUpperCase(), HttpActiveReplicaPacketType.SNAPSHOT.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestBody body = RequestBody.create(JSON, json.toString());
            okhttp3.Request req = new okhttp3.Request.Builder()
                    .url(containerUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(req).execute()) {
                System.out.println("Received response from nodejs app:"+response);
                // System.out.println("Content:"+response.body().string());

                return response.body()!=null? response.body().string(): null;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    /**
     * name is the serviceName, state is the state associated with the serviceName
     * If state is null, that means we are about to delete the serviceName.
     * Otherwise we need to restore the state for serviceName.
     */
    @Override
    public boolean restore(String name, String state) {
        // app name is the name before xdnServiceDecimal "_xdn_"
        String appName = name.split(XDNConfig.xdnServiceDecimal)[0];
        System.out.println("Name: "+name+"\nAppName: "+appName+"\nState: "+state);

        // handle XDN service restore
        if (name.equals(PaxosConfig.getDefaultServiceName())){
            // Don't do any thing, this is the default app
            return true;
        } else if (name.startsWith(PaxosConfig.getDefaultServiceName())){
            // this is the device service name, restore serviceNames and containerizedApps
            if (state == null) {
                // clean up the state of this XDN app
                containerizedApps.clear();
                serviceNames.clear();
            } else {
                try {
                    JSONObject xdnState = new JSONObject(state);
                    JSONObject apps = xdnState.getJSONObject(XDNAppKeys.APP.toString());
                    Iterator<?> iter = apps.keys();
                    while(iter.hasNext()) {
                        String containerizedAppName = iter.next().toString();
                        containerizedApps.put(containerizedAppName,
                                new DockerContainer(apps.getJSONObject(containerizedAppName)));
                    }

                    JSONObject services = xdnState.getJSONObject(XDNAppKeys.SERVICE_NAME.toString());
                    iter = services.keys();
                    while(iter.hasNext()){
                        String serviceName = iter.next().toString();
                        serviceNames.put(serviceName, services.getString(serviceName));
                    }

                    return true;
                } catch (JSONException e) {
                    // unable to process this restore event, quit and raise an error
                    return false;
                }
            }
        }

        // Handle serviceName (name) restore
        if (serviceNames.containsKey(name)){
            // if service name exists, app name must also exist
            assert(containerizedApps.containsKey(appName));
            // this is a registered service name
            DockerContainer container = containerizedApps.get(appName);
            assert (container != null);

            if ( state == null ){
                // If we need to remove name
                serviceNames.remove(name);
                // remove pointer from service name list in containerizedApps, must succeed
                boolean cleared = container.removeServiceName(name);
                // serviceName must be successfully removed from service list in container class
                assert (cleared);
                if (container.isEmpty()) {
                    // If service list is empty, we need to clean up the container state on this node

                    // Stop the container
                    List<String> stopCommand = getStopCommand(container.getName());
                    boolean stopped = run(stopCommand);
                    // container must be stopped successfully
                    assert (stopped);

                    // Remove the container's checkpoint
                    List<String> removeCheckpointCommand = getCheckpointRemoveCommand(container.getName());
                    boolean removed = run(removeCheckpointCommand);
                    assert (removed);

                    // We also need to remove the image
                    List<String> removeImageCommand = getRemoveImageCommand(container.getUrl());
                    removed = run(removeImageCommand);
                    assert (removed);

                    // Note: we must keep the docker info in containerizedApps
                    // Because the associated information can only be acquired upon service name creation
                    // containerizedApps.remove(appName);

                    runningApps.remove(appName);
                }
                return true;
            } else {
                // restore from a checkpoint
                if (XDNConfig.largeCheckPointerEnabled) {
                    /*
                    Here is the restore complexity:
                    Current version of docker (19.03) does not support restore from a customized directory.
                    Therefore, we have to copy it to the corresponding location of the docker image.
                    */

                    // If the instance is running, stop and prepare to restart
                    if (runningApps.contains(appName)) {
                        List<String> stopCommand = getStopCommand(appName);
                        assert (run(stopCommand));
                    }

                    String dest = XDNConfig.defaultCheckpointDir+containerizedApps.get(appName).getID()+"/checkpoints/";
                    String filename = XDNConfig.checkpointDir+appName+".tar.gz";
                    File cp = new File(filename);
                    LargeCheckpointer.restoreCheckpointHandle(state, cp.getAbsolutePath());
                    List<String> unTarCommand = getUntarCommand(filename, dest);
                    assert(run(unTarCommand));

                    // When largeCheckPointerEnabled is true, each user service is run in different containers
                    // This implies it won't work for a general scenario with the typical DNS service discovery process

                    List<String> startCommand = getStartCommand(appName);
                    assert (run(startCommand));
                    DockerContainer c = containerizedApps.get(appName);

                    updateServiceAndApps(appName, name, c);

                    return true;
                } else {
                    // send restore request to the underlying app
                    String containerUrl = null;
                    if (serviceNames.containsKey(name) && containerizedApps.containsKey(serviceNames.get(name)))
                        containerUrl = getContainerUrl(containerizedApps.get(serviceNames.get(name)).getAddr());

                    if (containerUrl == null)
                        return false;

                    // send checkpoint request to the underlying app
                    JSONObject json = new JSONObject();
                    try {
                        json.put(HttpActiveReplicaRequest.Keys.NAME.toString(), name);
                        json.put(JSONPacket.PACKET_TYPE.toUpperCase(), HttpActiveReplicaPacketType.RECOVER.toString());
                        json.put(HttpActiveReplicaRequest.Keys.QVAL.toString(), state);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    RequestBody body = RequestBody.create(JSON, json.toString());
                    okhttp3.Request req = new okhttp3.Request.Builder()
                            .url(containerUrl)
                            .post(body)
                            .build();

                    try (Response response = httpClient.newCall(req).execute()) {
                        System.out.println("Received response from nodejs app:"+response);
                        // System.out.println("Content:"+response.body().string());
                        return response.code()==200;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return false;
                }
            }

        } else {
            /*
             * This is not a registered service name, follow the steps to set up the service if the app does not exist yet
             * 1. Extract the initial service information:
             * docker image name on Docker hub, docker runtime command, file path, etc.
             * 2. Pull the image
             * 3. Boot up the service, if succeed, then add it to runtime manager and service map, return true.
             * 4. If service boot-up failed, try to terminate some running instance to yield some resources and retry.
             * If succeed, add the service to runtime manager and service map, return true.
             * If still unable to boot-up the service, return false.
             * 5. restore user state
             */
            if ( !containerizedApps.containsKey(appName) ) {
                try {
                    if (state == null)
                        // The first time to create a service name, state can not be null
                        return false;

                    // 1. Extract the initial service information
                    JSONObject json = new JSONObject(state);
                    int port = json.has(DockerKeys.PORT.toString()) ? json.getInt(DockerKeys.PORT.toString()) : -1;
                    String url = json.has(DockerKeys.IMAGE_URL.toString()) ? json.getString(DockerKeys.IMAGE_URL.toString()) : null;
                    JSONArray jEnv = json.has(DockerKeys.ENV.toString()) ? json.getJSONArray(DockerKeys.ENV.toString()) : null;
                    List<String> env = new ArrayList<>();
                    if (jEnv != null) {
                        for (int i = 0; i < jEnv.length(); i++) {
                            env.add(jEnv.getString(i));
                        }
                    }

                    // 2. Pull service and boot-up
                    List<String> pullCommand = getPullCommand(url);
                    boolean pulled = run(pullCommand);
                    if (!pulled)
                        return false;

                    // 3. Boot up the service
                    List<String> startCommand = getRunCommand(appName, port, env, url);
                    ProcessResult result = null;
                    try {
                        result = ProcessRuntime.executeCommand(startCommand);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (result == null) {
                        // there may be an interruption, let's retry. No need to stop another instance
                        try {
                            result = ProcessRuntime.executeCommand(startCommand);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        assert (result != null);
                        if (result.getRetCode() != 0) {
                            // give up and raise an error
                            return false;
                        } else {
                            DockerContainer container = new DockerContainer(appName, url, port, jEnv);
                            updateServiceAndApps(appName, name, container);
                            System.out.println(">>>>>>>>> Service name " + name + " has been created successfully after retry.");
                            return true;
                        }
                    } else {
                        if (result.getRetCode() != 0) {
                            // no enough resource, stop an unused container and retry
                            if (!selectAndStopContainer()){
                                // if no container is stopped , then give up and return an error
                                return false;
                            }
                            try {
                                result = ProcessRuntime.executeCommand(startCommand);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (result.getRetCode() == 0 ) {
                                DockerContainer container = new DockerContainer(appName, url, port, jEnv);
                                updateServiceAndApps(appName, name, container);
                                System.out.println(">>>>>>>>> Service name " + name + " has been created successfully after stop and retry.");
                                return true;
                            }
                            return false;
                        } else {
                            // String id = result.getResult().trim();
                            DockerContainer container = new DockerContainer(appName, url, port, jEnv);
                            updateServiceAndApps(appName, name, container);
                            System.out.println(">>>>>>>>> Service name " + name + " has been created successfully.");
                            return true;
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    return false;
                }
            } else if ( !runningApps.contains(appName) ) {
                // there is already an app instance, if it's not running, boot it up

            } else {
                // TODO: the app is running, then what to do?
            }
        }
        return true;
    }

    private void updateServiceAndApps(String appName, String name, DockerContainer container){
        if (container != null) {
            List<String> inspectCommand = getInspectCommand(appName);
            try {
                ProcessResult result = ProcessRuntime.executeCommand(inspectCommand);
                JSONArray arr = new JSONArray(result.getResult());
                JSONObject json = arr.getJSONObject(0);
                // "id"
                String id = json.getString("Id");
                container.setID(id);
                // "NetworkSettings" -> "IPAddress"
                String ipAddr = json.getJSONObject("NetworkSettings").getString("IPAddress");
                container.setAddr(ipAddr);
            } catch (IOException | InterruptedException | JSONException e) {
                e.printStackTrace();
            }
            container.addServiceName(name);
            containerizedApps.put(appName, container);
            runningApps.add(appName);
        }
        serviceNames.put(name, appName);
    }

    private boolean selectAndStopContainer() {
        // select a container to stop
        Iterator<String> iter = runningApps.iterator();
        // FIXME: select a random one and stop it
        if (iter.hasNext()){
            DockerContainer container = containerizedApps.get(iter.next());
            List<String> stopCommand = getStopCommand(container.getName());
            return run(stopCommand);
        }
        return false;
    }

    /**
     * Inspect a docker to get the information such as ip address
     */
    private List<String> getInspectCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("inspect");
        command.add(name);
        return command;
    }

    /**
     * Run command is the command to run a docker for the first time
     */
    // docker run --name xdn-demo-app -p 8080:3000 -e ADDR=172.17.0.1 -d oversky710/xdn-demo-app --ip 172.17.0.100
    private List<String> getRunCommand(String name, int port, List<String> env, String url) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");

        // name is unique globally, otherwise it won't be created successfully
        command.add("--name");
        command.add(name);

        //FIXME: only works on cloud node
        if (port > 0){
            command.add("-p");
            command.add("80:"+port);
        }

        if (env != null ){
            for (String e:env) {
                command.add("-e");
                command.add(e);
            }
        }

        command.add("-d");
        command.add(url);

        return command;
    }

    private List<String> getCheckpointCreateCommand(String name){
        return getCheckpointCreateCommand(name, false);
    }

    // docker checkpoint create --leave-running=true --checkpoint-dir=/tmp/test name test
    private List<String> getCheckpointCreateCommand(String name, boolean leaveRunning) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("checkpoint");
        command.add("create");
        if (leaveRunning)
            command.add("--leave-running=true");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        command.add(name);
        return command;
    }

    private List<String> getCheckpointRemoveCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("checkpoint");
        command.add("rm");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        command.add(name);
        return command;
    }

    private List<String> getCheckpointListCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("checkpoint");
        command.add("ls");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);

        return command;
    }

    List<String> getRemoveImageCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("image");
        command.add("rm");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        return command;
    }

    /**
     * Start command is the command to restart a docker
     */
    // docker start --checkpoint=xdn-demo-app xdn-demo-app
    private List<String> getStartCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("start");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        return command;
    }

    // docker pull oversky710/xdn-demo-app
    private List<String> getPullCommand(String url) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("pull");
        command.add(url);
        return command;
    }

    private List<String> getStopCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("stop");
        command.add(name);
        return command;
    }

    private List<String> getTarCommand(String filename, String path, String dest) {
        List<String> command = new ArrayList<>();
        command.add("sudo");
        command.add("tar");
        command.add("zcf");
        command.add(filename);
        command.add("-C");
        command.add(path);
        command.add(dest);
        return command;
    }

    private List<String> getUntarCommand(String filename, String dest) {
        List<String> command = new ArrayList<>();
        command.add("sudo");
        command.add("tar");
        command.add("zxf");
        command.add(filename);
        command.add("-C");
        command.add(dest);
        return command;
    }

    private boolean run(List<String> command) {
        ProcessResult result;
        try {
            result = ProcessRuntime.executeCommand(command);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return result.getRetCode() == 0;
    }

    @Override
    public boolean execute(Request request) {
        return execute(request,false);
    }

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

    @Override
    public String toString() {
        return this.getClass().getSimpleName(); //+"("+myID+")";
    }

    public static void main(String[] args) {

    }
}
