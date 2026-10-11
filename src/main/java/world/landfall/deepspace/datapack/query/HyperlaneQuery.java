package world.landfall.deepspace.datapack.query;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlaneData;
import world.landfall.deepspace.datapack.spacedata.hyperlane.HyperlanePoint;

/**
 * 超空间航道的只读查询：以正反两张邻接表为基础，提供邻居检索与寻路。
 */
public class HyperlaneQuery {
    private final ObjectList<HyperlaneData> edges;
    private final Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> adjacentList;
    private final Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> revAdjacentList;

    public HyperlaneQuery(ObjectList<HyperlaneData> edges,
                          Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> adjacentList,
                          Object2ObjectOpenHashMap<HyperlanePoint, ObjectSet<HyperlanePoint>> revAdjacentList) {
        this.edges = edges;
        this.adjacentList = adjacentList;
        this.revAdjacentList = revAdjacentList;
    }


    public ObjectSet<HyperlanePoint> linksTo(HyperlanePoint from) {
        return adjacentList.getOrDefault(from, ObjectSets.emptySet());
    }
    public ObjectSet<HyperlanePoint> linksFrom(HyperlanePoint to) {
        return revAdjacentList.getOrDefault(to, ObjectSets.emptySet());
    }

    public boolean isRandomSource(HyperlanePoint from) {
        for (HyperlaneData edge : edges) {
            if ( edge.isRandomLink() && from.equals(edge.getFrom()) ) { return true; }
        }
        return false;
    }

    /**
     * 两点之间的最少跳数；不可达返回 -1，同一个点返回 0。
     * <p>
     * BFS搜索
     */
    public int getDistance(HyperlanePoint from, HyperlanePoint to) {
        if (from == null || to == null) { return -1; }
        if ( from.equals(to) ) { return 0; }

        Object2IntOpenHashMap<HyperlanePoint> distance = new Object2IntOpenHashMap<>();
        ObjectArrayList<HyperlanePoint> queue = new ObjectArrayList<>();

        distance.put(from, 0);
        queue.add(from);

        for (int head = 0; head < queue.size(); head++) {
            HyperlanePoint current = queue.get(head);
            int next = distance.getInt(current) + 1;

            for (HyperlanePoint neighbor : linksTo(current)) {
                if ( distance.containsKey(neighbor) ) { continue; }
                if ( neighbor.equals(to) ) { return next; }

                distance.put(neighbor, next);
                queue.add(neighbor);
            }
        }
        return -1;
    }
}
