package com.p1nero.multiplayer_boss_fight;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiplayerBossManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Type CONFIG_LIST_TYPE = new TypeToken<List<RawBossConfig>>() {
    }.getType();
    private static final String DEFAULT_CONFIG_RESOURCE = "config.json";
    private static final String DEMO_CONFIG_RESOURCE = "demo.jsonc";
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get()
            .resolve(MultiplayerBossFightMod.MOD_ID);
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(MultiplayerBossFightMod.MOD_ID)
            .resolve(DEFAULT_CONFIG_RESOURCE);
    private static final List<ResolvedBossConfig> CONFIGS = new ArrayList<>();

    public static void init() {
        CONFIGS.clear();
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException exception) {
            LOGGER.error("Failed to create config directory: {}", CONFIG_DIR, exception);
            return;
        }

        if (!Files.exists(CONFIG_PATH)) {
            LOGGER.warn("Boss config file does not exist: {}, restoring defaults", CONFIG_PATH);
            if (!restoreDefaultConfigs()) {
                return;
            }
        }

        if (!loadConfigsFromDisk()) {
            LOGGER.warn("Boss config file is invalid or damaged, restoring defaults from resources");
            if (!restoreDefaultConfigs()) {
                return;
            }
            if (!loadConfigsFromDisk()) {
                LOGGER.error("Failed to load restored boss config from {}", CONFIG_PATH);
            }
        }
    }

    private static boolean loadConfigsFromDisk() {
        try {
            String json = stripJsonComments(Files.readString(CONFIG_PATH, StandardCharsets.UTF_8));
            List<RawBossConfig> rawConfigs = GSON.fromJson(json, CONFIG_LIST_TYPE);
            if (rawConfigs == null) {
                LOGGER.warn("Boss config file is empty: {}", CONFIG_PATH);
                return false;
            }

            List<ResolvedBossConfig> loadedConfigs = new ArrayList<>();
            for (RawBossConfig rawConfig : rawConfigs) {
                ResolvedBossConfig resolvedConfig = resolveConfig(rawConfig);
                if (resolvedConfig != null) {
                    loadedConfigs.add(resolvedConfig);
                }
            }

            if (loadedConfigs.isEmpty()) {
                LOGGER.warn("No valid boss config entries found in {}", CONFIG_PATH);
                return false;
            }

            CONFIGS.clear();
            CONFIGS.addAll(loadedConfigs);
            LOGGER.info("Loaded {} multiplayer boss config entries from {}", CONFIGS.size(), CONFIG_PATH);
            return true;
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load multiplayer boss config from {}", CONFIG_PATH, exception);
            return false;
        }
    }

    private static boolean restoreDefaultConfigs() {
        try {
            Files.createDirectories(CONFIG_DIR);
            copyResourceToConfig(DEFAULT_CONFIG_RESOURCE, CONFIG_DIR.resolve(DEFAULT_CONFIG_RESOURCE));
            copyResourceToConfig(DEMO_CONFIG_RESOURCE, CONFIG_DIR.resolve(DEMO_CONFIG_RESOURCE));
            return true;
        } catch (IOException exception) {
            LOGGER.error("Failed to restore default boss config files to {}", CONFIG_DIR, exception);
            return false;
        }
    }

    private static void copyResourceToConfig(String resourcePath, Path targetPath) throws IOException {
        try (InputStream inputStream = MultiplayerBossFightMod.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing bundled resource: " + resourcePath);
            }
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void refreshBossAttributes(Entity target) {
        if (!(target instanceof LivingEntity livingEntity) || target.level().isClientSide()) {
            return;
        }

        ResolvedBossConfig config = findConfig(target.getType());
        if (config == null) {
            return;
        }

        int range = Math.max(1, target.getType().clientTrackingRange()) * 16;
        int nearbyPlayers = target.level().getEntitiesOfClass(Player.class, target.getBoundingBox().inflate(range),
                player -> player.isAlive() && !player.isSpectator()).size();
        applyAttributeModifiers(livingEntity, config, nearbyPlayers);
    }

    private static void applyAttributeModifiers(LivingEntity livingEntity, ResolvedBossConfig config, int nearbyPlayers) {
        for (ResolvedAttributeModifier modifierConfig : config.attributeModifiers()) {
            AttributeInstance attributeInstance = livingEntity.getAttribute(modifierConfig.attribute());
            if (attributeInstance == null) {
                continue;
            }

            attributeInstance.removeModifier(modifierConfig.id());
            if (nearbyPlayers <= 0 || nearbyPlayers > modifierConfig.maxLimit()) {
                continue;
            }

            double amount = modifierConfig.value() * nearbyPlayers;
            AttributeModifier modifier = new AttributeModifier(
                    modifierConfig.id(),
                    amount,
                    modifierConfig.operation()
            );
            attributeInstance.addTransientModifier(modifier);
        }

        if (livingEntity.getHealth() > livingEntity.getMaxHealth()) {
            livingEntity.setHealth(livingEntity.getMaxHealth());
        }
    }

    private static ResolvedBossConfig findConfig(EntityType<?> entityType) {
        for (ResolvedBossConfig config : CONFIGS) {
            if (config.matchesExact(entityType)) {
                return config;
            }
        }

        for (ResolvedBossConfig config : CONFIGS) {
            if (config.matchesTag(entityType)) {
                return config;
            }
        }

        return null;
    }

    private static ResolvedBossConfig resolveConfig(RawBossConfig rawConfig) {
        if (rawConfig == null) {
            return null;
        }

        Set<EntityType<?>> entityTypes = new HashSet<>();
        List<TagKey<EntityType<?>>> entityTags = new ArrayList<>();
        for (String entityId : safeList(rawConfig.entities())) {
            if (entityId == null || entityId.isBlank()) {
                continue;
            }

            if (entityId.startsWith("#")) {
                ResourceLocation tagLocation = parseResourceLocation(entityId.substring(1));
                if (tagLocation == null) {
                    LOGGER.warn("Invalid entity tag in config: {}", entityId);
                    continue;
                }
                entityTags.add(TagKey.create(Registries.ENTITY_TYPE, tagLocation));
                continue;
            }

            ResourceLocation entityLocation = parseResourceLocation(entityId);
            if (entityLocation == null) {
                LOGGER.warn("Invalid entity id in config: {}", entityId);
                continue;
            }

            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityLocation);
            if (entityType == null) {
                LOGGER.warn("Unknown entity id in config: {}", entityId);
                continue;
            }
            entityTypes.add(entityType);
        }

        List<ResolvedAttributeModifier> attributeModifiers = new ArrayList<>();
        for (RawAttributeModifier rawModifier : safeList(rawConfig.attributeModifiers())) {
            ResolvedAttributeModifier modifier = resolveAttributeModifier(rawModifier);
            if (modifier != null) {
                attributeModifiers.add(modifier);
            }
        }

        if ((entityTypes.isEmpty() && entityTags.isEmpty()) || attributeModifiers.isEmpty()) {
            return null;
        }
        return new ResolvedBossConfig(entityTypes, entityTags, attributeModifiers);
    }

    private static ResolvedAttributeModifier resolveAttributeModifier(RawAttributeModifier rawModifier) {
        if (rawModifier == null || rawModifier.attribute() == null || rawModifier.operation() == null) {
            return null;
        }

        ResourceLocation attributeLocation = parseResourceLocation(rawModifier.attribute());
        if (attributeLocation == null) {
            LOGGER.warn("Invalid attribute id in config: {}", rawModifier.attribute());
            return null;
        }

        ResourceKey<Attribute> attributeKey = ResourceKey.create(Registries.ATTRIBUTE, attributeLocation);
        Holder.Reference<Attribute> attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(attributeKey).orElse(null);
        if (attributeHolder == null) {
            LOGGER.warn("Unknown attribute id in config: {}", rawModifier.attribute());
            return null;
        }

        try {
            AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(rawModifier.operation());
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                    MultiplayerBossFightMod.MOD_ID,
                    rawModifier.attribute().replace(':', '/')
            );
            return new ResolvedAttributeModifier(
                    attributeHolder,
                    operation,
                    rawModifier.value(),
                    Math.max(0, rawModifier.maxLimit()),
                    modifierId
            );
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Invalid attribute modifier operation in config: {}", rawModifier.operation());
            return null;
        }
    }

    private static ResourceLocation parseResourceLocation(String id) {
        try {
            return ResourceLocation.parse(id);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String stripJsonComments(String content) {
        StringBuilder builder = new StringBuilder(content.length());
        boolean inString = false;
        boolean escaping = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            char next = i + 1 < content.length() ? content.charAt(i + 1) : '\0';

            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    builder.append(current);
                }
                continue;
            }

            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }

            if (inString) {
                builder.append(current);
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '/' && next == '/') {
                lineComment = true;
                i++;
                continue;
            }

            if (current == '/' && next == '*') {
                blockComment = true;
                i++;
                continue;
            }

            builder.append(current);
            if (current == '"') {
                inString = true;
            }
        }

        return builder.toString();
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record RawBossConfig(List<String> entities, List<RawAttributeModifier> attribute_modifiers) {
        List<RawAttributeModifier> attributeModifiers() {
            return attribute_modifiers;
        }
    }

    private record RawAttributeModifier(String attribute, String operation, double value, int max_limit) {
        int maxLimit() {
            return max_limit;
        }
    }

    private record ResolvedBossConfig(
            Set<EntityType<?>> entityTypes,
            List<TagKey<EntityType<?>>> entityTags,
            List<ResolvedAttributeModifier> attributeModifiers
    ) {
        boolean matchesExact(EntityType<?> entityType) {
            return entityTypes.contains(entityType);
        }

        boolean matchesTag(EntityType<?> entityType) {
            for (TagKey<EntityType<?>> entityTag : entityTags) {
                if (entityType.is(entityTag)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record ResolvedAttributeModifier(
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation,
            double value,
            int maxLimit,
            ResourceLocation id
    ) {
    }
}
