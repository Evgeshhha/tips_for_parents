package org.example.tips_for_parents.service.card;

import lombok.NoArgsConstructor;
import org.example.tips_for_parents.model.value.VarSolve;

import java.util.List;

@NoArgsConstructor
final class DaySolver {
    static double[] solve(List<VarSolve> varSolves, double[] target, double lambda, int sweeps) {
        int n = varSolves.size();
        double[] s2 = new double[3];
        for (int k = 0; k < 3; k++) s2[k] = Math.pow(Math.max(target[k], 1.0), 2);

        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = clamp(varSolves.get(i).ref(), varSolves.get(i).lo(), varSolves.get(i).hi());

        for (int sweep = 0; sweep < sweeps; sweep++) {
            for (int i = 0; i < n; i++) {
                VarSolve v = varSolves.get(i);
                double ref = Math.max(v.ref(), 1e-6);
                double num = lambda / ref;                 // λ·ref / ref²
                double den = lambda / (ref * ref);
                for (int k = 0; k < 3; k++) {
                    double rest = 0;
                    for (int j = 0; j < n; j++) if (j != i) rest += varSolves.get(j).d()[k] * x[j];
                    num += v.d()[k] * (target[k] - rest) / s2[k];
                    den += v.d()[k] * v.d()[k] / s2[k];
                }
                x[i] = clamp(num / den, v.lo(), v.hi());
            }
        }
        return x;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.min(Math.max(v, lo), hi);
    }
}
