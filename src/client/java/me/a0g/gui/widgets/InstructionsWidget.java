package me.a0g.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Маленькая подсказка по управлению, рисуется в углу экрана. Контент
 * статичный — пары [клавиша → действие] подгоняются под наши хендлеры.
 */
public class InstructionsWidget {
    private static final int HEADER_HEIGHT = 16;
    private static final int LINE_HEIGHT = 11;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;

    private static final String HEADER = "How it works";
    private static final String[][] LINES = {
            {"LMB",  "send to chat"},
            {"RMB",  "pin to slot"},
            {"MMB",  "favorite"},
            {"Enter","send typed text"},
            {"ESC",  "cancel"},
    };

    public static int width(MinecraftClient client) {
        TextRenderer tr = client.textRenderer;
        int maxKey = 0;
        int maxAct = 0;
        for (String[] line : LINES) {
            maxKey = Math.max(maxKey, tr.getWidth(line[0]));
            maxAct = Math.max(maxAct, tr.getWidth(line[1]));
        }
        return Math.max(tr.getWidth(HEADER), maxKey + 8 + maxAct) + PADDING_X * 2;
    }

    public static int height() {
        return HEADER_HEIGHT + LINES.length * LINE_HEIGHT + PADDING_Y * 2 + 1;
    }

    private final int x, y, width, height;

    public InstructionsWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // Фон + рамка в едином стиле с остальными панелями.
        context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        int border = 0xFF555555;
        context.drawHorizontalLine(x, x + width, y, border);
        context.drawHorizontalLine(x, x + width, y + height, border);
        context.drawVerticalLine(x, y, y + height, border);
        context.drawVerticalLine(x + width, y, y + height, border);

        // Шапка
        context.fill(x + 1, y + 1, x + width, y + 1 + HEADER_HEIGHT, 0xFF2D2D2D);
        context.drawHorizontalLine(x + 1, x + width - 1, y + 1 + HEADER_HEIGHT, 0xFF444444);
        int hw = tr.getWidth(HEADER);
        context.drawText(tr, HEADER,
                x + (width - hw) / 2, y + 1 + (HEADER_HEIGHT - 8) / 2,
                0xFFFFFFFF, false);

        // Строки
        int lineY = y + 1 + HEADER_HEIGHT + PADDING_Y;
        // Самая широкая клавиша → выравнивание по столбцу
        int keyCol = 0;
        for (String[] line : LINES) keyCol = Math.max(keyCol, tr.getWidth(line[0]));
        for (String[] line : LINES) {
            context.drawText(tr, line[0], x + PADDING_X, lineY, 0xFFFFCC33, false);
            context.drawText(tr, line[1], x + PADDING_X + keyCol + 6, lineY, 0xFFCFCFCF, false);
            lineY += LINE_HEIGHT;
        }
    }
}
