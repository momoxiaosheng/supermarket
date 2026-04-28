package com.example.supermarket2;

import com.example.supermarket2.handler.CartWebSocketHandler;
import com.example.supermarket2.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@EnableScheduling
// 如果你想加开关，就把下面这行注解的注释去掉
// @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "dev")
public class MockDataAutoRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataAutoRunner.class);

    // 注入你需要的 Service 和 Handler
    @Autowired
    private CartService cartService;

    @Autowired
    private CartWebSocketHandler cartWebSocketHandler;

    // 用于生成随机数
    private final Random random = new Random();

    // 模拟用户ID（对应你配置文件里的 device.fixed.userId，默认是1）
    private static final Long MOCK_USER_ID = 1L;

    // 模拟几个商品ID（你数据库里如果有真实商品，改成真实的ID）
    private static final Long[] MOCK_PRODUCT_IDS = {1L, 2L, 6L};

    /**
     * 核心模拟逻辑：每隔 8 秒，自动往购物车里加一个商品
     */
    @Scheduled(fixedRate = 8000)
    public void simulateSmartShoppingCartDevice() {
        try {
            // 1. 随机选一个商品
//            Long randomProductId = MOCK_PRODUCT_IDS[random.nextInt(MOCK_PRODUCT_IDS.length)];
//
//            // 2. 随机生成数量 (1-3件)
//            int randomQuantity = random.nextInt(3) + 1;
//
//            log.info("【Mock智能购物车】模拟扫码: 用户={}, 商品ID={}, 数量={}", MOCK_USER_ID, randomProductId, randomQuantity);
//
//            // 3. 调用业务逻辑：加入购物车
//            // 注意：这里直接调用了你真实的 CartService，就像硬件通过 MQTT 传过来一样
//            cartService.addToCart(MOCK_USER_ID, randomProductId, randomQuantity);
//
//            // 4. 调用 WebSocket 推送：通知前端刷新
//            // 注意：虽然 CartService 内部可能已经调过推送了，但这里为了演示，我们手动再确保推一次
//            cartWebSocketHandler.sendCartUpdateToUser(MOCK_USER_ID);

// ==================== 红富士苹果模拟已禁用 ====================
//            // 模拟称重商品逻辑
//            Long randomProductId = 1L; // 假设这是一个称重商品的ID
//            double weightInGram = 500 + random.nextDouble() * 2000; // 随机生成 500g - 2500g
//
//            log.info("【Mock智能秤】模拟称重: 用户={}, 商品ID={}, 重量={}g", MOCK_USER_ID, randomProductId, String.format("%.1f", weightInGram));
//
//            // 调用另一个重载的 addToCart 方法 (Double weight)
//            cartService.addToCart(MOCK_USER_ID, randomProductId, weightInGram);
//
//            cartWebSocketHandler.sendCartUpdateToUser(MOCK_USER_ID);
//
//            log.info("【Mock智能购物车】推送完成");

        } catch (Exception e) {
            log.error("【Mock智能购物车】模拟失败", e);
        }
    }
}