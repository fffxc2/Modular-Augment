package fffxc2.modularaugment.components.requirements;

import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentItem;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import javax.annotation.Nonnull;
import java.util.List;
import static hellfirepvp.modularmachinery.common.machine.IOType.INPUT;

public class RequirementDeferredItem extends ComponentRequirement.PerTick<ItemStack, RequirementTypeDeferred<ItemStack, RequirementDeferredItem>> {
    private static RequirementTypeDeferred<ItemStack, RequirementDeferredItem> REQUIREMENT_TYPE_DEFERRED_ITEM;
    public final RequirementItem.ItemRequirementType requirementType;

    public final ItemStack required;

    public final String oreDictName;
    public final int oreDictItemAmount;

    public NBTTagCompound tag = null;
    public NBTTagCompound previewDisplayTag = null;

    private ProcessingComponent<?> itemLocation;

    public RequirementDeferredItem(ItemStack item) {
        super(REQUIREMENT_TYPE_DEFERRED_ITEM, INPUT);
        this.requirementType = RequirementItem.ItemRequirementType.ITEMSTACKS;
        this.required = item.copy();
        this.oreDictName = null;
        this.oreDictItemAmount = 0;
    }

    public RequirementDeferredItem(String oreDictName, int oreDictAmount) {
        super(REQUIREMENT_TYPE_DEFERRED_ITEM, INPUT);
        this.requirementType = RequirementItem.ItemRequirementType.OREDICT;
        this.oreDictName = oreDictName;
        this.oreDictItemAmount = oreDictAmount;
        this.required = ItemStack.EMPTY;
    }

    @Override
    public ComponentRequirement<ItemStack, RequirementTypeDeferred<ItemStack, RequirementDeferredItem>> deepCopy() {
        RequirementDeferredItem item;
        switch (this.requirementType) {
            case OREDICT:
                item = new RequirementDeferredItem(this.oreDictName, this.oreDictItemAmount);
                break;
            default:
            case ITEMSTACKS:
                item = new RequirementDeferredItem(this.required.copy());
                break;
        }
        if(this.tag != null) {
            item.tag = this.tag.copy();
        }
        if(this.previewDisplayTag != null) {
            item.previewDisplayTag = this.previewDisplayTag.copy();
        }
        return item;
    }

    @Override
    public ComponentRequirement<ItemStack, RequirementTypeDeferred<ItemStack, RequirementDeferredItem>> deepCopyModified(List<RecipeModifier> modifiers) {
        RequirementDeferredItem item;
        switch (this.requirementType) {
            case OREDICT:
                int inOreAmt = Math.round(RecipeModifier.applyModifiers(modifiers, this, this.oreDictItemAmount, false));
                item = new RequirementDeferredItem(this.oreDictName, inOreAmt);
                break;
            default:
            case ITEMSTACKS:
                ItemStack inReq = this.required.copy();
                int amt = Math.round(RecipeModifier.applyModifiers(modifiers, this, inReq.getCount(), false));
                inReq.setCount(amt);
                item = new RequirementDeferredItem(inReq);
                break;
        }

        if(this.tag != null) {
            item.tag = this.tag.copy();
        }
        if(this.previewDisplayTag != null) {
            item.previewDisplayTag = this.previewDisplayTag.copy();
        }
        return item;
    }

    @Override
    public JEIComponent<ItemStack> provideJEIComponent() {
        RequirementItem output;
        switch (this.requirementType) {
            case OREDICT:
                output = new RequirementItem(INPUT, this.oreDictName, this.oreDictItemAmount);
                break;
            default:
            case ITEMSTACKS:
                output = new RequirementItem(INPUT, this.required.copy());
                break;
        }
        return new JEIComponentItem(output);
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.getComponent();
        return cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_ITEM) &&
                cmp instanceof MachineComponent.ItemBus &&
                cmp.getIOType() == INPUT;
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
        return "Missing deferred item input";
    }

    private boolean validItemState(ProcessingComponent<?> component, RecipeCraftingContext context) {
        IOInventory handler = (IOInventory) component.getProvidedComponent();
        switch (this.requirementType) {
            case ITEMSTACKS:
                ItemStack stackRequired = this.required.copy();
                int amt = Math.round(RecipeModifier.applyModifiers(context, this, stackRequired.getCount(), false));
                stackRequired.setCount(amt);
                return ItemUtils.consumeFromInventory(handler, stackRequired, true, this.tag);
            case OREDICT:
                int requiredOredict = Math.round(RecipeModifier.applyModifiers(context, this, this.oreDictItemAmount, false));
                return ItemUtils.consumeFromInventoryOreDict(handler, this.oreDictName, requiredOredict, true, this.tag);
        }
        return false;
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        return validItemState(component, context) ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.item.input");
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return validItemState(component, context);
    }

    @Override
    @Nonnull
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if(oreDictName == null && required.isEmpty()) {
            throw new IllegalStateException("Invalid item output!");
        }
        IOInventory handler = (IOInventory) component.getProvidedComponent();
        if (component != this.itemLocation) return CraftCheck.skipComponent();
        switch (this.requirementType) {
            case OREDICT:
                int requiredOredict = Math.round(RecipeModifier.applyModifiers(context, this, this.oreDictItemAmount, false));
                ItemUtils.consumeFromInventoryOreDict(handler, this.oreDictName, requiredOredict, false, this.tag);
                return CraftCheck.success();
            default:
            case ITEMSTACKS:
                ItemStack stackRequired = this.required.copy();
                int amt = Math.round(RecipeModifier.applyModifiers(context, this, stackRequired.getCount(), false));
                stackRequired.setCount(amt);
                ItemUtils.consumeFromInventory(handler, stackRequired, false, this.tag);
                return CraftCheck.success();
        }
    }


    @Override
    public void startIOTick(RecipeCraftingContext context, float durationMultiplier) {
        this.itemLocation = null;
    }

    @Nonnull
    @Override
    public CraftCheck resetIOTick(RecipeCraftingContext context) {
        return (this.itemLocation != null) ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.item.input");
    }

    @Nonnull
    @Override
    public CraftCheck doIOTick(ProcessingComponent<?> component, RecipeCraftingContext context) {
        if (validItemState(component, context)) {
            this.itemLocation = component;
            return CraftCheck.success();
        }
        return CraftCheck.partialSuccess();
    }


}