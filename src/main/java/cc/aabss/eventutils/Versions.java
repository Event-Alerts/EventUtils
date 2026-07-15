package cc.aabss.eventutils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class Versions {
    @Nullable public static final String MC_VERSION = getVersion("minecraft");
    /**
     * NOT semantic as-is (ex: 1.21.6-3.0.0, 1.21.6-dev, etc.)
     */
    @Nullable public static final String EU_VERSION = getVersion("eventutils");

    @Nullable
    private static String getVersion(@NotNull String id) {
        return FabricLoader.getInstance().getModContainer(id)
                .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    @Nullable
    public static SemanticVersion getSemantic(@NotNull String string) {
        try {
            return SemanticVersion.parse(string);
        } catch (final VersionParsingException e) {
            EventUtils.LOGGER.error("Failed to parse version: {}", string, e);
            return null;
        }
    }
}
