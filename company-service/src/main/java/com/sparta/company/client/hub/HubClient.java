package com.sparta.company.client.hub;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hub-service")
public interface HubClient {

    @GetMapping("/api/hubs/{hubId}")
    HubResponse getHub(@PathVariable("hubId") String hubId);
}
