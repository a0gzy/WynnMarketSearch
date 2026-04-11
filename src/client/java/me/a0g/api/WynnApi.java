package me.a0g.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.a0g.Wms;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Класс для работы с Wynncraft API
 */
public class WynnApi {
    private static final String API_URL = "https://api.wynncraft.com/v3/item/database?fullResult";
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final String CACHE_FILENAME = "items_cache.json";
    private static final String METADATA_FILENAME = "cache_metadata.json";
    private static final String API_KEY = "b_-mLSgGU7a5FILyGLiPc_1HBXsPotPYjc6AJBt974Y";

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

    /**
     * Асинхронная загрузка данных из API или кэша
     */
    public CompletableFuture<Void> loadDataAsync() {
        if (isLoading || isLoaded) {
            return CompletableFuture.completedFuture(null);
        }

        isLoading = true;

        return CompletableFuture.runAsync(() -> {
            try {
                // Проверяем кэш
                if (isCacheValid()) {
                    loadFromCache();
                    if (isLoaded) {
                        Wms.LOGGER.info("Loaded {} items from cache", cachedItems.size());
                        isLoading = false;
                        return;
                    }
                }

                // Загружаем из API
                loadFromApi();
            } catch (Exception e) {
                Wms.LOGGER.error("Failed to load Wynncraft items", e);
                // Пробуем загрузить из кэша даже если он устарел
                if (!isLoaded) {
                    loadFromCache();
                }
            } finally {
                isLoading = false;
            }
        });
    }

    /**
     * Проверка валидности кэша (не старше 1 дня)
     */
    private boolean isCacheValid() {
        try {
            Path metadataPath = configDir.resolve(METADATA_FILENAME);
            if (!Files.exists(metadataPath)) {
                return false;
            }

            String metadataJson = Files.readString(metadataPath);
            JsonObject metadata = GSON.fromJson(metadataJson, JsonObject.class);
            
            String cachedDate = metadata.get("date").getAsString();
            LocalDate cacheDate = LocalDate.parse(cachedDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate today = LocalDate.now();

            return cacheDate.equals(today);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Загрузка данных из кэша
     */
    private void loadFromCache() {
        try {
            Path cachePath = configDir.resolve(CACHE_FILENAME);
            if (!Files.exists(cachePath)) {
                return;
            }

            String cacheJson = Files.readString(cachePath);
            cachedData = GSON.fromJson(cacheJson, JsonObject.class);
            parseItems();
            isLoaded = true;
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to load items from cache", e);
        }
    }

    /**
     * Загрузка данных из API с сохранением в кэш
     */
    private void loadFromApi() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Wms.LOGGER.warn("Wynncraft API returned error code: {}", responseCode);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                cachedData = GSON.fromJson(response.toString(), JsonObject.class);
                parseItems();
                isLoaded = true;
                Wms.LOGGER.info("Wynncraft items loaded from API: {} items", cachedItems.size());

                // Сохраняем в кэш
                saveCache();
            }

            connection.disconnect();
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to load Wynncraft items from API", e);
        }
    }

    /**
     * Сохранение данных в кэш
     */
    private void saveCache() {
        try {
            // Создаём директорию если нет
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // Сохраняем данные
            Path cachePath = configDir.resolve(CACHE_FILENAME);
            Files.writeString(cachePath, GSON_PRETTY.toJson(cachedData));

            // Сохраняем метаданные (дату)
            Path metadataPath = configDir.resolve(METADATA_FILENAME);
            JsonObject metadata = new JsonObject();
            metadata.addProperty("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            Files.writeString(metadataPath, GSON_PRETTY.toJson(metadata));

            Wms.LOGGER.info("Cache saved to {}", configDir.toAbsolutePath());
        } catch (Exception e) {
            Wms.LOGGER.error("Failed to save cache", e);
        }
    }

    /**
     * Принудительная перезагрузка из API
     */
    public CompletableFuture<Void> forceReloadFromApi() {
        return CompletableFuture.runAsync(() -> {
            isLoaded = false;
            isLoading = false;
            cachedData = null;
            cachedItems = null;
            loadDataAsync().join();
        });
    }

    /**
     * Парсинг предметов из JSON данных
     */
    private void parseItems() {
        cachedItems = new ArrayList<>();

        if (cachedData == null) {
            return;
        }

        for (Map.Entry<String, com.google.gson.JsonElement> entry : cachedData.entrySet()) {
            String itemName = entry.getKey();
            JsonObject itemJson = entry.getValue().getAsJsonObject();

            try {
                WynnItem item = new WynnItem(itemName, itemJson);
                cachedItems.add(item);
            } catch (Exception e) {
                Wms.LOGGER.warn("Failed to parse item: {}", itemName, e);
            }
        }
    }

    /**
     * Поиск предметов по запросу (API + пользовательские)
     */
    public List<WynnItem> searchItems(String query) {
        List<WynnItem> results = new ArrayList<>();

        // Ищем в API предмета
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

        // Ищем в пользовательских предметах
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

    /**
     * Получение всех загруженных предметов
     */
    public List<WynnItem> getAllItems() {
        if (!isLoaded || cachedItems == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(cachedItems);
    }

    /**
     * Проверка загруженности данных
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Проверка идет ли загрузка
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * Получение менеджера пользовательских предметов
     */
    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }
}
