package com.sparta.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successResponsesUseStringHttpStatusCode() {
        assertThat(ApiResponse.success("data").getCode()).isEqualTo("200");
        assertThat(ApiResponse.success("완료", "data").getCode()).isEqualTo("200");
        assertThat(ApiResponse.success().getCode()).isEqualTo("200");
    }
}
