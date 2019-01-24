//
// Created by Zhan,Fang on 2018/9/14.
//

#ifndef INSPECTORCLIENT_H_
#define INSPECTORCLIENT_H_

#include <v8.h>
#include <v8-inspector.h>
#include "JEnv.h"
#include "src/inspector/v8-inspector-impl.h"
#include "src/inspector/v8-inspector-session-impl.h"
#include "src/inspector/protocol/Forward.h"
#include "src/inspector/string-16.h"
#include "V8Engine.h"

using namespace v8_inspector;

class InspectorClient : V8InspectorClient, v8_inspector::V8Inspector::Channel {
public:
    static InspectorClient* GetInstance();

    template <class TypeName> static v8::Local<TypeName> PersistentToLocal(v8::Isolate* isolate, const v8::Persistent<TypeName>& persistent);

    void init();
    void connect(jobject connection);
    void scheduleBreak();
    void createInspectorSession(v8::Isolate* isolate, const v8::Local<v8::Context>& context);
    void disconnect();
    void dispatchMessage(const std::string& message);
    void doDispatchMessage(v8::Isolate* isolate, const std::string& message);

    void sendResponse(int callId, std::unique_ptr<StringBuffer> message) override;
    void sendNotification(const std::unique_ptr<StringBuffer> message) override;
    void flushProtocolNotifications() override;

    static void sendToFrontEndCallback(const v8::FunctionCallbackInfo<v8::Value>& args);
    static void consoleLogCallback(const std::string& message, const std::string& logLevel);

    void runMessageLoopOnPause(int context_group_id) override;
    void quitMessageLoopOnPause() override;
    v8::Local<v8::Context> ensureDefaultContextInGroup(int contextGroupId) override;

    void waitForFrontend();

    static void attachInspectorCallbacks(v8::Isolate* isolate, v8::Local<v8::ObjectTemplate>& globalObjectTemplate);
    static void InspectorIsConnectedGetterCallback(v8::Local<v8::String> property, const v8::PropertyCallbackInfo<v8::Value>& info);
    static bool inspectorIsConnected() {
        return InspectorClient::GetInstance()->isConnected;
    }

    std::unique_ptr<V8Inspector> inspector_;
    v8::Isolate* isolate_;
    bool isConnected;

private:
    InspectorClient(v8::Isolate* isolate);

    void runMessageLoop();

    static InspectorClient* instance;
    static jclass inspectorClass;
    static jmethodID sendMethod;
    static jmethodID getInspectorMessageMethod;
    static jmethodID sendToDevToolsConsoleMethod;
    static int contextGroupId;

    v8::Persistent<v8::Context> context_;
    std::unique_ptr<V8InspectorSession> session_;
    jobject connection;
    bool running_nested_loop_;
    bool terminated_;
    bool waiting_for_frontend_;
    bool waiting_for_resume_;
};


#endif /* INSPECTORCLIENT_H_ */
