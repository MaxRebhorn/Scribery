package de.scribery.core;

import java.util.function.IntBinaryOperator;

/**
 * Wie weit ein Bauwerk an einer Stelle nach oben oder unten verschoben wird, damit es dem Gelaende folgt (Wracks, eingestuerzte
 * Haeuser). Die Bodenhoehe wird auf einem groben Raster gelesen (u laengs, v quer), mehrfach geglaettet, auf {@code maxShift}
 * begrenzt und zwischen den Rasterpunkten linear gemischt. So biegt sich ein Schiff sanft durch, statt Stufen zu bilden.
 * Reine Mathematik ohne Minecraft: u und v sind Bauwerks-Koordinaten, {@code ground} liefert die Bodenhoehe dort.
 */
public final class TerrainShift {
    private final int uMin, vMin, step, maxShift;
    private final double[][] grid;

    /**
     * @param base    Hoehe, bei der die Verschiebung 0 ist (Bodenhoehe am Startpunkt)
     * @param ground  Bodenhoehe an der Stelle (u, v)
     * @param passes  wie oft das Raster geglaettet wird (je mehr, desto sanfter)
     */
    public TerrainShift(int uMin, int uMax, int vMin, int vMax, int step, int maxShift, int passes, int base, IntBinaryOperator ground) {
        this.uMin = uMin;
        this.vMin = vMin;
        this.step = step;
        this.maxShift = maxShift;
        int nu = (uMax - uMin) / step + 2, nv = (vMax - vMin) / step + 2;
        double[][] g = new double[nu][nv];
        for (int i = 0; i < nu; i++) for (int j = 0; j < nv; j++) g[i][j] = ground.applyAsInt(uMin + i * step, vMin + j * step) - base;
        for (int p = 0; p < passes; p++) {
            double[][] n = new double[nu][nv];
            for (int i = 0; i < nu; i++) {
                for (int j = 0; j < nv; j++) {
                    double sum = 0;
                    int cnt = 0;
                    for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
                        int a = i + di, b = j + dj;
                        if (a < 0 || a >= nu || b < 0 || b >= nv) continue;
                        sum += g[a][b];
                        cnt++;
                    }
                    n[i][j] = sum / cnt;
                }
            }
            g = n;
        }
        this.grid = g;
    }

    /** Verschiebung in Bloecken an der Stelle (u, v): negativ = nach unten, begrenzt auf +-maxShift. */
    public int shift(int u, int v) {
        double fu = (u - uMin) / (double) step, fv = (v - vMin) / (double) step;
        int i = (int) Math.floor(fu), j = (int) Math.floor(fv);
        i = Math.max(0, Math.min(grid.length - 2, i));
        j = Math.max(0, Math.min(grid[0].length - 2, j));
        double tu = Math.max(0, Math.min(1, fu - i)), tv = Math.max(0, Math.min(1, fv - j));
        double a = grid[i][j] * (1 - tu) + grid[i + 1][j] * tu, b = grid[i][j + 1] * (1 - tu) + grid[i + 1][j + 1] * tu;
        int s = (int) Math.round(a * (1 - tv) + b * tv);
        return Math.max(-maxShift, Math.min(maxShift, s));
    }
}
