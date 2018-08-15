package jp.nephy.glados.core.feature

import io.ktor.util.findAllSupertypes
import jp.nephy.glados.config
import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.feature.subscription.*
import net.dv8tion.jda.core.events.Event
import kotlin.system.measureTimeMillis

class FeatureManager(prefix: String) {
    private val logger = Logger("GLaDOS.FeatureManager", false)
    val commandClient = CommandSubscriptionClient()
    val listenerClient = ListenerSubscriptionClient()
    val poolClient = PoolSubscriptionClient()

    init {
        val loadingTimeMs = measureTimeMillis {
            ClassPath(prefix).classes<BotFeature>()
                    .forEach {
                        val instance = try {
                            it.newInstance()
                        } catch (e: Exception) {
                            logger.error(e) { "${it.canonicalName} のインスタンスの作成に失敗しました." }
                            return@forEach
                        }

                        load(instance)
                    }
            commandClient.onReady()
            listenerClient.onReady()
        }

        logger.info { "${loadingTimeMs}ms でFeatureのロードを完了しました." }
    }

    private fun load(instance: BotFeature) {
        val globalGuildKeys = instance.javaClass.getAnnotation(Feature::class.java)?.guilds.orEmpty()

        instance.javaClass.declaredMethods.filter { !it.isDefault }.sortedBy { it.name }.forEach {
            when {
                it.isAnnotationPresent(Command::class.java) -> {
                    if (it.parameterTypes.size != 1 || it.parameterTypes.first() != CommandEvent::class.java) {
                        logger.warn { "[${instance.javaClass.simpleName}#${it.name}] @Command が付与されていますが, 引数の型がCommandEventではありません. スキップします." }
                        return@forEach
                    }

                    val methodAnnotation = it.getAnnotation(Command::class.java)
                    val guilds = if (methodAnnotation.guilds.isNotEmpty()) {
                        methodAnnotation.guilds
                    } else {
                        globalGuildKeys
                    }.mapNotNull {
                        config.guilds[it]
                    }

                    commandClient.subscriptions.add(CommandSubscription(methodAnnotation, instance, it, guilds))
                    logger.info { "Command: ${instance.javaClass.simpleName}#${it.name} をロードしました." }
                }
                it.isAnnotationPresent(Listener::class.java) -> {
                    if (it.parameterTypes.size != 1 || Event::class.java !in it.parameters.first().type.findAllSupertypes()) {
                        logger.warn { "[${instance.javaClass.simpleName}#${it.name}] @Listener が付与されていますが, 引数の型がEventを継承していません. スキップします." }
                        return@forEach
                    }

                    val methodAnnotation = it.getAnnotation(Listener::class.java)
                    val guilds = if (methodAnnotation.guilds.isNotEmpty()) {
                        methodAnnotation.guilds
                    } else {
                        globalGuildKeys
                    }.mapNotNull {
                        config.guilds[it]
                    }

                    listenerClient.subscriptions.add(ListenerSubscription(methodAnnotation, instance, it, guilds))
                    logger.info { "Listener: ${instance.javaClass.simpleName}#${it.name} をロードしました." }
                }
                it.isAnnotationPresent(Pool::class.java) -> {
                    if (it.parameterTypes.isNotEmpty()) {
                        logger.warn { "[${instance.javaClass.simpleName}#${it.name}] @Pool が付与されていますが, 引数の数は0である必要があります. スキップします." }
                        return@forEach
                    }

                    val methodAnnotation = it.getAnnotation(Pool::class.java)

                    poolClient.subscriptions.add(PoolSubscription(methodAnnotation, instance, it))
                    logger.info { "Pool: ${instance.javaClass.simpleName}#${it.name} をロードしました." }
                }
                else -> {
                    if (it.name.startsWith("on") && it.parameterTypes.size == 1 && Event::class.java in it.parameters.first().type.findAllSupertypes()) {
                        logger.warn { "[${instance.javaClass.simpleName}#${it.name}] イベントリスナーの可能性がありますが, @Listener アノテーションが付与されていません." }
                    }
                    return@forEach
                }
            }
        }
    }
}
