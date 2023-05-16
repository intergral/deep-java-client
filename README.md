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
