package me.a0g.api;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Предмет из Wynncraft API
 */
public class WynnItem {
    private final String name;
    private final String internalName;
    private final String type;
    private final String tier;
    private final String rarity;
    private final JsonObject icon;

    public WynnItem(String name, JsonObject json) {
        this.name = name;
        this.internalName = json.has("internalName") ? json.get("internalName").getAsString() : name;
        this.type = json.has("type") ? json.get("type").getAsString() : null;
        this.tier = json.has("tier") ? json.get("tier").getAsString() : null;
        this.rarity = json.has("rarity") ? json.get("rarity").getAsString() : null;
        this.icon = json.has("icon") ? json.getAsJsonObject("icon") : null;
    }

    public String getName() {
        return name;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getType() {
        return type;
    }

    public String getTier() {
        return tier;
    }

    public String getRarity() {
        return rarity;
    }

    public JsonObject getIcon() {
        return icon;
    }

    /**
     * Получение цвета форматирования на основе редкости предмета
     */
    public Formatting getRarityColor() {
        // Сначала проверяем rarity (для обычных предметов)
        if (rarity != null) {
            return switch (rarity.toLowerCase()) {
                case "normal" -> Formatting.GRAY;
                case "unique" -> Formatting.YELLOW;
                case "rare" -> Formatting.LIGHT_PURPLE;
                case "legendary" -> Formatting.AQUA;
                case "fabled" -> Formatting.RED;
                case "mythic" -> Formatting.DARK_PURPLE;
                case "set" -> Formatting.GREEN;
                default -> Formatting.WHITE;
            };
        }

        // Затем проверяем tier (для ингредиентов и материалов)
        if (tier != null) {
            return switch (tier.toLowerCase()) {
                case "0", "normal" -> Formatting.GRAY;
                case "1", "unique" -> Formatting.YELLOW;
                case "2", "rare" -> Formatting.LIGHT_PURPLE;
                case "3", "legendary" -> Formatting.AQUA;
                case "4", "fabled" -> Formatting.RED;
                case "5", "mythic" -> Formatting.DARK_PURPLE;
                default -> Formatting.WHITE;
            };
        }

        return Formatting.WHITE;
    }

    /**
     * Получение цвета в формате RGB для рендеринга
     */
    public int getRarityColorValue() {
        Formatting formatting = getRarityColor();
        // Прямые цвета Minecraft Formatting
        return switch (formatting) {
            case BLACK -> 0x000000;
            case DARK_BLUE -> 0x0000AA;
            case DARK_GREEN -> 0x00AA00;
            case DARK_AQUA -> 0x00AAAA;
            case DARK_RED -> 0xAA0000;
            case DARK_PURPLE -> 0xAA00AA;
            case GOLD -> 0xFFAA00;
            case GRAY -> 0xAAAAAA;
            case DARK_GRAY -> 0x555555;
            case BLUE -> 0x5555FF;
            case GREEN -> 0x55FF55;
            case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555;
            case LIGHT_PURPLE -> 0xFF55FF;
            case YELLOW -> 0xFFFF55;
            case WHITE -> 0xFFFFFF;
            default -> 0xFFFFFF;
        };
    }

    /**
     * Получение имени для отправки в чат
     */
    public String getNameToChat() {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name;
    }

    /**
     * Получение текста для отображения с цветом редкости
     */
    public Text getFormattedName() {
        if (name == null || name.isEmpty()) {
            return Text.literal("");
        }
        return Text.literal(name).formatted(getRarityColor());
    }

    /**
     * Проверка соответствия поисковому запросу
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }

        String lowerQuery = query.toLowerCase();
        String lowerName = name.toLowerCase();
        String lowerInternalName = internalName.toLowerCase();

        // Прямое совпадение
        if (lowerName.contains(lowerQuery)) {
            return true;
        }

        // Совпадение с заменой дефисов на пробелы
        String nameWithSpaces = lowerName.replace("-", " ");
        if (nameWithSpaces.contains(lowerQuery)) {
            return true;
        }

        // Проверка по внутреннему имени
        if (lowerInternalName.contains(lowerQuery)) {
            return true;
        }

        // Проверка по словам (для запросов типа "topaz morph")
        String[] queryWords = lowerQuery.split("\\s+");
        String[] nameWords = nameWithSpaces.split("\\s+");

        boolean allWordsFound = true;
        for (String queryWord : queryWords) {
            boolean wordFound = false;
            for (String nameWord : nameWords) {
                if (nameWord.contains(queryWord)) {
                    wordFound = true;
                    break;
                }
            }
            if (!wordFound) {
                allWordsFound = false;
                break;
            }
        }

        return allWordsFound;
    }
}
