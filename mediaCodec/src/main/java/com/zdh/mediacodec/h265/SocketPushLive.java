package com.zdh.mediacodec.h265;


import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * author: ZDH
 * Date: 2022/3/24
 * Description:
 */
public class SocketPushLive {

    private static final String TAG = "SocketPushLive";
    WebSocket webSocket;
    VSS vss;
    boolean start = true;
    int port = 2448;
    public void init() {
        new Thread() {
            @Override
            public void run() {
                while(true) {
                    if (start) {
                        close();
                        vss = new VSS(new InetSocketAddress(port++));
                        vss.start();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

//        vss = new VSS(new InetSocketAddress(2000));
//        vss.start();
    }

    public void close() {
        try {
            if(vss != null) {
                vss.stop();
            }
            if(webSocket != null) {
                webSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    class VSS extends WebSocketServer {

        public VSS(InetSocketAddress inetSocketAddress) {
            super(inetSocketAddress);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            Log.i(TAG, "onOpen: " + port);
            start = false;
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            Log.i(TAG, "onClose: ");
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            Log.i(TAG, "onMessage: " + message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            Log.i(TAG, "onError: " + ex.toString());
        }

        @Override
        public void onStart() {
            Log.i(TAG, "onStart: " + port);
        }
    }

//    File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM), "vi.h265");
//    FileOutputStream fileOutputStream;
    public void sendData(byte[] data) {
//        if(fileOutputStream == null) {
//            try {
//                fileOutputStream = new FileOutputStream(file);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            fileOutputStream.write(data);
//            fileOutputStream.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.e("TAG", "webSocket: " + (webSocket != null) );
        if (webSocket != null && webSocket.isOpen()) {
            Log.e("TAG", "sendData: " );
            webSocket.send(data);
        }
    }
}
