package world.landfall.deepspace.datapack.spacedata.body.ringworld;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;

/**
 * 环世界体：一个完整的环，由若干区段拼成。
 * <p>
 * 它继承抽象天体，因此 name / id / type 这些共性字段都由基类提供；
 * 但它【没有 size】—— 体积由各区段给出 —— 也不自己写位置（位置由引用它的星系给出）。
 * <p>
 * 区段不由本文件声明：环在 ringworlds/&lt;环名&gt;/ 下独占一个目录，
 * 其中与目录同名的文件是本体，其余文件一律视为区段；
 * 区段按各自的 index 升序取前 n 个（n 由 scale 决定），不足则循环重复补齐。
 */
@Getter @Setter
public class RingWorldBodyData extends AbstractCelestialBodyData {

    /**
     * 规模：决定段数与模型面数。
     * 牺牲自由度换取简化 —— 因此暂时不允许自定义几何模型或碰撞箱。
     */
    public enum RingWorldScale {
        small(2, 4),
        medium(4, 4),
        large(8, 8),
        huge(16, 8);

        private final int sectionCount;
        private final int faceCount;

        RingWorldScale(int sectionCount, int faceCount) {
            this.sectionCount = sectionCount;
            this.faceCount = faceCount;
        }


        public int sectionCount() {
            return this.sectionCount;
        }
        public int faceCount() {
            return this.faceCount;
        }
    }

    public static final String TYPE = "ring_world";

    @SerializedName("scale") private RingWorldScale scale;
    @SerializedName("orbit_radius") private int orbitRadius;

    private RingWorldBodyData() {
        super();
        setType(CelestialBodyType.ring_world);

        this.scale = RingWorldScale.medium;
        this.orbitRadius = 0;
    }

    public static RingWorldBodyData ofDefault() {
        return new RingWorldBodyData();
    }
}
