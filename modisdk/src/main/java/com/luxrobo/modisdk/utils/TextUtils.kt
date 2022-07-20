package com.luxrobo.modisdk.utils

fun String.addSeparator(): String = StringBuffer(this).apply {
    insert(1, ".")
    insert(3, ".")
}.toString()