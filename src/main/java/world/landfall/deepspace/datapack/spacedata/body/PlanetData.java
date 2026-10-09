package world.landfall.deepspace.datapack.spacedata.body;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2i;
import world.landfall.deepspace.datapack.spacedata.OrbitData;

import java.util.ArrayList;

/**
 * 行星：可以登陆的天体。
 * <p>
 * 两套尺寸不要混淆：
 *   size        —— 星系空间里的体积，用于「判定」（碰到了没有）
 *   landingArea —— 表面维度里的可用矩形，用于「映射」（落到哪 / 从哪飞出来）
 */
@Getter @Setter
public class PlanetData extends AbstractCelestialBodyData {
    public enum DecorationType {
        atmosphere,
        rings,
        asteroids,
        invalid
    }

    @Getter @Setter public static class DecorationData {

        @SerializedName("type") private DecorationType type;
        @SerializedName("scale") private float scale;
        @SerializedName("color") private int color;

        private DecorationData() {
            this.type = DecorationType.invalid;
            this.scale = 1.0F;
            this.color = 0;
        }

        public static DecorationData ofDefault() {
            return new DecorationData();
        }
    }

    @SerializedName("dimension") private String dimension;
    @SerializedName("landing_area") private Vector2i landingArea;
    @SerializedName("description") private String description;
    @SerializedName("texture") private String texture;
    @SerializedName("textures") private ArrayList<String> textures;
    @SerializedName("space_height") private int spaceHeight;
    @SerializedName("reentry_height") private int reentryHeight;
    @SerializedName("texture_generation_detail") private String textureGenerationDetail;

    @SerializedName("decorations") private ArrayList<DecorationData> decorations;
    // 卫星与星系里的环绕关系同构，共用 OrbitData
    @SerializedName("moons") private ArrayList<OrbitData> moons;

    private PlanetData() {
        super();
        setType(CelestialBodyType.planet);

        this.dimension = "";
        this.landingArea = new Vector2i(2000, 2000);
        this.description = "";
        this.texture = "";
        this.textures = new ArrayList<>();
        this.spaceHeight = 550;
        this.reentryHeight = 400;
        this.textureGenerationDetail = "";

        this.decorations = new ArrayList<>();
        this.moons = new ArrayList<>();
    }
    public static PlanetData ofDefault() {
        return new PlanetData();
    }
    public static PlanetData ofBasic(AbstractCelestialBodyData base) {
        PlanetData data = new PlanetData();
        data.copyBaseFrom(base);
        return data;
    }
}
