package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.versioning.VersionedClient;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.core.ParentComponent;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;


public abstract class ScreenWithParent<R extends ParentComponent> extends BaseOwoScreen<R> {
    @Nullable private final Screen parent;

    public ScreenWithParent(@Nullable Screen parent) {
        this.parent = parent;
    }

    @Override
    public void onClose() {
        if (minecraft != null) new VersionedClient(minecraft).setScreen(parent);
    }
}
