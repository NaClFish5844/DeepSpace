package world.landfall.deepspace.datapack.spacedata.body;

import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;
import world.landfall.deepspace.datapack.spacedata.OrbiterData;

/**
 * 行星：可以登陆的天体。
 * <p>
 * 两套尺寸不要混淆：
 *   size        —— 星系空间里的体积，用于「判定」（碰到了没有）
 *   landingArea —— 表面维度里的可用矩形，用于「映射」（落到哪 / 从哪飞出来）
 */
@Getter @Setter
public class PlanetData extends AbstractSphereCelestialBody {
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
    @SerializedName("landing_area") private Vector2d landingArea;
    @SerializedName("space_height") private int spaceHeight;
    @SerializedName("reentry_height") private int reentryHeight;
    @SerializedName("texture") private String texture;
    @SerializedName("textures") private ObjectList<String> textures;
    @SerializedName("texture_generation_detail") private String textureGenerationDetail;

    @SerializedName("decorations") private ObjectList<DecorationData> decorations;
    // *不要*去掉 transient
    // 这东西在json里是数组 不能直接GSON反序列化
    @SerializedName("moons") private transient Object2ObjectOpenHashMap<Integer, OrbiterData> moons;

    private PlanetData() {
        super();
        setType(CelestialBodyType.planet);

        this.dimension = "";
        this.landingArea = new Vector2d(2000.0D, 2000.0D);
        this.texture = "";
        this.textures = new ObjectArrayList<>();
        this.spaceHeight = 550;
        this.reentryHeight = 400;
        this.textureGenerationDetail = "";

        this.decorations = new ObjectArrayList<>();
        this.moons = new Object2ObjectOpenHashMap<>();
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
