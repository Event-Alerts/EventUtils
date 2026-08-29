package cc.aabss.eventutils.mixin;

//? if >=1.21.11 {
/*import cc.aabss.eventutils.accessor.PlayerEntityRenderStateAccessor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements PlayerEntityRenderStateAccessor {

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
*///?}
