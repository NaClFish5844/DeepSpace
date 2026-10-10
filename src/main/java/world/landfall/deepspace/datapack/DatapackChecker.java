package world.landfall.deepspace.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.spacedata.GalaxyData;
import world.landfall.deepspace.datapack.spacedata.GalaxyTeleporterData;
import world.landfall.deepspace.datapack.spacedata.OrbiterData;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;
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

    private static final Map<FileType, Set<String>> FILE_REQUIRED_KEYS = Map.of(
            FileType.star,               Set.of("name", "id", "type", "size", "hurt_radius"),
            FileType.planet,             Set.of("name", "id", "type", "size", "dimension", "landing_area", "space_height", "reentry_height"),
            FileType.teleporter,         Set.of("name", "id", "type", "size", "teleporter_type", "teleport_distance"),
            FileType.ring_world,         Set.of("name", "id", "type", "scale", "orbit_radius"),
            FileType.ring_world_section, Set.of("name", "id", "type", "index", "dimension", "healthy", "repairable", "space_height", "reentry_height"),
            FileType.galaxy,             Set.of("type", "dimension", "name", "mass_center")
    );
    private static final Map<FileType, Set<String>> FILE_ALLOWED_KEYS = Map.of(
            FileType.star,               getAllKeyNames(StarData.class),
            FileType.planet,             getAllKeyNames(PlanetData.class),
            FileType.teleporter,         getAllKeyNames(TeleporterData.class),
            FileType.ring_world,         getAllKeyNames(RingWorldBodyData.class),
            FileType.ring_world_section, getAllKeyNames(RingWorldSectionData.class),
            FileType.galaxy,             getAllKeyNames(GalaxyData.class)
    );
    private static final Map<String, Set<String>> ORBITER_REQUIRED_KEYS = Map.of(
            "central_bodies", Set.of("body", "offset"),
            "orbiters",       Set.of("body", "offset"),
            "moons",          Set.of("body", "offset"),
            "teleporters",    Set.of("body", "offset"),
            "decorations",    Set.of("type", "scale")
    );
    /** 子块数组 → 允许引用的目标数据类。环绕物引用的是「对象」，所以按类判而不是按 type 字符串判 */
    private static final Map<String, Set<Class<?>>> ORBITER_VALID_TYPES = Map.of(
            "central_bodies", Set.of(StarData.class, PlanetData.class),
            "orbiters",       Set.of(PlanetData.class, RingWorldBodyData.class),
            "moons",          Set.of(PlanetData.class),      // 卫星就是 PlanetData，moon 是位置不是类型
            "teleporters",    Set.of(TeleporterData.class)
    );
    private static Set<String> getAllKeyNames(Class<?> dataClass) {
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

    // 取文件
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

    // 二轮校验
    public static boolean checkReference(OrbiterData orbiter, Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> bodies) {
        ResourceLocation target = orbiter.getResourceLocation();
        return target != null && bodies.containsKey(target);
    }
    public static boolean checkOrbiterType(String arrayKey, OrbiterData orbiter, Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> bodies) {
        Set<Class<?>> allowed = ORBITER_VALID_TYPES.get(arrayKey);
        if (allowed == null) { return true; }

        AbstractCelestialBodyData target = bodies.get(orbiter.getResourceLocation());
        if (target == null) { return false; }

        for (Class<?> type : allowed) {
            if ( type.isInstance(target) ) { return true; }
        }
        return false;
    }

    /**
     * 星系内的环世界规则：环绕天体不允许同时存在星环和星球，且每种规模的星环最多一个。
     * <p>
     * 需要环世界体表才能判（规模写在环体里），而环世界体还没进内存，暂时放行。
     */
    public static boolean checkGalaxyOrbiter(GalaxyData galaxy) {
        // 一个星系内的环绕天体不允许同时存在星环和星球
        // 并且每种规模的星环最多只能存在一种
        return true;
    }

    /**
     * 传送点的特殊校验。
     * <p>
     * 「中继器必须成对」不在这里判 —— 那要等全部星系的传送点都筛完才能统一看，见 checkTeleporterPair。
     *
     * @param selfDimension 传送点自己所在星系的维度：不能把传送点指向自己
     * @param dimensions    MC 里真实存在的维度集合：传送目标必须落在其中
     */
    public static boolean checkGalaxyTeleporter(GalaxyTeleporterData teleporter, Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> bodies, Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> galaxies, String selfDimension, Set<ResourceLocation> dimensions) {
        // ① 必须写明指代维度，而且是个真实存在的维度
        ResourceLocation dimension = teleporter.getDimensionId();
        if (dimension == null || ! dimensions.contains(dimension)) { return false; }

        // ② 不能指向自己所在的星系
        if ( dimension.toString().equals(selfDimension) ) { return false; }

        // ③ 引用的传送点天体必须存在，而且确实是个传送点
        if ( ! (bodies.get(teleporter.getResourceLocation()) instanceof TeleporterData) ) { return false; }

        // ④ 指向的星系必须存在
        for (GalaxyData candidate : galaxies.values()) {
            if ( dimension.toString().equals(candidate.getDimension()) ) { return true; }
        }
        return false;
    }
    public static boolean checkTeleporterPair(Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> galaxies) {

    }
    public static boolean checkBigger(Number smaller, Number bigger) {
        return smaller.doubleValue() < bigger.doubleValue();
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

}
