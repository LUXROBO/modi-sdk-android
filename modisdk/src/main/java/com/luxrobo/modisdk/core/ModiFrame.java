package com.luxrobo.modisdk.core;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class ModiFrame {

    private byte[] mFrame;

    public ModiFrame(final byte[] frame) {
        mFrame = frame.clone();
    }

    public ModiFrame(int cmd, int sid, int did, byte[] binary) {
        mFrame = makeFrame(cmd, sid, did, binary);
    }

    public int cmd() {
        return (int)ByteBuffer.wrap(mFrame, 0, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public int sid() {
        return (int)ByteBuffer.wrap(mFrame, 2, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public int did() {
        return (int)ByteBuffer.wrap(mFrame, 4, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public int len() {
        return (int)ByteBuffer.wrap(mFrame, 6, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public byte[] data() {
        byte[] out = Arrays.copyOfRange(mFrame, 8, 8 + len());
        return out;
    }

    public byte[] rawFrame() {
        return mFrame;
    }

    public static byte[] makeFrame(int cmd, int sid, int did, byte[] data) {
        byte[] frame = new byte[16];

        stuffFrameHeader(frame, cmd, sid, did);
        stuffFrameData(frame, data);

        return frame;
    }

    public static ArrayList<byte[]> makeFrames(int cmd, int sid, int did, @NonNull byte[] data) {

        ArrayList<byte[]> frame_array = new ArrayList<>();

        byte[] frame = new byte[16];
        stuffFrameHeader(frame, cmd, sid, did);

        for (int i = 0; i < data.length; i += 8) {
            int begin = i;
            int end = i + 8;
            if (end > data.length)
                end = data.length;

            stuffFrameData(frame, Arrays.copyOfRange(data, begin, end));

            frame_array.add(frame.clone());
        }

        return frame_array;
    }

    private static void stuffFrameHeader(@NonNull byte[] buffer, int cmd, int sid, int did) {
        buffer[0] = (byte) (cmd & 0xFF);
        buffer[1] = (byte) ((cmd >> 8) & 0xFF);
        buffer[2] = (byte) (sid & 0xFF);
        buffer[3] = (byte) ((sid >> 8) & 0xFF);
        buffer[4] = (byte) (did & 0xFF);
        buffer[5] = (byte) ((did >> 8) & 0xFF);
    }

    private static void stuffFrameData(@NonNull byte[] buffer, @NonNull byte[] data) {
        buffer[6] = (byte) (data.length & 0xFF);
        buffer[7] = (byte) ((data.length >> 8) & 0xFF);
        for (int i = 0; i < data.length; i++) {
            buffer[i + 8] = data[i];
        }
    }
}
