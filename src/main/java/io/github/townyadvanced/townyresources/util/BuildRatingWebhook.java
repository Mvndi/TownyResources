package io.github.townyadvanced.townyresources.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.palmergames.bukkit.towny.object.Translatable;
import io.github.townyadvanced.townyresources.TownyResources;
import io.github.townyadvanced.townyresources.settings.TownyResourcesSettings;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Discord embed and HTTPS POST follow SiegeWar's webhook implementation, off the game thread. */
public final class BuildRatingWebhook {
    private BuildRatingWebhook() {}

    public static void announce(String town, double rating, String moderator) {
        if (!TownyResourcesSettings.isDiscordWebhookEnabled())
            return;
        String url = TownyResourcesSettings.getDiscordWebhookUrl().trim();
        if (url.isEmpty())
            return;
        // Capture names and translations now; async network work never touches Towny objects.
        String payload = createPayload(town, rating, moderator);
        TownyResources.getPlugin().getScheduler().runAsync(() -> {
            try {
                send(url, payload);
            } catch (IOException | IllegalArgumentException exception) {
                // Exception messages may contain the credential-bearing URL.
                TownyResources.getPlugin().getLogger().warning(
                        "Build-rating Discord announcement failed (" + exception.getClass().getSimpleName() + ").");
            }
        });
    }

    static String createPayload(String town, double rating, String moderator) {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", "TownyResources");
        JsonObject mentions = new JsonObject();
        mentions.add("parse", new JsonArray());
        payload.add("allowed_mentions", mentions);
        JsonObject embed = new JsonObject();
        embed.addProperty("title", Translatable.of("townyresources.build_rating.webhook_title").defaultLocale());
        embed.addProperty("color", 0x55AA55);
        JsonArray fields = new JsonArray();
        addField(fields, "town", town);
        addField(fields, "rating", BigDecimal.valueOf(rating).movePointRight(2).stripTrailingZeros().toPlainString()
                + "% (" + rating + ")");
        addField(fields, "moderator", moderator);
        embed.add("fields", fields);
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);
        return payload.toString();
    }

    private static void addField(JsonArray fields, String key, String value) {
        JsonObject field = new JsonObject();
        field.addProperty("name", Translatable.of("townyresources.build_rating.webhook_" + key).defaultLocale());
        field.addProperty("value", value);
        field.addProperty("inline", true);
        fields.add(field);
    }

    static void send(String url, String payload) throws IOException {
        URI endpoint = URI.create(url);
        if (!"https".equalsIgnoreCase(endpoint.getScheme()))
            throw new IllegalArgumentException("Webhook URL must use HTTPS");
        HttpsURLConnection connection = (HttpsURLConnection) endpoint.toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("User-Agent", "TownyResources-BuildRatings");
        connection.setInstanceFollowRedirects(false);
        connection.setDoOutput(true);
        try {
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(body);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300)
                throw new IOException("Webhook HTTP status " + status);
            connection.getInputStream().close();
        } finally {
            connection.disconnect();
        }
    }
}
