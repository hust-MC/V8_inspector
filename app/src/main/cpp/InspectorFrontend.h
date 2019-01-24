//
// Created by Zhan,Fang on 2018/9/14.
//

#ifndef TEST1_INSPECTORFRONTEND_H
#define TEST1_INSPECTORFRONTEND_H

#include <v8.h>
#include <v8-inspector.h>

using namespace v8;
using namespace v8_inspector;

class InspectorFrontend final : public v8_inspector::V8Inspector::Channel {
public:
    explicit InspectorFrontend(Local<Context> context);
    virtual ~InspectorFrontend() = default;

private:
    void sendResponse(int callId, std::unique_ptr<v8_inspector::StringBuffer> message) override;
    void sendNotification(std::unique_ptr<v8_inspector::StringBuffer> message) override;
    void flushProtocolNotifications() override;

    void Send(const v8_inspector::StringView& string);


    Isolate* isolate_;
    Global<Context> context_;
};


#endif //TEST1_INSPECTORFRONTEND_H
