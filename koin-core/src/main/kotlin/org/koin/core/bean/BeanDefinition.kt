package org.koin.core.bean

import kotlin.reflect.KClass

/**
 * Bean definition
 * @author - Arnaud GIULIANI
 *
 * Gather type of T
 * defined by lazy/function
 * has a type (clazz)
 * has a BeanType : default singleton
 * has a name, if specified
 *
 * @param name - bean name
 * @param clazz - bean class
 * @param isSingleton - is the bean a singleton
 * @param bindTypes - list of assignable types
 * @param definition - bean definition function
 */
data class BeanDefinition<out T>(
        val name: String = "",
        val clazz: KClass<*>,
        val isSingleton: Boolean = true,
        var bindTypes: List<KClass<*>> = arrayListOf(),
        val definition: () -> T
)

/**
 * Add a compatible type to current bounded definition
 */
infix fun <T> BeanDefinition<T>.bind(clazz: KClass<*>): BeanDefinition<*> {
    bindTypes += clazz
    return this
}