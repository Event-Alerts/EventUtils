package cc.aabss.eventutils;

import cc.aabss.eventutils.config.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


public class ModMenuImpl implements ModMenuApi {
    @Override
    public void attachModpackBadges(@NotNull Consumer<String> consumer) {
        consumer.accept("modmenu");
    }

    @Override @NotNull
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::getConfigScreen;
    }
}
