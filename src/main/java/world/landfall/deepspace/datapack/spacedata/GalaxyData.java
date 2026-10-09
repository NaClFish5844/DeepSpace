package world.landfall.deepspace.datapack.spacedata;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3i;

import java.util.ArrayList;

@Getter @Setter
public class GalaxyData {

    public static final String TYPE = "galaxy";

    @SerializedName("type") private String type;
    @SerializedName("dimension") private String dimension;
    @SerializedName("name") private String name;
    @SerializedName("mass_center") private Vector3i mass_center;

    @SerializedName("central_bodies") private ArrayList<OrbitData> centralBodies;
    @SerializedName("orbiters") private ArrayList<OrbitData> orbiters;
    @SerializedName("teleporters") private ArrayList<OrbitData> teleporters;

    private transient String galaxyId;
    private transient String packName;

    private GalaxyData() {
        this.type = TYPE;
        this.dimension = "";
        this.name = "";
        this.mass_center = new Vector3i();
        this.centralBodies = new ArrayList<>();
        this.orbiters = new ArrayList<>();
        this.teleporters = new ArrayList<>();
        this.galaxyId = "";
        this.packName = "";
    }
    public static GalaxyData ofDefault() {
        return new GalaxyData();
    }
}
