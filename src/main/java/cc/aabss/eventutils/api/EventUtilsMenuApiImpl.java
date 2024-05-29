package cc.aabss.eventutils.api;

import cc.aabss.eventutils.EventUtils;

import static cc.aabss.eventutils.EventUtils.CONFIG;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EventUtilsMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Event Utils Mod Config"));
            ConfigCategory generalCategory = builder.getOrCreateCategory(Text.literal("General"));
            generalCategory.addEntry(ConfigEntryBuilder.create()
                    .startTextField(Text.literal("Default Famous IP"), EventUtils.DEFAULT_FAMOUS_IP)
                    .setDefaultValue(() -> EventUtils.DEFAULT_FAMOUS_IP)
                    .setTooltip(Text.literal("The default ip for if a [potential] famous event is pinged with no ip inputted."))
                    .setSaveConsumer(newValue -> {
                        EventUtils.DEFAULT_FAMOUS_IP = newValue;
                        CONFIG.saveObject("default-famous-ip", EventUtils.DEFAULT_FAMOUS_IP);
                        CONFIG.saveConfig(CONFIG.JSON);
                    })
                    .build());
            generalCategory.addEntry(ConfigEntryBuilder.create()
                    .startBooleanToggle(Text.literal("Auto Teleport"), EventUtils.AUTO_TP)
                    .setDefaultValue(() -> EventUtils.AUTO_TP)
                    .setTooltip(Text.literal("Whether you should be automatically teleported to the server where the event resides."))
                    .setSaveConsumer(newValue -> {
                        EventUtils.AUTO_TP = newValue;
                        CONFIG.saveObject("auto-tp", EventUtils.AUTO_TP);
                        CONFIG.saveConfig(CONFIG.JSON);
                    })
                    .build());
            generalCategory.addEntry(ConfigEntryBuilder.create()
                    .startBooleanToggle(Text.literal("Discord RPC"), EventUtils.DISCORD_RPC)
                    .setDefaultValue(() -> EventUtils.DISCORD_RPC)
                    .setTooltip(Text.literal("Whether the Discord rich presence should be shown."))
                    .setSaveConsumer(newValue -> {
                        EventUtils.DISCORD_RPC = newValue;
                        CONFIG.saveObject("discord-rpc", EventUtils.DISCORD_RPC);
                        CONFIG.saveConfig(CONFIG.JSON);
                        if (newValue)
                            EventUtils.client.connect();
                        else
                            EventUtils.client.disconnect();

                    })
                    .build());
            generalCategory.addEntry(ConfigEntryBuilder.create()
                    .startBooleanToggle(Text.literal("Simplified Queue Message"), EventUtils.SIMPLE_QUEUE_MSG)
                    .setDefaultValue(() -> EventUtils.SIMPLE_QUEUE_MSG)
                    .setTooltip(Text.literal("Whether the queue message for InvadedLands should be simplified."))
                    .setSaveConsumer(newValue -> {
                        EventUtils.SIMPLE_QUEUE_MSG = newValue;
                        CONFIG.saveObject("simple-queue-msg", EventUtils.SIMPLE_QUEUE_MSG);
                        CONFIG.saveConfig(CONFIG.JSON);
                    })
                    .build());
            ConfigCategory alertCategory = builder.getOrCreateCategory(Text.literal("Alerts"));
            alertEntry(alertCategory, "Famous Events", EventUtils.FAMOUS_EVENT, newValue -> EventUtils.FAMOUS_EVENT = newValue);
            alertEntry(alertCategory, "Potential Famous Events", EventUtils.POTENTIAL_FAMOUS_EVENT, newValue -> EventUtils.POTENTIAL_FAMOUS_EVENT = newValue);
            alertEntry(alertCategory, "Money Events", EventUtils.MONEY_EVENT, newValue -> EventUtils.MONEY_EVENT = newValue);
            alertEntry(alertCategory, "Partner Events", EventUtils.PARTNER_EVENT, newValue -> EventUtils.PARTNER_EVENT = newValue);
            alertEntry(alertCategory, "Fun Events", EventUtils.FUN_EVENT, newValue -> EventUtils.FUN_EVENT = newValue);
            alertEntry(alertCategory, "Housing Events", EventUtils.HOUSING_EVENT, newValue -> EventUtils.HOUSING_EVENT = newValue);
            alertEntry(alertCategory, "Community Events", EventUtils.COMMUNITY_EVENT, newValue -> EventUtils.COMMUNITY_EVENT = newValue);
            alertEntry(alertCategory, "Civilization Events", EventUtils.CIVILIZATION_EVENT, newValue -> EventUtils.CIVILIZATION_EVENT = newValue);
            return builder.build();
        };
    }
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new HashMap<>();
        factories.put("category_name", parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Additional Config"));

            builder.getOrCreateCategory(Text.literal("Options")).addEntry(ConfigEntryBuilder.create()
                    .startIntField(Text.literal("Option Value"), 42)
                    .build());

            return builder.build();
        });

        return factories;
    }

    @Override
    public void attachModpackBadges(Consumer<String> consumer) {
        consumer.accept("modmenu");
    }

    private void alertEntry(
            ConfigCategory category,
            String name,
            Boolean booleanToggle,
            Consumer<Boolean> consumer
    ){
        category.addEntry(ConfigEntryBuilder.create()
                .startBooleanToggle(Text.literal(name), booleanToggle)
                .setDefaultValue(() -> booleanToggle)
                .setTooltip(Text.literal("Whether you should be alerted for "+name.toLowerCase()+"."))
                .setSaveConsumer(consumer
                        .andThen(newValue -> CONFIG.saveConfig(CONFIG.JSON))
                        .andThen(newValue -> CONFIG.saveObject(name.replaceAll("s$", ""), newValue)))
                .build());
    }

}
