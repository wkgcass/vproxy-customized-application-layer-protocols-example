# Example for using customized application layer protocols with vproxy

## What does this example do

1. Implements a very simple application layer protocol.
2. Implements the `Processor` for vproxy `tcp-lb` to use.
3. Starts 2 echo servers.
4. Starts the vproxy loadbalancer with customized processor loaded and configured.
5. Starts a client that reads your input from stdin, and prints the reply message.

## How to run:

You need java 11.

Then:

```
java -jar example.jar
```

## How can I get the example executable file

You may directly download the jar from the [release page](https://github.com/wkgcass/vproxy-customized-application-layer-protocols-example/releases).
