package org.koin

import org.koin.Koin.Companion.logger
import org.koin.core.bean.BeanDefinition
import org.koin.core.bean.BeanRegistry
import org.koin.core.instance.InstanceFactory
import org.koin.core.property.PropertyRegistry
import org.koin.error.DependencyResolutionException
import org.koin.error.MissingPropertyException
import org.koin.standalone.StandAloneKoinContext
import java.util.*
import kotlin.reflect.KClass

/**
 * Koin Application Context
 * Context from where you can get beans defines in modules
 *
 * @author Arnaud GIULIANI
 */
class KoinContext(val beanRegistry: BeanRegistry, val propertyResolver: PropertyRegistry, val instanceFactory: InstanceFactory) : StandAloneKoinContext {

    /**
     * call stack - bean definition resolution
     */
    private val resolutionStack = Stack<KClass<*>>()

    /**
     * Retrieve a bean instance
     */
    inline fun <reified T: Any> get(name: String? = null): T = resolveInstance(T::class, name)

    /**
     * Resolve a dependency for its bean definition
     */
    fun <T: Any> resolveInstance(type: KClass<*>, name: String?): T {
        logger.log("[Context] Resolve [${type.java.canonicalName}]")

        if (resolutionStack.contains(type)) {
            logger.err("[Context] Cyclic dependency detected while resolving $type")
            throw DependencyResolutionException("Cyclic dependency for $type")
        }

        if (!beanRegistry.isVisible(type, resolutionStack.toList())) {
            logger.err("[Context] Try to resolve $type but is not visible from classes context : $resolutionStack")
            throw DependencyResolutionException("Try to resolve $type but is not visible from classes context : $resolutionStack")
        }

        resolutionStack.add(type)

        val beanDefinition = name?.let {
            beanRegistry.searchByName(name)
        } ?: beanRegistry.searchAll(type)
        val instance: T = instanceFactory[beanDefinition]

        val head = resolutionStack.pop()
        if (head != type) {
            logger.err("[Context] Call stack error -- $resolutionStack")
            resolutionStack.clear()
            throw IllegalStateException("Calling HEAD was $head but should be $type")
        }
        return instance
    }

    /**
     * Check the all the loaded definitions - Try to resolve each definition
     */
    fun dryRun() {
        logger.log("(DRY RUN)")
        beanRegistry.definitions.keys.forEach { def ->
            Koin.logger.log("Testing $def ...")
            instanceFactory[def]
        }
    }

    /**
     * Drop all instances for given context
     * @param name
     */
    fun releaseContext(name: String) {
        logger.log("[Context] Release context : $name")

        val definitions: List<BeanDefinition<*>> = beanRegistry.getDefinitionsFromScope(name)
        instanceFactory.dropAllInstances(definitions)
    }

    /**
     * Retrieve a property by its key
     * can throw MissingPropertyException if the property is not found
     * @param key
     * @throws MissingPropertyException if key is not found
     */
    inline fun <reified T> getProperty(key: String): T = propertyResolver.getProperty(key)

    /**
     * Retrieve a property by its key or return provided default value
     * @param key - property key
     * @param defaultValue - default value if property is not found
     */
    inline fun <reified T> getProperty(key: String, defaultValue: T): T = propertyResolver.getProperty(key, defaultValue)

    /**
     * Set a property
     * @param key
     * @param value
     */
    fun setProperty(key: String, value: Any) = propertyResolver.add(key, value)

    /**
     * Delete properties from keys
     * @param keys
     */
    fun releaseProperties(vararg keys: String) {
        propertyResolver.deleteAll(keys)
    }

    /**
     * Close res
     */
    fun close() {
        logger.log("[Close] Closing Koin context")
        resolutionStack.clear()
        instanceFactory.clear()
        beanRegistry.clear()
        propertyResolver.clear()
    }
}