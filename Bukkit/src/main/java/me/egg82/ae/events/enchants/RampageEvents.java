package me.egg82.ae.events.enchants;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.egg82.ae.APIException;
import me.egg82.ae.api.AdvancedEnchantment;
import me.egg82.ae.api.BukkitEnchantableItem;
import me.egg82.ae.api.GenericEnchantableItem;
import me.egg82.ae.events.EventHolder;
import me.egg82.ae.services.CollectionProvider;
import me.egg82.ae.services.entity.EntityItemHandler;
import me.egg82.ae.utils.PermissionUtil;
import ninja.egg82.events.BukkitEventFilters;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class RampageEvents extends EventHolder {
    public RampageEvents(Plugin plugin) {
        events.add(
                BukkitEvents.subscribe(plugin, EntityDamageByEntityEvent.class, EventPriority.NORMAL)
                        .filter(BukkitEventFilters.ignoreCancelled())
                        .filter(this::townyIgnoreCancelled)
                        .filter(e -> e.getDamager() instanceof LivingEntity)
                        .filter(e -> PermissionUtil.canUseEnchant(e.getDamager(), "ae.enchant.rampage"))
                        .handler(this::damage)
        );
        events.add(
                BukkitEvents.subscribe(plugin, EntityDeathEvent.class, EventPriority.NORMAL)
                        .filter(e -> e.getEntity().getKiller() != null)
                        .filter(e -> PermissionUtil.canUseEnchant(e.getEntity().getKiller(), "ae.enchant.rampage"))
                        .handler(this::death)
        );
    }

    private void damage(EntityDamageByEntityEvent event) {
        EntityItemHandler entityItemHandler;
        try {
            entityItemHandler = ServiceLocator.get(EntityItemHandler.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        LivingEntity from = (LivingEntity) event.getDamager();

        Optional<ItemStack> mainHand = entityItemHandler.getItemInMainHand(from);
        GenericEnchantableItem enchantableMainHand = mainHand.isPresent() ? BukkitEnchantableItem.fromItemStack(mainHand.get()) : null;

        boolean hasEnchantment;
        int level;
        try {
            hasEnchantment = api.anyHasEnchantment(AdvancedEnchantment.RAMPAGE, enchantableMainHand);
            level = api.getMaxLevel(AdvancedEnchantment.RAMPAGE, enchantableMainHand);
        } catch (APIException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (!hasEnchantment) {
            return;
        }

        Integer value = CollectionProvider.getRampage().get(event.getDamager().getUniqueId());
        if (value == null) {
            return;
        }

        double damage = event.getDamage();
        damage += damage - (damage / (level * value + 0.75d));
        event.setDamage(damage);
    }

    private void death(EntityDeathEvent event) {
        EntityItemHandler entityItemHandler;
        try {
            entityItemHandler = ServiceLocator.get(EntityItemHandler.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        Optional<ItemStack> mainHand = entityItemHandler.getItemInMainHand(event.getEntity().getKiller());
        GenericEnchantableItem enchantableMainHand = mainHand.isPresent() ? BukkitEnchantableItem.fromItemStack(mainHand.get()) : null;

        boolean hasEnchantment;
        int level;
        try {
            hasEnchantment = api.anyHasEnchantment(AdvancedEnchantment.RAMPAGE, enchantableMainHand);
            level = api.getMaxLevel(AdvancedEnchantment.RAMPAGE, enchantableMainHand);
        } catch (APIException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }

        if (!hasEnchantment) {
            return;
        }

        UUID key = event.getEntity().getKiller().getUniqueId();

        Integer value = CollectionProvider.getRampage().get(key);
        value = (value == null) ? 1 : value + 1;
        CollectionProvider.getRampage().put(key, value, level * 5, TimeUnit.SECONDS);

        event.getEntity().getKiller().playSound(event.getEntity().getKiller().getEyeLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
    }
}
