package cc.aabss.eventutils.versioning;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public record VersionedGameProfile(@NotNull GameProfile profile) {
    @NotNull
    public UUID getId() {
        //? if >=1.21.11 {
        //return profile.id();
        //?} else {
        return profile.getId();
        //?}
    }

    @NotNull
    public String getName() {
        //? if >=1.21.11 {
        //return profile.name();
        //?} else {
        return profile.getName();
        //?}
    }
}
