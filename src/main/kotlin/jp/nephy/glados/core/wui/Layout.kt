package jp.nephy.glados.core.wui

import io.ktor.html.Placeholder
import io.ktor.html.Template
import io.ktor.html.insert
import kotlinx.html.*

class NavLayout: Template<HTML> {
    val navContent = Placeholder<HtmlBlockTag>()
    override fun HTML.apply() {
        insert(FooterLayout()) {
            footerContent {
                div("navbar navbar-expand-lg navbar-dark bg-primary") {
                    a("/", "", "navbar-brand") { +"GLaDOS" }
                }
                style {
                    unsafe {
                        +".alert { padding-top: 16px; }"
                    }
                }
                div("container") {
                    insert(navContent)
                }
            }
        }
    }
}

class FooterLayout: Template<HTML> {
    val footerContent = Placeholder<HtmlBlockTag>()
    override fun HTML.apply() {
        insert(MainLayout()) {
            content {
                insert(footerContent)
                div("container") {
                    hr()
                    div("text-center") {
                        style {
                            unsafe {
                                +".opacity05 { opacity: 0.5; }"
                            }
                        }
                        p {
                            +"GLaDOS-bot brought to you by "
                            a("https://github.com/SlashNephy") {
                                +"@SlashNephy"
                            }
                        }
                    }
                }
            }
        }
    }
}

class MainLayout: Template<HTML> {
    val content = Placeholder<HtmlBlockTag>()
    override fun HTML.apply() {
        head {
            meta(charset = "utf-8")
            meta("viewport", "width=device-width,initial-scale=1.0,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no")
            title { +"GLaDOS WebUI" }
            styleLink("https://cdnjs.cloudflare.com/ajax/libs/bootswatch/4.1.3/cosmo/bootstrap.min.css")
            styleLink("https://use.fontawesome.com/releases/v5.2.0/css/all.css")
        }
        body {
            insert(content)
        }
    }
}
