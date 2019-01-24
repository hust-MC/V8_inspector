package com.example.zhanfang.test;

import android.util.Log;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

public class V8Inspector {
    private String packageName;
    private V8InspectorServer server;

    private final Object debugLocker;
    private static String ApplicationDir;

    private Handler mainHandler;

    private static AtomicBoolean ReadyToProcessMessages = new AtomicBoolean(false);

    private LinkedBlockingQueue<String> inspectorMessages = new LinkedBlockingQueue<String>();
    private LinkedBlockingQueue<String> pendingInspectorMessages = new LinkedBlockingQueue<String>();

    V8Inspector(String filesDir, String packageName, Handler handler) {
        ApplicationDir = filesDir;
        this.packageName = packageName;
        this.debugLocker = new Object();
        mainHandler = handler;
    }

    public static Pair<String, String>[] getPageResources() {
        // necessary to align the data dir returned by context (emulator) and that used by the v8 inspector
        if (ApplicationDir.startsWith("/data/user/0/")) {
            ApplicationDir = ApplicationDir.replaceFirst("/data/user/0/", "/data/data/");
        }

        String dataDir = ApplicationDir;
        File rootFilesDir = new File(dataDir, "app");


        List<Pair<String, String>> resources = traverseResources(rootFilesDir);

        @SuppressWarnings("unchecked")
        Pair<String, String>[] result = resources.toArray((Pair<String, String>[]) Array.newInstance(resources.get(0).getClass(), resources.size()));
        return result;
    }

    private static List<Pair<String, String>> traverseResources(File dir) {
        List<Pair<String, String>> resources = new ArrayList<>();

        Queue<File> directories = new LinkedList<>();
        directories.add(dir);

        while (!directories.isEmpty()) {
            File currentDir = directories.poll();

            File[] files = currentDir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    directories.add(file);
                } else {
                    resources.add(new Pair<>("file://" + file.getAbsolutePath(), getMimeType(file.getAbsolutePath())));
                }
            }
        }

        return resources;
    }

    private class V8InspectorWebSocket extends NanoWSD.WebSocket {
        V8InspectorWebSocket(NanoHTTPD.IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            V8.connect(V8InspectorWebSocket.this);
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    V8.disconnect();
                }
            });
        }

        @Override
        protected void onMessage(final NanoWSD.WebSocketFrame message) {

            inspectorMessages.offer(message.getTextPayload());

            if (!ReadyToProcessMessages.get()) {
                String nextMessage = inspectorMessages.poll();
                while (nextMessage != null) {
                    pendingInspectorMessages.offer(nextMessage);
                    nextMessage = inspectorMessages.poll();
                }

                if (message.getTextPayload().contains("Debugger.enable")) {
                    synchronized(debugLocker) {
                        debugLocker.notify();
                    }
                }
            } else {
                mainHandler.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        String nextMessage = inspectorMessages.poll();
                        while (nextMessage != null) {
                            V8.dispatchMessage(nextMessage);
                            nextMessage = inspectorMessages.poll();
                        }
                    }
                });
            }
        }

        @Override
        public void send(String payload) throws IOException {
            super.send(payload);
        }

        private String getInspectorMessage(Object connection) {
            return ((V8InspectorWebSocket) connection).getInspectorMessage();
        }

        public String getInspectorMessage() {
            try {
                return inspectorMessages.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {
        }

        @Override
        protected void onException(IOException exception) {
            exception.printStackTrace();
            V8.disconnect();
        }
    }

    private class V8InspectorServer extends NanoWSD {
        V8InspectorServer(String name) {
            super(name);
        }

        @Override
        protected Response serveHttp(IHTTPSession session) {
            String listJson = "[ {\n" + "   \"description\": \"\",\n" +
                    "   \"devtoolsFrontendUrl\": \"http://chrome-devtools-frontend.appspot" +
                    ".com/serve_rev/@793eb32f50bafcb87fb5d1a0fd3f44e66470e580/inspector" +
                    ".html?ws=localhost:4000\",\n" +
                    "   \"id\": \"0\",\n" + "   \"title\": \"V8测试一下\",\n" + "   \"type\": \"page\",\n" +
                    "   \"url\": \"https://www.baidu.com/\",\n" +
                    "   \"webSocketDebuggerUrl\": \"ws://localhost:4000\"\n" + "} ]";



            Response response = newFixedLengthResponse(Response.Status.OK, "application/json", listJson);
            response.addHeader("Accept-Ranges", "bytes");
            return response;
        }

        @Override
        protected WebSocket openWebSocket(IHTTPSession handshake) {
            return new V8InspectorWebSocket(handshake);
        }
    }

    public void start() throws IOException {
        if (this.server == null) {
            Log.i("debug", this.packageName + "_devtools_remote");
            this.server = new V8InspectorServer(this.packageName + "_devtools_remote");
            this.server.start(-1);

        }
    }

    // pause the main thread for 30 seconds (30 * 1000 ms)
    // allowing the devtools frontend to establish connection with the inspector
    public void waitForDebugger(boolean shouldBreak) {
        if (shouldBreak) {
            synchronized(this.debugLocker) {
                try {
                    this.debugLocker.wait(1000 * 30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    ReadyToProcessMessages.set(true);
                    this.processDebugBreak();
                }
            }
        } else {
            ReadyToProcessMessages.set(true);
        }
    }

    // process all messages coming from the frontend necessary to initialize the inspector backend
    // schedule a debug line break at first convenience
    private void processDebugBreak() {
        processDebugBreakMessages();
        V8.scheduleBreak();
    }

    private void processDebugBreakMessages() {
        while (!pendingInspectorMessages.isEmpty()) {
            String inspectorMessage = pendingInspectorMessages.poll();
            V8.dispatchMessage(inspectorMessage);
        }
    }

    private static void sendToDevToolsConsole(Object connection, String message, String level) {
        try {
            JSONObject consoleMessage = new JSONObject();

            JSONObject params = new JSONObject();
            params.put("type", level);
            params.put("executionContextId", 0);
            params.put("timestamp", 0.000000000000000);

            JSONArray args = new JSONArray();
            args.put(message);
            params.put("args", args);

            consoleMessage.put("method", "Runtime.consoleAPICalled");
            consoleMessage.put("params", params);

            String sendingText = consoleMessage.toString();
            V8Inspector.send(connection, sendingText);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void send(Object connection, String payload) throws IOException {
        V8Inspector.V8InspectorWebSocket socketConnection = (V8Inspector.V8InspectorWebSocket) connection;
        if (socketConnection.isOpen()) {
            socketConnection.send(payload);
        }
    }

    private static String getInspectorMessage(Object connection) {
        return ((V8Inspector.V8InspectorWebSocket) connection).getInspectorMessage();
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (!extension.isEmpty()) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

            // getMimeType may sometime return incorrect results in the context of NativeScript
            // e.g. `.ts` returns video/MP2TS
            switch (extension) {
                case "js":
                    type = "text/javascript";
                    break;
                case "json":
                    type = "application/json";
                    break;
                case "css":
                    type = "text/css";
                    break;
                case "ts":
                    type = "text/typescript";
                    break;
                // handle shared libraries so they are marked properly and don't appear in the sources tab
                case "so":
                    type = "application/binary";
                    break;
            }
        }

        return type;
    }
}
