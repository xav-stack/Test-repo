package dev.cannons.mixin;

import dev.cannons.CannonItems;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapedRecipe.class)
abstract class ShapedRecipeMixin {
    @Shadow @Final private Identifier id;

    @Inject(method = "craft(Lnet/minecraft/inventory/RecipeInputInventory;Lnet/minecraft/registry/DynamicRegistryManager;)Lnet/minecraft/item/ItemStack;",
            at = @At("RETURN"), cancellable = true)
    private void cannons$replaceRecipeOutput(RecipeInputInventory inventory,
                                             DynamicRegistryManager registryManager,
                                             CallbackInfoReturnable<ItemStack> cir) {
        if (id.equals(new Identifier("cannons", "cannon"))) {
            cir.setReturnValue(CannonItems.create());
        }
    }
}
