package cc.aabss.eventutils;

import cc.aabss.eventutils.screen.config.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import org.jetbrains.annotations.NotNull;


@Entrypoint("modmenu")
public class ModMenuImpl implements ModMenuApi {
    @Override @NotNull
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::getConfigScreen;
    }
}
