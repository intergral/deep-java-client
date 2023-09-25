[![Tests & Lint](https://github.com/intergral/deep-java-client/actions/workflows/on_push.yml/badge.svg)](https://github.com/intergral/deep-java-client/actions/workflows/on_push.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.intergral.deep/agent)](https://central.sonatype.com/artifact/com.intergral.deep/agent)
[![Known Vulnerabilities](https://snyk.io/test/github/intergral/deep-java-client/badge.svg)](https://app.snyk.io/org/b.w.donnelly1/project/fac2666d-509e-4b7c-861a-8297f0fc9556)
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

## Developing

For help developing this see [DEVELOPMENT.md](./DEVELOPMENT.md)

## Licensing

For licensing info please see [LICENSING.md](./LICENSING.md)
