package me.a0g.gui.widgets;

import me.a0g.api.FavoritesManager;
import me.a0g.api.WynnItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

/**
 * Кнопка предмета в результатах поиска. Иконка строится через {@link IconBuilder}.
 */
public class ItemButtonWidget {
    protected int x, y, width, height;
    protected boolean visible = true;
    protected boolean hovered = false;

    private WynnItem item;
    private ItemStack itemStack = ItemStack.EMPTY;
    private final Consumer<WynnItem> onClick;
    private final FavoritesManager favorites;
    private final Runnable onFavoriteToggled;
    private final Consumer<WynnItem> onPinRequested;

    public ItemButtonWidget(int x, int y, int width, int height,
                            Consumer<WynnItem> onClick,
                            FavoritesManager favorites,
                            Runnable onFavoriteToggled,
                            Consumer<WynnItem> onPinRequested) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onClick = onClick;
        this.favorites = favorites;
        this.onFavoriteToggled = onFavoriteToggled;
        this.onPinRequested = onPinRequested;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setItem(WynnItem item) {
        this.item = item;
        this.itemStack = IconBuilder.stackFor(item);
    }

    public WynnItem getItem() { return item; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        MinecraftClient client = MinecraftClient.getInstance();
        hovered = isMouseOver(mouseX, mouseY);

        int bgColor = hovered ? 0xFF3A3A3A : 0xFF2A2A2A;
        context.fill(x, y, x + width, y + height, bgColor);

        int borderColor = hovered ? 0xFF777777 : 0xFF444444;
        context.drawHorizontalLine(x, x + width, y, borderColor);
        context.drawHorizontalLine(x, x + width, y + height - 1, borderColor);
        context.drawVerticalLine(x, y, y + height, borderColor);
        context.drawVerticalLine(x + width - 1, y, y + height, borderColor);

        if (!itemStack.isEmpty()) {
            int iconX = x + 4;
            int iconY = y + (height - 16) / 2;
            context.drawItem(itemStack, iconX, iconY);
        }

        if (item != null) {
            String itemName = item.getName();
            if (itemName != null && !itemName.isEmpty()) {
                int rarityColor = item.getRarityColorValue() | 0xFF000000;
                int textX = x + 24;
                int textY = y + (height - 8) / 2;
                context.drawText(client.textRenderer, itemName, textX, textY, rarityColor, false);

                if (favorites != null && favorites.isFavorite(itemName)) {
                    String star = "\u2605";
                    int starWidth = client.textRenderer.getWidth(star);
                    context.drawText(client.textRenderer, star, x + width - starWidth - 6, textY, 0xFFFFD700, false);
                }
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (item == null) return false;

        if (button == 0 && onClick != null) {
            onClick.accept(item);
            return true;
        }
        if (button == 1 && onPinRequested != null) {
            onPinRequested.accept(item);
            return true;
        }
        if (button == 2 && favorites != null) {
            favorites.toggle(item.getName());
            if (onFavoriteToggled != null) onFavoriteToggled.run();
            return true;
        }
        return false;
    }
}
