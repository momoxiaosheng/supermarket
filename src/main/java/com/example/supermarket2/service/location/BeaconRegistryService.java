package com.example.supermarket2.service.location;

import com.example.supermarket2.dto.location.BeaconData;
import com.example.supermarket2.entity.location.BeaconLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 信标注册服务（动态信标加载）
 * <p>
 * 负责管理全场信标位置列表。支持：
 * <ol>
 *   <li>硬编码默认信标列表作为兜底（fallback）</li>
 *   <li>运行时通过 {@link #registerBeacon} 动态注册或覆盖信标（
 *       实际项目可改为从数据库/配置中心加载）</li>
 *   <li>按信号质量（RSSI）选取参与定位的最优 Top-N 信标</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
public class BeaconRegistryService {

    /**
     * 默认信标列表（四角分布，作为没有外部数据时的兜底）。
     * 在实际超市场景中，可通过 {@link #loadBeaconsFromExternalSource} 替换为完整列表。
     */
    private static final List<BeaconLocation> DEFAULT_BEACONS = Arrays.asList(
            new BeaconLocation("01122334-4556-6778-899A-ABBCCDDEEFF1", 1.0, 1.0),
            new BeaconLocation("01122334-4556-6778-899A-ABBCCDDEEFF2", 28.0, 1.0),
            new BeaconLocation("01122334-4556-6778-899A-ABBCCDDEEFF3", 28.0, 48.0),
            new BeaconLocation("01122334-4556-6778-899A-ABBCCDDEEFF4", 1.0, 48.0)
    );

    /**
     * 运行时信标注册表（UUID → BeaconLocation）。
     * 初始化时用默认列表填充；可通过 API/DB 动态追加/替换。
     */
    private final Map<String, BeaconLocation> registry = new ConcurrentHashMap<>();

    public BeaconRegistryService() {
        // 将默认列表写入注册表
        for (BeaconLocation bl : DEFAULT_BEACONS) {
            registry.put(bl.getNormalizedUuid(), bl);
        }
        log.info("BeaconRegistryService 初始化，默认信标数量={}", registry.size());
    }

    /**
     * 动态注册（或更新）单个信标。
     * 实际项目中可在启动时从数据库批量调用此方法。
     *
     * @param beacon 信标位置信息
     */
    public void registerBeacon(BeaconLocation beacon) {
        if (beacon == null || beacon.getNormalizedUuid() == null) {
            return;
        }
        registry.put(beacon.getNormalizedUuid(), beacon);
        log.debug("注册信标: uuid={}, ({}, {})",
                beacon.getUuid(), beacon.getX(), beacon.getY());
    }

    /**
     * 占位加载方法：从外部数据源（数据库/配置中心）批量加载信标。
     * 当前为 stub，不做任何实际加载，已有默认列表保持不变。
     * <p>
     * 接入真实数据源时，在此处替换实现（如查询 beacon_location 表）。
     * </p>
     */
    public void loadBeaconsFromExternalSource() {
        // TODO: 接入数据库或配置中心加载信标列表
        // 示例（伪代码）：
        // List<BeaconLocation> beacons = beaconLocationMapper.selectAll();
        // beacons.forEach(this::registerBeacon);
        log.info("loadBeaconsFromExternalSource: 当前使用默认信标列表（共{}个）", registry.size());
    }

    /**
     * 根据 UUID 查找信标位置
     *
     * @param uuid 信标 UUID（原始格式或规范化格式均可）
     * @return 信标位置，若未找到则返回 null
     */
    public BeaconLocation findBeaconLocation(String uuid) {
        if (uuid == null) return null;
        String normalized = uuid.toLowerCase().replace("-", "");
        return registry.get(normalized);
    }

    /**
     * 获取完整信标列表
     *
     * @return 所有已注册信标的列表（不可变视图）
     */
    public List<BeaconLocation> getAllBeacons() {
        return new ArrayList<>(registry.values());
    }

    /**
     * 从扫描到的信标数据中，按信号质量（RSSI 从大到小）选取最多 topN 个信标，
     * 并要求对应信标位置已在注册表中存在。
     *
     * @param scannedBeacons 扫描到的信标列表（已按 RSSI 降序排序）
     * @param topN           最多选取数量
     * @return 选出的信标列表（≤ topN 个）
     */
    public List<BeaconData> selectTopBeaconsByQuality(List<BeaconData> scannedBeacons, int topN) {
        List<BeaconData> selected = new ArrayList<>();
        for (BeaconData beacon : scannedBeacons) {
            if (selected.size() >= topN) break;
            // 只选取注册表中有位置信息的信标
            if (findBeaconLocation(beacon.getUuid()) != null) {
                selected.add(beacon);
            }
        }
        log.debug("selectTopBeaconsByQuality: topN={}, 选出={}", topN, selected.size());
        return selected;
    }

    /**
     * 当前注册表中的信标数量
     */
    public int size() {
        return registry.size();
    }
}
