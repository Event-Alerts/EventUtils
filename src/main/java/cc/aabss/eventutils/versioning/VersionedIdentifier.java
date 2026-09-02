package cc.aabss.eventutils.versioning;

import cc.aabss.eventutils.BuildProperties;
import org.jetbrains.annotations.NotNull;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}


public class VersionedIdentifier {
    @NotNull
    //? if >=1.21.11 {
    /*public static Identifier of(@NotNull String path) {
        return Identifier.fromNamespaceAndPath(BuildProperties.MOD_ID, path);
    *///?} else {
    public static ResourceLocation of(@NotNull String path) {
        //? if >1.20.4 {
        return ResourceLocation.fromNamespaceAndPath(BuildProperties.MOD_ID, path);
        //?} else {
        /*return new ResourceLocation(BuildProperties.MOD_ID, path);
        *///?}
    //?}
    }
}
