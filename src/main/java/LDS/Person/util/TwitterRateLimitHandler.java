package LDS.Person.util;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Twitter API 速率限制处理工具
 * 
 * 功能：
 * 1. 检测 429 Too Many Requests 错误
 * 2. 提取 Retry-After 信息
 * 3. 实现请求重试机制
 * 4. 追踪 API 请求计数
 */
@Component
@Slf4j
public class TwitterRateLimitHandler {

  /**
   * 最近请求的时间戳队列（用于计算请求频率）
   * key: endpoint，value: 请求时间戳队列
   */
  private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestTimestamps = new ConcurrentHashMap<>();

  /**
   * 速率限制重置时间缓存
   * key: endpoint，value: 重置时间戳
   */
  private final ConcurrentHashMap<String, Long> rateLimitResets = new ConcurrentHashMap<>();

  /**
   * 解析异常错误信息，提取 Retry-After 时间
   *
   * @param exception HTTP 异常
   * @return Retry-After 时间（秒），默认返回 60
   */
  public long extractRetryAfter(HttpClientErrorException exception) {
    try {
      // 优先从 Response Header 读取 Retry-After
      String retryAfterHeader = exception.getResponseHeaders() != null
          ? exception.getResponseHeaders().getFirst("Retry-After")
          : null;

      if (retryAfterHeader != null) {
        long retryAfter = Long.parseLong(retryAfterHeader);
        log.warn("Twitter API 返回 Retry-After: {} 秒", retryAfter);
        return retryAfter;
      }

      // 尝试从响应体解析 reset 时间
      try {
        String body = exception.getResponseBodyAsString();
        if (body != null && !body.isEmpty()) {
          JSONObject json = JSONObject.parseObject(body);

          // 检查响应中的 reset_time 或类似字段
          if (json.containsKey("reset_time")) {
            long resetTime = json.getLongValue("reset_time");
            long now = System.currentTimeMillis() / 1000;
            long retryAfter = Math.max(1, resetTime - now);
            log.warn("从响应体提取 reset_time，需要等待: {} 秒", retryAfter);
            return retryAfter;
          }
        }
      } catch (Exception e) {
        log.debug("解析响应体失败", e);
      }

      // 默认等待 60 秒
      log.warn("无法提取 Retry-After，使用默认值 60 秒");
      return 60;

    } catch (Exception e) {
      log.error("提取 Retry-After 时出错", e);
      return 60; // 默认值
    }
  }

  /**
   * 判断是否是速率限制错误
   */
  public boolean isRateLimitError(Exception exception) {
    if (exception instanceof HttpClientErrorException) {
      HttpClientErrorException httpException = (HttpClientErrorException) exception;
      return httpException.getStatusCode().value() == 429;
    }
    return false;
  }

  /**
   * 记录 API 请求（用于监控请求频率）
   *
   * @param endpoint API 端点（例如 "/users/{id}/tweets"）
   */
  public void recordRequest(String endpoint) {
    long now = System.currentTimeMillis();
    requestTimestamps.computeIfAbsent(endpoint, k -> new ConcurrentLinkedQueue<>())
        .offer(now);

    // 清理 15 分钟前的请求记录
    long fifteenMinutesAgo = now - (15 * 60 * 1000);
    ConcurrentLinkedQueue<Long> timestamps = requestTimestamps.get(endpoint);
    while (!timestamps.isEmpty() && timestamps.peek() < fifteenMinutesAgo) {
      timestamps.poll();
    }
  }

  /**
   * 获取最近 15 分钟内的请求数
   *
   * @param endpoint API 端点
   * @return 请求数
   */
  public int getRequestCountInLastFifteenMinutes(String endpoint) {
    ConcurrentLinkedQueue<Long> timestamps = requestTimestamps.get(endpoint);
    if (timestamps == null) {
      return 0;
    }

    long now = System.currentTimeMillis();
    long fifteenMinutesAgo = now - (15 * 60 * 1000);

    return (int) timestamps.stream()
        .filter(timestamp -> timestamp >= fifteenMinutesAgo)
        .count();
  }

  /**
   * 设置速率限制重置时间
   *
   * @param endpoint       API 端点
   * @param resetTimestamp 重置时间戳（毫秒）
   */
  public void setRateLimitReset(String endpoint, long resetTimestamp) {
    rateLimitResets.put(endpoint, resetTimestamp);
  }

  /**
   * 获取距离下次可用的等待时间
   *
   * @param endpoint API 端点
   * @return 等待时间（秒），如果无限制返回 0
   */
  public long getWaitTimeUntilReset(String endpoint) {
    Long resetTime = rateLimitResets.get(endpoint);
    if (resetTime == null) {
      return 0;
    }

    long waitTime = (resetTime - System.currentTimeMillis()) / 1000;
    return Math.max(0, waitTime);
  }

  /**
   * 带重试的 API 调用包装
   *
   * @param action     要执行的操作
   * @param maxRetries 最大重试次数
   * @return 操作结果
   */
  public <T> T executeWithRetry(ApiAction<T> action, int maxRetries) {
    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        return action.execute();
      } catch (HttpClientErrorException e) {
        if (!isRateLimitError(e)) {
          throw e;
        }

        if (attempt < maxRetries) {
          long retryAfter = extractRetryAfter(e);
          log.warn("触发速率限制，第 {} 次重试，等待 {} 秒后重试...", attempt + 1, retryAfter);

          try {
            Thread.sleep(retryAfter * 1000);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试被中断", ie);
          }
        } else {
          log.error("已达最大重试次数 {}，放弃请求", maxRetries);
          throw e;
        }
      }
    }

    return null;
  }

  /**
   * 检查是否应该限流
   *
   * @param endpoint  API 端点
   * @param threshold 阈值（每 15 分钟允许的最大请求数）
   * @return true 表示应该限流，false 表示可以继续请求
   */
  public boolean shouldThrottle(String endpoint, int threshold) {
    int count = getRequestCountInLastFifteenMinutes(endpoint);
    if (count > threshold) {
      log.warn("达到限流阈值：endpoint={}, count={}, threshold={}", endpoint, count, threshold);
      return true;
    }
    return false;
  }

  /**
   * API 操作接口
   */
  @FunctionalInterface
  public interface ApiAction<T> {
    T execute() throws HttpClientErrorException;
  }

  /**
   * 获取诊断信息
   */
  public String getDiagnosticsInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("=== Twitter API 速率限制诊断 ===\n");

    for (String endpoint : requestTimestamps.keySet()) {
      int count = getRequestCountInLastFifteenMinutes(endpoint);
      long waitTime = getWaitTimeUntilReset(endpoint);
      sb.append(String.format("端点: %s\n", endpoint));
      sb.append(String.format("  最近15分钟请求数: %d\n", count));
      sb.append(String.format("  距离重置还需: %d 秒\n", waitTime));
    }

    return sb.toString();
  }
}
