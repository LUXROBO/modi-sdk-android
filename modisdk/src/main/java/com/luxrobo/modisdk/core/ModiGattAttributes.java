/*
 * Developement Part, Luxrobo INC., SEOUL, KOREA
 * Copyright(c) 2018 by Luxrobo Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a
 * retrieval system, or transmitted by any means without prior written
 * Permission of Luxrobo Inc.
 */

package com.luxrobo.modisdk.core;

import java.util.HashMap;
import java.util.UUID;


public class ModiGattAttributes {


    private static HashMap<String, String> attributes = new HashMap<>();
    public static final String DEFAULT_96BIT_LEFT = "0000XXXX-0000-1000-8000-00805f9b34fb";
    public static final String DEVICE_ADV_SERVICE = "CDAB";
    public static final String DEVICE_CHAR_SERVICE = "00FF";
    public static final String DEVICE_CHAR_TX_RX = "8421";
    public static final String DEVICE_TX_DESC = "2902";

    static {

        attributes.put(DEVICE_CHAR_SERVICE, "MODI SERVICE");
        attributes.put(DEVICE_CHAR_TX_RX, "DATA RX/TX");
        attributes.put(DEVICE_TX_DESC, "TX DESC");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static String lookup(UUID uuid, String defaultName) {
        String name = attributes.get(convert16UUID(uuid));
        return name == null ? defaultName : name;
    }

    public static boolean isUUIDExist(UUID uuid) {
        return attributes.containsKey(convert16UUID(uuid));
    }

    public static String convert16UUID(UUID uuid) {
        String uuid128 = uuid.toString();

        return uuid128.substring(4,8);
    }

    public static String convert16UUID(String uuid) {
        String uuid128 = uuid;

        return uuid128.substring(4,8);
    }

    public static boolean compareUUID(UUID uuid, String definedUUID) {
        String uuidString = convert16UUID(uuid);

        return uuidString.equalsIgnoreCase(definedUUID);
    }

    public static String convert128UUID(String uuid16) {
        String uuid128 = DEFAULT_96BIT_LEFT.replace("XXXX", uuid16);

        return uuid128;
    }
}
