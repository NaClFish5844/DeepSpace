package world.landfall.deepspace.datapack.query;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.spacedata.OrbiterData;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;
import world.landfall.deepspace.datapack.spacedata.galaxy.GalaxyData;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 星系的链式只读查询。
 */
public class GalaxyQuery {
    private final Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> source;
    private Predicate<GalaxyData> predicate = data -> true;

    public GalaxyQuery(Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> source) {
        this.source = source;
    }

    public GalaxyQuery ofDimension(String dimension) {
        predicate = predicate.and(data -> Objects.equals(data.getDimension(), dimension));
        return this;
    }
    public GalaxyQuery ofDimension(ResourceLocation dimension) {
        return ofDimension(dimension.toString());
    }

    /**
     * 只留引用了这个天体的星系。
     * <p>
     * 天体自己不写位置，位置由引用它的星系给出 —— 所以「这个天体在哪个星系里」只能反过来查引用：
     * 三个环绕体集合里只要有一处指向它，这个星系就算数。同一份天体文件可以被多个星系引用（跨星系复用），
     * 因此结果可能是多个。
     */
    public GalaxyQuery ofCelestialBody(AbstractCelestialBodyData celestialBody) {
        ResourceLocation target = (celestialBody == null) ? null : celestialBody.getDataId();
        if (target == null) { return this; }

        predicate = predicate.and(data -> refersTo(data.getCentralBodies(), target)
                                        || refersTo(data.getOrbiters(), target)
                                        || refersTo(data.getTeleporters(), target));
        return this;
    }

    public GalaxyQuery ofPackName(String packName) {
        predicate = predicate.and(data -> Objects.equals(data.getPackName(), packName));
        return this;
    }

    @Nullable public GalaxyData get(ResourceLocation resourceLocation) {
        return source.get(resourceLocation);
    }
    public Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> collect() {
        Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> result = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<ResourceLocation, GalaxyData> entry : source.entrySet()) {
            if ( predicate.test(entry.getValue()) ) { result.put(entry.getKey(), entry.getValue()); }
        }
        return result;
    }

    /** 两个星系在 connectedGalaxies 无向图里的距离；还没有连通信息，先返回 -1 */
    public int getDistance(ResourceLocation galaxy1, ResourceLocation galaxy2) {
        // 根据 GalaxyData.connectedGalaxies 搜索无向图中这俩玩意的距离
        return -1;
    }

    /** 这一个集合里有没有环绕体指向目标天体 */
    private static boolean refersTo(Object2ObjectOpenHashMap<Integer, OrbiterData> orbiters, ResourceLocation target) {
        if (orbiters == null || orbiters.isEmpty()) { return false; }

        for (OrbiterData orbiter : orbiters.values()) {
            if ( target.equals(orbiter.getResourceLocation()) ) { return true; }
        }
        return false;
    }
}
