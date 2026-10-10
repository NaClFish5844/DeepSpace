package world.landfall.deepspace.datapack.query;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.spacedata.GalaxyData;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 星系的链式只读查询。
 * <p>
 * 与 CelestialBodyQuery 同构：ofXxx 追加条件，collect() 遍历一次。
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
}
