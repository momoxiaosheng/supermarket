package com.example.supermarket2.dto.location;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 定位请求DTO
 * 封装定位请求参数
 */
@Data
public class BeaconRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;                   // 用户ID

    @NotBlank(message = "地图ID不能为空")
    private String mapId;                    // 地图ID
    private String sessionId;                // 会话ID（可选）

    @NotNull(message = "信标数据不能为空")
    @NotEmpty(message = "信标数据不能为空")
    @Valid // 级联校验BeaconData内部字段
    private List<BeaconData> beacons;        // 信标数据列表

    private Long timestamp;                  // 客户端时间戳
    private String deviceId;                 // 设备ID
    private Double lastKnownX;               // 上次已知位置X
    private Double lastKnownY;               // 上次已知位置Y

    /**
     * 验证请求有效性
     */
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty() &&
                mapId != null && !mapId.trim().isEmpty() &&
                beacons != null && !beacons.isEmpty();
    }

    /**
     * 获取有效信标数量
     */
    public int getValidBeaconCount() {
        if (beacons == null) return 0;
        return (int) beacons.stream()
                .filter(BeaconData::isValidBeacon)
                .count();
    }
}

