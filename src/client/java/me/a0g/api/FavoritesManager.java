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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Менеджер избранных предметов. Хранит displayName-ы в
 * {@code <runDir>/wms/favorites.json}. Используется для пометки звёздочкой
 * и подъёма таких предметов наверх результатов поиска.
 */
public class FavoritesManager {
    private static final String FILENAME = "favorites.json";
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private final LinkedHashSet<String> favorites = new LinkedHashSet<>();

    public FavoritesManager() {
        this.configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("wms");
        load();
    }

    private void load() {
        try {
            Path filePath = configDir.resolve(FILENAME);
            if (!Files.exists(filePath)) {
                return;
            }
            String json = Files.readString(filePath);
            List<String> loaded = GSON.fromJson(json, new TypeToken<List<String>>() {}.getType());
            if (loaded != null) {
                favorites.clear();
                favorites.addAll(loaded);
                Wms.LOGGER.info("Loaded {} favorites", favorites.size());
            }
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to load favorites", e);
        }
    }

    private void save() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            Path filePath = configDir.resolve(FILENAME);
            String json = GSON_PRETTY.toJson(new ArrayList<>(favorites));
            Files.writeString(filePath, json);
        } catch (IOException e) {
            Wms.LOGGER.error("Failed to save favorites", e);
        }
    }

    /**
     * Переключает состояние избранного для предмета с указанным displayName.
     * @return {@code true}, если предмет теперь в избранном; {@code false} — удалён.
     */
    public boolean toggle(String displayName) {
        if (displayName == null || displayName.isEmpty()) return false;
        boolean nowFavorite;
        if (favorites.contains(displayName)) {
            favorites.remove(displayName);
            nowFavorite = false;
        } else {
            favorites.add(displayName);
            nowFavorite = true;
        }
        save();
        return nowFavorite;
    }

    public boolean isFavorite(String displayName) {
        return displayName != null && favorites.contains(displayName);
    }

    public Set<String> getFavorites() {
        return Collections.unmodifiableSet(favorites);
    }
}
