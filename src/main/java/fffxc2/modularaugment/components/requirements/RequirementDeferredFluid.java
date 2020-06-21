package fffxc2.modularaugment.components.requirements;

import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentHybridFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidGas;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import mekanism.api.gas.GasStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static hellfirepvp.modularmachinery.common.machine.IOType.INPUT;

public class RequirementDeferredFluid extends ComponentRequirement.PerTick<HybridFluid, RequirementTypeDeferred<HybridFluid, RequirementDeferredFluid>> {
    public static RequirementTypeDeferred<HybridFluid, RequirementDeferredFluid> REQUIREMENT_TYPE_DEFERRED_FLUID;
    public final HybridFluid required;

    private NBTTagCompound tagMatch = null, tagDisplay = null;

    private ProcessingComponent<?> fluidLocation;

    public RequirementDeferredFluid(HybridFluid required) {
        super(REQUIREMENT_TYPE_DEFERRED_FLUID, INPUT);
        this.required = required.copy();
    }

    @Override
    public int getSortingWeight() {
        return PRIORITY_WEIGHT_FLUID;
    }

    @Override
    public ComponentRequirement<HybridFluid, RequirementTypeDeferred<HybridFluid, RequirementDeferredFluid>> deepCopy() {
        RequirementDeferredFluid fluid = new RequirementDeferredFluid(this.required.copy());
        fluid.tagMatch = getTagMatch();
        fluid.tagDisplay = getTagDisplay();
        return fluid;
    }

    @Override
    public ComponentRequirement<HybridFluid, RequirementTypeDeferred<HybridFluid, RequirementDeferredFluid>> deepCopyModified(List<RecipeModifier> modifiers) {
        HybridFluid hybrid = this.required.copy();
        hybrid.setAmount(Math.round(RecipeModifier.applyModifiers(modifiers, this, hybrid.getAmount(), false)));
        RequirementDeferredFluid fluid = new RequirementDeferredFluid(hybrid);

        fluid.tagMatch = getTagMatch();
        fluid.tagDisplay = getTagDisplay();
        return fluid;
    }

    @Override
    public JEIComponent<HybridFluid> provideJEIComponent() {
        // This might be wrong if we are working with a gas
        RequirementFluid output = new RequirementFluid(INPUT, this.required.copy().asFluidStack());
        // If we have Mekanism and this needs a gas, make sure the RequirementFluid is a gas RequirementFluid
        if(Mods.MEKANISM.isPresent() && this.required instanceof HybridFluidGas) {
            output = RequirementFluid.createMekanismGasRequirement(RequirementTypesMM.REQUIREMENT_GAS, INPUT, ((HybridFluidGas)this.required.copy()).asGasStack());
        }
        return new JEIComponentHybridFluid(output);
    }

    public void setMatchNBTTag(@Nullable NBTTagCompound tag) {
        this.tagMatch = tag;
    }

    @Nullable
    public NBTTagCompound getTagMatch() {
        if(tagMatch == null) {
            return null;
        }
        return tagMatch.copy();
    }

    public void setDisplayNBTTag(@Nullable NBTTagCompound tag) {
        this.tagDisplay = tag;
    }

    @Nullable
    public NBTTagCompound getTagDisplay() {
        if(tagDisplay == null) {
            return null;
        }
        return tagDisplay.copy();
    }

    @Override
    public void startRequirementCheck(ResultChance contextChance, RecipeCraftingContext context) {

    }

    @Override
    public void endRequirementCheck() {

    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        // TODO: Support localization
        if (Mods.MEKANISM.isPresent() && this.required instanceof HybridFluidGas) {
            return "Missing deferred gas input";
        }
        return "Missing deferred fluid input";
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.getComponent();
        return (cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_FLUID) || cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_GAS)) &&
                cmp instanceof MachineComponent.FluidHatch &&
                cmp.getIOType() == this.getActionType();
    }

    private boolean validFluidState(ProcessingComponent<?> component, RecipeCraftingContext context) {
        HybridTank handler = (HybridTank) component.getProvidedComponent();
        FluidStack drained = handler.drainInternal(this.required.copy().asFluidStack(), false);
        if (drained == null) {
            return false;
        }
        return this.required.getAmount() <= drained.amount;
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "mekanism")
    private Optional<Boolean> valueFluidStateWithMekanism(ProcessingComponent<?> component, RecipeCraftingContext context, HybridTank handler) {
        if(handler instanceof HybridGasTank) {
            HybridGasTank gasTank = (HybridGasTank) handler;
            if(this.required instanceof HybridFluidGas) {
                GasStack drained = gasTank.drawGas(EnumFacing.UP, this.required.getAmount(), false);
                if(drained == null) {
                    return Optional.of(false);
                }
                if(drained.getGas() != ((HybridFluidGas) this.required).asGasStack().getGas()) {
                    return Optional.of(false);
                }
                return Optional.of(this.required.getAmount() <= drained.amount);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        if(Mods.MEKANISM.isPresent()) {
            HybridTank handler = (HybridTank) component.getProvidedComponent();
            Optional<Boolean> mekResult = valueFluidStateWithMekanism(component, context, handler);
            if (mekResult.isPresent()) {
                return mekResult.get() ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.gas.input");
            }
        }

        return validFluidState(component, context) ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.fluid.input");
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if(Mods.MEKANISM.isPresent()) {
            HybridTank handler = (HybridTank) component.getProvidedComponent();
            Optional<Boolean> mekResult = valueFluidStateWithMekanism(component, context, handler);
            if (mekResult.isPresent()) {
                return mekResult.get();
            }
        }

        return validFluidState(component, context);
    }

    @Override
    @Nonnull
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        HybridTank handler = (HybridTank) component.getProvidedComponent();
        if (component != this.fluidLocation) return CraftCheck.skipComponent();
        if(Mods.MEKANISM.isPresent()) {
            Optional<CraftCheck> mekResult = finishWithMekanismHandling(handler, context, chance);
            if (mekResult.isPresent()) {
                return mekResult.get();
            }
        }

        handler.drainInternal(this.required.copy().asFluidStack(), true);
        return CraftCheck.success();
    }


    @net.minecraftforge.fml.common.Optional.Method(modid = "mekanism")
    @Nonnull
    private Optional<CraftCheck> finishWithMekanismHandling(HybridTank handler, RecipeCraftingContext context, ResultChance chance) {
        if (this.required instanceof HybridFluidGas && handler instanceof HybridGasTank) {
            HybridGasTank gasHandler = (HybridGasTank) handler;
            gasHandler.drawGas(EnumFacing.UP, this.required.getAmount(), true);
            return Optional.of(CraftCheck.success());
        }
        return Optional.empty();
    }

    @Override
    public void startIOTick(RecipeCraftingContext context, float durationMultiplier) {
        this.fluidLocation = null;
    }

    @Nonnull
    @Override
    public CraftCheck resetIOTick(RecipeCraftingContext context) {
        if(Mods.MEKANISM.isPresent() && this.required instanceof HybridFluidGas) {
            return (this.fluidLocation != null) ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.gas.input");
        }
        return (this.fluidLocation != null) ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.fluid.input");
    }

    @Nonnull
    @Override
    public CraftCheck doIOTick(ProcessingComponent<?> component, RecipeCraftingContext context) {
        if(Mods.MEKANISM.isPresent()) {
            HybridTank handler = (HybridTank) component.getProvidedComponent();
            Optional<Boolean> mekResult = valueFluidStateWithMekanism(component, context, handler);
            if (mekResult.isPresent() && mekResult.get()) {
                this.fluidLocation = component;
                return CraftCheck.success();
            }
        }

        if (validFluidState(component, context)) {
            this.fluidLocation = component;
            return CraftCheck.success();
        }
        return CraftCheck.partialSuccess();
    }
}