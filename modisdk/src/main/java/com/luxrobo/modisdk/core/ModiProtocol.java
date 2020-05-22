package com.luxrobo.modisdk.core;

import com.luxrobo.modisdk.utils.ModiLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class ModiProtocol {

    public enum FLASH_CMD {
        CHECK_CRC(0x101),
        ERASE(0x201);

        public int value;

        FLASH_CMD(int value) {
            this.value = value;
        }
    }

    public enum MODULE_STATE {
        RUN(0),         // task resume, module task enable
        STOP(3),        // task suspend, module task disable
        UPDATE(4),
        UPDATE_READY(5),
        RESET(6);

        public byte value;

        MODULE_STATE(int value) {
            this.value = (byte) (value & 0xFF);
        }
    }

    public enum MODULE_WARNING {
        NO(0),
        FIRMWARE(1),
        FIRMWARE_READY(2),
        EXCEPTION(3),
        ERROR(4),
        BUS(5),
        SRAM(6),
        FAULT(7);

        public byte value;

        MODULE_WARNING(int value) {
            this.value = (byte) (value & 0xFF);
        }
    }

    public static byte[] streamCommand(ModiStream stream) {

        byte[] stream_command_data = new byte[8];
        int streamBodySize = stream.streamBody != null ? stream.streamBody.length : 0;

        stream_command_data[0] = stream.streamId;
        stream_command_data[1] = stream.streamType.value;

        ByteBuffer bytebuffer = ByteBuffer.allocate(4);
        bytebuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(streamBodySize);

        for (int i = 0; i < 4; i++) {
            stream_command_data[i + 2] = bytebuffer.get(i);
        }

        return ModiFrame.makeFrame(0x12, 0, stream.moduleId, stream_command_data);
    }

    public static ArrayList<byte[]> streamDataList(ModiStream stream) {

        ArrayList<byte[]> dataList = new ArrayList<>();

        for (int i = 0; i < stream.streamBody.length; i += 7) {
            int begin = i;
            int end = i + 7;
            if (end > stream.streamBody.length)
                end = stream.streamBody.length;

            byte[] slice = Arrays.copyOfRange(stream.streamBody, begin, end);

            byte[] streamSlice = new byte[slice.length + 1];
            streamSlice[0] = stream.streamId;

            for (int j = 0; j < slice.length; j++) {
                streamSlice[j + 1] = slice[j];
            }

            dataList.add(ModiFrame.makeFrame(0x10, 0, stream.moduleId, streamSlice));
        }

        return dataList;
    }

    public static byte[] discoverModule(long module_uuid, byte flag) {
        byte[] data = new byte[8];

        for (int i = 0; i < 6; i++) {
            data[i] = (byte) (module_uuid & 0xFF);
            module_uuid = module_uuid >> 8;
        }

        data[7] = flag;

        return ModiFrame.makeFrame(0x08, 0, 0xFFF, data);
    }

    public static byte[] firmwareCommand(int moduleKey, FLASH_CMD flashCmd, int address, int crc) {

        byte[] data = new byte[8];

        ByteBuffer address_buffer = ByteBuffer.allocate(4);
        ByteBuffer crc_buffer = ByteBuffer.allocate(4);

        address_buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(address);
        crc_buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(crc);

        for (int i = 0; i < 4; i ++) {
            data[i] = crc_buffer.get(i);
            data[i + 4] = address_buffer.get(i);
        }

        return ModiFrame.makeFrame(0x0D, flashCmd.value, moduleKey, data);
    }

    public static byte[] firmwareData(int moduleKey, int segment, byte[] data) {
        return ModiFrame.makeFrame(0x0B, segment, moduleKey, data);
    }

    public static byte[] setModuleState(int module_key, MODULE_STATE state) {
        byte[] data = new byte[8];
        data[0] = state.value;

        return ModiFrame.makeFrame(0x09, 0, module_key, data);
    }

    public static byte[] resetNetworkModule(int moduleKey) {
        byte[] data = new byte[8];


        for (int i = 0; i < 7; i ++) {
            data[i] = 0x00;

        }

        return ModiFrame.makeFrame(0xA0, 26, moduleKey, data);
    }

    public static byte[] setChangeNetworkModuleLed(int moduleKey) {
        byte[] data = new byte[4];

        data[0] = 0x05;
        data[1] = 0x05;
        data[2] = 0x05;
        data[3] = 0x01;

        return ModiFrame.makeFrame(0xA9, 0, 0, data);
    }

}
