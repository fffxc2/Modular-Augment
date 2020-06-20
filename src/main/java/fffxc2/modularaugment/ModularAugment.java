package fffxc2.modularaugment;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;


@Mod(modid = ModularAugment.MODID, acceptedMinecraftVersions = "[1.12, 1.13)", dependencies = "required-after:modularmachinery;",
        name = ModularAugment.MOD_NAME, version  = ModularAugment.VERSION)
@Mod.EventBusSubscriber
public class ModularAugment
{
    public static final String MODID = "modularaugment";
    public static final String MOD_NAME = "modularaugment";
    public static final String VERSION = "1.1.0";

    //Configuration configuration; 

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        /* TODO: add config?
        configuration = new Configuration(event.getSuggestedConfigurationFile());
        configuration.load();

        if (configuration.hasChanged())
        {
            configuration.save();
        }
         */

        MinecraftForge.EVENT_BUS.register(Registry.class);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }
}