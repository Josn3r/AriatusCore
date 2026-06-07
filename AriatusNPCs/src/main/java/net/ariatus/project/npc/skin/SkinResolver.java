package net.ariatus.project.npc.skin;

import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.npc.AriatusNPC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SkinResolver {

    private static final String FILE = "skins.yml";

    private final AriatusNPCs module;

    public SkinResolver(AriatusNPCs module) {
        this.module = module;
    }

    public CompletableFuture<Optional<ResolvedSkin>> resolve(AriatusNPC npc, UUID viewerUuid) {
        NPCSkinData skinData = npc.skinData();

        if (skinData.mode() == NPCSkinMode.NONE) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (skinData.mode() == NPCSkinMode.MIRROR) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (skinData.hasTexture()) {
            return CompletableFuture.completedFuture(Optional.of(new ResolvedSkin(
                    skinData.value(),
                    skinData.signature()
            )));
        }

        if (skinData.mode() == NPCSkinMode.NAME) {
            return resolveByName(skinData.source()).thenApply(optional -> {
                optional.ifPresent(skin -> {
                    skinData.value(skin.value());
                    skinData.signature(skin.signature());
                });

                return optional;
            });
        }

        return CompletableFuture.completedFuture(Optional.empty());
    }

    public CompletableFuture<Optional<ResolvedSkin>> resolveByName(String name) {
        if (name == null || name.isBlank()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        String normalized = name.toLowerCase();

        Optional<ResolvedSkin> cached = cached(normalized);

        if (cached.isPresent()) {
            module.logger().info(module, "Skin cargada desde cache: " + name);
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                module.logger().info(module, "Resolviendo UUID de skin: " + name);

                String uuid = fetchUuid(name);

                if (uuid == null || uuid.isBlank()) {
                    module.logger().warn(module, "No se encontró UUID para skin: " + name);
                    return Optional.<ResolvedSkin>empty();
                }

                module.logger().info(module, "UUID resuelto para " + name + ": " + uuid);
                module.logger().info(module, "Resolviendo textura de skin: " + name);

                ResolvedSkin skin = fetchSkin(uuid);

                if (skin == null || !skin.valid()) {
                    module.logger().warn(module, "La skin no devolvió value/signature válido: " + name);
                    return Optional.<ResolvedSkin>empty();
                }

                module.logger().info(module, "Skin resuelta correctamente: " + name
                        + " value=" + skin.value().length()
                        + " signature=" + skin.signature().length());

                cache(normalized, uuid, skin);

                return Optional.of(skin);
            } catch (Exception exception) {
                module.logger().warn(module, "No se pudo resolver skin de " + name + ": " + exception.getMessage());
                return Optional.empty();
            }
        });
    }

    private Optional<ResolvedSkin> cached(String name) {
        String path = "skins.cache." + name + ".";

        String value = module.configString(FILE, path + "value", "");
        String signature = module.configString(FILE, path + "signature", "");

        if (value.isBlank() || signature.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new ResolvedSkin(value, signature));
    }

    private void cache(String name, String uuid, ResolvedSkin skin) {
        String path = "skins.cache." + name + ".";

        module.config(FILE).set(path + "uuid", uuid);
        module.config(FILE).set(path + "value", skin.value());
        module.config(FILE).set(path + "signature", skin.signature());
        module.config(FILE).set(path + "cached-at", System.currentTimeMillis());

        module.saveConfig(FILE);
    }

    private String fetchUuid(String name) throws Exception {
        String encodedName = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);

        String[] urls = {
                "https://api.minecraftservices.com/minecraft/profile/lookup/name/" + encodedName,
                "https://api.mojang.com/users/profiles/minecraft/" + encodedName
        };

        for (String url : urls) {
            String json = read(url);

            module.logger().info(module, "UUID lookup response from " + url + ": " + json);

            if (json == null || json.isBlank()) {
                continue;
            }

            String uuid = extractJsonString(json, "id");

            if (uuid != null && !uuid.isBlank()) {
                return uuid;
            }
        }

        return null;
    }

    private String extractJsonString(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\""
        );

        java.util.regex.Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    private ResolvedSkin fetchSkin(String uuid) throws Exception {
        String cleanUuid = uuid.replace("-", "");

        String url = "https://sessionserver.mojang.com/session/minecraft/profile/"
                + cleanUuid
                + "?unsigned=false";

        String json = read(url);

        module.logger().info(module, "Skin profile response length: " + (json == null ? 0 : json.length()));

        if (json == null || json.isBlank()) {
            return null;
        }

        String value = extractJsonString(json, "value");
        String signature = extractJsonString(json, "signature");

        if (value == null || value.isBlank() || signature == null || signature.isBlank()) {
            module.logger().warn(module, "Respuesta de sessionserver sin value/signature: " + json);
            return null;
        }

        return new ResolvedSkin(value, signature);
    }

    private String read(String url) throws Exception {
        java.net.URI uri = java.net.URI.create(url);

        java.net.HttpURLConnection connection =
                (java.net.HttpURLConnection) uri.toURL().openConnection();

        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "AriatusNPCs/1.0");
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();

        java.io.InputStream stream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        String body = "";

        if (stream != null) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)
            )) {
                StringBuilder builder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                body = builder.toString();
            }
        }

        if (responseCode < 200 || responseCode >= 300) {
            module.logger().warn(module, "HTTP " + responseCode + " leyendo " + url + " body=" + body);
        }

        return body;
    }

    private String extract(String input, String start, String end) {
        int startIndex = input.indexOf(start);

        if (startIndex < 0) {
            return null;
        }

        startIndex += start.length();

        int endIndex = input.indexOf(end, startIndex);

        if (endIndex < 0) {
            return null;
        }

        return input.substring(startIndex, endIndex);
    }
}