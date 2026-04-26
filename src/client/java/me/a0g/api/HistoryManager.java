package me.a0g.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.a0g.Wms;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * История последних отправленных в чат запросов с GUI поиска.
 * Хранится в {@code <runDir>/wms/history.json} в порядке от самого свежего к старому.
 * Дубликаты подтягиваются наверх, размер ограничен {@link #MAX_ENTRIES}.
 */
public class HistoryManager {
    public static final int MAX_ENTRIES = 20;
    private static final String FILENAME = "history.json";
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private final Deque<String> entries = new ArrayDeque<>();

    public HistoryManager() {
        this.configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("wms");
        load();
    }

    private void load() {
        try {
            Path filePath = configDir.resolve(FILENAME);
            if (!Files.exists(filePath)) return;
            String json = Files.readString(filePath);
            List<String> loaded = GSON.fromJson(json, new TypeToken<List<String>>() {}.getType());
            if (loaded != null) {
                entries.clear();
                for (String s : loaded) entries.addLast(s);
            }
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to load history", e);
        }
    }

    private void save() {
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path filePath = configDir.resolve(FILENAME);
            Files.writeString(filePath, GSON_PRETTY.toJson(new ArrayList<>(entries)));
        } catch (IOException e) {
            Wms.LOGGER.error("Failed to save history", e);
        }
    }

    /** Добавляет запрос наверх истории. Если он уже был — переносит наверх. */
    public void add(String text) {
        if (text == null || text.isBlank()) return;
        // удаляем дубликат если есть
        Iterator<String> it = entries.iterator();
        while (it.hasNext()) {
            if (it.next().equals(text)) {
                it.remove();
                break;
            }
        }
        entries.addFirst(text);
        while (entries.size() > MAX_ENTRIES) entries.removeLast();
        save();
    }

    public void remove(String text) {
        if (entries.remove(text)) save();
    }

    public void clear() {
        if (entries.isEmpty()) return;
        entries.clear();
        save();
    }

    /** Список от самого свежего к самому старому. */
    public List<String> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }
}
