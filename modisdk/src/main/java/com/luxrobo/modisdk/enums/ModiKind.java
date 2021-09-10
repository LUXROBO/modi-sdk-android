/*
 * Developement Part, Luxrobo INC., SEOUL, KOREA
 * Copyright(c) 2018 by Luxrobo Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a
 * retrieval system, or transmitted by any means without prior written
 * Permission of Luxrobo Inc.
 */

package com.luxrobo.modisdk.enums;


public enum ModiKind {

    MODI(0),
    MODI_PLUS(1);


    private final int value;
    ModiKind(int value) {

        this.value = value;
    }

    public int value() {

        return this.value;
    }
}
