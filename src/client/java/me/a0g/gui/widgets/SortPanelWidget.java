package me.a0g.gui.widgets;

import me.a0g.api.HistoryManager;
import me.a0g.api.WynnItem;
import me.a0g.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Боковая панель: однострочный ряд кнопок сортировки сверху, ниже список
 * последних запросов с иконками. ЛКМ по записи — повторно отправить в чат
 * через {@code onHistoryClick}; СКМ — удалить из истории.
 */
public class SortPanelWidget {
    private static final int HEADER_HEIGHT = 18;
    private static final int SORT_BUTTON_HEIGHT = 18;
    private static final int HISTORY_ROW_HEIGHT = 20;
    private static final int PADDING_X = 4;
    private static final int SORT_HISTORY_GAP = 2;

    private final int x, y, width, height;
    private final boolean showHistory;
    private final Runnable onSortChanged;
    private final HistoryManager history;
    private final Function<String, WynnItem> itemResolver;
    private final Consumer<String> onHistoryClick;

    public SortPanelWidget(int x, int y, int width, int height,
                           boolean showHistory,
                           Runnable onSortChanged,
                           HistoryManager history,
                           Function<String, WynnItem> itemResolver,
                           Consumer<String> onHistoryClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.showHistory = showHistory;
        this.onSortChanged = onSortChanged;
        this.history = history;
        this.itemResolver = itemResolver;
        this.onHistoryClick = onHistoryClick;
    }

    /** Минимальная высота когда история скрыта — только Sort-секция. */
    public static int minHeight() {
        // верхняя рамка + Sort header + 1px зазор + кнопки + 4px нижний паддинг + нижняя рамка
        return 1 + HEADER_HEIGHT + 1 + SORT_BUTTON_HEIGHT + 4 + 1;
    }

    public boolean isDescending() {
        return ModConfig.get().sortDescending;
    }

    private void setDescending(boolean desc) {
        if (ModConfig.get().sortDescending == desc) return;
        ModConfig.get().sortDescending = desc;
        AutoConfig.getConfigHolder(ModConfig.class).save();
        if (onSortChanged != null) onSortChanged.run();
    }

    // --- layout helpers ---
    private int sortHeaderY()    { return y + 1; }
    private int sortRowY()       { return sortHeaderY() + HEADER_HEIGHT + 1; }
    private int historyHeaderY() { return sortRowY() + SORT_BUTTON_HEIGHT + SORT_HISTORY_GAP; }
    private int historyListY()   { return historyHeaderY() + HEADER_HEIGHT + 3; }
    private int historyVisibleCount() {
        return Math.max(0, (y + height - 1 - historyListY()) / HISTORY_ROW_HEIGHT);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX >= x + width) return false;
        if (mouseY < y || mouseY >= y + height) return false;

        // Sort row
        int rowY = sortRowY();
        if (mouseY >= rowY && mouseY < rowY + SORT_BUTTON_HEIGHT && button == 0) {
            int half = (width - PADDING_X * 2) / 2;
            int ascX = x + PADDING_X;
            int descX = ascX + half;
            if (mouseX >= ascX && mouseX < ascX + half) {
                setDescending(false);
                return true;
            }
            if (mouseX >= descX && mouseX < descX + half) {
                setDescending(true);
                return true;
            }
        }

        // History list
        if (showHistory && history != null) {
            List<String> entries = history.getEntries();
            int visible = Math.min(entries.size(), historyVisibleCount());
            for (int i = 0; i < visible; i++) {
                int rY = historyListY() + i * HISTORY_ROW_HEIGHT;
                if (mouseY < rY || mouseY >= rY + HISTORY_ROW_HEIGHT) continue;
                if (mouseX < x + PADDING_X || mouseX >= x + width - PADDING_X) continue;

                String entry = entries.get(i);
                if (button == 0 && onHistoryClick != null) {
                    onHistoryClick.accept(entry);
                    return true;
                }
                if (button == 2) {
                    history.remove(entry);
                    return true;
                }
            }
        }

        return false;
    }

    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // Внешний контур + фон в том же стиле что и основная панель.
        context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        int border = 0xFF555555;
        context.drawHorizontalLine(x, x + width, y, border);
        context.drawHorizontalLine(x, x + width, y + height, border);
        context.drawVerticalLine(x, y, y + height, border);
        context.drawVerticalLine(x + width, y, y + height, border);

        // === Sort ===
        // Sort начинается у самой верхней рамки — отдельная верхняя линия не нужна.
        drawHeader(context, tr, sortHeaderY(), "Sort", false);

        boolean desc = isDescending();
        int half = (width - PADDING_X * 2) / 2;
        drawSortButton(context, tr, x + PADDING_X,        sortRowY() + 1, half, "A\u2192Z", !desc, mouseX, mouseY);
        drawSortButton(context, tr, x + PADDING_X + half, sortRowY() + 1, half, "Z\u2192A",  desc, mouseX, mouseY);

        if (!showHistory) return;

        // === History ===
        drawHeader(context, tr, historyHeaderY(), "History", true);

        if (history != null) {
            List<String> entries = history.getEntries();
            int visible = Math.min(entries.size(), historyVisibleCount());
            for (int i = 0; i < visible; i++) {
                int rY = historyListY() + i * HISTORY_ROW_HEIGHT;
                drawHistoryRow(context, tr, rY, entries.get(i), mouseX, mouseY);
            }
            if (entries.isEmpty()) {
                String hint = "(empty)";
                int hw = tr.getWidth(hint);
                context.drawText(tr, hint, x + (width - hw) / 2, historyListY() + 6, 0xFF666666, false);
            }
        }
    }

    private void drawHeader(DrawContext context, TextRenderer tr, int hy, String label, boolean drawTopLine) {
        // Заливаем всю ширину между боковыми рамками (включая колонку x+width-1).
        context.fill(x + 1, hy, x + width, hy + HEADER_HEIGHT, 0xFF2D2D2D);
        if (drawTopLine) {
            context.drawHorizontalLine(x + 1, x + width - 1, hy - 1, 0xFF444444);
        }
        context.drawHorizontalLine(x + 1, x + width - 1, hy + HEADER_HEIGHT, 0xFF444444);
        int w = tr.getWidth(label);
        context.drawText(tr, label, x + (width - w) / 2, hy + (HEADER_HEIGHT - 8) / 2, 0xFFFFFFFF, false);
    }

    private void drawSortButton(DrawContext context, TextRenderer tr,
                                int btnX, int btnY, int btnW, String label,
                                boolean selected, int mouseX, int mouseY) {
        boolean hovered = mouseX >= btnX && mouseX < btnX + btnW
                && mouseY >= btnY && mouseY < btnY + SORT_BUTTON_HEIGHT;

        int bg = selected ? 0xFF3A3A3A : (hovered ? 0xFF2F2F2F : 0xFF222222);
        int border = selected ? 0xFFFFCC33 : (hovered ? 0xFF777777 : 0xFF444444);

        context.fill(btnX, btnY, btnX + btnW, btnY + SORT_BUTTON_HEIGHT, bg);
        context.drawHorizontalLine(btnX, btnX + btnW - 1, btnY, border);
        context.drawHorizontalLine(btnX, btnX + btnW - 1, btnY + SORT_BUTTON_HEIGHT - 2, border);
        context.drawVerticalLine(btnX, btnY, btnY + SORT_BUTTON_HEIGHT - 2, border);
        context.drawVerticalLine(btnX + btnW - 1, btnY, btnY + SORT_BUTTON_HEIGHT - 2, border);

        int textColor = selected ? 0xFFFFCC33 : 0xFFE0E0E0;
        int textW = tr.getWidth(label);
        context.drawText(tr, label, btnX + (btnW - textW) / 2,
                btnY + (SORT_BUTTON_HEIGHT - 8) / 2, textColor, false);
    }

    private void drawHistoryRow(DrawContext context, TextRenderer tr,
                                int rY, String entry, int mouseX, int mouseY) {
        int rX = x + PADDING_X;
        int rW = width - PADDING_X * 2;
        boolean hovered = mouseX >= rX && mouseX < rX + rW
                && mouseY >= rY && mouseY < rY + HISTORY_ROW_HEIGHT;

        int bg = hovered ? 0xFF3A3A3A : 0xFF252525;
        int rowBorder = hovered ? 0xFF777777 : 0xFF3A3A3A;
        context.fill(rX, rY, rX + rW, rY + HISTORY_ROW_HEIGHT, bg);
        context.drawHorizontalLine(rX, rX + rW - 1, rY, rowBorder);
        context.drawHorizontalLine(rX, rX + rW - 1, rY + HISTORY_ROW_HEIGHT - 1, rowBorder);
        context.drawVerticalLine(rX, rY, rY + HISTORY_ROW_HEIGHT - 1, rowBorder);
        context.drawVerticalLine(rX + rW - 1, rY, rY + HISTORY_ROW_HEIGHT - 1, rowBorder);

        // Иконка слева, если предмет ещё известен.
        int textX = rX + 3;
        WynnItem resolved = itemResolver != null ? itemResolver.apply(entry) : null;
        if (resolved != null) {
            ItemStack stack = IconBuilder.stackFor(resolved);
            if (!stack.isEmpty()) {
                int iconX = rX + 2;
                int iconY = rY + (HISTORY_ROW_HEIGHT - 16) / 2;
                context.drawItem(stack, iconX, iconY);
                textX = iconX + 18;
            }
        }

        // Текст с обрезкой по ширине, цветом редкости если предмет найден.
        int textPad = 3;
        int maxTextW = rX + rW - textX - textPad;
        String shown = trimToWidth(tr, entry, maxTextW);
        int color = resolved != null ? (resolved.getRarityColorValue() | 0xFF000000) : 0xFFE0E0E0;
        context.drawText(tr, shown, textX, rY + (HISTORY_ROW_HEIGHT - 8) / 2, color, false);
    }

    private static String trimToWidth(TextRenderer tr, String s, int maxW) {
        if (tr.getWidth(s) <= maxW) return s;
        String ell = "\u2026";
        int ellW = tr.getWidth(ell);
        StringBuilder sb = new StringBuilder();
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            int cw = tr.getWidth(String.valueOf(s.charAt(i)));
            if (w + cw + ellW > maxW) break;
            sb.append(s.charAt(i));
            w += cw;
        }
        sb.append(ell);
        return sb.toString();
    }
}
