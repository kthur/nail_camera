package android.graphics;

public class Bitmap {
    public enum Config {
        ARGB_8888
    }

    private final int width;
    private final int height;
    private final int[][] pixels;

    public Bitmap(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width][height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("Pixel coordinate out of bounds: (" + x + ", " + y + ") for size " + width + "x" + height);
        }
        return pixels[x][y];
    }

    public void setPixel(int x, int y, int color) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("Pixel coordinate out of bounds: (" + x + ", " + y + ") for size " + width + "x" + height);
        }
        pixels[x][y] = color;
    }

    public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                pixels[offset + j * stride + i] = this.pixels[x + i][y + j];
            }
        }
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        return new Bitmap(width, height);
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        Bitmap cropped = new Bitmap(width, height);
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                cropped.setPixel(i, j, source.getPixel(x + i, y + j));
            }
        }
        return cropped;
    }

    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        Bitmap scaled = new Bitmap(dstWidth, dstHeight);
        float xRatio = ((float) src.getWidth()) / dstWidth;
        float yRatio = ((float) src.getHeight()) / dstHeight;
        for (int y = 0; y < dstHeight; y++) {
            int srcY = Math.min((int)(y * yRatio), src.getHeight() - 1);
            for (int x = 0; x < dstWidth; x++) {
                int srcX = Math.min((int)(x * xRatio), src.getWidth() - 1);
                scaled.setPixel(x, y, src.getPixel(srcX, srcY));
            }
        }
        return scaled;
    }

    public void recycle() {
        // No-op for JVM test environment
    }
}
