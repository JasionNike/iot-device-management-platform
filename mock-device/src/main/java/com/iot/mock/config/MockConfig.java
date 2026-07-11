package com.iot.mock.config;

/** 挡板配置 */
public class MockConfig {
    private final String platformUrl;
    private final int reportIntervalSec;

    public MockConfig(String platformUrl, int reportIntervalSec) {
        this.platformUrl = platformUrl;
        this.reportIntervalSec = reportIntervalSec;
    }

    public String getPlatformUrl() { return platformUrl; }
    public int getReportIntervalSec() { return reportIntervalSec; }
}
