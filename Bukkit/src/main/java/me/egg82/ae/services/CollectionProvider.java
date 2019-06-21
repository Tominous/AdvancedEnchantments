package me.egg82.ae.services;

import java.util.UUID;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class CollectionProvider {
    private CollectionProvider() {}

    private static ExpiringMap<UUID, Double> bleeding = ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();
    public static ExpiringMap<UUID, Double> getBleeding() { return bleeding; }
}