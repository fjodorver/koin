package org.koin.core.instance

import org.koin.Koin
import org.koin.core.bean.BeanDefinition
import org.koin.core.bean.BeanRegistry
import org.koin.error.BeanDefinitionException
import org.koin.error.BeanInstanceCreationException
import java.util.concurrent.ConcurrentHashMap

private fun <T> BeanDefinition<T>.createInstance() = try {
    definition()
} catch (e: Throwable) {
    Koin.logger.err("[Instance] Error can't create [$this] due to error : \n${e.stackTrace.take(10).joinToString(separator = "\n")}")
    throw BeanInstanceCreationException("Can't create bean $this due to error : $e")
}

/**
 * Instance factory - handle objects creation against BeanRegistry
 * @author - Arnaud GIULIANI
 */
class InstanceFactory(val beanRegistry: BeanRegistry) {

    val instances = ConcurrentHashMap<BeanDefinition<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(beanDefinition: BeanDefinition<*>): T {
        Koin.logger.log("[Instance] create [$beanDefinition]")
        if (beanDefinition !in beanRegistry) {
            throw BeanDefinitionException("Can't create bean $beanDefinition. Scope has not been declared")
        }
        val instance = if (beanDefinition.isSingleton) {
            instances.getOrPut(beanDefinition, beanDefinition::createInstance)
        } else {
            beanDefinition.definition()
        }
        return instance as T
    }

    /**
     * Drop all instances for definitions
     */
    fun dropAllInstances(definitions: List<BeanDefinition<*>>) {
        definitions.forEach { instances.remove(it) }
    }

    /**
     * Clear all resources
     */
    fun clear() {
        instances.clear()
    }
}