package cc.aabss.eventutils.stats.gson;

import cc.aabss.eventutils.stats.Statable;
import cc.aabss.eventutils.stats.gson.adapter.ConfigAdapter;
import cc.aabss.eventutils.stats.gson.adapter.DurationAdapter;
import cc.aabss.eventutils.stats.gson.adapter.StatableAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.okaeri.configs.OkaeriConfig;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;


public class StatsGson {
    @NotNull public static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(OkaeriConfig.class, new ConfigAdapter())
            .registerTypeAdapter(Duration.class, new DurationAdapter())
            .registerTypeHierarchyAdapter(Statable.class, new StatableAdapter())
            .create();
}
