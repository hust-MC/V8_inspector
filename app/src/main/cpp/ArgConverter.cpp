#include "ArgConverter.h"
#include "Util.h"
#include <sstream>
#include "JEnv.h"

using namespace v8;
using namespace std;
using namespace tns;

jstring ArgConverter::ObjectToString(jobject object) {
    return (jstring) object;
}

std::string ArgConverter::jstringToString(jstring value) {
    if (value == nullptr) {
        return string();
    }

    JEnv env;

    jboolean f = JNI_FALSE;
    auto chars = env.GetStringUTFChars(value, &f);
    string s(chars);
    env.ReleaseStringUTFChars(value, chars);

    return s;
}

Local<Value> ArgConverter::jstringToV8String(Isolate* isolate, jstring value) {
    if (value == nullptr) {
        return Null(isolate);
    }

    JEnv env;
    auto chars = env.GetStringChars(value, NULL);
    auto length = env.GetStringLength(value);
    auto v8String = ConvertToV8String(isolate, chars, length);
    env.ReleaseStringChars(value, chars);

    return v8String;
}

Local<String> ArgConverter::jcharToV8String(Isolate* isolate, jchar value) {
    auto v8String = ConvertToV8String(isolate, &value, 1);
    return v8String;
}

Local<Value> ArgConverter::ConvertFromJavaLong(Isolate* isolate, jlong value) {
    Local<Value> convertedValue;
    long long longValue = value;

    if ((-JS_LONG_LIMIT < longValue) && (longValue < JS_LONG_LIMIT)) {
        convertedValue = Number::New(isolate, longValue);
    } else {
        auto cache = GetTypeLongCache(isolate);
        char strNumber[24];
        sprintf(strNumber, "%lld", longValue);
        Local<Value> strValue = ConvertToV8String(isolate, strNumber);
        auto context = isolate->GetCurrentContext();
        convertedValue = Local<Function>::New(isolate, *cache->LongNumberCtorFunc)->CallAsConstructor(context, 1, &strValue).ToLocalChecked();
    }

    return convertedValue;
}

ArgConverter::TypeLongOperationsCache* ArgConverter::GetTypeLongCache(v8::Isolate* isolate) {
    TypeLongOperationsCache* cache;
    auto itFound = s_type_long_operations_cache.find(isolate);
    if (itFound == s_type_long_operations_cache.end()) {
        cache = new TypeLongOperationsCache;
        s_type_long_operations_cache.insert(make_pair(isolate, cache));
    } else {
        cache = itFound->second;
    }

    return cache;
}


string ArgConverter::ConvertToString(const v8::Local<String>& s) {
    if (s.IsEmpty()) {
        return string();
    } else {
        String::Utf8Value str(s);
        return string(*str);
    }
}

u16string ArgConverter::ConvertToUtf16String(const v8::Local<String>& s) {
    if (s.IsEmpty()) {
        return u16string();
    } else {
        auto str = ConvertToString(s);
        auto utf16str =  Util::ConvertFromUtf8ToUtf16(str);

        return utf16str;
    }
}

jstring ArgConverter::ConvertToJavaString(const Local<Value>& value) {
    JEnv env;
    String::Value stringValue(value);
    return env.NewString((const jchar*) *stringValue, stringValue.length());
}

Local<String> ArgConverter::ConvertToV8String(Isolate* isolate, const jchar* data, int length) {
    return String::NewFromTwoByte(isolate, (const uint16_t*) data, String::kNormalString, length);
}

Local<String> ArgConverter::ConvertToV8String(Isolate* isolate, const string& s) {
    Local<String> str =	String::NewFromUtf8(isolate, s.c_str(), NewStringType::kNormal, s.length()).ToLocalChecked();
    return str;
}

Local<String> ArgConverter::ConvertToV8String(Isolate* isolate, const char* data, int length) {
    return String::NewFromUtf8(isolate, (const char*) data, String::kNormalString, length);
}

Local<String> ArgConverter::ConvertToV8UTF16String(Isolate* isolate, const string& string) {
    auto utf16str = Util::ConvertFromUtf8ToUtf16(string);

    return ConvertToV8UTF16String(isolate, utf16str);
}

Local<String> ArgConverter::ConvertToV8UTF16String(Isolate* isolate, const u16string& utf16string) {
    return String::NewFromTwoByte(isolate, ((const uint16_t*) utf16string.data()));
}

Local<String> ArgConverter::ConvertToV8UTF16String(v8::Isolate* isolate, const uint16_t* utf16string, int size) {
    return String::NewFromTwoByte(isolate, utf16string, NewStringType::kNormal, size).ToLocalChecked();
}

std::map<Isolate*, ArgConverter::TypeLongOperationsCache*> ArgConverter::s_type_long_operations_cache;