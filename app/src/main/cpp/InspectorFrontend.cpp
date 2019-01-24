//
// Created by Zhan,Fang on 2018/9/14.
//

#include "InspectorFrontend.h"
#include <os-android.h>

struct Use {
    template <typename T>
    Use(T&&) {}  // NOLINT(runtime/explicit)
};
#define USE(...)                                         \
  do {                                                   \
    ::Use unused_tmp_array_for_use_macro[]{__VA_ARGS__}; \
    (void)unused_tmp_array_for_use_macro;                \
  } while (false)

#define DEBUG 0

#ifndef DCHECK
#define DCHECK(condition) assert(condition)
#endif

#ifndef CHECK
#define CHECK(condition) assert(condition)
#endif

InspectorFrontend::InspectorFrontend(Local<Context> context) {
    isolate_ = context->GetIsolate();
    context_.Reset(isolate_, context);
}

void InspectorFrontend::sendResponse(
        int callId,
        std::unique_ptr<v8_inspector::StringBuffer> message) {
    Send(message->string());
}

void InspectorFrontend::sendNotification(
        std::unique_ptr<v8_inspector::StringBuffer> message) {
    Send(message->string());
}

void InspectorFrontend::flushProtocolNotifications() {}

void InspectorFrontend::Send(const v8_inspector::StringView& string) {
    v8::Isolate::AllowJavascriptExecutionScope allow_script(isolate_);
    int length = static_cast<int>(string.length());
    DCHECK(length < v8::String::kMaxLength);
    Local<String> message =
            (string.is8Bit()
             ? v8::String::NewFromOneByte(
                            isolate_,
                            reinterpret_cast<const uint8_t*>(string.characters8()),
                            v8::NewStringType::kNormal, length)
             : v8::String::NewFromTwoByte(
                            isolate_,
                            reinterpret_cast<const uint16_t*>(string.characters16()),
                            v8::NewStringType::kNormal, length))
                    .ToLocalChecked();

//    v8::String::Utf8Value utf8(isolate_, message);
//    LOGI("v8engine receive %s", *utf8);

    Local<String> callback_name =
            v8::String::NewFromUtf8(isolate_, "receive", v8::NewStringType::kNormal)
                    .ToLocalChecked();
    Local<Context> context = context_.Get(isolate_);
    Local<Value> callback =
            context->Global()->Get(context, callback_name).ToLocalChecked();

//    v8::String::Utf8Value cal(isolate_, callback);
//    LOGI("v8engine receive %s", *cal);

    if (callback->IsFunction()) {
        v8::TryCatch try_catch(isolate_);
        Local<Value> args[] = {message};
        USE(Local<Function>::Cast(callback)->Call(context, Undefined(isolate_), 1,
                                                  args));

#ifdef DEBUG
        if (try_catch.HasCaught()) {
        Local<Object> exception = Local<Object>::Cast(try_catch.Exception());
        Local<String> key = v8::String::NewFromUtf8(isolate_, "message",
                                                    v8::NewStringType::kNormal)
                                .ToLocalChecked();
        Local<String> expected =
            v8::String::NewFromUtf8(isolate_,
                                    "Maximum call stack size exceeded",
                                    v8::NewStringType::kNormal)
                .ToLocalChecked();
        Local<Value> value = exception->Get(context, key).ToLocalChecked();
        DCHECK(value->StrictEquals(expected));
      }
#endif
    }
}