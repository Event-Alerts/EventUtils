package cc.aabss.eventutils.config;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * A named group of players with optional nametag visibility.
 * Used for cycling visibility (hide players keybind) and group chat.
 */
public class PlayerGroup {
    @NotNull private String name;
    @NotNull private List<String> players;
    private boolean showNametags;
    private boolean hideListedPlayers;
    private boolean hideListedNpcs;

    public PlayerGroup(@NotNull String name, @NotNull List<String> players, boolean showNametags) {
        this.name = name;
        this.players = new ArrayList<>(players);
        this.showNametags = showNametags;
        this.hideListedPlayers = false;
        this.hideListedNpcs = false;
    }

    /** For Gson deserialization */
    public PlayerGroup() {
        this.name = "New Group";
        this.players = new ArrayList<>();
        this.showNametags = true;
        this.hideListedPlayers = false;
        this.hideListedNpcs = false;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(@NotNull List<String> players) {
        this.players = new ArrayList<>(players);
    }

    public boolean isShowNametags() {
        return showNametags;
    }

    public void setShowNametags(boolean showNametags) {
        this.showNametags = showNametags;
    }

    public boolean isHideListedPlayers() {
        return hideListedPlayers;
    }

    public void setHideListedPlayers(boolean hideListedPlayers) {
        this.hideListedPlayers = hideListedPlayers;
    }

    public boolean isHideListedNpcs() {
        return hideListedNpcs;
    }

    public void setHideListedNpcs(boolean hideListedNpcs) {
        this.hideListedNpcs = hideListedNpcs;
    }

    /** Returns true if the given (lowercased) player name is in this group. */
    public boolean containsPlayer(@NotNull String nameLower) {
        return players.contains(nameLower);
    }
}
