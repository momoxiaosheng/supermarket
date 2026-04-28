//package com.example.supermarket2;
//
//import com.example.supermarket2.component.MqttMessageListener;
//import com.example.supermarket2.entity.Cart;
//import com.example.supermarket2.mapper.CartMapper;
//import com.example.supermarket2.service.CartService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//public class MqttIntegrationTest {
//
//    @Autowired
//    private MqttMessageListener mqttMessageListener;
//
//    @Autowired
//    private CartService cartService;
//
//    @Autowired
//    private CartMapper cartMapper;
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Test
//    public void testCompleteMqttToCartFlow() throws Exception {
//        // 准备测试数据
//        String topic = "/k2069zW7Drp/gwc_wxx/user/get";
//        String payload = "{\"user_id\": \"4\", \"cart_id\": \"test_cart\", \"product_id\": 3, \"quantity\": 3}";
//        MqttMessage message = new MqttMessage(payload.getBytes());
//
//        // 清空可能存在的测试数据
//        cartMapper.delete(null); // 删除所有购物车记录
//        redisTemplate.delete("cart:user:3");
//
//        // 执行测试 - 模拟MQTT消息到达
//        mqttMessageListener.messageArrived(topic, message);
//
//        // 等待异步处理完成
//        Thread.sleep(1000);
//
//        // 验证Redis中是否有数据
//        Long redisSize = redisTemplate.opsForHash().size("cart:user:3");
//        assertTrue(redisSize > 0, "Redis中应该有购物车数据");
//
//        // 验证数据库中是否有数据
//        List<Cart> cartItems = cartMapper.selectByUserId(3L);
//        assertFalse(cartItems.isEmpty(), "数据库中应该有购物车记录");
//        assertEquals(2L, cartItems.get(0).getProductId());
//        assertEquals(3, cartItems.get(0).getQuantity());
//    }
//
//    @Test
//    public void testWeightConversion() throws Exception {
//        // 准备测试数据 - 重量商品
//        String topic = "/k2069zW7Drp/gwc_wxx/user/get";
//        String payload = "{\"user_id\": \"1\", \"cart_id\": \"test_cart\", \"product_id\": 4, \"weight\": 1.5}";
//        MqttMessage message = new MqttMessage(payload.getBytes());
//
//        // 清空可能存在的测试数据
//        cartMapper.delete(null);
//        redisTemplate.delete("cart:user:1");
//
//        // 执行测试
//        mqttMessageListener.messageArrived(topic, message);
//
//        // 等待异步处理完成
//        Thread.sleep(1000);
//
//        // 验证 - 1.5kg 应该转换为 1500g
//        List<Cart> cartItems = cartMapper.selectByUserId(1L);
//        assertEquals(1500, cartItems.get(0).getQuantity());
//    }
//}