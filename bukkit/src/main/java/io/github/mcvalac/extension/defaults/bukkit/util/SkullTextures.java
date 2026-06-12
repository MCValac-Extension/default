package io.github.mcvalac.extension.defaults.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for applying a Base64 encoded Mojang texture value to a {@link SkullMeta}
 * using the Bukkit profile API.
 * <p>
 * This replaces the Paper-only {@code com.destroystokyo.paper.profile} based approach
 * so the module can build against the plain Spigot API. The Base64 value is the Mojang
 * "textures" property, e.g.
 * {@code {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/abc"}}}}.
 * </p>
 */
public final class SkullTextures {

    /** Pattern used to extract the skin URL from the decoded JSON without a JSON dependency. */
    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private SkullTextures() {
    }

    /**
     * Applies the given Base64 texture value to the supplied skull meta.
     * <p>
     * On any failure (invalid Base64, missing URL, malformed URL) the meta is left
     * unchanged and no exception is thrown.
     * </p>
     *
     * @param meta          The skull meta to update.
     * @param base64Texture The Base64 encoded Mojang "textures" property value.
     */
    public static void apply(SkullMeta meta, String base64Texture) {
        try {
            String json = new String(Base64.getDecoder().decode(base64Texture), java.nio.charset.StandardCharsets.UTF_8);
            Matcher matcher = URL_PATTERN.matcher(json);
            if (!matcher.find()) {
                return;
            }

            String url = matcher.group(1);
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception e) {
            // Leave the meta unchanged on failure.
        }
    }
}
