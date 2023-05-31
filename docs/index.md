# Deep Java Client

This is the java client for Deep, a dynamic monitor and debugging tool.

# Getting started

You will need to have a running version of the [DEEP server](#) to connect this client to.

## Install Agent

There are 2 ways to use deep with java. If using an application server like ColdFusion or Tomcat then use [Java Agent](#java-agent), if
you are a standalone application, or lambda then use [Dependency](#dependency).

### Java Agent

When using the java agent approach you need to download the jar, and attach it to JVM using the start-up argument 'javaagent'.

1. Download the JAR from [maven](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.intergral.deep&a=agent&v=LATEST)
2. Add `-javaagent:/path/to/deep.jar` to the JVM arguments.

### Dependency

When using the dependency approach you simple add deep to you project and start the agent.

```xml
<depedencies>
    <dependency>
        <groupId>com.intergral.deep</groupId>
        <artifactId>deep</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>compile</scope>
    </dependency>
    <dependency>
        <groupId>com.intergral.deep</groupId>
        <artifactId>agent</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</depedencies>
```

```groovy
import com.intergral.deep.Deep

Deep.start()
```

## Configuration

To configure the deep agent to connect to your services. You can use the following configs.

### In code (Dependency Only)

You can set config values in code to quickly try a connection.

```groovy
import com.intergral.deep.Deep

Deep.config()
        .setValue("service.url", "localhost:43315")
        .setValue("service.secure", false)
        .start()
```

### Environment

You can set the config for deep to use via environment variables. All config values can be set as environment
variables by simply prefixing 'DEEP_' to the key, also ensure that the key is upper case and uses '_' instead of '.'.

e.g. `service.url` would become `DEEP_SERVICE_URL`

### System Properties

You can set the config for deep to use via system properties. All config values can be set as system
properties by simply prefixing 'deep.' to the key, also ensure that the key is lower case and uses '.' instead of '.'.

e.g. `service.url` would become `deep.service.url`

For a full list of values see [config values](./config/config.md)
