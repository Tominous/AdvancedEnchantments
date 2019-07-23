package me.egg82.ae.events.enchants.inventory.inventoryClick;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import me.egg82.ae.api.BukkitEnchantableItem;
import me.egg82.ae.api.BukkitEnchantment;
import me.egg82.ae.api.GenericEnchantment;
import me.egg82.ae.services.material.MaterialLookup;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryClickAnvilRewrite implements Consumer<InventoryClickEvent> {
    private static Material enchantedBookMaterial;

    static {
        Optional<Material> m = MaterialLookup.get("ENCHANTED_BOOK");
        if (!m.isPresent()) {
            throw new RuntimeException("Could not get enchanted book material.");
        }
        enchantedBookMaterial = m.get();
    }

    public InventoryClickAnvilRewrite() { }

    public void accept(InventoryClickEvent event) {
        ItemStack carryoverItem = event.getInventory().getItem(0);
        ItemStack sacrificeItem = event.getInventory().getItem(1);
        ItemStack resultItem = event.getCurrentItem();

        if (carryoverItem == null || carryoverItem.getType() == Material.AIR || sacrificeItem == null || sacrificeItem.getType() == Material.AIR || resultItem == null || resultItem.getType() == Material.AIR) {
            return;
        }

        BukkitEnchantableItem enchantableResultItem = BukkitEnchantableItem.fromItemStack(resultItem);

        if (sacrificeItem.getType() == enchantedBookMaterial) {
            if (!sacrificeItem.hasItemMeta()) {
                return;
            }
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) sacrificeItem.getItemMeta();
            if (!meta.hasStoredEnchants()) {
                return;
            }

            applyEnchants(toGenericEnchants(meta.getStoredEnchants()), enchantableResultItem);
        } else {
            if (!sacrificeItem.hasItemMeta()) {
                return;
            }
            ItemMeta meta = sacrificeItem.getItemMeta();
            if (!meta.hasEnchants()) {
                return;
            }

            BukkitEnchantableItem enchantableSacrificeItem = BukkitEnchantableItem.fromItemStack(sacrificeItem);
            applyEnchants(enchantableSacrificeItem.getEnchantments(), enchantableResultItem);
        }

        enchantableResultItem.rewriteMeta();
        event.setCurrentItem((ItemStack) enchantableResultItem.getConcrete());
    }

    private void applyEnchants(Map<GenericEnchantment, Integer> enchants, BukkitEnchantableItem enchantableCarryoverItem) {
        Map<GenericEnchantment, Integer> newEnchants = new HashMap<>();

        // Add all enchants from sacrifice item
        for (Map.Entry<GenericEnchantment, Integer> kvp : enchants.entrySet()) {
            if (enchantableCarryoverItem.hasEnchantment(kvp.getKey())) {
                // carryover has enchant
                if (kvp.getValue() > enchantableCarryoverItem.getEnchantmentLevel(kvp.getKey())) {
                    // sacrifice's level is greater, so raise level to sacrifice's level
                    newEnchants.put(kvp.getKey(), kvp.getValue());
                } else if (kvp.getValue() == enchantableCarryoverItem.getEnchantmentLevel(kvp.getKey())) {
                    // sacrifice's level is equal, so raise level by one (if applicable)
                    if (enchantableCarryoverItem.getEnchantmentLevel(kvp.getKey()) < kvp.getKey().getMaxLevel()) {
                        newEnchants.put(kvp.getKey(), enchantableCarryoverItem.getEnchantmentLevel(kvp.getKey()) + 1);
                    }
                }
                // Do nothing if sacrifice's level is lower
                continue;
            }

            if (kvp.getKey().canEnchant(enchantableCarryoverItem)) {
                // Only add enchants that are compatible
                newEnchants.put(kvp.getKey(), kvp.getValue());
            }
        }

        enchantableCarryoverItem.setEnchantmentLevels(newEnchants);
    }

    private Map<GenericEnchantment, Integer> toGenericEnchants(Map<Enchantment, Integer> enchants) {
        Map<GenericEnchantment, Integer> retVal = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> kvp : enchants.entrySet()) {
            retVal.put(BukkitEnchantment.fromEnchant(kvp.getKey()), kvp.getValue());
        }
        return retVal;
    }
}