package android.graphics;

public class Color {
    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int rgb(int r, int g, int b) {
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static void RGBToHSV(int r, int g, int b, float[] hsv) {
        if (hsv == null || hsv.length < 3) {
            throw new IllegalArgumentException("hsv array must be at least 3 elements long");
        }
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0.0f;
        float s = 0.0f;
        float v = max;

        if (max != 0.0f) {
            s = delta / max;
        }

        if (delta != 0.0f) {
            if (max == rf) {
                h = (gf - bf) / delta;
            } else if (max == gf) {
                h = 2.0f + (bf - rf) / delta;
            } else {
                h = 4.0f + (rf - gf) / delta;
            }
            h *= 60.0f;
            if (h < 0.0f) {
                h += 360.0f;
            }
        }

        hsv[0] = h;
        hsv[1] = s;
        hsv[2] = v;
    }
}
