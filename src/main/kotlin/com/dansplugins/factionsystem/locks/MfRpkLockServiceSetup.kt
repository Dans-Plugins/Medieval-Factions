package com.dansplugins.factionsystem.locks

/**
 * Registers [MfRpkLockService] with RPKit without letting an incompatible RPKit version stop
 * Medieval Factions from enabling (#1770).
 *
 * RPKit 2.2.x declares `com.rpkit.core.service.Service.getPlugin()` as returning `RPKPlugin`;
 * RPKit 2.3.0 and later declare it as returning `Object`. Medieval Factions is compiled against
 * the newer API and is not an `RPKPlugin`, so on an older RPKit the registration fails with an
 * [AbstractMethodError]. The return type is checked up front, and any [LinkageError] raised while
 * registering is caught as a backstop, so the integration is skipped with a warning instead.
 */
object MfRpkLockServiceSetup {

    /**
     * @param serviceInterface RPKit's `Service` interface as loaded on this server
     * @param pluginClass the class [MfRpkLockService.getPlugin] returns
     * @param onIncompatible called when the integration is skipped
     * @param register creates and registers the lock service
     * @return true when the lock service was registered
     */
    fun setUp(
        serviceInterface: Class<*>,
        pluginClass: Class<*>,
        onIncompatible: (Throwable?) -> Unit,
        register: () -> Unit
    ): Boolean {
        if (!isCompatible(serviceInterface, pluginClass)) {
            onIncompatible(null)
            return false
        }
        return try {
            register()
            true
        } catch (error: LinkageError) {
            onIncompatible(error)
            false
        }
    }

    fun isCompatible(serviceInterface: Class<*>, pluginClass: Class<*>): Boolean = try {
        serviceInterface.getMethod("getPlugin").returnType.isAssignableFrom(pluginClass)
    } catch (exception: NoSuchMethodException) {
        false
    }
}
