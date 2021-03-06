package net.cassite.vproxy_application_layer_protocol_example;

import vproxy.processor.Processor;
import vproxy.processor.ProcessorRegistry;

import java.util.NoSuchElementException;

public class ExampleProcessorRegistry implements ProcessorRegistry {
    private final ExampleProcessor exampleProcessor = new ExampleProcessor();

    @Override
    public Processor get(String name) throws NoSuchElementException {
        if ("example".equals(name))
            return exampleProcessor;
        throw new NoSuchElementException(name);
    }
}
