package me.a0g.api;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import me.a0g.Wms;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Менеджер пользовательских предметов
 */
public class CustomItemManager {
    private static final String CUSTOM_ITEMS_FILENAME = "custom_items.json";
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private final Map<String, String> customItems = new LinkedHashMap<>();
    private final List<DefaultItem> DEFAULT_ITEMS = List.of(
        new DefaultItem("Corkian Insulator", "minecraft:stick"),
        new DefaultItem("Corkian Amplifier I", "minecraft:book"),
        new DefaultItem("Corkian Amplifier II", "minecraft:book"),
        new DefaultItem("Corkian Amplifier III", "minecraft:book"),
        new DefaultItem("Corkian Amplifier IV", "minecraft:book"),
        new DefaultItem("Corkian Simulator", "minecraft:copper_ingot")
    );

    public CustomItemManager() {
        this.configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("wms");
        loadCustomItems();
    }

    /**
     * Загрузка пользовательских предметов из файла
     */
    private void loadCustomItems() {
        try {
            Path filePath = configDir.resolve(CUSTOM_ITEMS_FILENAME);
            if (!Files.exists(filePath)) {
                Wms.LOGGER.info("Custom items file not found, initializing defaults");
                initDefaultItems();
                return;
            }

            String json = Files.readString(filePath);
            Wms.LOGGER.info("Loading custom items from: {}", json);
            Map<String, String> loaded = GSON.fromJson(json, new TypeToken<Map<String, String>>() {}.getType());

            if (loaded != null) {
                customItems.clear();
                customItems.putAll(loaded);
                Wms.LOGGER.info("Loaded {} custom items", customItems.size());
            } else {
                Wms.LOGGER.warn("Custom items file is empty or invalid, initializing defaults");
                initDefaultItems();
            }
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to load custom items", e);
        }
    }

    /**
     * Инициализация дефолтных предметов
     */
    private void initDefaultItems() {
        customItems.clear();
        for (DefaultItem item : DEFAULT_ITEMS) {
            customItems.put(item.name(), item.icon());
        }
        saveCustomItems();
        Wms.LOGGER.info("Initialized {} default custom items", DEFAULT_ITEMS.size());
        for (DefaultItem item : DEFAULT_ITEMS) {
            Wms.LOGGER.debug("  - {}", item.name());
        }
    }

    /**
     * Сохранение пользовательских предметов в файл
     */
    private void saveCustomItems() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Path filePath = configDir.resolve(CUSTOM_ITEMS_FILENAME);
            String json = GSON_PRETTY.toJson(customItems);
            Files.writeString(filePath, json);
            Wms.LOGGER.info("Saved {} custom items to {}", customItems.size(), filePath.toAbsolutePath());
        } catch (IOException e) {
            Wms.LOGGER.error("Failed to save custom items", e);
        }
    }

    /**
     * Добавление пользовательского предмета
     * @return true если добавлен успешно
     */
    public boolean addItem(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            Wms.LOGGER.warn("addItem called with null/empty name");
            return false;
        }

        String trimmedName = itemName.trim();
        if (customItems.containsKey(trimmedName)) {
            Wms.LOGGER.warn("Item '{}' already exists", trimmedName);
            return false;
        }

        customItems.put(trimmedName, "");
        Wms.LOGGER.info("Adding item '{}' (total: {})", trimmedName, customItems.size());
        saveCustomItems();
        return true;
    }

    /**
     * Удаление пользовательского предмета
     * @return true если удалён успешно
     */
    public boolean removeItem(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            Wms.LOGGER.warn("removeItem called with null/empty name");
            return false;
        }

        String trimmedName = itemName.trim();
        boolean removed = customItems.remove(trimmedName) != null;
        if (removed) {
            Wms.LOGGER.info("Removed item '{}' (total: {})", trimmedName, customItems.size());
            saveCustomItems();
        } else {
            Wms.LOGGER.warn("Item '{}' not found for removal", trimmedName);
        }
        return removed;
    }

    /**
     * Проверка существования предмета
     */
    public boolean hasItem(String itemName) {
        return customItems.containsKey(itemName);
    }

    /**
     * Получение всех пользовательских предметов
     */
    public Set<String> getCustomItems() {
        return Collections.unmodifiableSet(customItems.keySet());
    }

    /**
     * Получение списка предметов для автодополнения
     */
    public List<String> getSuggestions(String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(customItems.keySet());
        }

        String lowerQuery = query.toLowerCase();
        return customItems.keySet().stream()
                .filter(name -> name.toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /**
     * Создание WynnItem из пользовательского предмета
     */
    public WynnItem createCustomItem(String name) {
        if (!customItems.containsKey(name)) {
            return null;
        }

        String iconId = customItems.get(name);
        String itemId = (iconId == null || iconId.isEmpty()) ? "minecraft:barrier" : iconId;

        JsonObject value = new JsonObject();
        value.addProperty("id", itemId);

        JsonObject iconJson = new JsonObject();
        iconJson.addProperty("format", "attribute");
        iconJson.add("value", value);

        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("displayName", name);
        itemJson.addProperty("tier", "normal");
        itemJson.add("icon", iconJson);

        return new WynnItem(itemJson, true);
    }

    /**
     * Получение всех пользовательских предметов как WynnItem
     */
    public List<WynnItem> getAllCustomItems() {
        List<WynnItem> items = new ArrayList<>();
        for (String name : customItems.keySet()) {
            WynnItem item = createCustomItem(name);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * Default item record
     */
    private record DefaultItem(String name, String icon) {}
}
