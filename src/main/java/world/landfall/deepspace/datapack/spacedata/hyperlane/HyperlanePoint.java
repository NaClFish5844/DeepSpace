package world.landfall.deepspace.datapack.spacedata.hyperlane;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record HyperlanePoint(
        @SerializedName("galaxy_dimension") String galaxy,
        @SerializedName("teleporter_id") int teleporterID)
{
    public static HyperlanePoint ofRandom(int teleporterID) {
        return new HyperlanePoint("RANDOM", teleporterID);
    }
    @Nullable public ResourceLocation getGalaxyId() {
        return ResourceLocation.tryParse(this.galaxy);
    }
}
