package me.egg82.ae.api.enchantments;

import java.util.UUID;
import me.egg82.ae.api.AdvancedEnchantment;
import me.egg82.ae.api.AdvancedEnchantmentTarget;

public class ThunderousEnchantment extends AdvancedEnchantment {
    public ThunderousEnchantment() {
        super(UUID.randomUUID(), "thunderous", "Thunderous", false, 1, 5);
        targets.add(AdvancedEnchantmentTarget.WEAPON);
    }
}
