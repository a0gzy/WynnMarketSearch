package me.a0g.util;

import me.a0g.Wms;
import me.a0g.api.WynnApi;
import me.a0g.config.ModConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Менеджер для открытия GUI поиска
 */
public class SearchManager {
    private static final String MARKET_SEARCH_MESSAGE = "Type the item name or type 'cancel' to cancel:";

    private final WynnApi api;

    public SearchManager(WynnApi api) {
        this.api = api;
    }

    /**
     * Проверка и открытие GUI при обнаружении сообщения
     */
    public void checkAndOpen(String message) {
        if (!ModConfig.get().marketSearch) {
            return;
        }

        if (message.contains(MARKET_SEARCH_MESSAGE)) {
            Wms.LOGGER.info("Market search message detected, opening GUI");

            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                Wms.LOGGER.info("Opening SearchGuiScreen");
            });
        }
    }

    public WynnApi getApi() {
        return api;
    }
}
