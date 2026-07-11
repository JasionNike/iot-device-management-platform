package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.platform.common.BusinessException;
import com.iot.platform.entity.*;
import com.iot.platform.mapper.*;
import com.iot.platform.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 产品能力模型服务实现
 *
 * 产品是设备的抽象模板，定义了设备的属性(Property)、事件(Event)、指令(Command)三要素。
 * 设备注册时关联产品productKey，从而继承产品的能力模型定义。
 *
 * 核心设计：
 * 1. 产品 + 能力模型三要素组成完整的设备数据规范
 * 2. 属性定义设备可上报的数据点（如温度、湿度）
 * 3. 事件定义设备可上报的告警/通知（如超温告警）
 * 4. 指令定义平台可下发的控制操作（如设置阈值）
 * 5. 删除产品时级联删除其能力模型
 *
 * @author 王恒
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductPropertyMapper productPropertyMapper;
    private final ProductEventMapper productEventMapper;
    private final ProductCommandMapper productCommandMapper;

    /**
     * 创建产品
     *
     * 产品是能力模型的容器。创建产品后，可以在产品下定义属性、事件和指令。
     * productKey是产品的唯一标识，建议命名规范：<品类>-<型号>-<序号>，
     * 如：SENSOR-TH-001（温湿度传感器）、ENERGY-MTR-001（能源采集器）。
     *
     * @param product 产品信息
     * @return 创建后的产品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product createProduct(Product product) {
        // 校验productKey唯一性
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();
        query.eq(Product::getProductKey, product.getProductKey());
        if (productMapper.selectCount(query) > 0) {
            throw new BusinessException("产品标识已存在: " + product.getProductKey());
        }
        // 如果未指定productKey，自动生成
        if (product.getProductKey() == null || product.getProductKey().isEmpty()) {
            product.setProductKey("PROD-" + UUID.randomUUID().toString().substring(0, 8));
        }
        product.setCreateTime(LocalDateTime.now());
        productMapper.insert(product);
        log.info("产品创建成功：productKey={}, productName={}, manufacturer={}",
                product.getProductKey(), product.getProductName(), product.getManufacturer());
        return product;
    }

    /**
     * 更新产品信息
     *
     * @param product 待更新的产品信息（根据productKey定位）
     * @return 更新后的产品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product updateProduct(Product product) {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();
        query.eq(Product::getProductKey, product.getProductKey());
        Product existing = productMapper.selectOne(query);
        if (existing == null) {
            throw new BusinessException("产品不存在: " + product.getProductKey());
        }
        product.setId(existing.getId());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        return productMapper.selectOne(query);
    }

    /**
     * 删除产品（级联删除能力模型）
     *
     * 删除产品时会同时删除该产品下的所有属性、事件和指令定义。
     * 但不会删除已注册的设备，设备仅失去能力模型关联。
     *
     * @param productKey 产品标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(String productKey) {
        // 级联删除能力模型（属性/事件/指令）
        productPropertyMapper.delete(new LambdaQueryWrapper<ProductProperty>()
                .eq(ProductProperty::getProductKey, productKey));
        productEventMapper.delete(new LambdaQueryWrapper<ProductEvent>()
                .eq(ProductEvent::getProductKey, productKey));
        productCommandMapper.delete(new LambdaQueryWrapper<ProductCommand>()
                .eq(ProductCommand::getProductKey, productKey));
        productMapper.delete(new LambdaQueryWrapper<Product>()
                .eq(Product::getProductKey, productKey));
        log.info("产品已删除（含能力模型）：productKey={}", productKey);
    }

    /**
     * 根据productKey查询产品
     *
     * @param productKey 产品标识
     * @return 产品信息
     */
    @Override
    public Product getProduct(String productKey) {
        return productMapper.selectOne(
                new LambdaQueryWrapper<Product>().eq(Product::getProductKey, productKey));
    }

    /**
     * 查询所有产品列表
     *
     * @return 产品列表
     */
    @Override
    public List<Product> listProducts() {
        return productMapper.selectList(null);
    }

    // ========== 属性管理 ==========

    /**
     * 添加产品属性定义
     *
     * 属性定义了产品的数据采集点，如温度传感器的temperature属性。
     * 属性定义包含数据类型、单位、取值范围等约束信息，
     * 平台根据属性定义来校验设备上报的数据合法性。
     *
     * @param property 属性定义
     * @return 创建的属性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductProperty addProperty(ProductProperty property) {
        property.setCreateTime(LocalDateTime.now());
        productPropertyMapper.insert(property);
        log.info("产品属性已添加：productKey={}, propertyName={}", property.getProductKey(), property.getPropertyName());
        return property;
    }

    /**
     * 更新属性定义
     *
     * @param property 更新的属性信息
     * @return 更新后的属性
     */
    @Override
    public ProductProperty updateProperty(ProductProperty property) {
        property.setUpdateTime(LocalDateTime.now());
        productPropertyMapper.updateById(property);
        return productPropertyMapper.selectById(property.getId());
    }

    /**
     * 删除属性定义
     *
     * @param id 属性ID
     */
    @Override
    public void deleteProperty(Long id) {
        productPropertyMapper.deleteById(id);
    }

    /**
     * 查询产品的所有属性定义
     *
     * @param productKey 产品标识
     * @return 属性列表（按sortOrder排序）
     */
    @Override
    public List<ProductProperty> listProperties(String productKey) {
        return productPropertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getProductKey, productKey)
                        .orderByAsc(ProductProperty::getSortOrder));
    }

    // ========== 事件管理 ==========

    /**
     * 添加事件定义
     *
     * 事件定义设备可上报的告警或通知类型。
     * 例如：温度传感器可定义"超温告警"事件，
     * 事件包含输出参数，可在告警规则中作为匹配条件。
     *
     * @param event 事件定义
     * @return 创建的事件
     */
    @Override
    public ProductEvent addEvent(ProductEvent event) {
        event.setCreateTime(LocalDateTime.now());
        productEventMapper.insert(event);
        return event;
    }

    @Override
    public ProductEvent updateEvent(ProductEvent event) {
        event.setUpdateTime(LocalDateTime.now());
        productEventMapper.updateById(event);
        return productEventMapper.selectById(event.getId());
    }

    @Override
    public void deleteEvent(Long id) {
        productEventMapper.deleteById(id);
    }

    @Override
    public List<ProductEvent> listEvents(String productKey) {
        return productEventMapper.selectList(
                new LambdaQueryWrapper<ProductEvent>()
                        .eq(ProductEvent::getProductKey, productKey));
    }

    // ========== 指令管理 ==========

    /**
     * 添加指令定义
     *
     * 指令定义平台可向设备下发的远程控制操作。
     * 例如：温度传感器产品的setThreshold指令，金融终端的restart指令。
     * 指令包含输入参数定义，平台根据定义校验下发参数的合法性。
     *
     * @param command 指令定义
     * @return 创建的指令
     */
    @Override
    public ProductCommand addCommand(ProductCommand command) {
        command.setCreateTime(LocalDateTime.now());
        productCommandMapper.insert(command);
        return command;
    }

    @Override
    public ProductCommand updateCommand(ProductCommand command) {
        command.setUpdateTime(LocalDateTime.now());
        productCommandMapper.updateById(command);
        return productCommandMapper.selectById(command.getId());
    }

    @Override
    public void deleteCommand(Long id) {
        productCommandMapper.deleteById(id);
    }

    @Override
    public List<ProductCommand> listCommands(String productKey) {
        return productCommandMapper.selectList(
                new LambdaQueryWrapper<ProductCommand>()
                        .eq(ProductCommand::getProductKey, productKey));
    }
}
