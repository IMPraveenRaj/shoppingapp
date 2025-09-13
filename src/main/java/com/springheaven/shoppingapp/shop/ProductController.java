package com.springheaven.shoppingapp.shop;



import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin // allow local FE to call this API
public class ProductController {

    private static final List<Product> PRODUCTS = List.of(
            new Product("sku-1", "Coffee", 3.50),
            new Product("sku-2", "Tea", 2.90),
            new Product("sku-3", "Cookie", 1.20)
    );

    @GetMapping("/products")
    public List<Product> products() {
        return PRODUCTS;
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable("id") String id) {
        return PRODUCTS.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }

    // ✅ POST endpoint to add a new product
    @PostMapping("/products")
    public Product addProduct(@RequestBody Product product) {
        PRODUCTS.add(product);
        return product; // return what was added
    }
}
