import vproxy.processor.ProcessorRegistry;
import net.cassite.vproxy_application_layer_protocol_example.ExampleProcessorRegistry;

module vproxy.example {
    requires vproxy;
    provides ProcessorRegistry with ExampleProcessorRegistry;
}
