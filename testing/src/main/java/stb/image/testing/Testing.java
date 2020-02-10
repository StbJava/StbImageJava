package stb.image.testing;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.nothings.stb.image.ColorComponents;
import org.nothings.stb.image.ImageResult;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Testing {
	interface LoadInterface {
		ImageResult Do() throws Exception;
	}

	private static int tasksStarted;
	private static int filesProcessed, filesMatches;
	private static int stbJavaLoadingFromMemory;
	private static int stbNativeLoadingFromMemory;

	private static final ExecutorService pool = Executors.newFixedThreadPool(1);

	private static final int LoadTries = 10;

	private static final int[] JpgQualities = {1, 4, 8, 16, 25, 32, 50, 64, 72, 80, 90, 100};
	private static final String[] FormatNames = {"BMP", "TGA", "HDR", "PNG", "JPG"};

	public static void Log(String message) {
		System.out.println(Thread.currentThread().getId() + " -- " + message);
	}

	public static boolean RunTests() throws Exception {
		String imagesPath = "D:/Projects/TestImages";

		File folder = new File(imagesPath);

		final File[] files = folder.listFiles();
/*		final File[] files = new File[1];
		files[0] = new File("D:/Projects/TestImages/DefaultChartPanel.jpg");*/
		Log("Files count: " + files.length);

		for (int i = 0; i < files.length; ++i) {
			final int i2 = i;
			Runnable runnable = () -> ThreadProc(files[i2].getAbsolutePath());

			pool.execute(runnable);
			tasksStarted++;
		}

		while (true) {
			Thread.sleep(1000);

			if (tasksStarted == 0) {
				break;
			}
		}

		return true;
	}

	private static void ParseTest(LoadInterface load1, LoadInterface load2) throws Exception {
		Log("With StbJava");

		long start = System.currentTimeMillis();

		ImageResult parsed = null;
		for (int i = 0; i < LoadTries; ++i) {
			parsed = load1.Do();
		}

		Log(String.format("x: %d, y: %d, comp: %s, size: %d", parsed.getWidth(), parsed.getHeight(), parsed.getColorComponents(), parsed.getData().length));

		int load1Passed = (int) (System.currentTimeMillis() - start) / LoadTries;

		Log(String.format("Span: %d ms", load1Passed));

		Log("With Stb.Native");
		ImageResult parsed2 = null;

		start = System.currentTimeMillis();
		for (int i = 0; i < LoadTries; ++i) {
			parsed2 = load2.Do();
		}

		Log(String.format("x: %d, y: %d, comp: %s, size: %d", parsed2.getWidth(), parsed2.getHeight(), parsed2.getColorComponents(), parsed2.getData().length));
		int load2Passed = (int) (System.currentTimeMillis() - start) / LoadTries;
		Log(String.format("Span: %d ms", load2Passed));

		stbJavaLoadingFromMemory += load1Passed;
		stbNativeLoadingFromMemory += load2Passed;

		if (parsed.getWidth() != parsed2.getWidth()) {
			throw new Exception(String.format("Inconsistent x: StbJava=%d, Stb.Native=%d", parsed.getWidth(), parsed2.getWidth()));
		}

		if (parsed.getHeight() != parsed2.getHeight()) {
			throw new Exception(String.format("Inconsistent y: StbJava=%d, Stb.Native=%d", parsed.getHeight(), parsed2.getHeight()));
		}

		if (parsed.getColorComponents() != parsed2.getColorComponents()) {
			throw new Exception(String.format("Inconsistent comp: StbJava=%d, Stb.Native=%d", parsed.getColorComponents(), parsed2.getColorComponents()));
		}

		if (parsed.getData().length != parsed2.getData().length) {
			throw new Exception(String.format("Inconsistent parsed length: StbJava=%d, Stb.Native=%d", parsed.getData().length, parsed2.getData().length));
		}

		for (int i = 0; i < parsed.getData().length; ++i) {

			if (Math.abs(parsed.getData()[i] - parsed2.getData()[i]) > 0) {
				throw new Exception(String.format("Inconsistent data: index=%d, StbJava=%d, Stb.Native=%d",
						i,
						(int) parsed.getData()[i],
						(int) parsed2.getData()[i]));
			}
		}
	}

	private static void ThreadProc(final String f) {
		if (!f.endsWith(".bmp") && !f.endsWith(".jpg") && !f.endsWith(".png") &&
				!f.endsWith(".jpg") && !f.endsWith(".psd") && !f.endsWith(".pic") &&
				!f.endsWith(".tga")) {
			--tasksStarted;
			return;
		}

		try {

			Log("");
			Log(String.format("%s -- #%d: Loading %s into memory", new Date().toString(), filesProcessed, f));
			Log("----------------------------");

			ParseTest(
					() -> {
						final byte[] data = Files.readAllBytes(new File(f).toPath());
						return ImageResult.FromData(data, ColorComponents.RedGreenBlueAlpha);
					},
					() -> {
						IntBuffer x = BufferUtils.createIntBuffer(1);
						IntBuffer y = BufferUtils.createIntBuffer(1);
						IntBuffer comp = BufferUtils.createIntBuffer(1);
						ByteBuffer result = STBImage.stbi_load(f,
								x,
								y,
								comp,
								4);

						if (result == null) {
							throw new Exception(STBImage.stbi_failure_reason());
						}

						byte[] bytes = new byte[result.remaining()];
						result.get(bytes);
						short[] shorts = new short[bytes.length];
						for (int i = 0; i < shorts.length; ++i) {
							shorts[i] = (short) (bytes[i] & 0xff);
						}

						return new ImageResult(x.get(0),
								y.get(0),
								ColorComponents.fromInt(comp.get(0)),
								ColorComponents.RedGreenBlueAlpha,
								8,
								shorts);
					});

			++filesMatches;
		} catch (Exception ex) {
			Log("Error: " + ex.getMessage());
		} finally {
			++filesProcessed;
			--tasksStarted;

			Log(String.format("Total StbJava Loading From memory Time: %d ms", stbJavaLoadingFromMemory));
			Log(String.format("Total Stb.Native Loading From memory Time: %d ms", stbNativeLoadingFromMemory));
			Log(String.format("Files matches/processed: %d/%d", filesMatches, filesProcessed));
			Log(String.format("Tasks left: %d", tasksStarted));
		}
	}

	public static void main(String[] args) {
		long start = System.currentTimeMillis();

		try {
			boolean res = RunTests();
			int passed = (int) (System.currentTimeMillis() - start);
			Log(String.format("Span: %d ms", passed));
			Log(new Date().toString() + " -- " + (res ? "Success" : "Failure"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}