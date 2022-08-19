package com.example.loomo.basic.communication.bluetooth.connect;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.loomo.message.LoomState;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @Description:
 * Thread used to send messages
 * Will be a member in Object of BluetoothUnit
 * There is two sending mode:
 *  1. Send data for once, use method: sendData(LoomOrder)
 *  2. Keep sending data, use following method:
 *      2.1 setData(LoomOrder)
 *      2.2 startSend()
 *      2.3 finishSend()
 *
 * @author : Zhouyao
 * Date: 2021/12/26
 */
public class SendThread extends Thread {
    private final String TAG = "SEND THREAD";

    /* ********************************
    *     Fields to config thread     *
    ***********************************/
    private final BluetoothSocket mmSocket;
    private final OutputStream mmOutStream;

    /* **********************************
     *     Fields to Control Stream     *
     ************************************/
    private boolean msgFlag;
    private boolean msgFlagLock;
    private LoomState state;
    private long dataInterval = 0; //unit: ms

    public SendThread(BluetoothSocket socket) {
        mmSocket = socket;
        OutputStream tmpOut = null;
        // 使用临时对象获取输入和输出流，因为成员流是最终的
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmOutStream = tmpOut;
        msgFlag = false;
    }

    public void run() {
        while (true) {
            if(msgFlag){
                try {
                    mmOutStream.write(state.toJsonBytes());
                    // The msgFlag should set to false there
                    // But if msgFlag is locked, then do not change the value, which is true
                    msgFlag = msgFlagLock;
                    sleep(dataInterval);
                } catch (IOException e) {
                    Log.e(TAG, "run: Error when write out stream");
                    break;
                } catch (InterruptedException e) {
                    Log.e(TAG, "run: thread error on sleep()");
                    break;
                }
            }
        }
    }

    /**
     * Called in Connect Thread
     * Set the message for once
     * @param state
     */
    public void send(LoomState state){
        this.state = state;
        msgFlag = true;
    }

    /**
     * Used to continuously send data
     * @param state
     */
    public void setData(LoomState state){
        this.state = state;
    }

    /**
     * Start sending data with some interval between two transmission
     * @param dataInterval interval between transmission, unit: ms
     */
    public void startSend(long dataInterval){
        this.msgFlag = true;
        this.msgFlagLock = true;

        this.dataInterval = dataInterval;
    }

    /**
     * Finish data sending
     */
    public void finishSend(){
        this.msgFlag = true;
        this.msgFlagLock = false;

        this.dataInterval = 0;
    }


    /**
     * 在main中调用此函数，断开连接
     */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
