package com.iptiq.loadbalancer.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.net.ConnectException
import java.net.Socket
import java.net.URL
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.concurrent.thread

@Service
class LoadbalancerService(
    @Value("\${invocation.algorithm}") private val invocationMethod: String,
    @Value("\${provider.ports}") private val providerPorts: Array<Int>,
    @Value("\${instance.per.provider}") private val instancePerProvider: Int,
    @Value("\${number.of.providers}") private val numberOfProviders: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private var providerLiveQueue: BlockingQueue<Int> = LinkedBlockingQueue(numberOfProviders)
    private var healthCheckMap = HashMap<Int, Int>()
    private val providerUrl = "http://provider-"  //"http://localhost:"

    fun getProviderIdentifier(): String {
        var providerPort: Int = providerLiveQueue.poll(100, TimeUnit.MILLISECONDS)
            ?: throw HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Server is full")

        var counter = 1
        while (healthCheckMap.containsKey(providerPort)) {
            providerLiveQueue.put(providerPort)
            if (counter == providerLiveQueue.size) {
                throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Server is full")
            }
            providerPort = providerLiveQueue.poll(100, TimeUnit.MILLISECONDS)!!
            counter++
        }
        val result = getProviderIdentifier(providerPort)

        //Simulate server delay/ timeout error
        Thread.sleep(1000)
        //Rotation
        providerLiveQueue.put(providerPort)
        return result
    }

    fun check() {
        providerPorts.forEach { port: Int ->
            try {
                Socket(URL("$providerUrl$port").host, port).use {
                    if (healthCheckMap.size > 0 && healthCheckMap.containsKey(port)) {
                        healthCheckMap[port] = healthCheckMap[port]!! + 1
                        if (healthCheckMap[port] == 2) {
                            log.info("Registered after 2 success health check : $port")
                            healthCheckMap.remove(port)
                        }
                    }
                }
            } catch (e: ConnectException) {
                log.error("Failed to connect to: $providerUrl$port")
                //if not alive – it should exclude the provider node from load balancing
                healthCheckMap[port] = 0
            } catch (ex: Exception) {
                log.error("Exception: ${ex.message}")
                ex.printStackTrace()
            }
        }
    }

    @PostConstruct
    fun scheduleHealthChecker() {
        //The load balancer should invoke every X seconds each of its registered providers
        val delay: Long = 10000
        thread(start = true) {
            while (true) {
                //a special method to check if providers are alive
                check()
                try {
                    Thread.sleep(delay)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @PostConstruct
    fun initProviderQueue() {
        providerLiveQueue = LinkedBlockingQueue(
            buildList {
                repeat(instancePerProvider) {
                    addAll(providerPorts)
                }
                if (invocationMethod == "Random") {
                    shuffle()
                }
            }
        )
    }

    fun getProviderIdentifier(port: Int): String {
        val restTemplate = RestTemplate()
        return restTemplate.getForObject("$providerUrl$port:$port/identifier", String::class.java)
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Error reaching provider $port")
    }
}