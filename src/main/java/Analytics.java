import robot.World;

import java.util.LinkedList;
import java.util.List;

public class Analytics {
    private final Cell[][] cells;
    private final World world;
    private final List<Path> paths = new LinkedList<>();

    public Analytics(World world) {
        this.cells = new Cell[world.getN()][world.getN()];
        this.world = world;
    }

    public void addPath(Path path) {
        paths.add(path
        );
    }

    public List<Path> getPaths() {
        return paths;
    }

    public Cell getCellOrCreate(Position pos) {
        return getCellOrCreate(pos.getX(), pos.getY());
    }

    public Cell getCellOrCreate(int x, int y) {
        Cell cell = getCell(x, y);
        if (cell == null) {
            cell = new Cell();
            cells[y][x] = cell;
        }
        return cells[y][x];
    }

    public Cell getCell(int x, int y) {
        return cells[y][x];
    }

    public class Cell {
        public static float maxCount = 0;
        public static float maxCost = 0;

        public boolean hotSpot;
        public int material;
        public int time;
        public int moveCount;
        public int generation;
        public int lastCost;

        public void onEnter(Position point, boolean hotSpot) {
            if (moveCount == 0) {
                time = world.getFieldTime(point.getX(), point.getY());
            }
            material = Math.max(world.getFieldMaterials(point.getX(), point.getY()), material);
            this.hotSpot |= hotSpot;
            moveCount++;
            maxCount = Math.max(moveCount, maxCount);
        }

        public void onUse(int generation, int lastCost) {
            this.generation = generation;
            this.lastCost = lastCost;
            maxCost = Math.max(maxCost, lastCost);
        }
    }
}
