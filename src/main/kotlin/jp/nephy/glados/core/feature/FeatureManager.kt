package jp.nephy.glados.core.feature

import jp.nephy.glados.config
import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.feature.subscription.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.kotlinFunction
import kotlin.system.measureTimeMillis

class FeatureManager(private val prefix: String) {
    private val logger = Logger("GLaDOS.FeatureManager", false)
    val commandClient = CommandSubscriptionClient()
    val listenerEventClient = ListenerEventSubscriptionClient()
    val loopClient = LoopSubscriptionClient()
    private val audioEventSubscriptions = arrayListOf<EventSubscription>()
    private val connectionListenerSubscriptions = arrayListOf<EventSubscription>()

    fun loadAll() {
        val loadingTimeMs = measureTimeMillis {
            for (it in ClassPath(prefix).classes<BotFeature>()) {
                val instance = try {
                    it.newInstance()
                } catch (e: Exception) {
                    logger.error(e) { "${it.canonicalName} のインスタンスの作成に失敗しました。" }
                    continue
                }

                loadClass(instance)
            }

            commandClient.onReady()
            listenerEventClient.onReady()
        }

        logger.info { "${loadingTimeMs}ms で Feature のロードを完了しました。" }
    }

    fun bindTo(client: AudioEventSubscriptionClient) = client.apply {
        subscriptions.addAll(audioEventSubscriptions.filter { it.matches(client.guildPlayer.guild) })
        onReady()
    }

    fun bindTo(client: ConnectionListenerSubscriptionClient) = client.apply {
        subscriptions.addAll(connectionListenerSubscriptions.filter { it.matches(client.guild) })
        onReady()
    }

    private val listenerAdapterMethods = DiscordEventModel::class.java.declaredMethods.filter { it.isAnnotationPresent(FromListenerAdapter::class.java) }.mapNotNull { it.kotlinFunction }
    private val connectionListenerMethods = DiscordEventModel::class.java.declaredMethods.filter { it.isAnnotationPresent(FromConnectionListener::class.java) }.mapNotNull { it.kotlinFunction }
    private val audioEventAdapterMethods = DiscordEventModel::class.java.declaredMethods.filter { it.isAnnotationPresent(FromAudioEventAdapter::class.java) }.mapNotNull { it.kotlinFunction }

    @Suppress("UNUSED")
    private fun loadClass(instance: BotFeature) {
        val globalGuildKeys = instance::class.findAnnotation<Feature>()?.guilds.orEmpty()

        fun KFunction<*>.loadCommand(annotation: Command): Boolean {
            return if (valueParameters.size == 1 && valueParameters.first().type == CommandEvent::class.createType()) {
                val guilds = annotation.guilds.ifEmpty {
                    globalGuildKeys
                }.mapNotNull {
                    config.guilds[it]
                }

                commandClient.subscriptions += CommandSubscription(annotation, instance, this, guilds)
                logger.info { "Command: ${instance.javaClass.simpleName}#$name をロードしました。" }
                true
            } else {
                logger.warn { "[${instance.javaClass.simpleName}#$name] @Command が付与されていますが, 引数が [CommandEvent] ではありません。スキップします。" }
                false
            }
        }

        fun KFunction<*>.loadLoop(annotation: Loop): Boolean {
            return if (valueParameters.isEmpty()) {
                loopClient.subscriptions += LoopSubscription(annotation, instance, this)
                logger.info { "Loop: ${instance.javaClass.simpleName}#$name をロードしました。" }
                true
            } else {
                logger.warn { "[${instance.javaClass.simpleName}#$name] @Loop が付与されていますが, 引数の数は 0 である必要があります。スキップします。" }
                false
            }
        }

        fun KFunction<*>.loadEventListener(annotation: Event): Boolean {
            val guilds = annotation.guilds.ifEmpty {
                globalGuildKeys
            }.mapNotNull {
                config.guilds[it]
            }

            return when {
                this match listenerAdapterMethods -> {
                    listenerEventClient.subscriptions += EventSubscription(annotation, instance, this, guilds)
                    logger.info { "JDAEvent: ${instance.javaClass.simpleName}#$name をロードしました。" }
                    true
                }
                this match connectionListenerMethods -> {
                    connectionListenerSubscriptions += EventSubscription(annotation, instance, this, guilds)
                    logger.info { "ConnectionListener: ${instance.javaClass.simpleName}#$name をロードしました。" }
                    true
                }
                this match audioEventAdapterMethods -> {
                    audioEventSubscriptions += EventSubscription(annotation, instance, this, guilds)
                    logger.info { "AudioEvent: ${instance.javaClass.simpleName}#$name をロードしました。" }
                    true
                }
                else -> {
                    logger.warn { "[${instance.javaClass.simpleName}#$name] イベントリスナーですが, @Event が付与されていません。" }
                    false
                }
            }
        }

        for (function in instance.javaClass.declaredMethods.filter { !it.isDefault }.mapNotNull { it.kotlinFunction }.sortedBy { it.name }) {
            val annotation = function.findAnnotation<Command>() ?: function.findAnnotation<Loop>() ?: function.findAnnotation<Event>()

            if (annotation is Command && function.loadCommand(annotation)) {
                continue
            } else if (annotation is Loop && function.loadLoop(annotation)) {
                continue
            } else if (annotation is Event && function.loadEventListener(annotation)) {
                continue
            }
        }
    }

    private infix fun KFunction<*>.match(originalFunctions: List<KFunction<*>>): Boolean {
        originalFunctions.filter { it.valueParameters.size == valueParameters.size }.find { originalFunction ->
            originalFunction.valueParameters.zip(valueParameters).all {
                it.first.type == it.second.type
            }
        } ?: return false

        return true
    }
}
