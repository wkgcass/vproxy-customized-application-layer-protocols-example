# Example for Using Customized Application Layer Protocols with VPROXY

## What does this example do

1. Implements a very simple application layer protocol.
2. Implements the `Processor` for vproxy `tcp-lb` to use.
3. Starts 2 echo servers.
4. Starts the vproxy loadbalancer with customized processor loaded and configured.
5. Starts a client that reads your input from stdin, and prints the reply message.

## How to run:

```
java -jar example.jar
```

or run with the main class

```
net.cassite.vproxy_application_layer_protocol_example.Main
```

## How can I get the example executable file

You may directly download the zip package in the release page, which contains a script for running the example.
