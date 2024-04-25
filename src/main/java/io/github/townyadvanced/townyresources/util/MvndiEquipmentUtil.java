package io.github.townyadvanced.townyresources.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.mvndicraft.mvndiequipment.ItemManager;

public class MvndiEquipmentUtil {

    @Nullable
    public static ItemStack getItemStack(String materialName, int amount) {
        return ItemManager.createItem(materialName, amount);
    }

    public static String getMaterialNameForDisplay(String materialName) {
        return ItemManager.getItem(materialName).getDisplayName(); 
    }

	public static boolean isValidItem(String materialName) {
		return ItemManager.exists(materialName);
	}

}
