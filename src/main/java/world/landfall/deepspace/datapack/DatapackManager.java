package world.landfall.deepspace.datapack;

import world.landfall.deepspace.datapack.query.CelestialBodyQuery;
import world.landfall.deepspace.datapack.query.GalaxyQuery;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.spacedata.GalaxyData;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;

import static world.landfall.deepspace.util.AccessUtil.*;


@Getter
public class DatapackManager {
    public static final DatapackManager INSTANCE = new DatapackManager();

    private final Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> CELESTIAL_BODY_MANAGER = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> GALAXY_MANAGER = new Object2ObjectOpenHashMap<>();

    public void reloadDatapack() {
        CELESTIAL_BODY_MANAGER.clear();
        GALAXY_MANAGER.clear();

        // 一轮加载：校验必填 → 反序列化 → 子块过滤填充
        Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> celestialBodyBuffer = DatapackLoader.loadCelestialBodyData();
        Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> galaxyBuffer = DatapackLoader.loadGalaxyData();

        // 二轮校验：引用合法性 / 字段交叉 / 复杂 type 规则，不通过的在这里被摘掉
        celestialBodyBuffer = DatapackLoader.filterCelestialBodyData(celestialBodyBuffer);
        galaxyBuffer = DatapackLoader.filterGalaxyData(galaxyBuffer, celestialBodyBuffer);

        CELESTIAL_BODY_MANAGER.putAll(celestialBodyBuffer);
        GALAXY_MANAGER.putAll(galaxyBuffer);

        getLogger().info(
                "空间数据加载完成：{} 个天体，{} 个星系 | space data loaded: {} celestial bodies, {} galaxies",
                CELESTIAL_BODY_MANAGER.size(), GALAXY_MANAGER.size(),
                CELESTIAL_BODY_MANAGER.size(), GALAXY_MANAGER.size()
        );
    }

    public GalaxyQuery galaxyQuery() {
        return new GalaxyQuery(GALAXY_MANAGER);
    }
    public CelestialBodyQuery celestialBodyQuery() {
        return new CelestialBodyQuery(CELESTIAL_BODY_MANAGER);
    }

}
