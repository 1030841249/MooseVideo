package com.zdh.mediacodec.h265;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

/**
 * author: ZDH
 * Date: 2022/3/24
 * Description:
 */
public class SocketPlayLive {
    WebSocketClient mWebSocketClient;
    SocketCallback mCallback;
    H265Decoder h265Decoder;
    public SocketPlayLive(SurfaceView surfaceView) {
        h265Decoder = new H265Decoder();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                h265Decoder.init(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        URI uri = null;
        try {
            uri = new URI("ws://192.168.128.128:12001");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mWebSocketClient = new MyWebSocketClient(uri);
        try {
            mWebSocketClient.connectBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {

        }

        @Override
        public void onMessage(String message) {

        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);
            if (mCallback != null) {
                mCallback.onCallback(data);
            }
            h265Decoder.decode(data);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

        }

        @Override
        public void onError(Exception ex) {

        }
    }

    public interface SocketCallback {
        void onCallback(byte[] data);
    }
}
