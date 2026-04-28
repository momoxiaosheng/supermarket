//package com.example.supermarket2.service;
//
//import com.example.supermarket2.utils.ValidationUtil;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class DeviceDataServiceTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(DeviceDataServiceTest.class);
//
//    @Mock
//    private CartService cartService;
//
//    @InjectMocks
//    private DeviceDataService deviceDataService;
//
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    public void setUp() {
//        objectMapper = new ObjectMapper();
//    }
//
//    @Test
//    public void testProcessShoppingCartDataWithQuantity() throws Exception {
//        // 准备测试数据 - 确保包含所有必要字段
//        String jsonPayload = "{\"user_id\": \"123\", \"cart_id\": \"cart_05\", \"product_id\": 1001, \"quantity\": 2}";
//        JsonNode data = objectMapper.readTree(jsonPayload);
//
//        // 执行测试
//        deviceDataService.processDeviceData("test_device", "/test/topic", data);
//
//        // 验证
//        verify(cartService, times(1)).addToCart(eq(123L), eq(1001L), eq(2));
//    }
//
//    @Test
//    public void testProcessShoppingCartDataWithWeight() throws Exception {
//        // 准备测试数据 - 确保包含所有必要字段
//        String jsonPayload = "{\"user_id\": \"456\", \"cart_id\": \"cart_06\", \"product_id\": 2005, \"weight\": 0.5}";
//        JsonNode data = objectMapper.readTree(jsonPayload);
//
//        // 执行测试
//        deviceDataService.processDeviceData("test_device", "/test/topic", data);
//
//        // 验证 - 0.5kg 应该转换为 500g
//        verify(cartService, times(1)).addToCart(eq(456L), eq(2005L), eq(500));
//    }
//
//    @Test
//    public void testInvalidUserId() throws Exception {
//        // 准备测试数据 - 用户ID不是数字
//        String jsonPayload = "{\"user_id\": \"invalid\", \"product_id\": 1001, \"quantity\": 1}";
//        JsonNode data = objectMapper.readTree(jsonPayload);
//
//        // 执行测试
//        deviceDataService.processDeviceData("test_device", "/test/topic", data);
//
//        // 验证 - 由于用户ID无效，不应该调用购物车服务
//        verify(cartService, never()).addToCart(anyLong(), anyLong(), anyInt());
//    }
//
//    @Test
//    public void testMissingRequiredFields() throws Exception {
//        // 准备测试数据 - 缺少必要的字段
//        String jsonPayload = "{\"user_id\": \"123\"}";
//        JsonNode data = objectMapper.readTree(jsonPayload);
//
//        // 执行测试
//        deviceDataService.processDeviceData("test_device", "/test/topic", data);
//
//        // 验证 - 由于缺少必要字段，不应该调用购物车服务
//        verify(cartService, never()).addToCart(anyLong(), anyLong(), anyInt());
//    }
//}