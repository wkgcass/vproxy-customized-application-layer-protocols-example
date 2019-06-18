package net.cassite.vproxy_application_layer_protocol_example;

import vproxy.processor.Processor;
import vproxy.util.ByteArray;

import java.net.InetSocketAddress;

/*
 * The protocol:
 * +-------------+-------------------------------+
 * | Length (24) |       Payload (Length)        |
 * +-------------+-------------------------------+
 */

class ExampleContext extends Processor.Context {
    int writeToConn = -1;
}

class ExampleSubContext extends Processor.SubContext {
    final int connId;

    boolean isReadingHead = true;
    int payloadLength;

    ExampleSubContext(int connId) {
        this.connId = connId;
    }
}

public class ExampleProcessor implements Processor<ExampleContext, ExampleSubContext> {
    @Override
    public String name() {
        return "example";
    }

    @Override
    public ExampleContext init(InetSocketAddress ignore) {
        return new ExampleContext();
    }

    @Override
    public ExampleSubContext initSub(ExampleContext ctx, int connId, InetSocketAddress ignore) {
        return new ExampleSubContext(connId);
    }

    @Override
    public Mode mode(ExampleContext ctx, ExampleSubContext subCtx) {
        if (subCtx.isReadingHead) {
            return Mode.handle; // we want to check length of the payload
        } else {
            return Mode.proxy; // we want to proxy the payload
        }
    }

    @Override
    public boolean expectNewFrame(ExampleContext ctx, ExampleSubContext sub) {
        // when it's reading head, it's expecting a new frame
        return sub.isReadingHead;
    }

    @Override
    public int len(ExampleContext ctx, ExampleSubContext subCtx) {
        if (subCtx.isReadingHead) {
            return 3; // read 3 bytes of data
        } else {
            return subCtx.payloadLength; // proxy the length defined in the frame head
        }
    }

    @Override
    public ByteArray feed(ExampleContext ctx, ExampleSubContext subCtx, ByteArray data) throws Exception {
        subCtx.isReadingHead = false;
        subCtx.payloadLength = data.uint24(0);
        if (subCtx.payloadLength > 16384) {
            // you may end the process if input data is invalid
            throw new Exception("the payload is too long: " + subCtx.payloadLength);
        }
        if (subCtx.payloadLength == 0) { // if no payload, then it's expecting another head
            subCtx.isReadingHead = true;
        }
        return data; // send exactly the same data (frame head) to the other endpoint
    }

    @Override
    public ByteArray produce(ExampleContext ctx, ExampleSubContext subCtx) {
        return null; // never write back
    }

    @Override
    public void proxyDone(ExampleContext ctx, ExampleSubContext subCtx) {
        if (subCtx.connId == 0) {
            ctx.writeToConn = -1; // when proxy is done, reset the connectionId to -1 to let the lib pick a backend
        }
        subCtx.isReadingHead = true; // reset the reading state
    }

    @Override
    public int connection(ExampleContext ctx, ExampleSubContext subCtx) {
        return ctx.writeToConn;
    }

    @Override
    public void chosen(ExampleContext ctx, ExampleSubContext front, ExampleSubContext subCtx) {
        ctx.writeToConn = subCtx.connId; // a backend is chosen, will be used when proxying the payload
    }

    @Override
    public ByteArray connected(ExampleContext ctx, ExampleSubContext subCtx) {
        return null; // do not send any data when connection establishes
    }
}
