package com.luxrobo.modisdk.core;

public class ModiStream {
    public enum STREAM_TYPE {
        INTERPRETER(3);

        public byte value;

        STREAM_TYPE(int value) {
            this.value = (byte) (value & 0xFF);
        }
    }

    public enum STREAM_RESPONSE {
        SUCCESS(0),
        DUPLICATE(3),
        UNDEFINED(4),
        TIMEOUT(5);

        public byte value;

        STREAM_RESPONSE(int value) {
            this.value = (byte) (value & 0xFF);
        }
    }

    public int moduleId;
    public byte streamId;
    public STREAM_TYPE streamType;
    public byte[] streamBody;

    public static ModiStream makeStream(int moduleId, int streamId, STREAM_TYPE streamType, byte[] streamBody) {
        ModiStream stream = new ModiStream();
        stream.moduleId = moduleId & 0xFFF;
        stream.streamId = (byte)streamId;
        stream.streamType = streamType;
        stream.streamBody = streamBody;

        return stream;
    }
}
