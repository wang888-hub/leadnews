package com.aaliyun.leadnews.ai.exception;

import com.aaliyun.leadnews.common.exception.BusinessException;

public final class AiExceptions {

 private AiExceptions() {
 }

 public static final class NotConfigured extends BusinessException {
  public NotConfigured() {
   super(50310, "AI capability is not configured");
  }
 }

 public static final class ProviderUnavailable extends BusinessException {
  public ProviderUnavailable() {
   super(50311, "AI provider is unavailable");
  }
 }

 public static final class Timeout extends BusinessException {
  public Timeout() {
   super(50410, "AI provider timed out");
  }
 }

 public static final class OutputParse extends BusinessException {
  public OutputParse() {
   super(50210, "AI structured output is invalid");
  }
 }

 public static final class CapacityExceeded extends BusinessException {
  public CapacityExceeded() {
   super(50312, "AI concurrency capacity exceeded");
  }
 }

 public static final class InvalidImage extends BusinessException {
  public InvalidImage(String message) {
   super(40010, message);
  }
 }
}