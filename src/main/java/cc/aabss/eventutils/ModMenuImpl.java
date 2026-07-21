package cc.aabss.eventutils;

import cc.aabss.eventutils.screen.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


@Entrypoint("modmenu")
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
