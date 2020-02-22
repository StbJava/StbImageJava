package stb.image.testing;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.nothings.stb.image.ColorComponents;
import org.nothings.stb.image.ImageResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Testing {
	static class LoadResult {
		public ImageResult Image;
		public int TimeInMs;
	}

	static class LoadingTimes {
		private final ConcurrentHashMap<String, Integer> _byExtension = new ConcurrentHashMap<String, Integer>();
		private final ConcurrentHashMap<String, Integer> _byExtensionCount = new ConcurrentHashMap<String, Integer>();
		private int _total, _totalCount;

		public void add(String extension, int value) {

			if (!_byExtension.containsKey(extension)) {
				_byExtension.put(extension, 0);
				_byExtensionCount.put(extension, 0);
			}

			int val = _byExtension.get(extension);
			val += value;
			_byExtension.put(extension, val);

			val = _byExtensionCount.get(extension);
			++val;
			_byExtensionCount.put(extension, val);
			_total += value;
			++_totalCount;
		}

		public String buildString() {
			StringBuilder sb = new StringBuilder();

			for (String key : _byExtension.keySet()) {
				sb.append(key + ": " + _byExtension.get(key) + ", ");
			}

			sb.append("Total: " + _total + " ms");

			return sb.toString();
		}

		public String buildStringCount() {
			StringBuilder sb = new StringBuilder();
			for (String key : _byExtensionCount.keySet()) {
				sb.append(key + ": " + _byExtensionCount.get(key) + ", ");
			}

			sb.append("Total: " + _totalCount + "");

			return sb.toString();
		}
	}


	interface LoadInterface {
		ImageResult Do() throws Exception;
	}

	private static final AtomicInteger tasksStarted = new AtomicInteger();
	private static final AtomicInteger filesProcessed = new AtomicInteger();
	private static final AtomicInteger filesMatches = new AtomicInteger();
	private static final LoadingTimes stbImageJavaLoadingTimes = new LoadingTimes();
	private static final LoadingTimes stbNativeLoadingTimes = new LoadingTimes();
	private static final LoadingTimes imageIoLoadingTimes = new LoadingTimes();

	private static final ExecutorService pool = Executors.newFixedThreadPool(3);

	private static final int LoadTries = 10;

	public static void Log(String message) {
		System.out.println(Thread.currentThread().getId() + " -- " + message);
	}

	public static boolean RunTests(String imagesPath) throws Exception {
		File folder = new File(imagesPath);
		if (!folder.exists()) {
			throw new Exception(String.format("Could not find folder '%s'", imagesPath));
		}

		final File[] files = folder.listFiles();
/*		final File[] files = new File[1];
		files[0] = new File("D:/Projects/TestImages/DefaultChartPanel.jpg");*/
		Log("Files count: " + files.length);

		for (int i = 0; i < files.length; ++i) {
			final int i2 = i;
			Runnable runnable = () -> ThreadProc(files[i2].getAbsolutePath());

			pool.execute(runnable);
			tasksStarted.incrementAndGet();
		}

		while (true) {
			Thread.sleep(1000);

			if (tasksStarted.get() == 0) {
				break;
			}
		}

		return true;
	}

	private static LoadResult ParseTest(String name, LoadInterface load) throws Exception {
		Log("With " + name);

		long start = System.currentTimeMillis();

		ImageResult parsed = null;
		for (int i = 0; i < LoadTries; ++i) {
			parsed = load.Do();
		}

		Log(String.format("x: %d, y: %d, comp: %s, size: %d", parsed.getWidth(), parsed.getHeight(),
				parsed.getColorComponents(),
				parsed.getData() != null ? parsed.getData().length : 0));

		int passed = (int) (System.currentTimeMillis() - start) / LoadTries;

		Log(String.format("Span: %d ms", passed));

		LoadResult result = new LoadResult();
		result.Image = parsed;
		result.TimeInMs = passed;

		return result;
	}

	private static void ThreadProc(final String f) {
		if (!f.endsWith(".bmp") && !f.endsWith(".jpg") && !f.endsWith(".png") &&
				!f.endsWith(".jpg") && !f.endsWith(".psd") && !f.endsWith(".pic") &&
				!f.endsWith(".tga")) {
			tasksStarted.decrementAndGet();
			return;
		}


		int i = f.lastIndexOf('.');
		String extension = f.substring(i + 1);
		boolean match = false;

		try {
			Log("");
			Log(String.format("%s -- #%d: Loading %s into memory", new Date().toString(), filesProcessed.get(), f));
			Log("----------------------------");

			LoadResult stbImageJavaResult = ParseTest("StbImageJava",
					() -> {
						final byte[] data = Files.readAllBytes(new File(f).toPath());
						return ImageResult.FromData(data, ColorComponents.RedGreenBlueAlpha);
					});

			LoadResult stbNativeResult = ParseTest("Stb.Native",
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

						return new ImageResult(x.get(0),
								y.get(0),
								ColorComponents.fromInt(comp.get(0)),
								ColorComponents.RedGreenBlueAlpha,
								8,
								bytes);
					});

			ImageResult parsed = stbImageJavaResult.Image;
			ImageResult parsed2 = stbNativeResult.Image;

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

			for (i = 0; i < parsed.getData().length; ++i) {

				if (Math.abs(parsed.getData()[i] - parsed2.getData()[i]) > 0) {
					throw new Exception(String.format("Inconsistent data: index=%d, StbJava=%d, Stb.Native=%d",
							i,
							(int) parsed.getData()[i],
							(int) parsed2.getData()[i]));
				}
			}

			if (!extension.equals("tga") && !extension.equals("psd")) {
				LoadResult imageSharpResult = ParseTest(
						"ImageIO",
						() -> {
							BufferedImage image = ImageIO.read(new File(f));

							int numComp = image.getColorModel().getNumComponents();
							ColorComponents comp = ColorComponents.fromInt(numComp);
							return new ImageResult(image.getWidth(),
									image.getHeight(),
									comp,
									comp,
									8,
									null);
						});

				imageIoLoadingTimes.add(extension, imageSharpResult.TimeInMs);
			}

			match = true;
			stbImageJavaLoadingTimes.add(extension, stbImageJavaResult.TimeInMs);
			stbNativeLoadingTimes.add(extension, stbNativeResult.TimeInMs);

		} catch (Exception ex) {
			Log("Error: " + ex.getMessage());
		} finally {
			if (match) {
				filesMatches.incrementAndGet();
			}

			filesProcessed.incrementAndGet();
			tasksStarted.decrementAndGet();

			Log(String.format("StbImageJava - %s", stbImageJavaLoadingTimes.buildString()));
			Log(String.format("Stb.Native - %s", stbNativeLoadingTimes.buildString()));
			Log(String.format("ImageIO - %s", imageIoLoadingTimes.buildString()));
			Log(String.format("Total files processed - %s", stbImageJavaLoadingTimes.buildStringCount()));
			Log(String.format("StbImageJava/Stb.Native matches/processed - %d/%d", filesMatches.get(), filesProcessed.get()));
			Log(String.format("Tasks left - %d", tasksStarted.get()));
		}
	}

	public static void main(String[] args) {
		try {
			if (args == null || args.length < 1) {
				System.out.println("Usage: java -jar testing.jar <path_to_directory_with_images>");
				return;
			}

			long start = System.currentTimeMillis();

			boolean res = RunTests(args[0]);
			int passed = (int) (System.currentTimeMillis() - start);
			Log(String.format("Span: %d ms", passed));
			Log(new Date().toString() + " -- " + (res ? "Success" : "Failure"));
			System.exit(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}