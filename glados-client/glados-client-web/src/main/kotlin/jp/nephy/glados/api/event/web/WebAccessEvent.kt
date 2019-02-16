package jp.nephy.glados.api.event.web

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.event.Event

data class WebAccessEvent(
    override val glados: GLaDOS,
    val context: PipelineContext<*, ApplicationCall>,
    val matchResult: MatchResult?,
    val fragments: Map<String, String>
): Event

val WebAccessEvent.call: ApplicationCall
    get() = context.call
