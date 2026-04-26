package me.a0g.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.a0g.Wms;
import me.a0g.config.ModConfig;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Загрузка и кэширование slim-базы предметов с нашего бэкенда.
 * <p>
 * Бэкенд (см. wms-site) отдаёт {@code { fetchedAt, items: [...] }}, где у каждого
 * предмета есть только {@code displayName}, {@code tier} и {@code icon}.
 */
public class WynnApi {
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final String CACHE_FILENAME = "items_cache.json";
    private static final String METADATA_FILENAME = "cache_metadata.json";

    private final Path configDir;
    private JsonObject cachedData;
    private List<WynnItem> cachedItems;
    private final CustomItemManager customItemManager;
    private final FavoritesManager favoritesManager;
    private final HistoryManager historyManager;
    private final PinnedSlotsManager pinnedSlotsManager;
    private boolean isLoading = false;
    private boolean isLoaded = false;

    public WynnApi() {
        this.configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("wms");
        this.customItemManager = new CustomItemManager();
        this.favoritesManager = new FavoritesManager();
        this.historyManager = new HistoryManager();
        this.pinnedSlotsManager = new PinnedSlotsManager();
    }

    public CompletableFuture<Void> loadDataAsync() {
        if (isLoading || isLoaded) {
            return CompletableFuture.completedFuture(null);
        }
        isLoading = true;

        return CompletableFuture.runAsync(() -> {
            try {
                if (isCacheValid() && loadFromCache()) {
                    Wms.LOGGER.info("Loaded {} items from cache", cachedItems.size());
                    return;
                }

                if (loadFromApi()) {
                    return;
                }

                // Сеть недоступна или сервер вернул ошибку — fallback на старый файл, даже если он устарел.
                if (loadFromCache()) {
                    Wms.LOGGER.warn("API unavailable, fell back to stale cache ({} items)", cachedItems.size());
                } else {
                    Wms.LOGGER.error("API unavailable and no cache on disk");
                }
            } finally {
                isLoading = false;
            }
        });
    }

    private boolean isCacheValid() {
        try {
            Path metadataPath = configDir.resolve(METADATA_FILENAME);
            if (!Files.exists(metadataPath)) {
                return false;
            }
            String metadataJson = Files.readString(metadataPath);
            JsonObject metadata = GSON.fromJson(metadataJson, JsonObject.class);
            String cachedDate = metadata.get("date").getAsString();
            return LocalDate.parse(cachedDate, DateTimeFormatter.ISO_LOCAL_DATE).equals(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean loadFromCache() {
        try {
            Path cachePath = configDir.resolve(CACHE_FILENAME);
            if (!Files.exists(cachePath)) {
                return false;
            }
            String cacheJson = Files.readString(cachePath);
            cachedData = GSON.fromJson(cacheJson, JsonObject.class);
            parseItems();
            isLoaded = cachedItems != null && !cachedItems.isEmpty();
            return isLoaded;
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to load items from cache", e);
            return false;
        }
    }

    private boolean loadFromApi() {
        String apiUrl = ModConfig.get().apiUrl;
        if (apiUrl == null || apiUrl.isBlank()) {
            Wms.LOGGER.warn("apiUrl is not configured");
            return false;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "WynnMarketSearch/" + Wms.MOD_ID);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Wms.LOGGER.warn("WMS API returned {} from {}", responseCode, apiUrl);
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                cachedData = GSON.fromJson(response.toString(), JsonObject.class);
                parseItems();
                isLoaded = cachedItems != null && !cachedItems.isEmpty();
                if (!isLoaded) {
                    Wms.LOGGER.warn("API responded OK but produced 0 items");
                    return false;
                }
                Wms.LOGGER.info("Wynncraft items loaded from API: {} items", cachedItems.size());
                saveCache();
                return true;
            }
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to load items from API", e);
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void saveCache() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            Path cachePath = configDir.resolve(CACHE_FILENAME);
            Files.writeString(cachePath, GSON_PRETTY.toJson(cachedData));

            Path metadataPath = configDir.resolve(METADATA_FILENAME);
            JsonObject metadata = new JsonObject();
            metadata.addProperty("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            Files.writeString(metadataPath, GSON_PRETTY.toJson(metadata));
            Wms.LOGGER.info("Cache saved to {}", configDir.toAbsolutePath());
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to save cache", e);
        }
    }

    public CompletableFuture<Void> forceReloadFromApi() {
        return CompletableFuture.runAsync(() -> {
            isLoaded = false;
            cachedData = null;
            cachedItems = null;
            isLoading = true;
            try {
                // Принципиально пропускаем isCacheValid() — иначе reload бессмысленен.
                if (loadFromApi()) return;
                // Только если API недоступно — поднимаем устаревший локальный кэш.
                if (loadFromCache()) {
                    Wms.LOGGER.warn("Force reload: API unavailable, fell back to stale cache ({} items)", cachedItems.size());
                } else {
                    Wms.LOGGER.error("Force reload failed: API unavailable and no cache on disk");
                }
            } finally {
                isLoading = false;
            }
        });
    }

    private void parseItems() {
        cachedItems = new ArrayList<>();
        if (cachedData == null || !cachedData.has("items")) {
            return;
        }
        JsonElement itemsElement = cachedData.get("items");
        if (!itemsElement.isJsonArray()) {
            return;
        }
        JsonArray items = itemsElement.getAsJsonArray();
        for (JsonElement el : items) {
            if (!el.isJsonObject()) continue;
            JsonObject itemJson = el.getAsJsonObject();
            try {
                cachedItems.add(new WynnItem(itemJson));
            } catch (Exception e) {
                Wms.LOGGER.warn("Failed to parse item", e);
            }
        }
    }

    public List<WynnItem> searchItems(String query) {
        // Сначала отдельным проходом собираем все совпавшие избранные — чтобы они
        // гарантированно попали в результат и не были срезаны лимитом.
        List<WynnItem> favs = new ArrayList<>();
        List<WynnItem> rest = new ArrayList<>();

        if (isLoaded && cachedItems != null) {
            for (WynnItem item : cachedItems) {
                if (!item.matchesSearch(query)) continue;
                if (favoritesManager.isFavorite(item.getName())) {
                    favs.add(item);
                } else {
                    rest.add(item);
                }
            }
        }

        for (WynnItem customItem : customItemManager.getAllCustomItems()) {
            if (!customItem.matchesSearch(query)) continue;
            if (favoritesManager.isFavorite(customItem.getName())) {
                favs.add(customItem);
            } else {
                rest.add(customItem);
            }
        }

        List<WynnItem> results = new ArrayList<>(favs);
        for (WynnItem r : rest) {
            if (results.size() >= 14) break;
            results.add(r);
        }
        return results;
    }

    /**
     * Ищет предмет по точному {@code displayName}. Сначала смотрит в API-кеше,
     * потом в пользовательских предметах. Используется для рендера иконок в
     * списке истории, где у нас на руках только строка-имя.
     */
    public WynnItem findByName(String displayName) {
        if (displayName == null || displayName.isEmpty()) return null;
        if (cachedItems != null) {
            for (WynnItem item : cachedItems) {
                if (displayName.equals(item.getName())) return item;
            }
        }
        for (WynnItem custom : customItemManager.getAllCustomItems()) {
            if (displayName.equals(custom.getName())) return custom;
        }
        return null;
    }

    public List<WynnItem> getAllItems() {
        if (!isLoaded || cachedItems == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(cachedItems);
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }

    public FavoritesManager getFavoritesManager() {
        return favoritesManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public PinnedSlotsManager getPinnedSlotsManager() {
        return pinnedSlotsManager;
    }
}
