package cc.aabss.eventutils.api;

import cc.aabss.eventutils.config.EventUtil;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EventUtilsMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EventUtil::screen;
    }
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new HashMap<>();
        factories.put("category_name", parent -> {
            YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                    .title(Text.literal("Additional Config"));

            builder.category(
                    ConfigCategory.createBuilder().name(Text.literal("Options"))
                            .option(Option.<Integer>createBuilder().name(Text.literal("Option Value")).build())
                            .build());
            return builder.build().generateScreen(parent);
        });
        return factories;
    }

    @Override
    public void attachModpackBadges(Consumer<String> consumer) {
        consumer.accept("modmenu");
    }

}
