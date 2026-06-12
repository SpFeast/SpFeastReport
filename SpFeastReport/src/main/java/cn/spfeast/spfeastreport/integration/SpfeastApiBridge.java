package cn.spfeast.spfeastreport.integration;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public final class SpfeastApiBridge {
    private static final String API_CLASS = "com.andyoctopus.spfeastapi.SpfeastApi";

    private final SpFeastReportPlugin plugin;
    private Object api;
    private Method tempBanMethod;
    private Method getBanTemplateKeysMethod;
    private Method tempMuteMethod;
    private Method getMuteReasonKeysMethod;

    public SpfeastApiBridge(@NotNull SpFeastReportPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        api = null;
        tempBanMethod = null;
        getBanTemplateKeysMethod = null;
        tempMuteMethod = null;
        getMuteReasonKeysMethod = null;

        Class<?> apiType;
        try {
            apiType = Class.forName(API_CLASS);
        } catch (ClassNotFoundException ignored) {
            return;
        }

        RegisteredServiceProvider<?> registration = plugin.getServer().getServicesManager().getRegistration(apiType);
        if (registration == null) {
            return;
        }

        Object provider = registration.getProvider();
        if (provider == null) {
            return;
        }

        try {
            tempBanMethod = apiType.getMethod("tempBan", String.class, String.class, String.class, String.class, String.class);
            getBanTemplateKeysMethod = apiType.getMethod("getBanTemplateKeys");
            tempMuteMethod = apiType.getMethod("tempMute", String.class, String.class, String.class, String.class);
            getMuteReasonKeysMethod = apiType.getMethod("getMuteReasonKeys");
        } catch (NoSuchMethodException exception) {
            plugin.getLogger().warning("spfeastApi methods are missing: " + exception.getMessage());
            return;
        }

        api = provider;
    }

    public boolean isAvailable() {
        return api != null && (tempBanMethod != null || tempMuteMethod != null);
    }

    public @Nullable PunishmentResult tempBan(
            @NotNull String targetQuery,
            @NotNull String templateKey,
            @NotNull String durationText,
            @NotNull String reason,
            @NotNull String actorName
    ) {
        if (!isAvailable() || tempBanMethod == null) {
            return null;
        }

        Object result;
        try {
            result = tempBanMethod.invoke(api, targetQuery, templateKey, durationText, reason, actorName);
        } catch (Exception exception) {
            return new PunishmentResult(false, exception.getClass().getSimpleName() + ": " + exception.getMessage(), null, null);
        }

        return parsePunishmentResult(result);
    }

    public @Nullable PunishmentResult tempMute(
            @NotNull String targetQuery,
            @NotNull String reasonKey,
            @NotNull String durationText,
            @NotNull String actorName
    ) {
        if (!isAvailable() || tempMuteMethod == null) {
            return null;
        }

        Object result;
        try {
            result = tempMuteMethod.invoke(api, targetQuery, reasonKey, durationText, actorName);
        } catch (Exception exception) {
            return new PunishmentResult(false, exception.getClass().getSimpleName() + ": " + exception.getMessage(), null, null);
        }

        return parsePunishmentResult(result);
    }

    public @NotNull List<String> getBanTemplateKeys() {
        if (api == null || getBanTemplateKeysMethod == null) {
            return List.of();
        }
        try {
            Object result = getBanTemplateKeysMethod.invoke(api);
            if (result instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    public @NotNull List<String> getMuteReasonKeys() {
        if (api == null || getMuteReasonKeysMethod == null) {
            return List.of();
        }
        try {
            Object result = getMuteReasonKeysMethod.invoke(api);
            if (result instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private @NotNull PunishmentResult parsePunishmentResult(@Nullable Object result) {
        if (result == null) {
            return new PunishmentResult(false, "No result returned from spfeastApi.", null, null);
        }

        try {
            Method success = result.getClass().getMethod("success");
            Method message = result.getClass().getMethod("message");
            Method referenceId = result.getClass().getMethod("referenceId");
            boolean ok = Boolean.TRUE.equals(success.invoke(result));
            String msg = String.valueOf(message.invoke(result));
            String ref = referenceId.invoke(result) != null ? String.valueOf(referenceId.invoke(result)) : null;
            return new PunishmentResult(ok, msg, ref, result);
        } catch (Exception exception) {
            return new PunishmentResult(false, "Failed to read spfeastApi result: " + exception.getMessage(), null, result);
        }
    }

    public record PunishmentResult(boolean success, @NotNull String message, @Nullable String referenceId, @Nullable Object rawResult) {
    }
}
