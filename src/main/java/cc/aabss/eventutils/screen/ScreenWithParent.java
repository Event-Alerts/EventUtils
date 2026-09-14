package cc.aabss.eventutils.screen;

import cc.aabss.eventutils.versioning.VersionedClient;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.core.ParentComponent;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class ScreenWithParent<R extends ParentComponent> extends BaseOwoScreen<R> {
    @Nullable protected final Screen parent;

    @Nullable private Component confirmCloseTitle;
    @Nullable private Component confirmCloseDescription;

    public ScreenWithParent(@Nullable Screen parent) {
        this.parent = parent;
    }

    @NotNull
    public ScreenWithParent<R> confirmClose(@NotNull Component title, @NotNull Component description) {
        this.confirmCloseTitle = title;
        this.confirmCloseDescription = description;
        return this;
    }

    public void closeWithoutConfirmation() {
        this.confirmCloseTitle = null;
        this.confirmCloseDescription = null;
        onClose();
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        final VersionedClient vClient = new VersionedClient(minecraft);

        // Confirm close
        if (confirmCloseTitle != null && confirmCloseDescription != null) {
            vClient.setScreen(new ConfirmScreen(
                    result -> vClient.setScreen(result ? parent : this),
                    confirmCloseTitle, confirmCloseDescription));
            return;
        }

        // Immediate close
        vClient.setScreen(parent);
    }
}
