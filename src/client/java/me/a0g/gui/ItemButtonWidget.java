package me.a0g.gui;

import me.a0g.api.WynnItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * Кнопка для отображения предмета в списке поиска
 */
public class ItemButtonWidget implements Element, Selectable {
    private final int x, y, width, height;
    private WynnItem item;
    private final Consumer<WynnItem> onSelect;
    private ItemStack itemStack = ItemStack.EMPTY;
    private boolean hovered = false;

    public ItemButtonWidget(int x, int y, int width, int height, Consumer<WynnItem> onSelect) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.item = null;
        this.onSelect = onSelect;
    }

    public void setItem(WynnItem newItem) {
        this.item = newItem;
        updateItemStack();
    }

    public WynnItem getItem() {
        return item;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void updateItemStack() {
        if (item == null || item.getIcon() == null) {
            this.itemStack = ItemStack.EMPTY;
            return;
        }

        try {
            var iconJson = item.getIcon();
            if (iconJson.has("format") && iconJson.has("value")) {
                String format = iconJson.get("format").getAsString();
                var valueJson = iconJson.getAsJsonObject("value");

                if ("attribute".equals(format)) {
                    String itemId = valueJson.has("id") ? valueJson.get("id").getAsString() : "minecraft:barrier";
                    var mcItem = net.minecraft.registry.Registries.ITEM.get(Identifier.tryParse(itemId));

                    if (mcItem != null && mcItem != Items.AIR) {
                        this.itemStack = new ItemStack(mcItem);
                    } else {
                        this.itemStack = new ItemStack(Items.BARRIER);
                    }
                } else if ("skin".equals(format)) {
                    this.itemStack = new ItemStack(Items.PLAYER_HEAD);
                } else {
                    this.itemStack = new ItemStack(Items.BARRIER);
                }
            } else {
                this.itemStack = new ItemStack(Items.BARRIER);
            }
        } catch (Exception e) {
            this.itemStack = new ItemStack(Items.BARRIER);
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Проверка наведения
        hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        // Фон кнопки
        int bgColor = hovered ? 0xFF3A3A3A : 0xFF2A2A2A;
        context.fill(x, y, x + width, y + height, bgColor);

        // Рамка
        int borderColor = hovered ? 0xFF777777 : 0xFF444444;
        context.drawHorizontalLine(x, x + width, y, borderColor);
        context.drawHorizontalLine(x, x + width, y + height - 1, borderColor);
        context.drawVerticalLine(x, y, y + height, borderColor);
        context.drawVerticalLine(x + width - 1, y, y + height, borderColor);

        // Рендер иконки предмета
        if (!itemStack.isEmpty()) {
            context.drawItem(itemStack, x + 4, y + (height - 16) / 2);
        }

        // Рендер текста
        if (item != null) {
            String itemName = item.getName();
            if (itemName != null && !itemName.isEmpty()) {
                // Получаем цвет из редкости
                int textColor;
                if (hovered) {
                    textColor = 0xFFFFFF55; // Желтый при наведении
                } else {
                    textColor = item.getRarityColorValue();
                }
                
                context.drawTextWithShadow(
                        client.textRenderer,
                        itemName,
                        x + 24,
                        y + 14,
                        textColor
                );
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered && button == 0 && item != null) {
            onSelect.accept(item);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public SelectionType getType() {
        return hovered ? SelectionType.HOVERED : SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        // Пустая реализация
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        // Не поддерживаем фокус
    }
}
