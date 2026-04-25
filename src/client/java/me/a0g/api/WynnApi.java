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
    private boolean isLoading = false;
    private boolean isLoaded = false;

    public WynnApi() {
        this.configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("wms");
        this.customItemManager = new CustomItemManager();
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
            isLoading = false;
            cachedData = null;
            cachedItems = null;
            loadDataAsync().join();
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
        List<WynnItem> results = new ArrayList<>();

        if (isLoaded && cachedItems != null) {
            for (WynnItem item : cachedItems) {
                if (item.matchesSearch(query)) {
                    results.add(item);
                    if (results.size() >= 14) {
                        return results;
                    }
                }
            }
        }

        for (WynnItem customItem : customItemManager.getAllCustomItems()) {
            if (customItem.matchesSearch(query)) {
                results.add(customItem);
                if (results.size() >= 14) {
                    return results;
                }
            }
        }

        return results;
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
}
