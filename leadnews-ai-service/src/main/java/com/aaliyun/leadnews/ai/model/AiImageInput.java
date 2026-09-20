package com.aaliyun.leadnews.ai.model;
import java.net.URI;
public record AiImageInput(String mimeType, byte[] bytes, URI url) {}
