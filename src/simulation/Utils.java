package simulation;

import java.util.Random;

public class Utils {
    public static int uniform_discr(Random r, int min, int max) {
        return r.nextInt((max - min) + 1) + min;
    }
}
