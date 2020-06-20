package fffxc2.modularaugment.components.requirements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import mekanism.api.gas.GasStack;

import javax.annotation.Nonnull;

public class RequirementTypeDeferredFluid extends RequirementType<HybridFluid, RequirementDeferredFluid> {
    @Nonnull
    @Override
    public RequirementDeferredFluid createRequirement(IOType type, JsonObject jsonObject) {
        RequirementDeferredFluid req;

        boolean no_fluid_key = !jsonObject.has("fluid") || !jsonObject.get("fluid").isJsonPrimitive() || !jsonObject.get("fluid").getAsJsonPrimitive().isString();
        boolean no_gas_key = !jsonObject.has("gas") || !jsonObject.get("gas").isJsonPrimitive() || !jsonObject.get("gas").getAsJsonPrimitive().isString();

        if (no_fluid_key && no_gas_key) {
            throw new JsonParseException("The ComponentType 'deferred-fluid' expects a 'fluid' or 'gas'-entry that defines the type of fluid!");
        }


        if (type != IOType.INPUT) {
            throw new JsonParseException("The ComponentType \'" + getRegistryName() + "\' can only be used when \'io-type\' is set to \'input\'!");
        }

        if (!jsonObject.has("amount") || !jsonObject.get("amount").isJsonPrimitive() ||
                !jsonObject.get("amount").getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("The ComponentType 'deferred-fluid' expects an 'amount'-entry that defines the amount of fluid!");
        }

        if (!no_fluid_key) {
            String fluidName = jsonObject.getAsJsonPrimitive("fluid").getAsString();
            int mbAmount = jsonObject.getAsJsonPrimitive("amount").getAsInt();
            Fluid fluid = FluidRegistry.getFluid(fluidName);
            if (fluid == null) {
                throw new JsonParseException("The fluid specified in the 'fluid'-entry (" + fluidName + ") doesn't exist!");
            }
            mbAmount = Math.max(0, mbAmount);
            FluidStack fluidStack = new FluidStack(fluid, mbAmount);
            req = new RequirementDeferredFluid(type, fluidStack);
        } else {
            String gasName = jsonObject.getAsJsonPrimitive("gas").getAsString();
            Gas gas = GasRegistry.getGas(gasName);
            if(gas == null) {
                throw new JsonParseException("The gas specified in the 'gas'-entry (" + gasName + ") doesn't exist!");
            }
            int mbAmount = jsonObject.getAsJsonPrimitive("amount").getAsInt();
            GasStack gasStack = new GasStack(gas, mbAmount);
            req = RequirementDeferredFluid.createMekanismGasRequirement(RequirementDeferredFluid.REQUIREMENT_TYPE_DEFERRED_FLUID, type, gasStack);
        }

        return req;
    }
}
