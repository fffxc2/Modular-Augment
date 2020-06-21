package fffxc2.modularaugment;

import fffxc2.modularaugment.components.requirements.RequirementTypeDeferred;
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

        // Generic registry for all deferred types
        ResourceLocation registryName = new ResourceLocation(ModularAugment.MODID,"deferred");
        RequirementTypeDeferred deferred = new RequirementTypeDeferred();
        deferred.setRegistryName(registryName);
        event.getRegistry().register(deferred);
    }
}