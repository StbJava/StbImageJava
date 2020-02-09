package stb.image.testing;

import org.lwjgl.stb.STBImage;
import stb.image.ColorComponents;
import stb.image.ImageResult;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Date;

public class Testing {
	static class Passed {
		public int load1;
		public int load2;

		public Passed(int load1, int load2) {
			this.load1 = load1;
			this.load2 = load2;
		}
	}

	interface LoadInterface	{
		ImageResult Do() throws Exception;
	}

	private static int tasksStarted;
	private static int filesProcessed;
	private static int stbSharpLoadingFromStream;
	private static int stbNativeLoadingFromStream;
	private static int stbSharpLoadingFromMemory;
	private static int stbNativeLoadingFromMemory;
	
	private static final int LoadTries = 10;

	private static final int[] JpgQualities = {1, 4, 8, 16, 25, 32, 50, 64, 72, 80, 90, 100};
	private static final String[] FormatNames = {"BMP", "TGA", "HDR", "PNG", "JPG"};

	public static void Log(String message)
	{
		System.out.println(Thread.currentThread().getId() + " -- " + message);
	}

	public static boolean RunTests() throws Exception
	{
		String imagesPath = "../../../../../../../TestImages";

		File folder = new File(imagesPath);
		final File[] files = folder.listFiles();
		Log("Files count: " + files.length);

		for(int i = 0; i < files.length; ++i)
		{
			final int i2 = i;
			Runnable runnable = () -> ThreadProc(files[i2].getAbsolutePath());

			Thread thread = new Thread(runnable);
			thread.start();
			tasksStarted++;
		}

		while (true)
		{
			Thread.sleep(1000);

			if (tasksStarted == 0)
			{
				break;
			}
		}

		return true;
	}

	private static Passed ParseTest(LoadInterface load1, LoadInterface load2) throws Exception
	{
		Log("With StbSharp");

		long start = System.currentTimeMillis();

		ImageResult parsed = null;
		for (int i = 0; i < LoadTries; ++i)
		{
			parsed = load1.Do();
		}

		Log(String.format("x: %d, y: %d, comp: %s, size: %d", parsed.getWidth(), parsed.getHeight(), parsed.getColorComponents(), parsed.getData().length));

		int load1Passed = (int)(System.currentTimeMillis() - start)/LoadTries;

		Log(String.format("Span: %d ms", load1Passed));

		Log("With Stb.Native");
		ImageResult parsed2 = null;

		start = System.currentTimeMillis();
		for (int i = 0; i < LoadTries; ++i)
		{
			parsed2 = load2.Do();
		}

		Log(String.format("x: %d, y: %d, comp: %s, size: %d", parsed2.getWidth(), parsed2.getHeight(), parsed2.getColorComponents(), parsed2.getData().length));
		int load2Passed = (int)(System.currentTimeMillis() - start)/LoadTries;
		Log(String.format("Span: %d ms", load2Passed));

		if (parsed.getWidth() != parsed2.getWidth())
		{
			throw new Exception(String.format("Inconsistent x: StbSharp=%d, Stb.Native=%d", parsed.getWidth(), parsed2.getWidth()));
		}

		if (parsed.getHeight() != parsed2.getHeight())
		{
			throw new Exception(String.format("Inconsistent y: StbSharp=%d, Stb.Native=%d", parsed.getHeight(), parsed2.getHeight()));
		}

		if (parsed.getColorComponents() != parsed2.getColorComponents())
		{
			throw new Exception(String.format("Inconsistent comp: StbSharp=%d, Stb.Native=%d", parsed.getColorComponents(), parsed2.getColorComponents()));
		}

		if (parsed.getData().length != parsed2.getData().length)
		{
			throw new Exception(String.format("Inconsistent parsed length: StbSharp=%d, Stb.Native=%d", parsed.getData().length, parsed2.getData().length));
		}

		for (int i = 0; i < parsed.getData().length; ++i)
		{
			if (parsed.getData()[i] != parsed2.getData()[i])
			{
				throw new Exception(String.format("Inconsistent data: index=%d, StbSharp=%d, Stb.Native=%d",
						i,
						(int) parsed.getData()[i],
						(int) parsed2.getData()[i]));
			}
		}
		
		return new Passed(load1Passed, load2Passed);
	}

	private static void ThreadProc(String f)
	{
		try
		{
			if (!f.endsWith(".bmp") && !f.endsWith(".jpg") && !f.endsWith(".png") &&
					!f.endsWith(".jpg") && !f.endsWith(".psd") && !f.endsWith(".pic") &&
					!f.endsWith(".tga"))
			{
				return;
			}

			Log("");
			Log(String.format("%s -- #%d: Loading %s into memory", new Date().toString(), filesProcessed, f));
			final byte[] data = Files.readAllBytes(new File(f).toPath());
			Log("----------------------------");

			Log("Loading from memory");

			Passed passed = ParseTest(
					() -> ImageResult.FromData(data, ColorComponents.RedGreenBlueAlpha),
					() -> {
						int[] x = new int[1];
						int[] y = new int[1];
						int[] comp = new int[1];
						ByteBuffer result = STBImage.stbi_load_from_memory(ByteBuffer.wrap(data),
								x,
								y,
								comp,
								4);

						byte[] bytes = result.array();
						short[] shorts = new short[bytes.length];
						for(int i = 0; i < shorts.length; ++i)
						{
							shorts[i] = (short)(bytes[i] & 0xff);
						}

						return new ImageResult(x[0],
								y[0],
								ColorComponents.fromInt(comp[0]),
								ColorComponents.RedGreenBlueAlpha,
								8,
								shorts);
					});

			stbSharpLoadingFromMemory += passed.load1;
			stbNativeLoadingFromMemory += passed.load2;

			Log(String.format("Total StbSharp Loading From Stream Time: %d ms", stbSharpLoadingFromStream));
			Log(String.format("Total Stb.Native Loading From Stream Time: %d ms", stbNativeLoadingFromStream));
			Log(String.format("Total StbSharp Loading From memory Time: %d ms", stbSharpLoadingFromMemory));
			Log(String.format("Total Stb.Native Loading From memory Time: %d ms", stbNativeLoadingFromMemory));

			++filesProcessed;
			Log(new Date().toString() + " -- " + " Files processed: " + filesProcessed);

		}
		catch (Exception ex)
		{
			Log("Error: " + ex.getMessage());
		}
		finally
		{
			--tasksStarted;
		}
	}

	public static int main(String[] args) {
		long start = System.currentTimeMillis();

		try {
			boolean res = RunTests();
			int passed = (int)(System.currentTimeMillis()- start);
			Log(String.format("Span: %d ms", passed));
			Log(new Date().toString() + " -- " + (res ? "Success" : "Failure"));

			return res ? 1 : 0;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return 0;
		}
	}
}