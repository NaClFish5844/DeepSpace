package world.landfall.deepspace.datapack.spacedata.body;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

/**
 * 传送点：超空间中继 / 虫洞。
 * <p>
 * 注意：此物不适用 AABB 式判定，否则玩家会撞上看不见的墙。
 * 判定改用「中心点 + 传送距离」；中心点由引用它的星系给出，这里只声明形状与行为。
 */
@Getter @Setter
public class TeleporterData extends AbstractCelestialBodyData {
    public enum TeleporterType {
        relay,
        wormhole,
        invalid
    }

    @SerializedName("teleporter_type") private TeleporterType teleporterType;
    @SerializedName("teleport_distance") private int teleportDistance;

    private TeleporterData() {
        super();
        setType(CelestialBodyType.teleporter);

        this.teleporterType = TeleporterType.invalid;
        this.teleportDistance = 128;
    }
    public static TeleporterData ofDefault() {
        return new TeleporterData();
    }
    public static TeleporterData ofBasic(AbstractCelestialBodyData base) {
        TeleporterData data = new TeleporterData();
        data.copyBaseFrom(base);
        return data;
    }
}
