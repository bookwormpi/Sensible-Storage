package bookwormpi.sensiblestorage.mixin;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to access protected methods in Screen class
 */
@Mixin(Screen.class)
public interface ScreenAccessor {
    
    @Invoker("addDrawableChild")
    <T extends Element> T sensiblestorage$addDrawableChild(T drawableElement);
}
