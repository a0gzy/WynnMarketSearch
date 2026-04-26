package me.a0g.gui.widgets;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import me.a0g.api.WynnItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Строит {@link ItemStack} для иконки {@link WynnItem} из slim-формата API:
 *  - {@code attribute} → vanilla item id + customModelData (custom-resourcepack);
 *  - {@code skin}      → PLAYER_HEAD с {@link ProfileComponent}, указывающим на
 *    texture-hash Wynncraft (см. memory/feedback_skin_profile.md).
 *
 * Кэширует {@link ProfileComponent} по hash, потому что построение записывает
 * профиль и base64-шифрует URL.
 */
public final class IconBuilder {
    private static final Map<String, ProfileComponent> PROFILE_CACHE = new ConcurrentHashMap<>();

    private IconBuilder() {}

    public static ItemStack stackFor(WynnItem item) {
        if (item == null || item.getIcon() == null) return ItemStack.EMPTY;
        try {
            return buildStack(item.getIcon());
        } catch (Exception e) {
            return new ItemStack(Items.BARRIER);
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
}
