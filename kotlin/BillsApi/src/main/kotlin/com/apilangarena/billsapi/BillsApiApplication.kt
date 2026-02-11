package com.apilangarena.billsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BillsApiApplication

fun main(args: Array<String>) {
    runApplication<BillsApiApplication>(*args)
}
