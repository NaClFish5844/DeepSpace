package world.landfall.deepspace.datapack.spacedata.hyperlane;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * 超空间连接图的文件形态：data/&lt;namespace&gt;/hyperlane.json 的顶层结构。
 * <p>
 * 三个列表对应临时文件里定的三类连接。加载时会展开成一条条 {@link HyperlaneData}：
 * 双向的每一对展开成两条互为反向的边，随机的终点为空（isRandomLink() 为 true）。
 */
@Getter @Setter
public class HyperlaneGraph {

    /** 双向连接：两端互通 */
    @SerializedName("bidirectional_links") private ArrayList<BiLinkData> bidirectionalLinks;
    /** 单向连接：只能从 from 到 to */
    @SerializedName("unidirectional_links") private ArrayList<HyperlaneData> unidirectionalLinks;
    /** 随机连接：这些点进去会被随机发射（发射范围待定） */
    @SerializedName("random_links") private ArrayList<HyperlanePoint> randomLinks;

    public HyperlaneGraph() {
        this.bidirectionalLinks = new ArrayList<>();
        this.unidirectionalLinks = new ArrayList<>();
        this.randomLinks = new ArrayList<>();
    }
    public static HyperlaneGraph ofDefault() { return new HyperlaneGraph(); }

    /** 双向连接的一对端点 */
    public record BiLinkData(
            @SerializedName("point_a") HyperlanePoint pointA,
            @SerializedName("point_b") HyperlanePoint pointB) {}
}
