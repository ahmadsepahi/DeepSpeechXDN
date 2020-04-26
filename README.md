Prerequisites: `Linux`, `Java 1.8+`, `ant`, `bash`, `docker`, `criu`

DeepSpeech XDN
-------------
Source:
```
git clone https://github.com/ahmadsepahi/DeepSpeechXDN
cd XDN
ant jar
```

Overview
-------------
In this experiment, we wrap deepspeech applicatin in XDN. The deepsppech application is in https://github.com/mozilla/DeepSpeech and its http server is from https://gist.github.com/touilleMan/eb02ea40b93e52604938.



### Tutorial 1: Standalone Setup
To start up XDN servers, you need to specify a config file. In this tutorial, we'll use the config file <tt>conf/xdn.local.properties</tt>.

    # XDN config file
    APPLICATION=edu.umass.cs.xdn.XDNApp
    
    DISABLE_RECONFIGURATION=true
    ENABLE_ACTIVE_REPLICA_HTTP=true
    GIGAPAXOS_DATA_DIR=/tmp/gigapaxos
    
    # format: active.<active_server_name>=host:port
    active.AR0=127.0.0.1:2000
    
    # format: reconfigurator.<active_server_name>=host:port
    reconfigurator.RC=127.0.0.1:5000

Run the servers as follows from the top-level directory:
```
script/gpServer.sh -DgigapaxosConfig=conf/xdn.local.properties start all
```

Run the client to create an XDN application:
```
script/gpClient.sh -DgigapaxosConfig=conf/xdn.local.properties test2.CreateServices
```

It creates an stateful counter app wrapped in docker called xdn-demo-app. You can find its docker image on DockerHub: [xdn-demo-app docker image](https://hub.docker.com/repository/docker/oversky710/xdn-demo-app).
Open your browser to check the counter's current value: [http://127.0.0.1/xdnapp](http://127.0.0.1/xdnapp).

Run the client to send a request:
```
script/gpClient.sh -DgigapaxosConfig=conf/xdn.local.properties test2.ExecuteServices
```

The client will send a request with a file "test3.wav", the the underlying app [xdn-demo-app](https://github.com/ZhaoyuUmass/xdn-demo-app).
Open your browser to check the counter's current value after operation: [http://127.0.0.1/xdnapp](http://127.0.0.1/xdnapp).

You may also issue a request directly to our HTTP API with `curl`:

```
curl "http://127.0.0.1:2300?name=xdn-demo-app_xdn_Alvin&qval=1"
```

Or open the following link with your browser to send a request: [http://127.0.0.1:2300?name=xdn-demo-app_xdn_Alvin&qval=1](http://127.0.0.1:2300?name=xdn-demo-app_xdn_Alvin&qval=1).

Find out more usage instructions with our HTTP APIs on [GigaPaxos Wiki](http://github.com/MobilityFirst/gigapaxos/wiki).

To clear up:
```
script/gpServer.sh -DgigapaxosConfig=conf/xdn.local.properties forceclear all
script/cleanup.sh
```
