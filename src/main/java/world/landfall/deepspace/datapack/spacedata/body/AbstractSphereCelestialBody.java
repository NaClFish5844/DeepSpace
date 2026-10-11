package world.landfall.deepspace.datapack.spacedata.body;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

/**
 * 球状天体：有体积、能描述的圆球 —— 恒星与行星继承它。
 * <p>
 * 环世界体与区段、传送点都【不是】球状天体，所以它们直接继承抽象天体：
 * 传送点判定走「中心点 + 传送距离」，环的体积由几何推导，都不适用 AABB。
 */
@Getter @Setter
public abstract class AbstractSphereCelestialBody extends AbstractCelestialBodyData {

    /** 未旋转的 AABB 体积，判定「碰到了没有」用（不是位置） */
    @SerializedName("size") private Vector3d size;
    @SerializedName("description") private String description;

    protected AbstractSphereCelestialBody() {
        this.size = new Vector3d(1.0D, 1.0D, 1.0D);
        this.description = "";
    }
}
