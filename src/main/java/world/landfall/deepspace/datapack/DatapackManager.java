package world.landfall.deepspace.datapack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import world.landfall.deepspace.datapack.load.DatapackLoader;
import world.landfall.deepspace.datapack.query.CelestialBodyQuery;
import world.landfall.deepspace.datapack.query.GalaxyQuery;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.query.HyperlaneQuery;
import world.landfall.deepspace.datapack.spacedata.galaxy.GalaxyData;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlaneData;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlanePoint;

import static world.landfall.deepspace.util.AccessUtil.*;


@Getter
public class DatapackManager {
    public static final DatapackManager INSTANCE = new DatapackManager();

    private final Object2ObjectOpenHashMap<ResourceLocation, AbstractCelestialBodyData> CELESTIAL_BODY_MANAGER = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<ResourceLocation, GalaxyData> GALAXY_MANAGER = new Object2ObjectOpenHashMap<>();

    private final ObjectList<HyperlaneData> HYPER_LINE_MANAGER = new ObjectArrayList<>();
    private final Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> ADJACENT_LIST = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> REV_ADJACENT_LIST = new Object2ObjectOpenHashMap<>();

    public void reloadDatapack() {
        CELESTIAL_BODY_MANAGER.clear();
        GALAXY_MANAGER.clear();
        HYPER_LINE_MANAGER.clear();
        ADJACENT_LIST.clear();
        REV_ADJACENT_LIST.clear();

        // 一轮加载后立刻落地 —— 二轮要读的引用表必须先就位（Checker 直接查 INSTANCE）
        CELESTIAL_BODY_MANAGER.putAll( DatapackLoader.loadCelestialBodyData() );
        GALAXY_MANAGER.putAll( DatapackLoader.loadGalaxyData() );
        // 超空间航道是合并语义：全部数据包的边都收，不取「生效的那份」
        HYPER_LINE_MANAGER.addAll( DatapackLoader.loadHyperlaneData() );

        // 二轮校验：就地清洗刚落地的那份数据
        DatapackLoader.filterCelestialBodyData();
        DatapackLoader.filterGalaxyData();
        DatapackLoader.filterHyperLaneData();

        // 剪枝之后才建邻接表：表里不该留下任何会被摘掉的边
        DatapackLoader.indexHyperLaneAdjacency();

        getLogger().info(
                "空间数据加载完成：{} 个天体，{} 个星系，{} 个超空间航道条目 | space data loaded: {} celestial bodies, {} galaxies, {} hyperlanes",
                CELESTIAL_BODY_MANAGER.size(), GALAXY_MANAGER.size(), HYPER_LINE_MANAGER.size(),
                CELESTIAL_BODY_MANAGER.size(), GALAXY_MANAGER.size(), HYPER_LINE_MANAGER.size()
        );
    }
    public void saveGeneratedData() {
        // 在数据生成时触发增量保存
        // 在服务器关闭时触发全量保存
        // WIP
    }

    /** 数据访问一律走这两个查询 */
    public GalaxyQuery galaxyQuery() {
        return new GalaxyQuery(GALAXY_MANAGER);
    }
    public CelestialBodyQuery celestialBodyQuery() {
        return new CelestialBodyQuery(CELESTIAL_BODY_MANAGER);
    }
    public HyperlaneQuery hyperlaneQuery() {
        return new HyperlaneQuery(HYPER_LINE_MANAGER, ADJACENT_LIST, REV_ADJACENT_LIST);
    }

}
