package cc.aabss.eventutils;

import org.jetbrains.annotations.NotNull;


/**
 * This class is used to store properties defined in the build.gradle.kts file
 * <br>Uses Stonecutter swaps
 */
public class BuildProperties {
    @NotNull public static final String MOD_ID = /*$ mod_id >> ';'*/ "eventutils";
    @NotNull public static final String MOD_NAME = /*$ mod_name >> ';'*/ "EventUtils";
    @NotNull public static final String MOD_VERSION = /*$ mod_version >> ';'*/ "0.0.0-snapshot";
    @NotNull public static final String MOD_VERSION_FULL = /*$ mod_version_full >> ';'*/ "1.21.4-0.0.0-snapshot";

    /**
     * This class cannot be instantiated
     */
    private BuildProperties() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
