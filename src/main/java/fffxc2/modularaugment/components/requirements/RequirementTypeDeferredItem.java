package fffxc2.modularaugment.components.requirements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.nbt.NBTJsonDeserializer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nonnull;

public class RequirementTypeDeferredItem extends RequirementType<ItemStack, RequirementDeferredItem> {
    @Nonnull
    @Override
    public RequirementDeferredItem createRequirement(IOType type, JsonObject jsonObject) {
        RequirementDeferredItem req;

        if (!jsonObject.has("item") || !jsonObject.get("item").isJsonPrimitive() ||
                !jsonObject.get("item").getAsJsonPrimitive().isString()) {
            throw new JsonParseException("The ComponentType 'deferred-item' expects an 'item'-entry that defines the item!");
        }

        if (type != IOType.INPUT) {
            throw new JsonParseException("The ComponentType \'" + getRegistryName() + "\' can only be used when \'io-type\' is set to \'input\'!");
        }


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
        int amount = 1;
        if (jsonObject.has("amount")) {
            if (!jsonObject.get("amount").isJsonPrimitive() || !jsonObject.getAsJsonPrimitive("amount").isNumber()) {
                throw new JsonParseException("'amount', if defined, needs to be a amount-number!");
            }
            amount = MathHelper.clamp(jsonObject.getAsJsonPrimitive("amount").getAsInt(), 1, 64);
        }

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

        if (jsonObject.has("nbt")) {
            if (!jsonObject.has("nbt") || !jsonObject.get("nbt").isJsonObject()) {
                throw new JsonParseException("The ComponentType 'nbt' expects a json compound that defines the NBT tag!");
            }
            String nbtString = jsonObject.getAsJsonObject("nbt").toString();
            try {
                req.tag = NBTJsonDeserializer.deserialize(nbtString);
            } catch (NBTException exc) {
                throw new JsonParseException("Error trying to parse NBTTag! Rethrowing exception...", exc);
            }

            if (jsonObject.has("nbt-display")) {
                if (!jsonObject.has("nbt-display") || !jsonObject.get("nbt-display").isJsonObject()) {
                    throw new JsonParseException("The ComponentType 'nbt-display' expects a json compound that defines the NBT tag meant to be used for displaying!");
                }
                String nbtDisplayString = jsonObject.getAsJsonObject("nbt-display").toString();
                try {
                    req.previewDisplayTag = NBTJsonDeserializer.deserialize(nbtDisplayString);
                } catch (NBTException exc) {
                    throw new JsonParseException("Error trying to parse NBTTag! Rethrowing exception...", exc);
                }
            } else {
                req.previewDisplayTag = req.tag.copy();
            }
        }
        return req;
    }
}
