package jp.nephy.glados.core

import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass

class ClassContainer<T: Any>(vararg classes: KClass<out T>): CopyOnWriteArraySet<KClass<out T>>(classes.toList())
