package me.a0g.gui.widgets;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import me.a0g.api.WynnItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Кнопка предмета в результатах поиска.
 * Иконка строится из slim-структуры icon: {format, value} с бэкенда:
 *  - "attribute" → vanilla item id + customModelData (для custom-resourcepack текстур);
 *  - "skin"      → PLAYER_HEAD с ProfileComponent, указывающим на texture-hash Wynncraft.
 */
public class ItemButtonWidget {
    private static final Map<String, ProfileComponent> PROFILE_CACHE = new HashMap<>();

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

    public WynnItem getItem() { return item; }
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
            this.itemStack = buildStack(item.getIcon());
        } catch (Exception e) {
            this.itemStack = new ItemStack(Items.BARRIER);
        }
    }

    private static ItemStack buildStack(JsonObject iconJson) {
        if (!iconJson.has("format") || !iconJson.has("value")) {
            return new ItemStack(Items.BARRIER);
        }
        String format = iconJson.get("format").getAsString();
        JsonElement valueEl = iconJson.get("value");

        if ("attribute".equals(format) && valueEl.isJsonObject()) {
            return buildAttributeStack(valueEl.getAsJsonObject());
        }
        if ("skin".equals(format) && valueEl.isJsonPrimitive()) {
            return buildSkinStack(valueEl.getAsString());
        }
        return new ItemStack(Items.BARRIER);
    }

    private static ItemStack buildAttributeStack(JsonObject value) {
        String itemId = value.has("id") ? value.get("id").getAsString() : "minecraft:barrier";
        var mcItem = Registries.ITEM.get(Identifier.tryParse(itemId));
        if (mcItem == null || mcItem == Items.AIR) {
            return new ItemStack(Items.BARRIER);
        }

        ItemStack stack = new ItemStack(mcItem);

        // Slim-формат: customModelData — число. Старый формат: customModelData.rangeDispatch[0].
        Integer cmd = readCustomModelData(value);
        if (cmd != null) {
            stack.set(
                    DataComponentTypes.CUSTOM_MODEL_DATA,
                    new CustomModelDataComponent(
                            List.of((float) cmd.intValue()),
                            List.of(),
                            List.of(),
                            List.of()
                    )
            );
        }
        return stack;
    }

    private static Integer readCustomModelData(JsonObject value) {
        if (!value.has("customModelData")) return null;
        JsonElement el = value.get("customModelData");
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return el.getAsInt();
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("rangeDispatch") && obj.get("rangeDispatch").isJsonArray()) {
                var arr = obj.getAsJsonArray("rangeDispatch");
                if (arr.size() > 0) return arr.get(0).getAsInt();
            }
        }
        return null;
    }

    private static ItemStack buildSkinStack(String textureHash) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, profileFor(textureHash));
        return head;
    }

    private static ProfileComponent profileFor(String textureHash) {
        ProfileComponent cached = PROFILE_CACHE.get(textureHash);
        if (cached != null) return cached;

        String url = "http://textures.minecraft.net/texture/" + textureHash;
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        String b64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        UUID id = UUID.nameUUIDFromBytes(("wms:" + textureHash).getBytes(StandardCharsets.UTF_8));
        Multimap<String, Property> properties = ImmutableMultimap.of("textures", new Property("textures", b64));
        PropertyMap propertyMap = new PropertyMap(properties);
        GameProfile profile = new GameProfile(id, "head", propertyMap);

        ProfileComponent component = ProfileComponent.ofStatic(profile);
        PROFILE_CACHE.put(textureHash, component);
        return component;
    }

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
