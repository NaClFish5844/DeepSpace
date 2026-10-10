package world.landfall.deepspace.datapack;

import com.google.gson.*;
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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static world.landfall.deepspace.util.AccessUtil.*;


public final class DatapackLoader {
    @Getter private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    // 一轮加载
    public static Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> loadCelestialBodyData() {
        Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> result = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<ResourceLocation, DatapackProvider.ProvidedJson> entry : DatapackProvider.getEffectiveSpaceData().entrySet()) {

            DatapackChecker.ParsedJson parsed = DatapackChecker.parseFileType(entry.getKey(), entry.getValue());

            if ( ! DatapackChecker.checkFile(parsed) ) { continue; }

            AbstractCelestialBodyData body = switch (parsed.type()) {
                case star               -> GSON.fromJson(parsed.root(), StarData.class);
                case planet             -> loadPlanet(parsed);
                case teleporter         -> GSON.fromJson(parsed.root(), TeleporterData.class);
                case ring_world_section -> GSON.fromJson(parsed.root(), RingWorldSectionData.class);
                case ring_world         -> GSON.fromJson(parsed.root(), RingWorldBodyData.class);
                case galaxy, invalid    -> null;
            };

            if (body == null) { continue; }

            body.setDataId(parsed.id());
            result.put(parsed.id(), body);
        }
        return result;
    }
    public static Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> loadGalaxyData() {
        Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> result = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<ResourceLocation, DatapackProvider.ProvidedJson> entry : DatapackProvider.getEffectiveSpaceData().entrySet()) {
            DatapackChecker.ParsedJson parsed = DatapackChecker.parseFileType(entry.getKey(), entry.getValue());
            if (parsed.type() != DatapackChecker.FileType.galaxy) { continue; }
            if ( ! DatapackChecker.checkFile(parsed) ) { continue; }

            GalaxyData galaxy = loadGalaxy(parsed);
            if (galaxy == null) { continue; }

            galaxy.setDataId(parsed.id());
            result.put(parsed.id(), galaxy);
        }
        return result;
    }

    // 二轮校验
    public static Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> filterCelestialBodyData(Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> buffer) {
        Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> result = new Object2ObjectOpenHashMap<>(buffer);

        for ( AbstractCelestialBodyData body : buffer.values() ) {
            // 只有行星带卫星子块
            if ( ! (body instanceof PlanetData planet) ) { continue; }

            planet.getMoons().removeIf(moon ->
                    ! DatapackChecker.checkReference(moon, buffer)
                 || ! DatapackChecker.checkOrbiterType("moons", moon, buffer));
        }

        return result;
    }
    /**
     * 星系二轮过滤：只摘坏子块，不删星系本身。
     * <p>
     * 目前只做 central_bodies —— 和行星的 moons 完全同构，差别只在 arrayKey。
     * orbiters 与 teleporters 的规则还在研究中。
     */
    public static Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> filterGalaxyData(Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> galaxyBuffer, Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> celestialBodyBuffer) {
        Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> result = new Object2ObjectOpenHashMap<>(galaxyBuffer);

        for ( GalaxyData galaxy : galaxyBuffer.values() ) {
            // 中心天体：引用存在 + 目标必须是恒星（不能是环世界）
            galaxy.getCentralBodies().removeIf(body ->
                    ! DatapackChecker.checkReference(body, celestialBodyBuffer)
                 || ! DatapackChecker.checkOrbiterType("central_bodies", body, celestialBodyBuffer));

            // 环绕天体：环世界和星球不能同时出现
            // 如果是环世界 每种规模的星环最多一个
            // 1 引用存在性
            // 2 类型互斥
            // 3 环世界规模

            // 传送点：
        }

        return result;
    }

    private static PlanetData loadPlanet(DatapackChecker.ParsedJson parsed) {
        PlanetData planet = GSON.fromJson(parsed.root(), PlanetData.class);
        if (planet == null) { return null; }

        planet.setMoons(
                parseList(
                        DatapackChecker.filterBlocks(parsed, "moons"), OrbiterData.class
                ));
        planet.setDecorations(
                parseList(
                        DatapackChecker.filterBlocks(parsed, "decorations"), PlanetData.DecorationData.class
                ));
        return planet;
    }
    private static GalaxyData loadGalaxy(DatapackChecker.ParsedJson parsed) {
        GalaxyData galaxy = GSON.fromJson(parsed.root(), GalaxyData.class);
        if (galaxy == null) { return null; }

        galaxy.setCentralBodies(
                parseList(
                        DatapackChecker.filterBlocks(parsed, "central_bodies"), OrbiterData.class
                ));
        galaxy.setOrbiters(
                parseList(
                        DatapackChecker.filterBlocks(parsed, "orbiters"), OrbiterData.class
                ));
        galaxy.setTeleporters(
                parseList(
                        DatapackChecker.filterBlocks(parsed, "teleporters"), GalaxyTeleporterData.class
                ));
        return galaxy;
    }
    private static <T> ArrayList<T> parseList(List<JsonObject> blocks, Class<T> type) {
        ArrayList<T> result = new ArrayList<>();

        for (JsonObject block : blocks) {
            try {
                T loaded = GSON.fromJson(block, type);
                if (loaded != null) { result.add(loaded); }
            } catch (RuntimeException exception) {
                getLogger().debug("子块反序列化失败，已丢弃 | sublevel deserialization failed, dropped: {}", exception.getMessage());
            }
        }
        return result;
    }

}
