package cc.aabss.eventutils.mixin;

import cc.aabss.eventutils.accessor.PlayerEntityRenderStateAccessor;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements PlayerEntityRenderStateAccessor {

    @Unique
    private String eventutils$rawName;

    @Override
    public String eventutils$getRawName() {
        return eventutils$rawName;
    }

    @Override
    public void eventutils$setRawName(String name) {
        this.eventutils$rawName = name;
    }
}
