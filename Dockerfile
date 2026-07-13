# ============================================================
# 智能物联设备管理平台 — Backend Dockerfile
# ============================================================
FROM eclipse-temurin:8-jre

LABEL maintainer="wangheng <535698505@qq.com>"
LABEL description="IoT Device Management Platform Backend"

WORKDIR /app

# 时区设置
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8 -Djasypt.encryptor.password=iot-platform-jasypt-key"

COPY backend/target/iot-platform-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
