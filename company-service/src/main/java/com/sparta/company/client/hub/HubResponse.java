package com.sparta.company.client.hub;

import java.util.UUID;

public record HubResponse(
        UUID hubId,
        String name,
        String address
) {
}
