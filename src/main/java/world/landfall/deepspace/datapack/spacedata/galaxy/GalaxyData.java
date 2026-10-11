package world.landfall.deepspace.datapack.spacedata.galaxy;

import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;
import world.landfall.deepspace.datapack.spacedata.OrbiterData;

import javax.annotation.Nullable;

import static world.landfall.deepspace.util.AccessUtil.getLogger;

@Getter @Setter
public class GalaxyData {

    public static final String TYPE = "galaxy";

    @SerializedName("type") private String type;
    @SerializedName("dimension") private String dimension;
    @SerializedName("name") private String name;
    @SerializedName("mass_center") private Vector3d mass_center;

    // *不要*去掉 transient
    // 这东西在json里是数组 不能直接GSON反序列化
    @SerializedName("central_bodies") private transient Object2ObjectOpenHashMap<Integer, OrbiterData> centralBodies;
    @SerializedName("orbiters") private transient Object2ObjectOpenHashMap<Integer, OrbiterData> orbiters;
    @SerializedName("teleporters") private transient Object2ObjectOpenHashMap<Integer, OrbiterData> teleporters;

    private transient String packName;
    private transient ResourceLocation dataId;
    private transient boolean isGenerated;

    /**
     * 某个环绕体在星系空间里的坐标 —— 即「质心 + 该环绕体的偏移」。
     * <p>
     * 天体自己不写位置，位置由引用它的环绕体给出，所以这是从「域内 id」问到实际坐标的唯一入口。
     * <p>
     * 三个集合的 id 各自独立编号、互不干扰（同一个星系里可以有三个 id: 0），所以必须指明是哪个集合，
     * 否则同一个 id 会指向三个不同的位置。
     *
     * @param id    环绕体的域内 id
     * @param field 集合名，取值与 JSON 键一致：central_bodies / orbiters / teleporters
     * @return 星系空间坐标；id 在该集合里不存在、或 field 不是上面三个之一时返回 null
     */
    @Nullable public Vector3d getCenterOf(int id, String field) {
        OrbiterData orbiter = findOrbiter(id, field);
        if (orbiter == null) { return null; }

        Vector3d offset = orbiter.getOffset();
        Vector3d center = new Vector3d(mass_center);

        return (offset == null) ? center : center.add(offset);
    }


    private GalaxyData() {
        this.type = TYPE;
        this.dimension = "";
        this.name = "";
        this.mass_center = new Vector3d();
        this.centralBodies = new Object2ObjectOpenHashMap<>();
        this.orbiters = new Object2ObjectOpenHashMap<>();
        this.teleporters = new Object2ObjectOpenHashMap<>();
        this.packName = "";
        this.dataId = null;
    }
    public static GalaxyData ofDefault() {
        return new GalaxyData();
    }

    /** 按「集合名 + 域内 id」找环绕体；集合名与 JSON 键一致 */
    @Nullable private OrbiterData findOrbiter(int id, String field) {
        Object2ObjectOpenHashMap<Integer, OrbiterData> orbiters = switch (field) {
            case "central_bodies" -> centralBodies;
            case "orbiters"       -> this.orbiters;
            case "teleporters"    -> teleporters;
            default               -> null;
        };
        if (orbiters == null) {
            getLogger().warn("未知的集合名：{}（应为 central_bodies / orbiters / teleporters） | unknown orbiter collection: {}", field, field);
            return null;
        }
        return orbiters.get(id);
    }
}
