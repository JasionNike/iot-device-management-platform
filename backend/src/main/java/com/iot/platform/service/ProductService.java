package com.iot.platform.service;

import com.iot.platform.entity.Product;
import com.iot.platform.entity.ProductProperty;
import com.iot.platform.entity.ProductEvent;
import com.iot.platform.entity.ProductCommand;

import java.util.List;

/**
 * 产品能力模型服务接口
 *
 * 管理产品及其能力模型定义，包括属性(Property)、事件(Event)、指令(Command)三要素。
 * 产品能力模型是设备接入的基础，决定了设备的数据结构和交互方式。
 *
 * 业务场景：
 * - 产品经理在平台上定义新产品的能力模型
 * - 设备注册时关联产品productKey，继承产品的能力模型定义
 * - 平台根据能力模型校验设备上报数据的合法性
 * - 告警规则可引用产品属性作为条件
 *
 * @author 王恒
 */
public interface ProductService {

    // ========== 产品CRUD ==========
    Product createProduct(Product product);
    Product updateProduct(Product product);
    void deleteProduct(String productKey);
    Product getProduct(String productKey);
    List<Product> listProducts();

    // ========== 属性管理 ==========
    ProductProperty addProperty(ProductProperty property);
    ProductProperty updateProperty(ProductProperty property);
    void deleteProperty(Long id);
    List<ProductProperty> listProperties(String productKey);

    // ========== 事件管理 ==========
    ProductEvent addEvent(ProductEvent event);
    ProductEvent updateEvent(ProductEvent event);
    void deleteEvent(Long id);
    List<ProductEvent> listEvents(String productKey);

    // ========== 指令管理 ==========
    ProductCommand addCommand(ProductCommand command);
    ProductCommand updateCommand(ProductCommand command);
    void deleteCommand(Long id);
    List<ProductCommand> listCommands(String productKey);
}
