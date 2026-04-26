package me.a0g.gui.widgets;

import me.a0g.api.PinnedSlotsManager;
import me.a0g.api.WynnItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Нижняя панель «закреплённые предметы». Два ряда по 9 слотов 22×22, как
 * расширенный хотбар. ЛКМ по слоту — отправить displayName в чат через
 * {@code onSlotClick}; ПКМ — снять закрепление.
 */
public class PinnedSlotsWidget {
    public static final int SLOT_SIZE = 22;
    public static final int SLOTS_PER_ROW = 9;
    public static final int ROWS = 2;
    private static final int SLOT_GAP = 1;

    private final int x, y, width, height;
    private final PinnedSlotsManager slots;
    private final Function<String, WynnItem> itemResolver;
    private final Consumer<String> onSlotClick;

    public PinnedSlotsWidget(int x, int y, int width, int height,
                             PinnedSlotsManager slots,
                             Function<String, WynnItem> itemResolver,
                             Consumer<String> onSlotClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.slots = slots;
        this.itemResolver = itemResolver;
        this.onSlotClick = onSlotClick;
    }

    private int gridWidth()  { return SLOTS_PER_ROW * SLOT_SIZE + (SLOTS_PER_ROW - 1) * SLOT_GAP; }
    private int gridHeight() { return ROWS * SLOT_SIZE + (ROWS - 1) * SLOT_GAP; }
    private int gridX()      { return x + (width - gridWidth()) / 2; }
    private int gridY()      { return y + (height - gridHeight()) / 2; }

    private int slotIndexAt(double mouseX, double mouseY) {
        int gx = gridX(), gy = gridY();
        if (mouseX < gx || mouseY < gy) return -1;
        int relX = (int) (mouseX - gx);
        int relY = (int) (mouseY - gy);
        int col = relX / (SLOT_SIZE + SLOT_GAP);
        int row = relY / (SLOT_SIZE + SLOT_GAP);
        if (col < 0 || col >= SLOTS_PER_ROW || row < 0 || row >= ROWS) return -1;
        // Проверяем что не попали в gap-полоску.
        int colStart = col * (SLOT_SIZE + SLOT_GAP);
        int rowStart = row * (SLOT_SIZE + SLOT_GAP);
        if (relX - colStart >= SLOT_SIZE) return -1;
        if (relY - rowStart >= SLOT_SIZE) return -1;
        return row * SLOTS_PER_ROW + col;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return false;

        int idx = slotIndexAt(mouseX, mouseY);
        if (idx < 0) return false;
        List<String> entries = slots.getSlots();
        if (idx >= entries.size()) return false;
        String name = entries.get(idx);

        if (button == 0 && onSlotClick != null) {
            onSlotClick.accept(name);
            return true;
        }
        if (button == 1) {
            slots.removeAt(idx);
            return true;
        }
        return false;
    }

    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // Внешний контур + фон в стиле основной панели.
        context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        int border = 0xFF555555;
        context.drawHorizontalLine(x, x + width, y, border);
        context.drawHorizontalLine(x, x + width, y + height, border);
        context.drawVerticalLine(x, y, y + height, border);
        context.drawVerticalLine(x + width, y, y + height, border);

        List<String> entries = slots.getSlots();
        int gx = gridX(), gy = gridY();
        int hovered = slotIndexAt(mouseX, mouseY);

        for (int i = 0; i < SLOTS_PER_ROW * ROWS; i++) {
            int col = i % SLOTS_PER_ROW;
            int row = i / SLOTS_PER_ROW;
            int sx = gx + col * (SLOT_SIZE + SLOT_GAP);
            int sy = gy + row * (SLOT_SIZE + SLOT_GAP);
            boolean isHovered = i == hovered;
            boolean filled = i < entries.size();

            int bg = isHovered ? 0xFF3A3A3A : 0xFF222222;
            int slotBorder = isHovered ? 0xFF777777 : 0xFF444444;
            context.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, bg);
            context.drawHorizontalLine(sx, sx + SLOT_SIZE - 1, sy, slotBorder);
            context.drawHorizontalLine(sx, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, slotBorder);
            context.drawVerticalLine(sx, sy, sy + SLOT_SIZE - 1, slotBorder);
            context.drawVerticalLine(sx + SLOT_SIZE - 1, sy, sy + SLOT_SIZE - 1, slotBorder);

            if (filled) {
                String name = entries.get(i);
                WynnItem resolved = itemResolver != null ? itemResolver.apply(name) : null;
                if (resolved != null) {
                    ItemStack stack = IconBuilder.stackFor(resolved);
                    if (!stack.isEmpty()) {
                        int iconX = sx + (SLOT_SIZE - 16) / 2;
                        int iconY = sy + (SLOT_SIZE - 16) / 2;
                        context.drawItem(stack, iconX, iconY);
                    }
                }
            }
        }

        // Тултип над hovered-слотом.
        if (hovered >= 0 && hovered < entries.size()) {
            // Тултип рендерим в screen-space через context.drawTooltip — но тут
            // мы ещё внутри scaled-блока; используем простой текст-всплывашку.
            String name = entries.get(hovered);
            int tx = (int) mouseX + 8;
            int ty = (int) mouseY - 12;
            int w = tr.getWidth(name) + 6;
            context.fill(tx - 1, ty - 1, tx + w, ty + 10, 0xFF101010);
            context.fill(tx, ty, tx + w - 2, ty + 9, 0xFF1F1F1F);
            context.drawText(tr, Text.literal(name), tx + 2, ty + 1, 0xFFE0E0E0, false);
        }
    }
}
