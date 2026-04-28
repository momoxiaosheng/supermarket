package com.example.supermarket2.service.impl.location;

import com.example.supermarket2.dto.location.NavigationRequest;
import com.example.supermarket2.dto.location.NavigationResponse;
import com.example.supermarket2.entity.location.MapGrid;
import com.example.supermarket2.service.WebSocketPushService;
import com.example.supermarket2.service.location.MapGridService;
import com.example.supermarket2.service.location.NavigationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 增强路径规划服务实现类
 * 基于A*算法，支持不可通行目标点调整，与接口解耦
 */
@Slf4j
@Service
public class EnhancedNavigationServiceImpl implements NavigationService {

    @Autowired
    private MapGridService mapGridService;

    @Autowired
    private WebSocketPushService webSocketPushService;

    // 路径缓存：避免重复计算相同路径
    private final Map<String, NavigationResponse> pathCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    // 性能统计
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger successfulPaths = new AtomicInteger(0);

    @Override
    public NavigationResponse calculateEnhancedPath(NavigationRequest request) {
        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();
        try {
            log.info("开始增强路径规划: mapId={}, start=({},{}), end=({},{})",
                    request.getMapId(), request.getStartX(), request.getStartY(),
                    request.getEndX(), request.getEndY());
            // 1. 验证请求参数
            if (!validateNavigationRequest(request)) {
                return buildFailureResponse("路径规划请求参数无效");
            }
            // 2. 检查缓存
            String cacheKey = generateCacheKey(request);
            NavigationResponse cachedResponse = getCachedPath(cacheKey);
            if (cachedResponse != null) {
                cacheHits.incrementAndGet();
                log.debug("路径缓存命中: {}", cacheKey);
                return cachedResponse;
            }
            // 3. 获取地图数据
            List<MapGrid> gridList = mapGridService.getMapGridByMapId(request.getMapId());
            if (gridList.isEmpty()) {
                return buildFailureResponse("地图数据为空");
            }
            // 4. 构建通行矩阵
            int[][] passable = buildPassableMatrix(gridList);
            int maxX = passable[0].length - 1;
            int maxY = passable.length - 1;
            // 5. 验证起点和终点
            Point start = new Point(request.getStartX(), request.getStartY());
            Point end = new Point(request.getEndX(), request.getEndY());
            if (!isWithinBounds(start, maxX, maxY) || !isPassable(passable, start.x, start.y)) {
                return buildFailureResponse("起点不可通行或超出地图范围");
            }
            // 6. 调整目标点（如果不可通行）
            Point adjustedTarget = adjustTargetIfNeeded(passable, end, maxX, maxY);
            if (adjustedTarget == null) {
                return buildFailureResponse("目标点附近无可通行区域");
            }
            // 7. 执行A*算法路径搜索
            List<Point> path = aStarSearch(passable, start, adjustedTarget, maxX, maxY);
            if (path.isEmpty()) {
                return buildFailureResponse("未找到可行路径");
            }
            // 8. 构建成功响应
            NavigationResponse response = buildSuccessResponse(path, request.getEndX(), request.getEndY(),
                    adjustedTarget.x, adjustedTarget.y);
            // 9. 缓存结果
            cachePath(cacheKey, response);
            successfulPaths.incrementAndGet();
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("路径规划完成: 路径长度={}, 调整目标={}, 耗时={}ms",
                    path.size(), !end.equals(adjustedTarget), processingTime);
            return response;
        } catch (Exception e) {
            log.error("增强路径规划异常", e);
            return buildFailureResponse("路径规划异常: " + e.getMessage());
        }
    }

    @Override
    public void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        Iterator<Map.Entry<String, Long>> iterator = cacheTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime - entry.getValue() > CACHE_DURATION) {
                pathCache.remove(entry.getKey());
                iterator.remove();
                removedCount++;
            }
        }
        if (removedCount > 0) {
            log.info("清理过期路径缓存: 移除{}条记录", removedCount);
        }
    }

    @Override
    public NavigationStats getServiceStats() {
        return new NavigationStats(
                totalRequests.get(),
                successfulPaths.get(),
                cacheHits.get(),
                pathCache.size()
        );
    }

    /**
     * 验证导航请求参数
     */
    private boolean validateNavigationRequest(NavigationRequest request) {
        return request != null &&
                request.getMapId() != null && !request.getMapId().trim().isEmpty() &&
                request.getStartX() != null && request.getStartY() != null &&
                request.getEndX() != null && request.getEndY() != null &&
                request.getStartX() >= 0 && request.getStartY() >= 0 &&
                request.getEndX() >= 0 && request.getEndY() >= 0 &&
                !(request.getStartX().equals(request.getEndX()) &&
                        request.getStartY().equals(request.getEndY()));
    }

    /**
     * 构建通行矩阵
     */
    private int[][] buildPassableMatrix(List<MapGrid> gridList) {
        // 计算地图尺寸
        int maxX = gridList.stream().mapToInt(MapGrid::getX).max().orElse(0);
        int maxY = gridList.stream().mapToInt(MapGrid::getY).max().orElse(0);
        int[][] passable = new int[maxY + 1][maxX + 1];
        // 初始化矩阵（默认不可通行）
        for (int i = 0; i <= maxY; i++) {
            Arrays.fill(passable[i], 0);
        }
        // 填充通行信息
        for (MapGrid grid : gridList) {
            if (grid.getY() <= maxY && grid.getX() <= maxX) {
                passable[grid.getY()][grid.getX()] = grid.getPassable();
            }
        }
        log.debug("通行矩阵构建完成: 尺寸={}x{}, 可通行网格={}",
                maxX + 1, maxY + 1,
                Arrays.stream(passable).flatMapToInt(Arrays::stream).filter(v -> v == 1).count());
        return passable;
    }

    /**
     * 检查位置是否可通行
     */
    private boolean isPassable(int[][] passable, int x, int y) {
        if (y < 0 || y >= passable.length || x < 0 || x >= passable[0].length) {
            return false;
        }
        return passable[y][x] == 1;
    }

    /**
     * 检查点是否在边界内
     */
    private boolean isWithinBounds(Point point, int maxX, int maxY) {
        return point.x >= 0 && point.x <= maxX && point.y >= 0 && point.y <= maxY;
    }

    /**
     * 调整目标点（如果不可通行）
     */
    private Point adjustTargetIfNeeded(int[][] passable, Point target, int maxX, int maxY) {
        // 如果目标点可通行，直接返回
        if (isWithinBounds(target, maxX, maxY) && isPassable(passable, target.x, target.y)) {
            return target;
        }
        log.info("目标点不可通行，寻找最近可通行点: ({}, {})", target.x, target.y);
        // 使用BFS寻找最近的可通行点
        return findNearestPassablePoint(passable, target, maxX, maxY);
    }

    /**
     * 寻找最近可通行点（BFS）
     */
    private Point findNearestPassablePoint(int[][] passable, Point target, int maxX, int maxY) {
        Queue<Point> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.offer(target);
        visited.add(target.x + "," + target.y);
        // 8个方向：上下左右 + 对角线
        int[][] directions = {
                {-1,0}, {1,0}, {0,-1}, {0,1},
                {-1,-1}, {1,-1}, {-1,1}, {1,1}
        };
        int searchCount = 0;
        int maxSearch = 200; // 限制搜索范围
        while (!queue.isEmpty() && searchCount < maxSearch) {
            Point current = queue.poll();
            searchCount++;
            // 检查当前点是否可通行
            if (isWithinBounds(current, maxX, maxY) && isPassable(passable, current.x, current.y)) {
                log.debug("找到最近可通行点: ({}, {}), 搜索次数={}", current.x, current.y, searchCount);
                return current;
            }
            // 探索相邻点
            for (int[] dir : directions) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                // 边界检查
                if (newX < 0 || newX > maxX || newY < 0 || newY > maxY) {
                    continue;
                }
                String key = newX + "," + newY;
                if (!visited.contains(key)) {
                    visited.add(key);
                    queue.offer(new Point(newX, newY));
                }
            }
        }
        log.warn("未找到可通行点，搜索范围耗尽: 搜索次数={}", searchCount);
        return null;
    }

    /**
     * A* 路径搜索算法（优化版）
     */
    private List<Point> aStarSearch(int[][] passable, Point start, Point end, int maxX, int maxY) {
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingInt(node -> node.f));
        Map<String, Node> allNodes = new HashMap<>();
        Node startNode = new Node(start.x, start.y, 0, calculateHeuristic(start, end));
        openList.add(startNode);
        allNodes.put(start.x + "," + start.y, startNode);
        // 4方向移动：上下左右（避免对角线穿越障碍物）
        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        int exploredNodes = 0;
        int maxNodes = 10000; // 防止无限循环
        while (!openList.isEmpty() && exploredNodes < maxNodes) {
            Node current = openList.poll();
            exploredNodes++;
            // 到达终点
            if (current.x == end.x && current.y == end.y) {
                log.debug("A*搜索完成: 探索节点数={}, 路径长度={}", exploredNodes, current.g);
                return reconstructPath(current);
            }
            // 标记为已关闭
            current.closed = true;
            // 探索邻居节点
            for (int[] dir : directions) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                // 边界检查
                if (newX < 0 || newX > maxX || newY < 0 || newY > maxY) {
                    continue;
                }
                // 可通行检查
                if (!isPassable(passable, newX, newY)) {
                    continue;
                }
                String neighborKey = newX + "," + newY;
                Node neighbor = allNodes.get(neighborKey);
                if (neighbor == null) {
                    // 新节点
                    neighbor = new Node(newX, newY, Integer.MAX_VALUE, calculateHeuristic(newX, newY, end.x, end.y));
                    allNodes.put(neighborKey, neighbor);
                }
                if (neighbor.closed) {
                    continue;
                }
                // 计算新的G值
                int tentativeG = current.g + 1;
                if (tentativeG < neighbor.g) {
                    neighbor.parent = current;
                    neighbor.g = tentativeG;
                    neighbor.f = neighbor.g + neighbor.h;
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }
        log.warn("A*搜索失败: 未找到路径，探索节点数={}", exploredNodes);
        return Collections.emptyList();
    }

    /**
     * 重构路径
     */
    private List<Point> reconstructPath(Node endNode) {
        List<Point> path = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            path.add(0, new Point(current.x, current.y));
            current = current.parent;
        }
        return path;
    }

    /**
     * 启发式函数（曼哈顿距离）
     */
    private int calculateHeuristic(Point from, Point to) {
        return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
    }

    private int calculateHeuristic(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    /**
     * 构建成功响应
     */
    private NavigationResponse buildSuccessResponse(List<Point> path, int originalEndX, int originalEndY,
                                                    int adjustedEndX, int adjustedEndY) {
        // 转换路径格式
        List<NavigationResponse.GridPoint> gridPath = new ArrayList<>();
        for (Point point : path) {
            gridPath.add(new NavigationResponse.GridPoint(point.x, point.y));
        }
        NavigationResponse response = new NavigationResponse();
        response.setSuccess(true);
        response.setPath(gridPath);
        response.setDistance((double) (path.size() - 1)); // 路径长度（格子数）
        response.setOriginalTarget(new NavigationResponse.GridPoint(originalEndX, originalEndY));
        response.setAdjustedTarget(new NavigationResponse.GridPoint(adjustedEndX, adjustedEndY));
        // 设置是否调整了目标点
        boolean targetAdjusted = !(originalEndX == adjustedEndX && originalEndY == adjustedEndY);
        response.setTargetAdjusted(targetAdjusted);
        if (targetAdjusted) {
            response.setMessage("目标点已调整到最近可通行位置");
        } else {
            response.setMessage("路径规划成功");
        }
        return response;
    }

    /**
     * 构建失败响应
     */
    private NavigationResponse buildFailureResponse(String message) {
        NavigationResponse response = new NavigationResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setPath(Collections.emptyList());
        response.setDistance(0.0);
        response.setTargetAdjusted(false);
        return response;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(NavigationRequest request) {
        return String.format("%s:%d,%d->%d,%d",
                request.getMapId(),
                request.getStartX(), request.getStartY(),
                request.getEndX(), request.getEndY());
    }

    /**
     * 获取缓存的路径
     */
    private NavigationResponse getCachedPath(String cacheKey) {
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            return pathCache.get(cacheKey);
        }
        // 清理过期缓存
        if (timestamp != null) {
            pathCache.remove(cacheKey);
            cacheTimestamps.remove(cacheKey);
        }
        return null;
    }

    /**
     * 缓存路径
     */
    private void cachePath(String cacheKey, NavigationResponse response) {
        pathCache.put(cacheKey, response);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }

    /**
     * A*算法节点类
     */
    private static class Node {
        int x, y;
        int g; // 实际距离
        int h; // 启发式距离
        int f; // f = g + h
        Node parent;
        boolean closed = false;

        public Node(int x, int y, int g, int h) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Node node = (Node) obj;
            return x == node.x && y == node.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}
