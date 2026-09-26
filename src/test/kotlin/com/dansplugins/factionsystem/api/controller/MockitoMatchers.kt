package com.dansplugins.factionsystem.api.controller

import org.mockito.ArgumentMatchers

/**
 * `ArgumentMatchers.any()` registers the matcher and then returns null. Javalin's
 * `Context.json(obj: Any)` declares its parameter non-null, so Kotlin asserts on that
 * expression at the call site and the bare matcher fails with "any() must not be null"
 * before Mockito is ever consulted.
 *
 * The elvis branch supplies a non-null placeholder. Mockito matches on the matcher it
 * just registered, never on this value, so the substitution does not weaken the stub.
 */
fun anyObject(): Any = ArgumentMatchers.any() ?: Any()
