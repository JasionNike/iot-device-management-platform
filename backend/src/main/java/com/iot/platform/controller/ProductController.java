package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.entity.*;
import com.iot.platform.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 产品能力模型控制器
 *
 * 提供产品及其能力模型（属性/事件/指令）的完整REST API。
 * 能力模型定义了设备的数据规范，是实现多厂商设备统一接入的基础。
 *
 * REST API设计说明：
 * - POST/PUT/DELETE/GET 对应CRUD操作
 * - /api/product 管理产品本身
 * - /api/product/{productKey}/properties 管理产品属性
 * - /api/product/{productKey}/events 管理产品事件
 * - /api/product/{productKey}/commands 管理产品指令
 *
 * @author 王恒
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ========== 产品CRUD ==========

    @PostMapping
    public Result<Product> createProduct(@RequestBody Product product) {
        return Result.success(productService.createProduct(product));
    }

    @PutMapping
    public Result<Product> updateProduct(@RequestBody Product product) {
        return Result.success(productService.updateProduct(product));
    }

    @DeleteMapping("/{productKey}")
    public Result<Void> deleteProduct(@PathVariable String productKey) {
        productService.deleteProduct(productKey);
        return Result.success();
    }

    @GetMapping("/{productKey}")
    public Result<Product> getProduct(@PathVariable String productKey) {
        return Result.success(productService.getProduct(productKey));
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> listProducts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(productService.listProducts(pageNum, pageSize));
    }

    // ========== 属性管理 ==========

    @PostMapping("/property")
    public Result<ProductProperty> addProperty(@RequestBody ProductProperty property) {
        return Result.success(productService.addProperty(property));
    }

    @PutMapping("/property")
    public Result<ProductProperty> updateProperty(@RequestBody ProductProperty property) {
        return Result.success(productService.updateProperty(property));
    }

    @DeleteMapping("/property/{id}")
    public Result<Void> deleteProperty(@PathVariable Long id) {
        productService.deleteProperty(id);
        return Result.success();
    }

    @GetMapping("/{productKey}/properties")
    public Result<List<ProductProperty>> listProperties(@PathVariable String productKey) {
        return Result.success(productService.listProperties(productKey));
    }

    // ========== 事件管理 ==========

    @PostMapping("/event")
    public Result<ProductEvent> addEvent(@RequestBody ProductEvent event) {
        return Result.success(productService.addEvent(event));
    }

    @PutMapping("/event")
    public Result<ProductEvent> updateEvent(@RequestBody ProductEvent event) {
        return Result.success(productService.updateEvent(event));
    }

    @DeleteMapping("/event/{id}")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        productService.deleteEvent(id);
        return Result.success();
    }

    @GetMapping("/{productKey}/events")
    public Result<List<ProductEvent>> listEvents(@PathVariable String productKey) {
        return Result.success(productService.listEvents(productKey));
    }

    // ========== 指令管理 ==========

    @PostMapping("/command")
    public Result<ProductCommand> addCommand(@RequestBody ProductCommand command) {
        return Result.success(productService.addCommand(command));
    }

    @PutMapping("/command")
    public Result<ProductCommand> updateCommand(@RequestBody ProductCommand command) {
        return Result.success(productService.updateCommand(command));
    }

    @DeleteMapping("/command/{id}")
    public Result<Void> deleteCommand(@PathVariable Long id) {
        productService.deleteCommand(id);
        return Result.success();
    }

    @GetMapping("/{productKey}/commands")
    public Result<List<ProductCommand>> listCommands(@PathVariable String productKey) {
        return Result.success(productService.listCommands(productKey));
    }
}
