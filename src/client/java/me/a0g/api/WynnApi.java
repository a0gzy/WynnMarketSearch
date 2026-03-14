package me.a0g.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.a0g.Wms;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    private JsonObject cachedData;
    private List<WynnItem> cachedItems;
    private boolean isLoading = false;
    private boolean isLoaded = false;

    public WynnApi() {
    }

    /**
     * Асинхронная загрузка данных из API
     */
    public CompletableFuture<Void> loadDataAsync() {
        if (isLoading || isLoaded) {
            return CompletableFuture.completedFuture(null);
        }

        isLoading = true;

        return CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Wms.LOGGER.warn("Wynncraft API returned error code: {}", responseCode);
                    isLoading = false;
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
                    Wms.LOGGER.info("Wynncraft items loaded: {} items", cachedItems.size());
                }

                connection.disconnect();
            } catch (Exception e) {
                Wms.LOGGER.error("Failed to load Wynncraft items", e);
            } finally {
                isLoading = false;
            }
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
     * Поиск предметов по запросу
     */
    public List<WynnItem> searchItems(String query) {
        if (!isLoaded || cachedItems == null) {
            return new ArrayList<>();
        }

        List<WynnItem> results = new ArrayList<>();

        for (WynnItem item : cachedItems) {
            if (item.matchesSearch(query)) {
                results.add(item);
                // Ограничиваем количество результатов
                if (results.size() >= 14) {
                    break;
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
}
