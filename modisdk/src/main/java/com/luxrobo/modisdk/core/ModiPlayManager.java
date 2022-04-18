package com.luxrobo.modisdk.core;

import android.os.Handler;
import android.os.Looper;

import com.luxrobo.modisdk.client.ModiFrameObserver;
import com.luxrobo.modisdk.enums.PlayCommand;
import com.luxrobo.modisdk.enums.PlayCommandData;
import com.luxrobo.modisdk.enums.PlayEvent;
import com.luxrobo.modisdk.listener.ModiPlayManagerListener;
import com.luxrobo.modisdk.utils.ModiLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ModiPlayManager implements ModiFrameObserver {

    private ModiManager mModiManager;
    private ModiPlayManagerListener mListener;
    private Handler mHandler;

    public ModiPlayManager(ModiManager manager) {
        mModiManager = manager;
        mHandler = new Handler(Looper.getMainLooper());

        manager.attach(this);
    }

    @Override
    protected void finalize() throws Throwable {
        mModiManager.detach(this);
        super.finalize();
    }

    public void setListener(ModiPlayManagerListener listener) {
        this.mListener = listener;
    }

    // options : read_and_clear option
    //  1 : 모듈이 이벤트를 읽고 0으로 초기화 함
    //  0 : 모듈이 이벤트를 읽고 값을 그대로 유지.
    public void fireEvent(PlayCommand command, PlayCommandData commandData, int options) throws Exception {

        try {
            if (!mModiManager.isConnected()) {
                throw new Exception("Can't send Play Command (module is not connected.)");
            }

            int target = mModiManager.getConnectedModiUuid() & 0xFFF;

            byte[] frameData = new byte[8];

            switch (command) {
                case BUTTON_CLICK:
                    frameData[2] = (byte) commandData.value;
                    break;
                case BUTTON_DOUBLE_CLICK:
                    frameData[4] = (byte) commandData.value;
                    break;
                case BUTTON_PRESS_STATUS:
                    frameData[0] = (byte) commandData.value;
                    break;
                default:
                    frameData[0] = (byte) commandData.value;
                    break;
            }

            frameData[7] = (byte) options;

//            byte[] did = new byte[2];
//
//            did[0] = (byte) 0x02;
//            did[1] = (byte) 0x01;
//
//            command.value

//            int didValue = ByteBuffer.wrap(did).order(ByteOrder.LITTLE_ENDIAN).getInt();

            // TODO: Command Refreshing handling
            sendFrame(ModiFrame.makeFrame(0x1F, target, command.value, frameData));
        }

        catch (Exception e) {
            ModiLog.e("fireEvent error : " + e.toString());
        }

    }

    public void modiPlusEvent(int index, int property, PlayCommand command, int commandData) throws Exception {

        try {
            if (!mModiManager.isConnected()) {
                throw new Exception("Can't send Play Command (module is not connected.)");
            }

            int target = mModiManager.getConnectedModiUuid() & 0xFFF;

            byte[] frameData = new byte[8];

            switch (command) {
                case BUTTON_CLICK:
                case IMU_ANGLE_PITCH :
                    frameData[2] = (byte) commandData;
                    break;
                case BUTTON_DOUBLE_CLICK:
                case IMU_ANGLE_YAW:
                    frameData[4] = (byte) commandData;
                    break;
                case RECEIVE_DATA:

                    frameData[3] = (byte) (commandData >> 24 & 0xFF);
                    frameData[2] = (byte) (commandData >> 16 & 0xFF);
                    frameData[1] = (byte) (commandData >> 8 & 0xFF);
                    frameData[0] = (byte) (commandData & 0xFF);

                    break;

                default:
                    frameData[0] = (byte) commandData;
                    break;
            }

            int did = property + (index * 100);

            sendFrame(ModiFrame.makeFrame(0x1F, target, did, frameData));
        }

        catch (Exception e) {
            ModiLog.e("fireEvent error : " + e.toString());
        }
    }

    private void sendFrame(byte[] data) {
        // TODO: Send Packet
        mModiManager.sendData(data);
    }

    private void notifyEvent(PlayEvent event, byte[] data) {
        switch (event) {
            case DATA:
                if (mListener != null) {
                    mListener.onEventData(data);
                } break;
            case CAMERA:
                if (mListener != null) {
                    mListener.onEventCamera(true);
                } break;
            case BUZZER:
                if (mListener != null) {
                    int buzzer_data = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    boolean enable = buzzer_data != 0;
                    mListener.onEventBuzzer(enable);
                } break;
            default:
                ModiLog.d("Invalid Mobile Event code : " + event);
                break;
        }
    }

    @Override
    public void onModiFrame(ModiFrame frame) {
        switch (frame.cmd()) {
            case 0x04: {

                int moduleKey = mModiManager.getConnectedModiUuid() & 0xfff;
                if (frame.did() == moduleKey) {
                    notifyEvent(PlayEvent.fromInteger(frame.sid()), frame.data());
                }
                break;
            }
            default:
                break;
        }
    }
}
