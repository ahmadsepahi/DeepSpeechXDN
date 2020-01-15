package edu.umass.cs.xdn.docker;

import edu.umass.cs.xdn.interfaces.XDNContainer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class DockerContainer implements XDNContainer {

    final String name;

    /**
     * Docker hub url
     */
    final String imageUrl;

    /**
     * The port number exposed to the public network
     * TODO: may need to expose multiple ports in the future
     */
    final int port;

    /**
     * This is long unique id generated by docker.
     */
    String id;

    /**
     *
     */
    String addr;

    final private List<String> serviceNames;

    JSONArray env;

    public DockerContainer(String name, String imageUrl, int port, JSONArray env) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.port = port;
        this.env = env;
        this.serviceNames = new ArrayList<>();
    }

    public DockerContainer(JSONObject json) throws JSONException {
        this.name = json.getString(DockerKeys.NAME.toString());
        this.env = json.getJSONArray(DockerKeys.ENV.toString());
        this.imageUrl = json.getString(DockerKeys.IMAGE_URL.toString());
        this.port = json.getInt(DockerKeys.PORT.toString());
        JSONArray users = json.getJSONArray(DockerKeys.SERVICE_NAMES.toString());
        this.serviceNames = new ArrayList<>();
        if (users != null) {
            for (int i=0; i<users.length(); i++){
                serviceNames.add(users.getString(i));
            }
        }
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();

        json.put(DockerKeys.NAME.toString(), name);
        json.put(DockerKeys.ENV.toString(), env);
        json.put(DockerKeys.IMAGE_URL.toString(), imageUrl);
        json.put(DockerKeys.PORT.toString(), port);

        JSONArray snJSON = new JSONArray(serviceNames);
        json.put(DockerKeys.SERVICE_NAMES.toString(), snJSON);

        return json;
    }


    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getAddr() {
        return addr;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }

    @Override
    public List<String> getStartCommand(String name) {
        return null;
    }

    @Override
    public List<String> getCheckpointCommand(String name) {
        return null;
    }

    @Override
    public List<String> getRestoreCommand(String name) {
        return null;
    }

    @Override
    public List<String> getStopCommand(String name) {
        return null;
    }

    @Override
    public List<String> getPullCommand(String name, boolean exists) {
        return null;
    }

    public void addServiceName(String name) {
        serviceNames.add(name);
    }

    public boolean removeServiceName(String name){
        return serviceNames.remove(name);
    }

    public boolean isEmpty() {
        return serviceNames.isEmpty();
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return imageUrl;
    }

    public static void main(String[] args) {

    }

}
