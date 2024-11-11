package io.github.townyadvanced.townyresources.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.mvndicraft.mvndiequipment.ItemManager;

public class MvndiEquipmentUtil {

    @Nullable
    public static ItemStack getItemStack(String materialName, int amount) {
        return ItemManager.getInstance().create(materialName, amount);
    }

    public static String getMaterialNameForDisplay(String materialName) {
        return ItemManager.getInstance().getItem(materialName).name;
    }

    public static boolean isValidItem(String materialName) {
        return ItemManager.getInstance().getItem(materialName) != null;
    }

}
