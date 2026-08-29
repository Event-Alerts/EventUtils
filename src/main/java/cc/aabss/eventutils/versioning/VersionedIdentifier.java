package cc.aabss.eventutils.versioning;

import cc.aabss.eventutils.BuildProperties;
//? if >=1.21.11 {
//import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import org.jetbrains.annotations.NotNull;


public class VersionedIdentifier {
    @NotNull
    //? if >=1.21.11 {
    /*public static Identifier of(@NotNull String path) {
        return Identifier.fromNamespaceAndPath(BuildProperties.MOD_ID, path);
    *///?} else {
    public static ResourceLocation of(@NotNull String path) {
        return ResourceLocation.fromNamespaceAndPath(BuildProperties.MOD_ID, path);
    //?}
    }
}
