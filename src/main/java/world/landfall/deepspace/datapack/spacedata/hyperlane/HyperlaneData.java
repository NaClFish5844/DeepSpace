package world.landfall.deepspace.datapack.spacedata.hyperlane;

import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * 超空间连接图里的一条连接（一条边）。
 * <p>
 * 信标（GalaxyTeleporterData）只描述「显示与物理设置」，谁通向谁完全由这些边决定 ——
 * 这样单向、随机这类「推导不出来」的关系才有地方表达。
 * <p>
 * to 为 null 表示随机连接：从 from 进去会被随机发射（发射范围待定）。
 */
@Getter @Setter
public class HyperlaneData {

    private HyperlanePoint from;
    @Nullable private HyperlanePoint to;

    public HyperlaneData(HyperlanePoint from, @Nullable HyperlanePoint to) {
        this.from = from;
        this.to = to;
    }
    public static HyperlaneData of(HyperlanePoint from, @Nullable HyperlanePoint to) {
        return new HyperlaneData(from, to);
    }

    /** 单向：只能从 from 到 to */
    public static HyperlaneData ofUniDir(HyperlanePoint from, HyperlanePoint to) {
        return new HyperlaneData(from, to);
    }
    /** 双向：展开成两条互为反向的边 */
    public static Pair<HyperlaneData, HyperlaneData> ofBiDir(HyperlanePoint point1, HyperlanePoint point2) {
        return Pair.of(
                new HyperlaneData(point1, point2),
                new HyperlaneData(point2, point1)
        );
    }
    /** 随机连接：没有终点 */
    public static HyperlaneData ofRandomLink(HyperlanePoint from) {
        return new HyperlaneData(from, null);
    }

    /** 是不是随机连接（没有终点） */
    public boolean isRandomLink() { return this.to == null; }
}
