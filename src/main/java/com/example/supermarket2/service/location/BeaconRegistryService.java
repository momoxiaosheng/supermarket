package com.example.supermarket2.service.location;

import com.example.supermarket2.dto.location.BeaconData;
import com.example.supermarket2.entity.location.BeaconLocation;
import com.example.supermarket2.mapper.location.BeaconLocationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

    @Autowired(required = false)
    private BeaconLocationMapper beaconLocationMapper;

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
     * 应用启动后自动从数据库加载信标。
     * 若数据库返回空列表，保留构造时写入的默认兜底信标。
     */
    @PostConstruct
    public void init() {
        loadBeaconsFromExternalSource();
    }

    /**
     * 从数据库的 beacon_location 表加载已启用的信标，
     * 并通过 {@link #registerBeacon} 注册到运行时注册表。
     * 若数据库未返回任何数据，已有的默认兜底信标保持不变。
     * <p>
     * 注意：此方法通过 {@link #init()} 在应用启动时自动调用一次；
     * 若需重新加载，请在调用前手动清空注册表以避免残留脏数据。
     * </p>
     */
    public void loadBeaconsFromExternalSource() {
        if (beaconLocationMapper == null) {
            log.info("loadBeaconsFromExternalSource: BeaconLocationMapper 未注入，跳过数据库加载，保留默认信标（共{}个）。", registry.size());
            return;
        }
        List<BeaconLocation> beacons = beaconLocationMapper.selectAllEnabled();
        if (beacons == null || beacons.isEmpty()) {
            log.info("loadBeaconsFromExternalSource: 数据库未返回信标数据，保留默认兜底信标（共{}个）。", registry.size());
            return;
        }
        beacons.forEach(this::registerBeacon);
        log.info("loadBeaconsFromExternalSource: 从数据库加载信标完成，共{}个。", beacons.size());
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
