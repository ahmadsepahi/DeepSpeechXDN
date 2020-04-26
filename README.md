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



### Setup
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

Run the client to send a request:
```
script/gpClient.sh -DgigapaxosConfig=conf/xdn.local.properties test2.ExecuteServices
```
To clear up:
```
script/gpServer.sh -DgigapaxosConfig=conf/xdn.local.properties forceclear all
script/cleanup.sh
```
