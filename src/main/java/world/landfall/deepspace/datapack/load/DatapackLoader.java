package world.landfall.deepspace.datapack.load;

import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.spacedata.galaxy.GalaxyData;
import world.landfall.deepspace.datapack.spacedata.OrbiterData;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;
import world.landfall.deepspace.datapack.spacedata.body.PlanetData;
import world.landfall.deepspace.datapack.spacedata.body.StarData;
import world.landfall.deepspace.datapack.spacedata.body.TeleporterData;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldBodyData;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldSectionData;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlaneData;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlaneGraph;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlanePoint;

import java.util.*;

import static world.landfall.deepspace.util.AccessUtil.*;


public final class DatapackLoader {
    @Getter private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    // 一轮加载：将数据写进内存
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
    public static ObjectList<HyperlaneData> loadHyperlaneData() {
        ObjectList<HyperlaneData> result = new ObjectArrayList<>();

        for (List<DatapackProvider.ProvidedJson> stack : DatapackProvider.getAllHyperlaneData().values()) {
            for (DatapackProvider.ProvidedJson provided : stack) {
                if (provided.json() == null || ! provided.json().isJsonObject()) { continue; }

                JsonObject root = provided.json().getAsJsonObject();

                // 双向：一对展开成两条互为反向的边
                for (JsonObject json : DatapackChecker.filterHyperlane("bidirectional_links", root)) {
                    HyperlaneGraph.BiLinkData bi = GSON.fromJson(json, HyperlaneGraph.BiLinkData.class);
                    if (bi == null || bi.pointA() == null || bi.pointB() == null) { continue; }

                    Pair<HyperlaneData, HyperlaneData> both = HyperlaneData.ofBiDir(bi.pointA(), bi.pointB());
                    result.add(both.left());
                    result.add(both.right());
                }
                // 单向：本身就是一条边
                for (JsonObject json : DatapackChecker.filterHyperlane("unidirectional_links", root)) {
                    HyperlaneData edge = GSON.fromJson(json, HyperlaneData.class);
                    if (edge != null) { result.add(edge); }
                }
                // 随机：终点为空
                for (JsonObject json : DatapackChecker.filterHyperlane("random_links", root)) {
                    HyperlanePoint point = GSON.fromJson(json, HyperlanePoint.class);
                    if (point != null) { result.add(HyperlaneData.ofRandomLink(point)); }
                }
            }
        }
        return result;
    }

    private static PlanetData loadPlanet(DatapackChecker.ParsedJson parsed) {
        PlanetData planet = GSON.fromJson(parsed.root(), PlanetData.class);
        if (planet == null) { return null; }

        planet.setMoons(
                parseMap(
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
                parseMap(
                        DatapackChecker.filterBlocks(parsed, "central_bodies"), OrbiterData.class
                ));
        galaxy.setOrbiters(
                parseMap(
                        DatapackChecker.filterBlocks(parsed, "orbiters"), OrbiterData.class
                ));
        galaxy.setTeleporters(
                parseMap(
                        DatapackChecker.filterBlocks(parsed, "teleporters"), OrbiterData.class
                ));
        return galaxy;
    }
    private static <T> ObjectList<T> parseList(List<JsonObject> blocks, Class<T> type) {
        ObjectList<T> result = new ObjectArrayList<>();

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
    private static <T extends OrbiterData> Object2ObjectOpenHashMap<Integer, T> parseMap(List<JsonObject> blocks, Class<T> type) {
        Object2ObjectOpenHashMap<Integer, T> result = new Object2ObjectOpenHashMap<>();

        for (JsonObject block : blocks) {
            try {
                T loaded = GSON.fromJson(block, type);
                if (loaded != null) { result.put(loaded.getId(), loaded); }
            } catch (RuntimeException exception) {
                getLogger().debug("子块反序列化失败，已丢弃 | sublevel deserialization failed, dropped: {}", exception.getMessage());
            }
        }
        return result;
    }

    // 二轮校验：就地清洗内存中的数据
    public static void filterCelestialBodyData() {
        Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> bodies =
                getDatapackManager().celestialBodyQuery().collect();

        for ( AbstractCelestialBodyData body : bodies.values() ) {
            // 只有行星带卫星子块
            if ( ! (body instanceof PlanetData planet) ) { continue; }

            planet.getMoons().values().removeIf(moon ->
                    ! DatapackChecker.checkReference(moon)
                 || ! DatapackChecker.checkOrbiterType("moons", moon));
        }
    }
    public static void filterGalaxyData() {
        Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> galaxies =
                getDatapackManager().galaxyQuery().collect();

        for ( GalaxyData galaxy : galaxies.values() ) {
            // 引用存在 + 基础判据
            galaxy.getCentralBodies().values().removeIf(body ->
                    ! DatapackChecker.checkReference(body)
                 || ! DatapackChecker.checkOrbiterType("central_bodies", body));

            galaxy.getOrbiters().values().removeIf(orbiter ->
                    ! DatapackChecker.checkReference(orbiter)
                 || ! DatapackChecker.checkOrbiterType("orbiters", orbiter));

            galaxy.getTeleporters().values().removeIf(teleporter ->
                    ! DatapackChecker.checkReference(teleporter)
                 || ! DatapackChecker.checkOrbiterType("teleporters", teleporter));

            // 交叉判据
            galaxy.setOrbiters(
                    DatapackChecker.filterExclusiveOrbiters(galaxy.getOrbiters())
            );
        }
    }
    public static void filterHyperLaneData() {
        getDatapackManager().getHYPER_LINE_MANAGER().removeIf(edge ->
                ! DatapackChecker.checkHyperLaneData(edge));
    }
    public static void indexHyperLaneAdjacency() {
        Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> adjacentList    = getDatapackManager().getADJACENT_LIST();
        Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> revAdjacentList = getDatapackManager().getREV_ADJACENT_LIST();

        adjacentList.clear();
        revAdjacentList.clear();

        for (HyperlaneData edge : getDatapackManager().getHYPER_LINE_MANAGER()) {
            if ( edge.isRandomLink() ) { continue; }

            link(adjacentList, edge.getFrom(), edge.getTo());
            link(revAdjacentList, edge.getTo(), edge.getFrom());
        }
    }
    private static void link(Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> adjList, HyperlanePoint from, HyperlanePoint to) {
        adjList.computeIfAbsent(from, key -> new ObjectOpenHashSet<>()).add(to);
    }

}
