package de.scribery;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Kollisions- und Auswahlformen der Werkbloecke. Die Quader stammen aus art/gen_stations.py (art/out/station_boxes.json) und
 * passen zu den Modellen; sie gelten fuer Blickrichtung Norden und werden fuer die anderen Richtungen gedreht.
 */
public final class StationShapes {
    private StationShapes() {}

    /** scribe_lectern: x1, y1, z1, x2, y2, z2 fuer Blickrichtung Norden (Vorderseite = -z). */
    public static final double[][] SCRIBE_LECTERN = new double[][] {
            {3, 0, 3, 13, 3, 13},
            {6, 3, 6, 10, 11, 10},
            {1, 11, 1, 15, 13, 15},
            {1, 13, 13, 15, 16, 15} };

    /** translator_desk: x1, y1, z1, x2, y2, z2 fuer Blickrichtung Norden (Vorderseite = -z). */
    public static final double[][] TRANSLATOR_DESK = new double[][] {
            {0, 11, 1, 16, 13, 15},
            {1, 0, 2, 3, 11, 4},
            {13, 0, 2, 15, 11, 4},
            {1, 0, 12, 3, 11, 14},
            {13, 0, 12, 15, 11, 14},
            {3, 6, 3, 13, 11, 13},
            {0, 13, 13, 16, 16, 15} };

    /** printing_press: x1, y1, z1, x2, y2, z2 fuer Blickrichtung Norden (Vorderseite = -z). */
    public static final double[][] PRINTING_PRESS = new double[][] {
            {0, 0, 1, 16, 10, 15},
            {1, 10, 1, 16, 16, 13} };

    /** letter_cabinet: x1, y1, z1, x2, y2, z2 fuer Blickrichtung Norden (Vorderseite = -z). */
    public static final double[][] LETTER_CABINET = new double[][] {
            {0, 0, 1, 16, 10, 15},
            {1, 10, 2, 9, 12, 14},
            {10, 10, 9, 15, 14, 14},
            {10, 10, 2.5, 15, 11.5, 8.5} };

    /** minting_table: x1, y1, z1, x2, y2, z2 fuer Blickrichtung Norden (Vorderseite = -z). */
    public static final double[][] MINTING_TABLE = new double[][] {
            {0, 0, 0, 16, 10, 16},
            {2, 10, 2, 8, 16, 8},
            {10, 9, 9, 16, 14, 14} };

    /** Formen fuer alle vier Blickrichtungen. Drehung wie im Blockstate (y = 90 im Uhrzeigersinn von oben: Norden wird Osten). */
    public static Map<Direction, VoxelShape> build(double[][] boxes) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        Direction[] order = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int turns = 0; turns < 4; turns++) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
                for (int i = 0; i < turns; i++) {   // (x, z) -> (16 - z, x)
                    double nx1 = 16 - z2, nz1 = x1, nx2 = 16 - z1, nz2 = x2;
                    x1 = nx1; z1 = nz1; x2 = nx2; z2 = nz2;
                }
                shape = Shapes.or(shape, Shapes.box(x1 / 16, b[1] / 16, z1 / 16, x2 / 16, b[4] / 16, z2 / 16));
            }
            out.put(order[turns], shape);
        }
        return out;
    }
}
