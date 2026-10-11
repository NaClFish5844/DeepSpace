package world.landfall.deepspace.datapack.load;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.spacedata.galaxy.GalaxyData;
import world.landfall.deepspace.datapack.spacedata.OrbiterData;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;
import world.landfall.deepspace.datapack.spacedata.body.PlanetData;
import world.landfall.deepspace.datapack.spacedata.body.StarData;
import world.landfall.deepspace.datapack.spacedata.body.TeleporterData;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldBodyData;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldBodyData.RingWorldScale;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlaneData;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlanePoint;

import javax.annotation.Nullable;
import java.util.*;

import static world.landfall.deepspace.util.AccessUtil.*;

public final class DatapackChecker {

    public record ParsedJson(ResourceLocation id, FileType type, @Nullable JsonObject root) {}
    
    public enum FileType {
        star,
        planet,
        teleporter,
        ring_world,
        ring_world_section,
        galaxy,
        invalid;

        public static FileType of(String name) {
            for (FileType type : values()) {
                if (type.name().equals(name)) { return type; }
            }
            return invalid;
        }
    }

    private static final Map<FileType, Set<String>> FILE_REQUIRED_KEYS = Map.of(
            FileType.star,               Set.of("name", "type", "size", "hurt_radius"),
            FileType.planet,             Set.of("name", "type", "size", "dimension", "landing_area", "space_height", "reentry_height"),
            FileType.teleporter,         Set.of("name", "type", "scale", "teleporter_type", "teleport_distance"),
            FileType.ring_world,         Set.of("name", "type", "scale", "orbit_radius"),
            FileType.ring_world_section, Set.of("name", "type", "index", "dimension", "healthy", "repairable", "space_height", "reentry_height"),
            FileType.galaxy,             Set.of("type", "dimension", "name", "mass_center")
    );
    private static final Map<String, Set<String>> ORBITER_REQUIRED_KEYS = Map.of(
            "central_bodies", Set.of("id", "body", "offset"),
            "orbiters",       Set.of("id", "body", "offset"),
            "moons",          Set.of("id", "body", "offset"),
            "teleporters",    Set.of("id", "body", "offset"),
            "decorations",    Set.of("type", "scale")
    );
    /** 子块数组 → 允许引用的目标数据类。环绕物引用的是「对象」，所以按类判而不是按 type 字符串判 */
    private static final Map<String, Set<Class<?>>> ORBITER_VALID_TYPES = Map.of(
            "central_bodies", Set.of(StarData.class, PlanetData.class),
            "orbiters",       Set.of(StarData.class, PlanetData.class, RingWorldBodyData.class),
            "moons",          Set.of(StarData.class, PlanetData.class, RingWorldBodyData.class),
            "teleporters",    Set.of(TeleporterData.class)
    );

    // 取天体和星系文件
    public static ParsedJson parseFileType(ResourceLocation id, DatapackProvider.ProvidedJson providedJson) {
        JsonElement json = providedJson.json();
        if (json == null || !json.isJsonObject()) { return new ParsedJson(id, FileType.invalid, null); }

        JsonObject root = json.getAsJsonObject();
        if ( !isString("type", root) ) { return new ParsedJson(id, FileType.invalid, null); }

        if ( !checkStringInEnum("type", root, FileType.class) ) { return new ParsedJson(id, FileType.invalid, null); }

        return new ParsedJson(id, FileType.of(root.get("type").getAsString()), root);
    }

    // 一轮校验
    public static boolean checkFile(ParsedJson parsed) {
        if (parsed.root() == null) { return false; }
        return checkRequired(parsed);
    }
    public static List<JsonObject> filterBlocks(ParsedJson parsed, String key) {
        List<JsonObject> clean = new ArrayList<>();

        JsonElement element = parsed.root().get(key);
        if (element == null) { return clean; }

        if ( ! element.isJsonArray() ) {
            getLogger().debug("[{}] {} 不是列表", parsed.id(), key);
            return clean;
        }

        for (JsonElement raw : element.getAsJsonArray()) {
            if ( ! raw.isJsonObject() ) {
                getLogger().debug("[{}] {}[] 存在非对象元素，已丢弃", parsed.id(), key);
                continue;
            }

            JsonObject block = raw.getAsJsonObject();
            if ( ! checkBlock(parsed, key, block) ) { continue; }
            clean.add(block);
        }
        return clean;
    }
    public static boolean checkBlock(ParsedJson parsed, String arrayKey, JsonObject block) {
        for (String key : ORBITER_REQUIRED_KEYS.getOrDefault(arrayKey, Set.of())) {
            if ( ! block.has(key) ) {
                getLogger().debug("[{}] {} 的子块缺少必填字段 {}", parsed.id(), arrayKey, key);
                return false;
            }
        }
        return true;
    }
    private static boolean checkRequired(ParsedJson parsed) {
        boolean pass = true;

        for (String key : FILE_REQUIRED_KEYS.getOrDefault(parsed.type(), Set.of())) {
            if ( ! parsed.root().has(key) ) {
                getLogger().debug("[{}] 缺少必填字段 {}", parsed.id(), key);
                pass = false;
            }
        }
        return pass;
    }
    /**
     * 取出某个方向的全部边并逐条校验，不合法的直接丢掉。
     *
     * @param direction 三个列表的键名：bidirectional_links / unidirectional_links / random_links
     */
    public static List<JsonObject> filterHyperlane(String direction, JsonObject root) {
        if (root == null) { return List.of(); }

        JsonElement element = root.get(direction);
        if (element == null || ! element.isJsonArray()) { return List.of(); }

        List<JsonObject> result = new ArrayList<>();

        for (JsonElement item : element.getAsJsonArray()) {
            if ( ! item.isJsonObject() ) { continue; }

            JsonObject edge = item.getAsJsonObject();
            if ( checkHyperlaneEdge(direction, edge) ) { result.add(edge); }
            else { getLogger().warn("非法的超空间航道，已丢弃：{} | invalid hyperlane edge dropped", edge); }
        }
        return result;
    }
    /**
     * 一条边是否合法。
     * <p>
     * 键名按方向不同：双向是 point_a / point_b，单向是 from / to，
     * 随机连接的端点直接写在对象本身（没有终点）。
     */
    private static boolean checkHyperlaneEdge(String direction, JsonObject edge) {
        boolean isRandom      = "random_links".equals(direction);
        boolean bidirectional = "bidirectional_links".equals(direction);

        if (isRandom) {
            if ( ! isString("galaxy_dimension", edge.get("galaxy_dimension")) ) { return false; }
        } else if ( ! isObject(edge.get(bidirectional ? "point_a" : "from")) ) {
            return false;
        }

        return isRandom || isObject(edge.get(bidirectional ? "point_b" : "to"));
    }


    // 二轮校验：交叉引用的规则没法只看当前文件，所以直接查 Manager 的查询器
    public static boolean checkReference(OrbiterData orbiter) {
        ResourceLocation target = orbiter.getResourceLocation();
        if (target == null) { return false; }

        return getDatapackManager().celestialBodyQuery().get(target) != null;
    }
    /**
     *  某个环绕的地方只支持某几种类型的天体
     *  <p>
     *  比如 传送点显然不能是行星 恒星 星环
     *  <p>
     *  而中心天体显然不可以是星环
     */
    public static boolean checkOrbiterType(String arrayKey, OrbiterData orbiter) {
        Set<Class<?>> allowed = ORBITER_VALID_TYPES.get(arrayKey);
        if (allowed == null) { return true; }

        ResourceLocation target = orbiter.getResourceLocation();
        if (target == null) { return false; }

        AbstractCelestialBodyData body = getDatapackManager().celestialBodyQuery().get(target);
        if (body == null) { return false; }

        for (Class<?> type : allowed) {
            if ( type.isInstance(body) ) { return true; }
        }
        return false;
    }
    /**
     * 适用于所有「环绕关系」，包括恒星系的星系轨道和行星的卫星轨道。
     * <p>
     * 环绕天体不允许同时存在星环和星球，且每种规模的星环最多一个。
     * 优先级：星环 &gt; 星球/恒星 —— 一旦出现星环，非环的全部丢掉。
     * <p>
     * @return 过滤后的新集合，不修改入参
     */
    public static Object2ObjectOpenHashMap<Integer, OrbiterData> filterExclusiveOrbiters(Object2ObjectOpenHashMap<Integer, OrbiterData> orbiters) {
        Object2ObjectOpenHashMap<Integer, OrbiterData> kept = new Object2ObjectOpenHashMap<>();
        if (orbiters == null || orbiters.isEmpty()) { return kept; }

        // ① 分拣：顺路把「是不是环、什么规模」写进 orbiter —— isRing() / getRingScale() 靠它
        Object2ObjectOpenHashMap<Integer, OrbiterData> rings  = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<Integer, OrbiterData> others = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<Integer, OrbiterData> entry : orbiters.entrySet()) {
            if ( markRingScale(entry.getValue()) ) { rings.put(entry.getKey(), entry.getValue()); }
            else { others.put(entry.getKey(), entry.getValue()); }
        }

        // ② 星环优先：没有环就全是星球或恒星
        if ( rings.isEmpty() ) { return others; }

        // ③ 有环则非环的全丢，且每种规模的环最多留一个
        Set<RingWorldScale> seenScales = new HashSet<>();
        for (Map.Entry<Integer, OrbiterData> entry : rings.entrySet()) {
            if ( seenScales.add(entry.getValue().getRingScale()) ) { kept.put(entry.getKey(), entry.getValue()); }
        }
        return kept;
    }
    /**
     * 一条连接是否成立：两个端点都得落在真实存在的信标上，且不能自己连自己。
     * <p>
     * 随机连接没有终点，所以只查起点 —— "自己连自己"也就无从谈起。
     */
    public static boolean checkHyperLaneData(HyperlaneData hyperlaneData) {
        HyperlanePoint from = hyperlaneData.getFrom();
        if ( ! pointExists(from) ) { return false; }

        HyperlanePoint to = hyperlaneData.getTo();
        if (to == null) { return true; }

        return pointExists(to) && ! from.equals(to);
    }
    private static boolean pointExists(HyperlanePoint point) {
        if (point == null) { return false; }

        ResourceLocation dimension = point.getGalaxyId();
        if (dimension == null) { return false; }

        Map<ResourceLocation, GalaxyData> matched = getDatapackManager().galaxyQuery()
                .ofDimension(dimension)
                .collect();
        if ( matched.isEmpty() ) { return false; }

        // teleporters 以信标 id 为键，所以直接查键即可
        return matched.values().iterator().next().getTeleporters().containsKey(point.teleporterID());
    }
    private static boolean markRingScale(OrbiterData orbiter) {
        ResourceLocation target = orbiter.getResourceLocation();
        if (target == null) { return false; }

        if ( getDatapackManager().celestialBodyQuery().get(target) instanceof RingWorldBodyData ring ) {
            orbiter.setRingScale(ring.getScale());
            return true;
        }
        return false;
    }

    private static boolean isObject(@Nullable JsonElement element) {
        return element != null && element.isJsonObject();
    }
    public static boolean checkStringInEnum( String key, JsonObject root, Class< ? extends Enum<?> > enumClass ) {
        JsonElement element = root.get(key);

        Set<String> valid = enumNamesIgnoreFlag(enumClass);
        if ( !isString( key, element ) ) { return false; }

        String value = element.getAsString();
        String validValues = String.join(", ", valid);

        if ( !valid.contains(value) ) {
            getLogger().warn("[{}] 名称非法，为{}，（合法值为{}） | [{}] invalid name: {}, (valid: {})", key, value, validValues, key, value, validValues);
            return false;
        }

        return true;
    }

    public static boolean isNumber( String key, JsonElement element ) {
        if (
                element == null ||
                        ! element.isJsonPrimitive() ||
                        ! element.getAsJsonPrimitive().isNumber()
        ) {
            getLogger().warn("[{}] 缺失或不是数字 | [{}] missing or not a number", key, key);
            return false;
        }
        return true;
    }
    public static boolean isInteger( String key, JsonElement element ) {
        if ( !isNumber( key, element ) ) { return false; }
        if ( element.getAsJsonPrimitive().getAsBigDecimal().stripTrailingZeros().scale() > 0 ) {
            getLogger().warn("[{}] 不是整数 | [{}] not an integer", key, key);
        }
        return true;
    }
    public static boolean isString( String key, JsonElement element ) {
        if (
                element == null ||
                        ! element.isJsonPrimitive() ||
                        ! element.getAsJsonPrimitive().isString()
        ) {
            getLogger().warn("[{}] 缺失或不是字符串 | [{}] missing or not a string", key, key);
            return false;
        }
        return true;
    }
    public static boolean isArrList( String key, JsonElement element ) {
        if (
                element == null ||
                        ! element.isJsonArray()
        ) {
            getLogger().warn("[{}] 缺失或不是列表 | [{}] missing or not a list", key, key);
            return false;
        }
        return true;
    }
    public static boolean isBoolean( String key, JsonElement element ) {
        if (
                element == null ||
                        ! element.isJsonPrimitive() ||
                        ! element.getAsJsonPrimitive().isBoolean()
        ) {
            getLogger().warn("[{}] 缺失或不是布尔值 | [{}] missing or not a boolean", key, key);
            return false;
        }
        return true;
    }

    public static boolean isNumber( String key, JsonObject root ) {
        JsonElement element = root.get(key);
        return isNumber(key,element);
    }
    public static boolean isInteger( String key, JsonObject root ) {
        JsonElement element = root.get(key);
        return isInteger(key,element);
    }
    public static boolean isString( String key, JsonObject root ) {
        JsonElement element = root.get(key);
        return isString(key,element);
    }
    public static boolean isArrList( String key, JsonObject root ) {
        JsonElement element = root.get(key);
        return isArrList(key,element);
    }
    public static boolean isBoolean( String key, JsonObject root ) {
        JsonElement element = root.get(key);
        return isBoolean(key,element);
    }


    private static final Set<String> ENUM_SENTINELS = Set.of("invalid", "none", "unknown", "undefined");
    private static Set<String> enumNames(Class<? extends Enum<?>> enumClass) {
        Set<String> names = new HashSet<>();
        for (Enum<?> constant : enumClass.getEnumConstants()) { names.add(constant.name()); }
        return names;
    }
    private static Set<String> enumNamesIgnoreFlag(Class<? extends Enum<?>> enumClass) {
        Set<String> names = enumNames(enumClass);
        names.removeAll(ENUM_SENTINELS);

        return names;
    }

}
