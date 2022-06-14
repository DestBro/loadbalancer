package com.iptiq.loadbalancer.controller

import com.iptiq.loadbalancer.service.LoadbalancerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@CrossOrigin
class LoadbalancerController(
    @Autowired private val loadbalancerService: LoadbalancerService
) {
    @GetMapping("/identifier")
    @ResponseBody
    fun get() = loadbalancerService.getProviderIdentifier()

    @GetMapping("/health-check")
    @ResponseBody
    fun healthCheck() {
        loadbalancerService.check()
    }
}