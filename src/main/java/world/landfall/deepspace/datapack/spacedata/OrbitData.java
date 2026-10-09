package world.landfall.deepspace.datapack.spacedata;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;
import org.joml.Vector3i;

/**
 * 环绕关系：一条「谁围绕谁、在哪个位置、朝哪边」的引用。
 * <p>
 * 卫星、星系中心天体、环绕天体、传送点四处的写法完全同构，因此共用这一个数据类。
 * 被引用的天体本体不在里面 —— body 只写引用，本体在它自己的文件里。
 */
@Getter @Setter
public class OrbitData {

    /** 环绕物的种类；只有星系的 orbiters 需要区分星球 / 星环，其余位置留 invalid */
    public enum OrbitType {
        planet,
        ring,
        invalid
    }

    @SerializedName("body") private String body;
    @SerializedName("type") private OrbitType type;
    @SerializedName("offset") private Vector3i offset;
    @SerializedName("normal") private Vector3d normal;

    public OrbitData() {
        this.body = "";
        this.offset = new Vector3i();
        this.normal = new Vector3d(0.0D, 1.0D, 0.0D);
        this.type = OrbitType.invalid;
    }
    public static OrbitData ofDefault() {
        return new OrbitData();
    }

    public boolean isRing() {
        return this.type == OrbitType.ring;
    }
    public boolean isPlanet() {
        return this.type == OrbitType.planet;
    }
}
