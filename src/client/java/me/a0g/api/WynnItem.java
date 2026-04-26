package me.a0g.api;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Slim-предмет с нашего бэкенда: только displayName, tier и icon.
 */
public class WynnItem {
    private final String name;
    private final String tier;
    private final JsonObject icon;
    private final boolean isCustom;

    public WynnItem(JsonObject json) {
        this(json, false);
    }

    public WynnItem(JsonObject json, boolean isCustom) {
        this.name = json.has("displayName") ? json.get("displayName").getAsString() : "";
        this.tier = json.has("tier") ? json.get("tier").getAsString().toLowerCase() : "normal";
        this.icon = json.has("icon") && json.get("icon").isJsonObject() ? json.getAsJsonObject("icon") : null;
        this.isCustom = isCustom;
    }

    public String getName() {
        return name;
    }

    public String getTier() {
        return tier;
    }

    public JsonObject getIcon() {
        return icon;
    }

    public boolean isCustom() {
        return isCustom;
    }

    /**
     * Цвет редкости. Бэкенд нормализует tier к одному из:
     * normal | unique | rare | legendary | mythic | fabled.
     */
    public Formatting getRarityColor() {
        if (isCustom) {
            return Formatting.GOLD;
        }
        return switch (tier) {
            case "unique" -> Formatting.YELLOW;
            case "rare" -> Formatting.LIGHT_PURPLE;
            case "legendary" -> Formatting.AQUA;
            case "fabled" -> Formatting.RED;
            case "mythic" -> Formatting.DARK_PURPLE;
            case "custom" -> Formatting.DARK_AQUA; //а как в getRarityColorValue
            default -> Formatting.GRAY;
        };
    }

    public int getRarityColorValue() {
        return switch (getRarityColor()) {
            case GOLD -> 0xFFAA00;
            case YELLOW -> 0xFFFF55;
            case LIGHT_PURPLE -> 0xFF55FF;
            case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555;
            case DARK_PURPLE -> 0xAA00AA;
            case GRAY -> 0xAAAAAA;
            case DARK_AQUA -> 0x00FFFF;
            default -> 0xFFFFFF;
        };
    }

    public String getNameToChat() {
        return name == null ? "" : name;
    }

    public Text getFormattedName() {
        if (name == null || name.isEmpty()) {
            return Text.literal("");
        }
        if (isCustom) {
            return Text.literal("§7[Custom] §6" + name).formatted(Formatting.GOLD);
        }
        return Text.literal(name).formatted(getRarityColor());
    }

    public boolean matchesSearch(String query) {
        if (query == null || query.isEmpty() || name == null) {
            return true;
        }

        String lowerQuery = query.toLowerCase();
        String lowerName = name.toLowerCase();

        if (lowerName.contains(lowerQuery)) {
            return true;
        }

        String nameWithSpaces = lowerName.replace("-", " ");
        if (nameWithSpaces.contains(lowerQuery)) {
            return true;
        }

        String[] queryWords = lowerQuery.split("\\s+");
        String[] nameWords = nameWithSpaces.split("\\s+");

        for (String queryWord : queryWords) {
            boolean wordFound = false;
            for (String nameWord : nameWords) {
                if (nameWord.contains(queryWord)) {
                    wordFound = true;
                    break;
                }
            }
            if (!wordFound) return false;
        }
        return true;
    }
}
