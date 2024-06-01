package cc.aabss.eventutils.config;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class HidePlayers {
    public static boolean HIDEPLAYERS = false;
    public static KeyBinding HIDEPLAYERSBIND;

    public static void loadBinds(){
        HIDEPLAYERSBIND = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.eventutils.hideplayers",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_F10,
                        "key.category.eventutils"
                )
        );
    }
}
