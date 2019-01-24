package com.emercy.v8;

public interface ExtractPolicy {
    boolean shouldExtract(android.content.Context context);

    boolean forceOverwrite();

    FileExtractor extractor();
}
