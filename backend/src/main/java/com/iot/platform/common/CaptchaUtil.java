package com.iot.platform.common;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

/**
 * 图形验证码生成工具类
 * <p>
 * 生成4位数字+字母混合验证码，包含干扰线和噪点。
 * 参照商业项目标准实现：透明背景、随机颜色、旋转字符、干扰元素。
 * <p>
 * 输出格式：PNG → Base64，适合前后端分离架构。
 *
 * @author 王恒
 */
public class CaptchaUtil {

    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;
    private static final int CODE_LENGTH = 4;
    /** 排除易混淆字符：0/O/I/1/l */
    private static final String CHAR_POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final Random RNG = new Random();
    private static final Font[] FONTS = {
            new Font("Arial", Font.BOLD, 28),
            new Font("Arial", Font.ITALIC, 26),
            new Font("Arial", Font.PLAIN, 30)
    };

    /**
     * 生成验证码图片
     *
     * @return CaptchaResult 包含Base64图片和验证码文本
     */
    public static CaptchaResult generate() {
        String code = generateCode();
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 启用抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. 填充背景（浅灰渐变）
        GradientPaint gradient = new GradientPaint(0, 0, new Color(245, 247, 250),
                WIDTH, HEIGHT, new Color(232, 238, 245));
        g.setPaint(gradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. 绘制干扰线（6条随机弧线）
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor(100, 180));
            int x1 = RNG.nextInt(WIDTH);
            int y1 = RNG.nextInt(HEIGHT);
            int x2 = x1 + RNG.nextInt(60) - 30;
            int y2 = y1 + RNG.nextInt(30) - 15;
            g.setStroke(new BasicStroke(1.5f + RNG.nextFloat()));
            g.drawLine(x1, y1, x2, y2);
        }

        // 3. 绘制干扰点（约80个随机噪点）
        for (int i = 0; i < 80; i++) {
            g.setColor(randomColor(80, 200));
            int x = RNG.nextInt(WIDTH);
            int y = RNG.nextInt(HEIGHT);
            g.fillOval(x, y, 2, 2);
        }

        // 4. 绘制验证码字符（逐个旋转+偏移）
        int charWidth = WIDTH / (CODE_LENGTH + 1);
        for (int i = 0; i < code.length(); i++) {
            String ch = String.valueOf(code.charAt(i));
            g.setFont(FONTS[RNG.nextInt(FONTS.length)]);
            g.setColor(randomColor(30, 120));

            // 随机旋转角度 ±15度
            double angle = Math.toRadians(RNG.nextDouble() * 30 - 15);
            g.rotate(angle, charWidth * (i + 0.7), HEIGHT / 2.0 + RNG.nextInt(8) - 4);

            // 随机Y轴偏移
            int x = charWidth * i + RNG.nextInt(8) + 8;
            int y = HEIGHT - RNG.nextInt(12) - 8;
            g.drawString(ch, x, y);

            // 恢复旋转
            g.rotate(-angle, charWidth * (i + 0.7), HEIGHT / 2.0 + RNG.nextInt(8) - 4);
        }

        // 5. 绘制干扰边框
        g.setColor(randomColor(150, 200));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(2, 2, WIDTH - 4, HEIGHT - 4, 6, 6);

        g.dispose();

        // 编码为Base64
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            String base64 = "data:image/png;base64," +
                    Base64.getEncoder().encodeToString(baos.toByteArray());
            return new CaptchaResult(code, base64);
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败", e);
        }
    }

    private static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHAR_POOL.charAt(RNG.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    private static Color randomColor(int min, int max) {
        return new Color(
                min + RNG.nextInt(max - min),
                min + RNG.nextInt(max - min),
                min + RNG.nextInt(max - min)
        );
    }

    /**
     * 验证码生成结果
     */
    public static class CaptchaResult {
        private final String code;
        private final String base64Image;

        public CaptchaResult(String code, String base64Image) {
            this.code = code;
            this.base64Image = base64Image;
        }

        public String getCode() { return code; }
        public String getBase64Image() { return base64Image; }
    }
}
