package cc.aabss.eventutils.config;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class PlayerGroup {
    @SerializedName("name")
    private String name;
    @SerializedName("players")
    private List<String> players;
    @SerializedName("radius")
    private int radius;

    public PlayerGroup(String name, List<String> players, int radius) {
        this.name = name;
        this.players = players;
        this.radius = radius;
    }

    public String getName() {
        return name;
    }
    public List<String> getPlayers() { return new ArrayList<>(players); }
    public int getRadius() { return radius; }
}
