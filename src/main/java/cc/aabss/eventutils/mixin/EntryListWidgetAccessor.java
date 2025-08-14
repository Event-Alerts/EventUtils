package cc.aabss.eventutils.mixin;

import net.minecraft.client.gui.widget.EntryListWidget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(EntryListWidget.class)
public interface EntryListWidgetAccessor {
	@Invoker("getRowTop")
	int invokeGetRowTop(int index);
}


