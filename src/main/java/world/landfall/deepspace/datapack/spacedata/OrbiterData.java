package world.landfall.deepspace.datapack.spacedata;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;
import world.landfall.deepspace.datapack.spacedata.body.ringworld.RingWorldBodyData.RingWorldScale;

import javax.annotation.Nullable;

/**
 * 环绕关系：一条「谁围绕谁、在哪个位置、朝哪边」的引用。
 * <p>
 * 卫星、星系中心天体、环绕天体、传送点四处的写法完全同构，因此共用这一个数据类。
 * 被引用的天体本体不在里面 —— body 只写引用，本体在它自己的文件里。
 */
@Getter @Setter
public class OrbiterData {
    /** 域内唯一标识：同一个星系里 central_bodies / orbiters / teleporters 各自的 id 可重复，域内不行。-1 表示没写 */
    @SerializedName("id") private int id;
    @SerializedName("body") private String body;
    @SerializedName("offset") private Vector3d offset;
    @SerializedName("normal") private Vector3d normal;

    /**
     * 引用目标是星环时记下它的规模，否则为 null。
     * <p>
     * 运行时不参与 JSON —— 类型不在本类里写死，而是由二轮按 body 指向的对象判定后注入
     * （见 DatapackChecker.ORBITER_VALID_TYPES）。规模一并留着，因为「每种规模的星环最多一个」要用它。
     */
    @Nullable private transient RingWorldScale ringScale;

    /** 供工厂方法用：三个字段一次写全（id / body 取默认值） */
    private OrbiterData(Vector3d normal, Vector3d offset, @Nullable RingWorldScale ringScale) {
        this.id = -1;
        this.body = "";
        this.offset = offset;
        this.normal = normal;
        this.ringScale = ringScale;
    }

    public OrbiterData(Vector3d normal, Vector3d offset) {
        this(normal, offset, null);
    }
    public static OrbiterData ofDefaultBody() {
        return new OrbiterData(new Vector3d(0.0D, 1.0D, 0.0D), new Vector3d());
    }
    /**
     * 传送点的默认环绕体：姿态指向质心，而不是像别的环绕体那样朝上。
     * <p>
     * 法线 = 偏移方向的反向单位向量 —— 姿态是静态倾斜（无自转），所以「面朝质心」就等于
     * 「面朝自己与质心连线的反向」。缺省 normal 为 [0,1,0] 那条规则是给其余环绕体的，
     * 传送点这里是唯一的例外。
     *
     * @param offset 该传送点相对质心的偏移
     * @return 姿态朝向质心的环绕体；偏移为零（或长度可忽略）时退回默认的 [0,1,0]
     */
    @Nullable public static OrbiterData ofDefaultTeleporter(Vector3d offset) {
        if (offset == null) { return null; }

        double length = offset.length();
        if (length < 1.0E-6D) { return new OrbiterData(new Vector3d(0.0D, 1.0D, 0.0D), new Vector3d(offset)); }

        Vector3d toMassCenter = new Vector3d(offset).mul(-1.0D / length);
        return new OrbiterData(toMassCenter, new Vector3d(offset));
    }

    /** 引用的目标是不是星环 */
    public boolean isRing() { return this.ringScale != null; }
    @Nullable public ResourceLocation getResourceLocation() {
        return ResourceLocation.tryParse(this.body);
    }
}
