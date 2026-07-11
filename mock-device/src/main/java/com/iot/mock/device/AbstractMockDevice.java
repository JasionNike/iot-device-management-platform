package com.iot.mock.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.mock.config.MockConfig;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 抽象模拟设备基类
 * 所有模拟设备类型的共同行为：
 * 1. 向平台注册设备
 * 2. 定时上报遥测数据
 * 3. 定时发送心跳
 * 4. 随机触发事件/告警
 *
 * @author 王恒
 */
public abstract class AbstractMockDevice {

    protected final String deviceSn;
    protected final MockConfig config;
    protected final int deviceIndex;
    protected final Random random = new Random();
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected String deviceId;
    protected String deviceName;
    protected String productKey;
    protected boolean registered = false;

    public AbstractMockDevice(String deviceSn, MockConfig config, int index) {
        this.deviceSn = deviceSn;
        this.config = config;
        this.deviceIndex = index;
    }

    /** 获取产品型号 */
    public abstract String getProductKey();

    /** 获取设备名称前缀 */
    public abstract String getDeviceTypeName();

    /** 生成遥测数据（每个设备类型不同） */
    public abstract Map<String, Object> generateTelemetry();

    /**
     * 注册设备并启动定时上报
     */
    public void register(ScheduledExecutorService scheduler) {
        this.productKey = getProductKey();
        this.deviceName = getDeviceTypeName() + "-" + String.format("%04d", deviceIndex);

        // 向平台注册
        try {
            Map<String, Object> result = callApi("POST", "/device/register", buildRegisterPayload());
            if (result != null && result.containsKey("deviceId")) {
                this.deviceId = (String) result.get("deviceId");
                this.registered = true;
                System.out.println("  [" + deviceSn + "] registered -> " + deviceId);
            }
        } catch (Exception e) {
            System.err.println("  [" + deviceSn + "] register failed: " + e.getMessage());
            this.deviceId = "DEV-" + deviceSn;
        }

        // 定时上报遥测
        scheduler.scheduleAtFixedRate(() -> {
            if (!registered) return;
            try {
                Map<String, Object> telemetry = generateTelemetry();
                callApi("POST", "/shadow/reported/" + deviceId, telemetry);
                // 5%概率触发告警事件
                if (random.nextInt(100) < 5) {
                    triggerRandomAlert(telemetry);
                }
            } catch (Exception ignored) {}
        }, random.nextInt(10), config.getReportIntervalSec(), TimeUnit.SECONDS);

        // 定时心跳
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> hb = new HashMap<>();
                hb.put("deviceId", deviceId);
                hb.put("timestamp", System.currentTimeMillis());
                callApi("POST", "/device/detail/" + deviceId, null);
            } catch (Exception ignored) {}
        }, 5, 30, TimeUnit.SECONDS);
    }

    /** 触发随机告警 */
    protected void triggerRandomAlert(Map<String, Object> telemetry) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("deviceId", deviceId);
            event.put("eventType", "ALARM");
            event.putAll(telemetry);
            callApi("POST", "/shadow/reported/" + deviceId + "?alert=true", event);
        } catch (Exception ignored) {}
    }

    /** 构建注册请求 */
    protected Map<String, Object> buildRegisterPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceName", deviceName);
        payload.put("productKey", productKey);
        payload.put("deviceSn", deviceSn);
        payload.put("location", "Mock-Simulator-Location-" + (deviceIndex % 10));
        return payload;
    }

    /** HTTP API调用 */
    protected Map<String, Object> callApi(String method, String path, Map<String, Object> body) throws Exception {
        String url = config.getPlatformUrl() + path;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        if (body != null) {
            conn.setDoOutput(true);
            String json = objectMapper.writeValueAsString(body);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
            }
        }
        int code = conn.getResponseCode();
        if (code == 200) {
            java.io.InputStream is = conn.getInputStream();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            is.close();
            String resp = baos.toString("UTF-8");
            Map<String, Object> result = objectMapper.readValue(resp, Map.class);
            return (Map<String, Object>) result.get("data");
        }
        conn.disconnect();
        return null;
    }
}
