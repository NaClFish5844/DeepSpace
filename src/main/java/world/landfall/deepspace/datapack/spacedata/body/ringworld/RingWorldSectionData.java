package world.landfall.deepspace.datapack.spacedata.body.ringworld;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;
import world.landfall.deepspace.datapack.spacedata.body.AbstractCelestialBodyData;

import java.util.ArrayList;

/**
 * 环世界区段：环的一段，本身是一个天体（有自己的维度、可登陆）。
 * <p>
 * 位置不在这里：段的中心点由「环的环绕半径 + 序号 + 倾斜」推导，不需要手写。
 */
@Getter @Setter
public class RingWorldSectionData extends AbstractCelestialBodyData {

    @SerializedName("index") private int index;
    @SerializedName("dimension") private String dimension;
    @SerializedName("healthy") private boolean healthy;
    @SerializedName("repairable") private boolean repairable;
    @SerializedName("texture") private String texture;
    @SerializedName("textures") private ArrayList<String> textures;
    @SerializedName("space_height") private int spaceHeight;
    @SerializedName("reentry_height") private int reentryHeight;

    private RingWorldSectionData() {
        super();
        setType(CelestialBodyType.ring_world_section);

        this.index = -1;
        this.dimension = "";
        this.healthy = true;
        this.repairable = false;
        this.texture = "";
        this.textures = new ArrayList<>();
        this.spaceHeight = 550;
        this.reentryHeight = 400;
    }
    public static RingWorldSectionData ofDefault() {
        return new RingWorldSectionData();
    }
    public static RingWorldSectionData ofBasic(AbstractCelestialBodyData base) {
        RingWorldSectionData data = new RingWorldSectionData();
        data.copyBaseFrom(base);
        return data;
    }
}
