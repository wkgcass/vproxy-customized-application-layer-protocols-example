package net.cassite.vproxy_application_layer_protocol_example;

class Serializer {
    private Serializer() {
    }

    static byte[] serialize(String str) {
        byte[] data = str.getBytes();
        byte[] ret = new byte[3 + data.length];
        int len = data.length;
        ret[0] = (byte) ((len >> 16) & 0xff);
        ret[1] = (byte) ((len >> 8) & 0xff);
        ret[2] = (byte) ((len) & 0xff);
        System.arraycopy(data, 0, ret, 3, len);
        return ret;
    }
}
