package cc.aabss.eventutils.utility;

import org.jetbrains.annotations.NotNull;


public class StringUtility {
    @NotNull
    public static String capitalize(@NotNull String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }
}
