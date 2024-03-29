# Development

Here we have a few hints on how to develop this module.

## Environment

### Java SDK

To set up the java environment we recommend using [SDKMAN](https://sdkman.io/). Once sdkman is installed simply run `sdk env` in the project
root to set the JDK.

### Building

To simplify the building the maven build is split into a few profiles to reduce the build time. There are a few commands in
the [`Makefile`](./Makefile) that can help with the builds.

## CF Debugging

To debug in CF I found the easiest way is to use the docker. We can do this with this command:

```bash
docker run --name cf_2018 --rm -p 8500:8500 -e acceptEULA=YES -e password:admin -e JAVA_OPTS="-Ddeep.service.url=172.17.0.1:43315 -Ddeep.logging.level=FINE -Ddeep.service.secure=false -Ddeep.transform.path=/opt/dispath" -v ${PWD}/dispath:/opt/dispath  -v ${PWD}/agent/target/agent-1.0-SNAPSHOT.jar:/opt/deep/deep.jar ghcr.io/intergral/deep:coldfusion
```

Changing the -v paths to point to the locations on your machine. You also need to use the debug listen config ("Listen for Docker debug
connections") that should be available with this project in idea.