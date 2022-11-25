package com.voxelmodpack.hdskins.server;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.annotations.Expose;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.util.UUIDTypeAdapter;
import com.voxelmodpack.hdskins.gui.Feature;
import com.voxelmodpack.hdskins.util.IndentedToStringStyle;
import com.voxelmodpack.hdskins.util.MoreHttpResponses;
import com.voxelmodpack.hdskins.util.NetClient;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpHead;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import javax.annotation.Nullable;

@Deprecated
@ServerType("legacy")
public class LegacySkinServer implements SkinServer {

    private static final String SERVER_ID = "7853dfddc358333843ad55a2c7485c4aa0380a51";

    private static final Set<Feature> FEATURES = Sets.newHashSet(
            Feature.DOWNLOAD_USER_SKIN,
            Feature.UPLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN
    );

    private static final Logger logger = LogManager.getLogger();

    @Expose
    private final String address;

    @Expose
    private final String gateway;

    public LegacySkinServer(String address, @Nullable String gateway) {
        this.address = address;
        this.gateway = gateway;
    }

    @Override
    public TexturePayload getPreviewTextures(GameProfile profile) throws IOException, AuthenticationException {
        SkinServer.verifyServerConnection(Minecraft.getMinecraft().getSession(), SERVER_ID);

        if (Strings.isNullOrEmpty(gateway)) {
            throw gatewayUnsupported();
        }

        Map<String, MinecraftProfileTexture> map = new HashMap<>();
        for (MinecraftProfileTexture.Type type : MinecraftProfileTexture.Type.values()) {
            map.put(type.name(), new MinecraftProfileTexture(getPath(gateway, type, profile), null));
        }

        return new TexturePayload(profile, map);
    }

    @Override
    public TexturePayload loadProfileData(GameProfile profile) throws IOException {
        ImmutableMap.Builder<String, MinecraftProfileTexture> builder = ImmutableMap.builder();
        for (MinecraftProfileTexture.Type type : MinecraftProfileTexture.Type.values()) {

            String url = getPath(address, type, profile);
            try {
                builder.put(type.name(), loadProfileTexture(profile, url));
            } catch (IOException e) {
                logger.trace("Couldn't find texture for {} at {}. Does it exist?", profile.getName(), url, e);
            }
        }

        Map<String, MinecraftProfileTexture> map = builder.build();
        if (map.isEmpty()) {
            throw new HttpException(String.format("No textures found for %s at %s", profile, this.address), 404, null);
        }
        return new TexturePayload(profile, map);
    }

    private MinecraftProfileTexture loadProfileTexture(GameProfile profile, String url) throws IOException {
        try (MoreHttpResponses resp = MoreHttpResponses.execute(HTTP_CLIENT, new HttpHead(url))) {
            resp.requireOk();
            logger.debug("Found skin for {} at {}", profile.getName(), url);

            Header eTagHeader = resp.response().getFirstHeader(HttpHeaders.ETAG);
            final String eTag = eTagHeader == null ? "" : StringUtils.strip(eTagHeader.getValue(), "\"");

            // Add the ETag onto the end of the texture hash. Should properly cache the textures.
            return new MinecraftProfileTexture(url, null) {
                @Override
                public String getHash() {
                    return super.getHash() + eTag;
                }
            };
        }
    }

    @Override
    public void performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException {
        if (Strings.isNullOrEmpty(gateway)) {
            throw gatewayUnsupported();
        }

        SkinServer.verifyServerConnection(upload.getSession(), SERVER_ID);

        NetClient client = new NetClient("POST", gateway);

        client.putFormData(createHeaders(upload), "image/png");

        if (upload.getImage() != null) {
            client.putFile(upload.getType().toString().toLowerCase(Locale.US), "image/png", upload.getImage());
        }

        MoreHttpResponses resp = client.send();
        String response = resp.reader().readLine();

        if (response.startsWith("ERROR: ")) {
            response = response.substring(7);
        }

        if (!response.equalsIgnoreCase("OK") && !response.endsWith("OK")) {
            throw new HttpException(response, resp.responseCode(), null);
        }
    }

    private UnsupportedOperationException gatewayUnsupported() {
        return new UnsupportedOperationException("Server does not have a gateway.");
    }

    private Map<String, ?> createHeaders(SkinUpload upload) {
        Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("user", upload.getSession().getUsername())
                .put("uuid", UUIDTypeAdapter.fromUUID(upload.getSession().getProfile().getId()))
                .put("type", upload.getType().toString().toLowerCase(Locale.US));

        if (upload.getImage() == null) {
            builder.put("clear", "1");
        }

        return builder.build();
    }

    private static String getPath(String address, MinecraftProfileTexture.Type type, GameProfile profile) {
        String uuid = UUIDTypeAdapter.fromUUID(profile.getId());
        String path = type.toString().toLowerCase() + "s";
        return String.format("%s/%s/%s.png?%s", address, path, uuid, Long.toString(new Date().getTime() / 1000));
    }

    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public String toString() {
        return new IndentedToStringStyle.Builder(this)
                .append("address", address)
                .append("gateway", gateway)
                .build();
    }

}