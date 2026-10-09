package com.example.ecommerce.service;

import com.example.ecommerce.model.Models.ProductPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductPageServiceTest {

    @Autowired ProductPageService service;

    @Test
    void parallelIsFasterThanSequential() {
        ProductPage seq = service.buildPageSequential("P-100");
        ProductPage par = service.buildPageParallel("P-100");

        assertThat(par.reviews()).isNotEmpty();
        assertThat(par.timeTakenMs()).isLessThan(seq.timeTakenMs());
    }
}
