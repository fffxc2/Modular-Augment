package fffxc2.modularaugment;

import fffxc2.modularaugment.components.requirements.RequirementTypeDeferredFluid;
import fffxc2.modularaugment.components.requirements.RequirementTypeDeferredItem;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class Registry {
    @SubscribeEvent
    public static void registerAllEvents(RegistryEvent.Register event) {
        if (RequirementType.class != event.getGenericType()) {
            return;
        }

        ResourceLocation itemRegistryName = new ResourceLocation(ModularAugment.MODID,"deferred-item");
        RequirementTypeDeferredItem deferredItem = new RequirementTypeDeferredItem();
        deferredItem.setRegistryName(itemRegistryName);
        event.getRegistry().register(deferredItem);

        ResourceLocation fluidRegistryName = new ResourceLocation(ModularAugment.MODID,"deferred-fluid");
        RequirementTypeDeferredFluid deferredFluid = new RequirementTypeDeferredFluid();
        deferredFluid.setRegistryName(fluidRegistryName);
        event.getRegistry().register(deferredFluid);
    }
}