package com.example.jobpuzzle.document.extraction;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

final class OcrTestImages {

    private OcrTestImages() {
    }

    static BufferedImage createTextImage(String text) {
        BufferedImage image = new BufferedImage(600, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("맑은 고딕", Font.PLAIN, 40));
        graphics.drawString(text, 20, 80);
        graphics.dispose();
        return image;
    }

    static BufferedImage createParagraphImage(String[] lines) {
        // 실제 PDFRenderer가 300DPI로 렌더링하는 것과 비슷한 밀도를 흉내내기 위해 스케일을 키움
        int scale = 3;
        int lineHeight = 50 * scale;
        BufferedImage image = new BufferedImage(1000 * scale, lineHeight * lines.length + 40 * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("맑은 고딕", Font.PLAIN, 28 * scale));
        for (int i = 0; i < lines.length; i++) {
            graphics.drawString(lines[i], 20 * scale, (40 + i * 50) * scale);
        }
        graphics.dispose();
        return image;
    }
}