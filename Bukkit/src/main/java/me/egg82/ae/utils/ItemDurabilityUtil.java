package me.egg82.ae.utils;

import java.util.Optional;
import java.util.Random;
import me.egg82.ae.api.BukkitEnchantableItem;
import me.egg82.ae.extended.CachedConfigValues;
import me.egg82.ae.services.sound.SoundLookup;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemDurabilityUtil {
    private static final Logger logger = LoggerFactory.getLogger(ItemDurabilityUtil.class);

    private static final Random rand = new Random();

    private ItemDurabilityUtil() { }

    private static Sound breakSound;
    private static boolean hasItemBreakEvent;

    static {
        Optional<Sound> tempSound = SoundLookup.get("ENTITY_ITEM_BREAK", "ITEM_BREAK");
        if (!tempSound.isPresent()) {
            throw new RuntimeException("Could not get break sound.");
        }
        breakSound = tempSound.get();

        try {
            Class.forName("org.bukkit.event.player.PlayerItemDamageEvent");
            hasItemBreakEvent = true;
        } catch (ClassNotFoundException ignored) {
            hasItemBreakEvent = false;
        }
    }

    public static boolean removeDurability(BukkitEnchantableItem item, int durabilityToRemove, Location soundLocation) {
        return removeDurability(null, item, durabilityToRemove, soundLocation);
    }

    public static boolean removeDurability(Player player, BukkitEnchantableItem item, int durabilityToRemove, Location soundLocation) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null.");
        }

        if (durabilityToRemove == 0) {
            return true;
        }

        ItemStack i = (ItemStack) item.getConcrete();

        if (i.hasItemMeta() && i.getItemMeta().hasEnchant(Enchantment.DURABILITY)) {
            Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
            if (!cachedConfig.isPresent()) {
                logger.error("Cached config could not be fetched.");
                return true;
            }

            if (!cachedConfig.get().getBypassUnbreaking()) {
                double unbreakingLevel = i.getItemMeta().getEnchantLevel(Enchantment.DURABILITY);
                double unbreakingChance = EnchantmentTarget.ARMOR.includes(i) ? 1.0d - (0.6d + (0.4d / (unbreakingLevel + 1.0d))) : 1.0d - 1.0d / (unbreakingLevel + 1.0d);
                if (rand.nextDouble() <= unbreakingChance) {
                    return true;
                }
            }
        }

        if (player != null && hasItemBreakEvent) {
            org.bukkit.event.player.PlayerItemDamageEvent event = new org.bukkit.event.player.PlayerItemDamageEvent(player, i, durabilityToRemove);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return true;
            }

            durabilityToRemove = event.getDamage();
        }

        short durability = i.getDurability();
        if (durability >= i.getType().getMaxDurability() - durabilityToRemove) {
            if (soundLocation != null) {
                soundLocation.getWorld().playSound(soundLocation, breakSound, 1.0f, 1.0f);
            }
            return false;
        }

        i.setDurability((short) (i.getDurability() + durabilityToRemove));

        return true;
    }

    public static void addDurability(BukkitEnchantableItem item, int durabilityToAdd) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null.");
        }

        if (durabilityToAdd == 0) {
            return;
        }

        ItemStack i = (ItemStack) item.getConcrete();

        int oldDurability = i.getDurability();
        short newDurability = (short) Math.max(0, oldDurability - durabilityToAdd);

        if (newDurability == oldDurability) {
            return;
        }

        i.setDurability(newDurability);
    }
}
