package world.landfall.deepspace.datapack.query;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;
import world.landfall.deepspace.datapack.spacedata.body.PlanetData;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldBodyData;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldSectionData;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 天体的链式只读查询。
 */
public class CelestialBodyQuery {
    private final Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> source;
    private Predicate<AbstractCelestialBodyData> predicate = data -> true;

    public CelestialBodyQuery(Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> source) {
        this.source = source;
    }

    public CelestialBodyQuery ofPlanet()           { return ofType(AbstractCelestialBodyData.CelestialBodyType.planet); }
    public CelestialBodyQuery ofStar()             { return ofType(AbstractCelestialBodyData.CelestialBodyType.star); }
    public CelestialBodyQuery ofTeleporter()       { return ofType(AbstractCelestialBodyData.CelestialBodyType.teleporter); }
    public CelestialBodyQuery ofRingWorldSection() { return ofType(AbstractCelestialBodyData.CelestialBodyType.ring_world_section); }
    public CelestialBodyQuery ofRingWorldBody()    { return ofType(AbstractCelestialBodyData.CelestialBodyType.ring_world); }
    public CelestialBodyQuery ofType(AbstractCelestialBodyData.CelestialBodyType type) {
        predicate = predicate.and(data -> data.getType() == type);
        return this;
    }

    public CelestialBodyQuery ofPackName(String packName) {
        predicate = predicate.and(data -> Objects.equals(data.getPackName(), packName));
        return this;
    }

    public ObjectList<RingWorldSectionData> ringSectionsOf(ResourceLocation ringWorldBody) {
        ObjectList<RingWorldSectionData> result = new ObjectArrayList<>();
        if (ringWorldBody == null) { return result; }

        String bodyPath = ringWorldBody.getPath();
        int slash = bodyPath.lastIndexOf('/');
        if (slash < 0) { return result; }

        String directory = bodyPath.substring(0, slash + 1);

        for (Map.Entry<ResourceLocation, AbstractCelestialBodyData> entry : source.entrySet()) {
            ResourceLocation id = entry.getKey();

            if ( ! id.getNamespace().equals(ringWorldBody.getNamespace()) ) { continue; }
            if ( ! id.getPath().startsWith(directory) ) { continue; }
            if ( entry.getValue() instanceof RingWorldSectionData section ) { result.add(section); }
        }
        return result;
    }
    public RingWorldBodyData ringBodyOf(ResourceLocation ringWorldSection) {
        if (ringWorldSection == null) { return null; }

        String sectionPath = ringWorldSection.getPath();
        int slash = sectionPath.lastIndexOf('/');
        if (slash < 0) { return null; }

        String directory = sectionPath.substring(0, slash);
        ResourceLocation bodyId = ResourceLocation.fromNamespaceAndPath(
                ringWorldSection.getNamespace(), directory + "/" + directory);

        return (source.get(bodyId) instanceof RingWorldBodyData body) ? body : null;
    }

    /**
     * 只留「表面落在某个维度里」的天体 —— 行星与环世界区段。
     * <p>
     * 只有这两类自带 dimension 字段：恒星、传送点、环世界体都不指代任何维度，所以按维度查的时候
     * 天然被排除在外。这是「从维度反查天体」的唯一入口（旧链路里对应 getPlanetByDimension）。
     */
    public CelestialBodyQuery ofDimension(String dimension) {
        predicate = predicate.and(data -> Objects.equals(dimensionOf(data), dimension));
        return this;
    }
    public CelestialBodyQuery ofDimension(ResourceLocation dimension) {
        return (dimension == null) ? this : ofDimension(dimension.toString());
    }

    @Nullable public AbstractCelestialBodyData get(ResourceLocation resourceLocation) {
        return source.get(resourceLocation);
    }
    public Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> collect() {
        Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> result = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<ResourceLocation, AbstractCelestialBodyData> entry : source.entrySet()) {
            if ( predicate.test(entry.getValue()) ) { result.put(entry.getKey(), entry.getValue()); }
        }
        return result;
    }

    /** 天体指代的维度；只有行星与环世界区段有，其余返回 null（null 永远不会等于任何维度名） */
    @Nullable private static String dimensionOf(AbstractCelestialBodyData data) {
        if (data instanceof PlanetData planet) { return planet.getDimension(); }
        if (data instanceof RingWorldSectionData section) { return section.getDimension(); }
        return null;
    }
}
