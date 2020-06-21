package fffxc2.modularaugment.components.requirements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidGas;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;

import javax.annotation.Nonnull;

public class RequirementTypeDeferred<T, V extends ComponentRequirement<T, ? extends RequirementType<T, V>>> extends RequirementType<T, V> {
    @Nonnull
    @Override
    public ComponentRequirement<T, ? extends RequirementType<T, V>> createRequirement(IOType type, JsonObject jsonObject) {
        // TODO: is this needed or can I remove the iotype key with MM barfing??
        if (type != IOType.INPUT) {
            throw new JsonParseException("The ComponentType \'" + getRegistryName() + "\' can only be used when \'io-type\' is set to \'input\'!");
        }
        if (!jsonObject.has("amount") || !jsonObject.get("amount").isJsonPrimitive() ||
                !jsonObject.get("amount").getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("The ComponentType \'" + getRegistryName() + "\'  expects an 'amount'-entry that defines the amount of input!");
        }

        if (!jsonObject.get("amount").isJsonPrimitive() || !jsonObject.getAsJsonPrimitive("amount").isNumber()) {
            throw new JsonParseException("'amount', if defined, needs to be a amount-number!");
        }

        // Maybe create a registry for all RequirementDeferred types so that this can smartly look them up, for now hardcoded
        boolean fluid_type = jsonObject.has("fluid") && jsonObject.get("fluid").isJsonPrimitive() && jsonObject.get("fluid").getAsJsonPrimitive().isString();
        boolean gas_type = jsonObject.has("gas") && jsonObject.get("gas").isJsonPrimitive() && jsonObject.get("gas").getAsJsonPrimitive().isString();
        boolean item_type = jsonObject.has("item") && jsonObject.get("item").isJsonPrimitive() && jsonObject.get("item").getAsJsonPrimitive().isString();

        if(fluid_type) {
            return parseFluid(jsonObject);
        } else if (item_type) {
            return parseItem(jsonObject);
        } else if (gas_type && Mods.MEKANISM.isPresent()) {
            return parseGas(jsonObject);
        } else {
            throw new JsonParseException("The ComponentType \'" + getRegistryName() + "\'  expects an entry of type [fluid, gas, item] defining the deferred input");
        }
    }

    public ComponentRequirement<T, ? extends RequirementType<T, V>> parseFluid(JsonObject jsonObject) {
        RequirementDeferredFluid req;

        String fluidName = jsonObject.getAsJsonPrimitive("fluid").getAsString();
        int mbAmount = jsonObject.getAsJsonPrimitive("amount").getAsInt();
        Fluid fluid = FluidRegistry.getFluid(fluidName);
        if (fluid == null) {
            throw new JsonParseException("The fluid specified in the 'fluid'-entry (" + fluidName + ") doesn't exist!");
        }
        mbAmount = Math.max(0, mbAmount);
        FluidStack fluidStack = new FluidStack(fluid, mbAmount);
        req = new RequirementDeferredFluid(new HybridFluid(fluidStack));

        return (ComponentRequirement)req;
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "mekanism")
    public ComponentRequirement<T, ? extends RequirementType<T, V>> parseGas(JsonObject jsonObject) {
        RequirementDeferredFluid req;

        String gasName = jsonObject.getAsJsonPrimitive("gas").getAsString();
        Gas gas = GasRegistry.getGas(gasName);
        if(gas == null) {
            throw new JsonParseException("The gas specified in the 'gas'-entry (" + gasName + ") doesn't exist!");
        }
        int mbAmount = jsonObject.getAsJsonPrimitive("amount").getAsInt();
        GasStack gasStack = new GasStack(gas, mbAmount);
        req = new RequirementDeferredFluid(new HybridFluidGas(gasStack));

        return (ComponentRequirement)req;
    }

    public ComponentRequirement<T, ? extends RequirementType<T, V>> parseItem(JsonObject jsonObject) {
        RequirementDeferredItem req;

        String itemDefinition = jsonObject.getAsJsonPrimitive("item").getAsString();
        int meta = 0;
        int indexMeta = itemDefinition.indexOf('@');
        if (indexMeta != -1 && indexMeta != itemDefinition.length() - 1) {
            try {
                meta = Integer.parseInt(itemDefinition.substring(indexMeta + 1));
            } catch (NumberFormatException exc) {
                throw new JsonParseException("Expected a metadata number, got " + itemDefinition.substring(indexMeta + 1), exc);
            }
            itemDefinition = itemDefinition.substring(0, indexMeta);
        }
        int amount = MathHelper.clamp(jsonObject.getAsJsonPrimitive("amount").getAsInt(), 1, 64);

        ResourceLocation res = new ResourceLocation(itemDefinition);
        if (res.getResourceDomain().equalsIgnoreCase("ore")) {
            req = new RequirementDeferredItem(itemDefinition.substring(4), amount);
        } else {
            Item item = ForgeRegistries.ITEMS.getValue(res);
            if (item == null || item == Items.AIR) {
                throw new JsonParseException("Couldn't find item with registryName '" + res.toString() + "' !");
            }
            ItemStack result;
            if (meta > 0) {
                result = new ItemStack(item, amount, meta);
            } else {
                result = new ItemStack(item, amount);
            }
            req = new RequirementDeferredItem(result);
        }
        return (ComponentRequirement)req;
    }
}
