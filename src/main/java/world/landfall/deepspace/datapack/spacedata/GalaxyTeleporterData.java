package world.landfall.deepspace.datapack.spacedata;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@Getter @Setter
public class GalaxyTeleporterData extends OrbiterData {

    @SerializedName("dimension") private String dimension;

    public GalaxyTeleporterData() {
        super();
        this.dimension = "";
    }
    public static GalaxyTeleporterData ofDefault() {
        return new GalaxyTeleporterData();
    }

    @Nullable public ResourceLocation getDimensionId() {
        return ResourceLocation.tryParse(this.dimension);
    }

}
