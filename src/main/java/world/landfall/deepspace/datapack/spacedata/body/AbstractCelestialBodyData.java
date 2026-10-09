package world.landfall.deepspace.datapack.spacedata.body;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

/**
 * 抽象天体：所有可被星系引用的天体共有的字段。
 * <p>
 * 身份由文件位置（namespace:path）决定，id 只作短名与显示用。
 * 位置不在这里：天体自己声明尺寸，位置一律由引用它的星系给出。
 */
@Getter @Setter
public abstract class AbstractCelestialBodyData {

    /** 天体种类；JSON 里的 type 字段，解析时据此分派到具体子类。
     *  枚举名与 JSON 值保持一致（小写 + 下划线），因此不需要额外的 @SerializedName */
    public enum CelestialBodyType {
        star,
        planet,
        teleporter,
        ring_world_section,
        invalid
    }

    @SerializedName("name") private String name;
    @SerializedName("id") private String id;
    @SerializedName("type") private CelestialBodyType type;
    @SerializedName("size") private Vector3d size;

    private transient String packName;

    protected AbstractCelestialBodyData() {
        this.name = "";
        this.id = "";
        this.type = CelestialBodyType.invalid;
        this.size = new Vector3d(1.0D, 1.0D, 1.0D);
        this.packName = "";
    }
    protected void copyBaseFrom(AbstractCelestialBodyData base) {
        if (base == null) { return; }

        this.name = base.name;
        this.id = base.id;
        this.type = base.type;
        this.size = base.size;
        this.packName = base.packName;
    }
}
