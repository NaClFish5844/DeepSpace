package world.landfall.deepspace.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.spacedata.GalaxyData;
import world.landfall.deepspace.datapack.spacedata.OrbitData;
import world.landfall.deepspace.datapack.spacedata.body.PlanetData;
import world.landfall.deepspace.datapack.spacedata.body.StarData;
import world.landfall.deepspace.datapack.spacedata.body.TeleporterData;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldBodyData;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldSectionData;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
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

    private static final Map<FileType, Set<String>> REQUIRED_KEYS = Map.of(
            FileType.star,               Set.of("name", "id", "type", "size", "hurt_radius"),
            FileType.planet,             Set.of("name", "id", "type", "size", "dimension", "landing_area", "space_height", "reentry_height"),
            FileType.teleporter,         Set.of("name", "id", "type", "size", "teleporter_type", "teleport_distance"),
            FileType.ring_world,         Set.of("name", "id", "type", "scale", "orbit_radius"),
            FileType.ring_world_section, Set.of("name", "id", "type", "size", "index", "dimension", "healthy", "repairable", "space_height", "reentry_height"),
            FileType.galaxy,             Set.of("type", "dimension", "name", "mass_center")
    );
    private static final Map<FileType, Set<String>> ALLOWED_KEYS = Map.of(
            FileType.star,               propertyKeyNames(StarData.class),
            FileType.planet,             propertyKeyNames(PlanetData.class),
            FileType.teleporter,         propertyKeyNames(TeleporterData.class),
            FileType.ring_world,         propertyKeyNames(RingWorldBodyData.class),
            FileType.ring_world_section, propertyKeyNames(RingWorldSectionData.class),
            FileType.galaxy,             propertyKeyNames(GalaxyData.class)
    );

    private static final Set<String> ORBIT_KEYS = propertyKeyNames(OrbitData.class);
    private static final Set<String> DECORATION_KEYS = propertyKeyNames(PlanetData.DecorationData.class);

    private static Set<String> propertyKeyNames(Class<?> dataClass) {
        Set<String> keys = new HashSet<>();

        for (Field field : dataClass.getDeclaredFields()) {
            if ( java.lang.reflect.Modifier.isStatic(field.getModifiers()) ) { continue; }
            if ( java.lang.reflect.Modifier.isTransient(field.getModifiers()) ) { continue; }
            if ( field.isSynthetic() ) { continue; }

            SerializedName name = field.getAnnotation(SerializedName.class);
            keys.add(name == null ? field.getName() : name.value());
        }
        return keys;
    }
    /**
     * 单文件校验：通过就交出 ParsedJson，不通过交 null。
     * <p>
     * 只做单文件内的判断；引用有效性、加载次序一律交给 Loader —— 那里有全量数据，判断天然更准。
     */

    public static ParsedJson parseFileType(ResourceLocation id, DatapackProvider.ProvidedJson providedJson) {
        JsonElement json = providedJson.json();
        if (json == null || !json.isJsonObject()) { return new ParsedJson(id, FileType.invalid, null); }

        JsonObject root = json.getAsJsonObject();
        if ( !isString("type", root) ) { return new ParsedJson(id, FileType.invalid, null); }

        if ( !checkStringInEnum("type", root, FileType.class) ) { return new ParsedJson(id, FileType.invalid, null); }

        return new ParsedJson(id, FileType.of(root.get("type").getAsString()), root);
    }

    /**
     * 单文件校验：输入是**已经分好类**的 ParsedJson，返回校验结果。
     * <p>
     * 分类交给 parseFileType，引用有效性与加载次序交给 Loader —— 这里只管单文件内部的事。
     */
    public static CheckResult check(ParsedJson parsed) {
        if (parsed.root() == null) { return CheckResult.unusable(); }

        return CheckResult.of(switch (parsed.type()) {
            case star               -> checkStar(parsed);
            case planet             -> checkPlanet(parsed);
            case teleporter         -> checkTeleporter(parsed);
            case ring_world         -> checkRingWorld(parsed);
            case ring_world_section -> checkRingWorldSection(parsed);
            case galaxy             -> checkGalaxy(parsed);
            case invalid            -> false;
        });
    }

    private static boolean checkStar(ParsedJson parsed) {
        return checkCommon(parsed);
    }
    private static boolean checkPlanet(ParsedJson parsed) {
        boolean pass = checkCommon(parsed);
        pass &= checkVectorSize(parsed, "landing_area", 2);
        pass &= checkHeightOrder(parsed);
        pass &= checkBlocks(parsed, "decorations", DECORATION_KEYS);
        pass &= checkBlocks(parsed, "moons", ORBIT_KEYS);
        return pass;
    }
    private static boolean checkTeleporter(ParsedJson parsed) {
        return checkCommon(parsed);
    }
    private static boolean checkRingWorld(ParsedJson parsed) {
        return checkCommon(parsed);
    }
    private static boolean checkRingWorldSection(ParsedJson parsed) {
        boolean pass = checkCommon(parsed);
        pass &= checkHeightOrder(parsed);
        return pass;
    }
    private static boolean checkGalaxy(ParsedJson parsed) {
        boolean pass = checkCommon(parsed);
        pass &= checkVectorSize(parsed, "mass_center", 3);
        pass &= checkBlocks(parsed, "central_bodies", ORBIT_KEYS);
        pass &= checkBlocks(parsed, "orbiters", ORBIT_KEYS);
        pass &= checkBlocks(parsed, "teleporters", ORBIT_KEYS);
        pass &= checkOrbiterKinds(parsed);
        return pass;
    }

    /** 六种类型共用：必填字段 + 未知字段 */
    private static boolean checkCommon(ParsedJson parsed) {
        boolean pass = checkRequired(parsed);
        pass &= checkUnknownKeys(parsed);
        return pass;
    }
    /** 顶层未知字段：只提醒，不拦截 */
    private static boolean checkUnknownKeys(ParsedJson parsed) {
        Set<String> allowed = ALLOWED_KEYS.get(parsed.type());
        if (allowed == null) { return true; }

        for (String key : parsed.root().keySet()) {
            if ( ! allowed.contains(key) ) { warn(parsed, "字段 " + key + " 未知，将被忽略"); }
        }
        return true;
    }
    /** 必填字段：只看 JSON 层面在不在，缺了就走数据类的默认值 */
    private static boolean checkRequired(ParsedJson parsed) {
        boolean pass = true;

        for (String key : REQUIRED_KEYS.getOrDefault(parsed.type(), Set.of())) {
            if ( ! parsed.root().has(key) ) {
                warn(parsed, "缺少必填字段 " + key);
                pass = false;
            }
        }
        return pass;
    }
    /** 数组子块：只查未知字段，类型错误交给 Gson 的反序列化 */
    private static boolean checkBlocks(ParsedJson parsed, String key, Set<String> allowed) {
        JsonElement element = parsed.root().get(key);
        if (element == null) { return true; }

        if ( ! element.isJsonArray() ) {
            warn(parsed, key + " 不是列表");
            return false;
        }

        boolean pass = true;
        for (JsonElement block : element.getAsJsonArray()) {
            if ( ! block.isJsonObject() ) {
                warn(parsed, key + "[] 存在非对象元素");
                pass = false;
                continue;
            }
            for (String field : block.getAsJsonObject().keySet()) {
                if ( ! allowed.contains(field) ) { warn(parsed, key + "[] 的 " + field + " 未知，将被忽略"); }
            }
        }
        return pass;
    }
    /** 校验告警统一出口：带文件与类型，debug 级输出（正常数据不打） */
    private static void warn(ParsedJson parsed, String message) {
        getLogger().debug("[{}] {} 未通过校验：{}", parsed.id(), parsed.type(), message);
    }
    /** 再入高度必须小于地表太空高度 */
    private static boolean checkHeightOrder(ParsedJson parsed) {
        JsonElement reentry = parsed.root().get("reentry_height");
        JsonElement space = parsed.root().get("space_height");

        if ( ! isNumberElement(reentry) || ! isNumberElement(space) ) { return true; }

        if ( reentry.getAsInt() >= space.getAsInt() ) {
            warn(parsed, "reentry_height 必须小于 space_height");
            return false;
        }
        return true;
    }
    /** 向量字段的元数：landing_area 是 [宽, 深]，其余是 3 维 */
    private static boolean checkVectorSize(ParsedJson parsed, String key, int size) {
        JsonElement element = parsed.root().get(key);
        if (element == null || !element.isJsonArray()) { return true; }

        if ( element.getAsJsonArray().size() != size ) {
            warn(parsed, key + " 必须是 " + size + " 元");
            return false;
        }
        return true;
    }
    /** 环绕天体的 type 只能是 planet / ring */
    private static boolean checkOrbiterKinds(ParsedJson parsed) {
        JsonElement orbiters = parsed.root().get("orbiters");
        if (orbiters == null || !orbiters.isJsonArray()) { return true; }

        boolean pass = true;
        for (JsonElement orbiter : orbiters.getAsJsonArray()) {
            if ( ! orbiter.isJsonObject() ) { continue; }

            JsonElement type = orbiter.getAsJsonObject().get("type");
            if ( ! isStringElement(type) ) {
                warn(parsed, "orbiters[].type 缺失或不是字符串");
                pass = false;
                continue;
            }
            if ( ! "planet".equals(type.getAsString()) && ! "ring".equals(type.getAsString()) ) {
                warn(parsed, "orbiters[].type 只能是 planet 或 ring");
                pass = false;
            }
        }
        return pass;
    }
    // 需要一个静默路径？
    // 实际上 sablespawner里也遇到过类似的情况 最后被忽略了
    private static boolean isStringElement(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }
    private static boolean isNumberElement(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
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
    public static boolean checkDimensionKey( String key, JsonObject root ) {
        JsonElement element = root.get(key);
        if ( !isString(key, element) ) { return false; }
        if ( ResourceLocation.tryParse( element.getAsString() ) == null ) {
            getLogger().warn("[{}] 不是合法的维度标识（应为 namespace:dim，如 deepspace:space） | [{}] invalid dimension id (expected namespace:dim, e.g. deepspace:space)", key, key);
            return false;
        }
        return true;
    }
    public static Set<String> extractStringArrList(JsonElement element ) {
        if ( element == null || !element.isJsonArray() ) { return new HashSet<>(); }

        Set<String> set = new HashSet<>();
        for (JsonElement e : element.getAsJsonArray()) {
            if ( e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ) { set.add(e.getAsString()); }
        }
        return set;
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

    /**
     * 校验结果。当前只是占位：一个总的是非位，够 check 跑通就行。
     * <p>
     * 以后按 SableSpawner 的做法展开成分项（类型 / 必填 / 跨字段 / 子块），
     * Loader 就能只挑其中几项当硬门槛，其余宽容放行 —— 尽量把数据救回来。
     */
    public record CheckResult(boolean isClean) {
        public static CheckResult of(boolean clean) { return new CheckResult(clean); }
        public static CheckResult unusable() { return new CheckResult(false); }
    }
}
