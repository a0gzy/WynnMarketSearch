package me.a0g.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.a0g.Wms;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * «Хотбар» закреплённых предметов: упорядоченный список displayName-ов,
 * хранится в {@code <runDir>/wms/pinned_slots.json}. Заполняется ПКМ-ом
 * по результату поиска (берёт первый свободный слот). Если предмет уже
 * закреплён — ничего не делаем.
 */
public class PinnedSlotsManager {
    /** Жёсткий лимит — два «хотбара» по 9 слотов. */
    public static final int MAX = 18;

    private static final String FILENAME = "pinned_slots.json";
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private final List<String> slots = new ArrayList<>();

    public PinnedSlotsManager() {
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
                slots.clear();
                for (String s : loaded) {
                    if (slots.size() >= MAX) break;
                    if (s != null && !s.isBlank() && !slots.contains(s)) slots.add(s);
                }
            }
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to load pinned slots", e);
        }
    }

    private void save() {
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path filePath = configDir.resolve(FILENAME);
            Files.writeString(filePath, GSON_PRETTY.toJson(slots));
        } catch (IOException e) {
            Wms.LOGGER.error("Failed to save pinned slots", e);
        }
    }

    /**
     * Добавляет предмет в первый свободный слот. Если уже есть — ничего не
     * делает. Если все слоты заняты — тоже отказ.
     * @return {@code true} если действительно добавили
     */
    public boolean add(String displayName) {
        if (displayName == null || displayName.isBlank()) return false;
        if (slots.contains(displayName)) return false;
        if (slots.size() >= MAX) return false;
        slots.add(displayName);
        save();
        return true;
    }

    public boolean remove(String displayName) {
        if (slots.remove(displayName)) {
            save();
            return true;
        }
        return false;
    }

    public boolean removeAt(int index) {
        if (index < 0 || index >= slots.size()) return false;
        slots.remove(index);
        save();
        return true;
    }

    public boolean contains(String displayName) {
        return slots.contains(displayName);
    }

    public List<String> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public int size() {
        return slots.size();
    }
}
