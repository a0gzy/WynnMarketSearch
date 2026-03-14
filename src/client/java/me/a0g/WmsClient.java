package me.a0g;

import me.a0g.api.WynnApi;
import me.a0g.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class WmsClient implements ClientModInitializer {
    private static WynnApi wynnApi;

    @Override
    public void onInitializeClient() {
        // Регистрация конфигурации
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

        Wms.LOGGER.info("WynnMarketSearch client initializing...");

        // Инициализация API
        wynnApi = new WynnApi();

        // Загрузка данных в фоне
        wynnApi.loadDataAsync().thenRun(() -> {
            MinecraftClient.getInstance().execute(() -> {
                if (wynnApi.isLoaded()) {
                    Wms.LOGGER.info("Wynncraft items loaded successfully");
                } else {
                    Wms.LOGGER.warn("Failed to load Wynncraft items");
                    // Показываем тост об ошибке
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.getToastManager() != null) {
                        client.getToastManager().add(
                                SystemToast.create(
                                        client,
                                        SystemToast.Type.WORLD_BACKUP,
                                        Text.literal("WynnMarketSearch"),
                                        Text.literal("Failed to load items")
                                )
                        );
                    }
                }
            });
        });

        Wms.LOGGER.info("WynnMarketSearch client initialized");
    }

    public static WynnApi getWynnApi() {
        return wynnApi;
    }
}