/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class CBOR {

    private final static ObjectMapper MAPPER = new ObjectMapper(new CBORFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    //*********************************************************************************************
    // Encode
    //*********************************************************************************************

    /*
     * Bytes
     */
    public static byte[] toBytes(@Nullable Object obj) throws IOException {
        return MAPPER.writeValueAsBytes(obj);
    }

    //*********************************************************************************************
    // Decode
    //*********************************************************************************************

    /*
     * String
     */
    public static String toString(@NotNull byte[] data) throws IOException {
        return MAPPER.readTree(data).toString();
    }

    /*
     * Object
     */
    public static <T> T toObject(@NotNull byte[] data, @NotNull Class<T> type) throws IOException {
        return MAPPER.readValue(data, type);
    }

    /*
     * Map
     */
    public static <T> Map<String, T> toMap(@NotNull byte[] data, @NotNull Class<T> type) throws IOException {
        MapType mapType = MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, type);
        return MAPPER.readValue(data, mapType);
    }

    /*
     * List
     */
    public static <T> List<T> toList(@NotNull byte[] data, @NotNull Class<T> type) throws IOException {
        CollectionType listType = MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, type);
        return MAPPER.readValue(data, listType);
    }

    //*********************************************************************************************
    // Decode Value
    //*********************************************************************************************

    /*
     * Object
     */
    public static <T> T getObject(@NotNull byte[] data, @NotNull String key, @NotNull Class<T> type) throws IOException {
        return MAPPER.convertValue(MAPPER.readTree(data).get(key), type);
    }

    /*
     * Integer
     */
    public static int getInt(@NotNull byte[] data, @NotNull String key) throws IOException {
        return assertNonNull(MAPPER.readTree(data).get(key), data, key).asInt();
    }
    public static int getInt(@NotNull byte[] data, @NotNull String key, int defaultValue) throws IOException {
        JsonNode node = MAPPER.readTree(data).get(key);
        return node == null ? defaultValue : node.asInt(defaultValue);
    }

    /*
     * Boolean
     */
    public static boolean getBoolean(@NotNull byte[] data, @NotNull String key) throws IOException {
        return assertNonNull(MAPPER.readTree(data).get(key), data, key).asBoolean();
    }
    public static boolean getBoolean(@NotNull byte[] data, @NotNull String key, boolean defaultValue) throws IOException {
        JsonNode node = MAPPER.readTree(data).get(key);
        return node == null ? defaultValue : node.asBoolean(defaultValue);
    }

    /*
     * Double
     */
    public static double getDouble(@NotNull byte[] data, @NotNull String key) throws IOException {
        return assertNonNull(MAPPER.readTree(data).get(key), data, key).asDouble();
    }
    public static double getDouble(@NotNull byte[] data, @NotNull String key, double defaultValue) throws IOException {
        JsonNode node = MAPPER.readTree(data).get(key);
        return node == null ? defaultValue : node.asDouble(defaultValue);
    }

    /*
     * Long
     */
    public static long getLong(@NotNull byte[] data, @NotNull String key) throws IOException {
        return assertNonNull(MAPPER.readTree(data).get(key), data, key).asLong();
    }
    public static long getLong(@NotNull byte[] data, @NotNull String key, long defaultValue) throws IOException {
        JsonNode node = MAPPER.readTree(data).get(key);
        return node == null ? defaultValue : node.asLong(defaultValue);
    }

    /*
     * String
     */
    public static String getString(@NotNull byte[] data, @NotNull String key) throws IOException {
        return assertNonNull(MAPPER.readTree(data).get(key), data, key).asText();
    }
    public static String getString(@NotNull byte[] data, @NotNull String key, String defaultValue) throws IOException {
        JsonNode node = MAPPER.readTree(data).get(key);
        return node == null ? defaultValue : node.asText(defaultValue);
    }


    //*********************************************************************************************
    // Utility Methods
    //*********************************************************************************************

    private static JsonNode assertNonNull(JsonNode node, byte[] data, String key) throws IOException {
        if (node == null) {
            throw new IOException("Property \"" + key + "\"not found in object: " + CBOR.toString(data));
        }
        return node;
    }
}
