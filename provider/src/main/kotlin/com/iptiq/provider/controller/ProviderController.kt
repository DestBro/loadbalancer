package com.iptiq.provider.controller

import com.iptiq.provider.service.ProviderService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@CrossOrigin
class ProviderController @Autowired constructor(
    private val providerService: ProviderService
) {

    @GetMapping("/identifier")
    @ResponseBody
    fun getProviderIdentifier() = providerService.get()
}