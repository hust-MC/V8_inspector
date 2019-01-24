package com.example.zhanfang.test;

public interface ExtractPolicy {
    boolean shouldExtract(android.content.Context context);

    boolean forceOverwrite();

    FileExtractor extractor();
}
