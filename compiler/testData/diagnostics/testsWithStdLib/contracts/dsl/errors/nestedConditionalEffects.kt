// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun foo(boolean: Boolean) {
    contract {
        (returns() implies (boolean)) <!UNRESOLVED_REFERENCE!>implies<!> (!boolean)
    }
}