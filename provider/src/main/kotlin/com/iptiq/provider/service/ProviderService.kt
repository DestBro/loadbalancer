package com.iptiq.provider.service

import org.springframework.stereotype.Service
import java.util.*

@Service
class ProviderService {

    private val providerUUID: UUID = UUID.randomUUID()

    fun get() = "provider_${System.getenv("port") ?: providerUUID}"
}