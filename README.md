[![Tests & Lint](https://github.com/intergral/deep-java-client/actions/workflows/on_push.yml/badge.svg)](https://github.com/intergral/deep-java-client/actions/workflows/on_push.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.intergral.deep/agent)](https://central.sonatype.com/artifact/com.intergral.deep/agent)
# DEEP Java Client

DEEP is an open source dynamic insight engine based on the Grafana stack. The idea is to allow dynamic collection of
Traces, Metrics, Logs and Snapshots via the Grafana UI.

## Usage

To use DEEP simple import the package and start the agent at the earliest point in the code.

```groovy
import com.intergral.deep.Deep

Deep.start()
```

## Examples

There are a couple of examples [available here](./examples/README.md).


## CF Debugging

To debug in CF I found the easiest way is to use the docker. We can do this with this command:
```bash
docker run --name cf_2018 --rm -p 8500:8500 -p 8088:8088 -p 5005:5005 -e STRIP_STD=true -e FR_ENABLED=false -e NV_ENABLED=false -e JAVA_OPTS="-javaagent:/opt/deep/deep.jar -Ddeep.service.url=172.17.0.1:43315 -Ddeep.logging.level=FINE -Ddeep.service.secure=false -Ddeep.transform.path=/opt/dispath -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005" -v ${PWD}/dispath:/opt/dispath  -v ${PWD}/agent/target/agent-1.0-SNAPSHOT.jar:/opt/deep/deep.jar registry.gitlab.com/intergral/docker/servers/coldfusion:2018
```

Changing the -v paths to point to the locations on your machine. You also need to use the debug listen config ("Listen for Docker debug connections") that should be available with this project in idea.