
import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Function;

import robot.Factory;
import robot.Robot;
import robot.World;


public class RobotUI {

    public RobotUI(int n, World world, Robot robot, Factory factory, Analytics analytics) {
        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame("Robot UI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new GridPanel(n, world, robot, factory, analytics));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static class GridPanel extends JPanel {

        public static Font FONT = new Font(Font.DIALOG, Font.PLAIN, 18);
        public static int TIME_SHADES = 6;
        public static int MATERIAL_SHADES = 4;
        public static int FREQUENCY_SHADES = 4;
        public static int COST_SHADES = 16;
        public static boolean DEBUG = false;

        private final int n;
        private final World world;
        private final Robot robot;
        private final Factory factory;
        private final Color[] timeColor;
        private final Color[] materialColor;
        private final Color[] frequencyColor;
        private final Color[] costColor;

        private final Analytics analytics;

        public GridPanel(int n, World world, Robot robot, Factory factory, Analytics analytics) {
            this.n = n;
            this.world = world;
            this.robot = robot;
            this.factory = factory;
            this.analytics = analytics;
            timeColor = createShades(TIME_SHADES, Color.GREEN, new Color(0x276235));
            materialColor = createShades(MATERIAL_SHADES, Color.CYAN, Color.BLUE);
            frequencyColor = createShades(FREQUENCY_SHADES, new Color(0xcfc91f), new Color(0xa13c20));
            costColor = createShades(COST_SHADES, new Color(0x2A07B8), new Color(0x8124BA));
        }

        private Color[] createShades(int count, Color start, Color end) {
            float[] startValue = new float[4];
            float[] endValue = new float[4];
            start.getComponents(startValue);
            end.getComponents(endValue);
            float[] colorDiff = new float[4];
            for (int i = 0; i < 4; i++) {
                colorDiff[i] = (endValue[i] - startValue[i]) / count;
            }
            Function<Integer, Color> createShade = (m) ->
                    new Color(startValue[0] + colorDiff[0] * m,
                            startValue[1] + colorDiff[1] * m,
                            startValue[2] + colorDiff[2] * m,
                            startValue[3] + colorDiff[3] * m);
            Color[] colorShades = new Color[count];
            for (int i = 0; i < count; i++) {
                colorShades[i] = createShade.apply(i);
            }
            return colorShades;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(500, 500);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g.setFont(FONT);
            int size = Math.min(getWidth() - 4, getHeight() - 4) / n;
            int width = getWidth() - (size * 2);
            int height = getHeight() - (size * 2);

            int xStart = (getWidth() - (size * n)) / 2;
            int yStart = (getHeight() - (size * n)) / 2;
            int y = (getHeight() - (size * n)) / 2;
            for (int horz = 0; horz < n; horz++) {
                int x = (getWidth() - (size * n)) / 2;
                for (int vert = 0; vert < n; vert++) {
                    Analytics.Cell cell = analytics.getCell(vert, horz);
                    int material = world.getFieldMaterials(vert, horz);
                    int time = world.getFieldTime(vert, horz);
                    if (time > 0) {
                        g.setColor(timeColor[Math.min(time, TIME_SHADES - 1)]);
                        g.fillRect(x, y, size, size);
                        g.setColor(Color.BLACK);
                    }
                    if (material > 0 || cell != null && cell.material > 0) {
                        g.setColor(materialColor[Math.min(Math.max(material, cell != null ? cell.material : 0), MATERIAL_SHADES - 1)]);
                        g.fillRect(x + size / 4, y + size / 4, size / 2, size / 2);
                        g.setColor(Color.BLACK);
                    }
                    g.drawRect(x, y, size, size);
                    if (!DEBUG) {
                        if (vert == factory.getX() && factory.getY() == horz) {
                            g.setColor(Color.BLUE);
                            g.fillRect(x + 2, y + 2, size - 3, size - 3);
                            g.setColor(Color.BLACK);
                        }
                        if (vert == robot.getX() && robot.getY() == horz) {
                            g.setColor(Color.red);
                            g.fillRect(x + 7, y + 7, size - 13, size - 13);
                            g.setColor(Color.BLACK);
                        }
                        if (cell != null) {
                            g.setColor(frequencyColor[(int) Math.ceil((cell.moveCount / Analytics.Cell.maxCount) * FREQUENCY_SHADES) - 1]);
                            g.fillRect(x + (size - size / 4) / 2, y + (size - size / 4) / 2, size / 4, size / 4);
                            g.setColor(Color.BLACK);
                            g.drawString(String.valueOf(cell.moveCount), x, y + size * (4 / 3));
                        }
                    } else {
                        if (cell != null && cell.generation == PathCreator.lastGeneration) {
                            g.setColor(costColor[Math.max((int) Math.ceil((cell.lastCost / Analytics.Cell.maxCost) * COST_SHADES) - 1, 0)]);
                            g.fillRect(x + (size - size / 3) / 2, y + (size - size / 3) / 2, size / 3, size / 3);
                            g.setColor(Color.BLACK);
                        }
                    }
                    x += size;
                }
                y += size;
            }

            g2d.dispose();
        }

    }

}
