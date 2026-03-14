package me.a0g.gui.widgets;

import me.a0g.api.WynnItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * Виджет кнопки предмета (аналог Widget из WynnExtras)
 */
public class ItemButtonWidget {
    protected int x, y, width, height;
    protected boolean visible = true;
    protected boolean hovered = false;

    private WynnItem item;
    private ItemStack itemStack = ItemStack.EMPTY;
    private final Consumer<WynnItem> onClick;

    public ItemButtonWidget(int x, int y, int width, int height, Consumer<WynnItem> onClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onClick = onClick;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setItem(WynnItem item) {
        this.item = item;
        updateItemStack();
    }

    public WynnItem getItem() {
        return item;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

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
                        
                        // Устанавливаем CustomModelData для кастомных текстур Wynncraft
                        if (valueJson.has("customModelData")) {
                            var cmdJson = valueJson.getAsJsonObject("customModelData");
                            if (cmdJson.has("rangeDispatch")) {
                                var rangeArray = cmdJson.getAsJsonArray("rangeDispatch");
                                if (rangeArray.size() > 0) {
                                    int cmd = rangeArray.get(0).getAsInt();
                                    this.itemStack.set(
                                            net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
                                            new net.minecraft.component.type.CustomModelDataComponent(
                                                    java.util.List.of((float) cmd),
                                                    java.util.List.of(),
                                                    java.util.List.of(),
                                                    java.util.List.of()
                                            )
                                    );
                                }
                            }
                        }
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

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        MinecraftClient client = MinecraftClient.getInstance();

        // Обновляем hover состояние
        hovered = isMouseOver(mouseX, mouseY);

        // Фон кнопки
        int bgColor = hovered ? 0xFF3A3A3A : 0xFF2A2A2A;
        context.fill(x, y, x + width, y + height, bgColor);

        // Рамка - меняется при наведении
        int borderColor = hovered ? 0xFF777777 : 0xFF444444;
        context.drawHorizontalLine(x, x + width, y, borderColor);
        context.drawHorizontalLine(x, x + width, y + height - 1, borderColor);
        context.drawVerticalLine(x, y, y + height, borderColor);
        context.drawVerticalLine(x + width - 1, y, y + height, borderColor);

        // Рендер иконки предмета (16x16 стандартный размер)
        if (!itemStack.isEmpty()) {
            int iconX = x + 4;
            int iconY = y + (height - 16) / 2;
            context.drawItem(itemStack, iconX, iconY);
        }

        // Рендер текста - ВСЕГДА виден, цвет от редкости (не меняется при наведении)
        if (item != null) {
            String itemName = item.getName();

            if (itemName != null && !itemName.isEmpty()) {
                // Получаем цвет редкости и гарантируем полный alpha канал
                int rarityColor = item.getRarityColorValue() | 0xFF000000;

                // Позиция текста
                int textX = x + 24;
                int textY = y + (height - 8) / 2;

                context.drawText(
                        client.textRenderer,
                        itemName,
                        textX,
                        textY,
                        rarityColor,
                        false
                );
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }

        if (item != null && onClick != null) {
            onClick.accept(item);
            return true;
        }
        return false;
    }
}
