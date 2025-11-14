package LDS.Person.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务器监控控制器 - 获取当前 Spring 服务的内存和 JVM 虚拟机情况
 */
@RestController
@RequestMapping("/api/serverinfo")
@Api(tags = "服务监控", description = "获取服务信息")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ServerInfoController {

    /**
     * 获取完整的 JVM 和系统概览
     */
    @GetMapping("/JVMoverview")
    @ApiOperation(value = "获取JVM信息", notes = "返回 JVM 内存、系统信息、线程等所有信息的汇总")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> overviewData = new HashMap<>();
            
            // 内存信息
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
            Map<String, Object> memSummary = new HashMap<>();
            memSummary.put("堆已使用_MB", heapMemory.getUsed() / 1024 / 1024);
            memSummary.put("堆最大_MB", heapMemory.getMax() / 1024 / 1024);
            memSummary.put("堆使用率", String.format("%.2f%%", (double) heapMemory.getUsed() / heapMemory.getMax() * 100));
            overviewData.put("内存概览", memSummary);
            
            // CPU 和线程信息
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Map<String, Object> cpuSummary = new HashMap<>();
            cpuSummary.put("可用处理器数", osBean.getAvailableProcessors());
            cpuSummary.put("系统负载平均值", osBean.getSystemLoadAverage());
            overviewData.put("CPU概览", cpuSummary);
            
            // 线程信息
            Map<String, Object> threadSummary = new HashMap<>();
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            threadSummary.put("当前线程数", threadMXBean.getThreadCount());
            threadSummary.put("峰值线程数", threadMXBean.getPeakThreadCount());
            overviewData.put("线程概览", threadSummary);
            
            // JVM 版本
            Map<String, Object> jvmSummary = new HashMap<>();
            jvmSummary.put("Java版本", System.getProperty("java.version"));
            jvmSummary.put("JVM名称", System.getProperty("java.vm.name"));
            overviewData.put("JVM信息", jvmSummary);
            
            response.put("状态码", 200);
            response.put("消息", "✅ 概览信息获取成功");
            response.put("数据", overviewData);
            response.put("时间戳", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 获取概览信息失败", e);
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取服务启动时间与内存信息（轻量接口）
     */
    @GetMapping("/startup")
    @ApiOperation(value = "服务启动与内存信息", notes = "返回应用启动时间、运行时长以及内存使用情况（MB）")
    public ResponseEntity<Map<String, Object>> getStartupAndMemory() {
        Map<String, Object> response = new HashMap<>();
        try {
            java.lang.management.RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            long startMillis = runtimeMXBean.getStartTime();
            long uptimeMillis = runtimeMXBean.getUptime();

            Instant startInstant = Instant.ofEpochMilli(startMillis);
            long uptimeSeconds = Duration.ofMillis(uptimeMillis).getSeconds();

            // JVM 内存（Heap / Non-heap）
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

            Runtime rt = Runtime.getRuntime();
            long runtimeTotal = rt.totalMemory();
            long runtimeFree = rt.freeMemory();
            long runtimeUsed = runtimeTotal - runtimeFree;

            Map<String, Object> data = new HashMap<>();
            data.put("启动时间", startInstant.toString());
            data.put("运行时长(秒)", uptimeSeconds);

            Map<String, Object> heapInfo = new HashMap<>();
            heapInfo.put("堆_已用_MB", heap.getUsed() / 1024 / 1024);
            heapInfo.put("堆_已提交_MB", heap.getCommitted() / 1024 / 1024);
            heapInfo.put("堆_最大_MB", heap.getMax() / 1024 / 1024);
            data.put("堆内存", heapInfo);

            Map<String, Object> nonHeapInfo = new HashMap<>();
            nonHeapInfo.put("非堆_已用_MB", nonHeap.getUsed() / 1024 / 1024);
            nonHeapInfo.put("非堆_已提交_MB", nonHeap.getCommitted() / 1024 / 1024);
            data.put("非堆内存", nonHeapInfo);

            Map<String, Object> runtimeInfo = new HashMap<>();
            runtimeInfo.put("JVM总内存_MB", runtimeTotal / 1024 / 1024);
            runtimeInfo.put("JVM空闲内存_MB", runtimeFree / 1024 / 1024);
            runtimeInfo.put("JVM已用内存_MB", runtimeUsed / 1024 / 1024);
            data.put("运行时信息", runtimeInfo);

            response.put("状态码", 200);
            response.put("消息", "服务启动时间与内存信息（已本地化）");
            response.put("数据", data);
            response.put("时间戳", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 获取启动与内存信息失败", e);
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(response);
        }
    }
}
