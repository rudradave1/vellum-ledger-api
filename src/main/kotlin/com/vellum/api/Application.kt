package com.vellum.api

import com.vellum.api.data.DatabaseFactory
import com.vellum.api.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init()
    configureHTTP()
    configureSerialization()
    configureAuth()
    configureRouting()
}
