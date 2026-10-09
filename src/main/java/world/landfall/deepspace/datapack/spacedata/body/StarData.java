package world.landfall.deepspace.datapack.spacedata.body;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

/**
 * 恒星：星系里的发光中心天体。
 * <p>
 * 颜色解析顺序：显式 color > 光谱型推导 > 兜底白。
 */
@Getter @Setter
public class StarData extends AbstractCelestialBodyData {

    @SerializedName("hurt_radius") private double hurtRadius;
    @SerializedName("spectral_class") private String spectralClass;
    @SerializedName("color") private JsonElement color;

    private StarData() {
        super();
        setType(CelestialBodyType.star);

        this.hurtRadius = 0.0D;
        this.spectralClass = "G";
        this.color = JsonNull.INSTANCE;
    }
    public static StarData ofDefault() {
        return new StarData();
    }
    public static StarData ofBasic(AbstractCelestialBodyData base) {
        StarData data = new StarData();
        data.copyBaseFrom(base);
        return data;
    }
}
