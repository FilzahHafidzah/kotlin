// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
package test

import kotlin.contracts.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public inline fun <R> myrun(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// FILE: 2.kt

import test.*

class A {
    val z: String
    init {
        myrun {
            z = "OK"
        }
    }
}

fun box(): String {
    return A().z
}