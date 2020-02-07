package stb.image;

import stb.image.Decoding.*;

import java.io.InputStream;

public	class ImageResult
{
	private int width;
	private int height;
	private ColorComponents colorComponents;
	private ColorComponents sourceComponents;
	private int bitsPerChannel;
	private short[] data;

	public ImageResult(int width,
					   int height,
					   ColorComponents sourceComponents,
					   ColorComponents colorComponents,
					   int bitsPerChannel,
					   short[] data)
	{
		this.width = width;
		this.height = height;
		this.sourceComponents = sourceComponents;
		this.colorComponents =colorComponents;
		this.bitsPerChannel = bitsPerChannel;
		this.data = data;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public ColorComponents getColorComponents() {
		return colorComponents;
	}

	public stb.image.ColorComponents getSourceComponents() {
		return sourceComponents;
	}

	public int getBitsPerChannel() {
		return bitsPerChannel;
	}

	public short[] getData() {
		return data;
	}

	public static ImageResult FromInputStream(InputStream stream, ColorComponents  requiredComponents, boolean use8BitsPerChannel) throws Exception
	{
		ImageResult result = null;
		if (JpgDecoder.Test(stream))
		{
			result = JpgDecoder.Decode(stream, requiredComponents);
		}
/*		else if (PngDecoder.Test(stream))
		{
			result = PngDecoder.Decode(stream, requiredComponents);
		}*/
		else if (BmpDecoder.Test(stream))
		{
			result = BmpDecoder.Decode(stream, requiredComponents);
		}
/*		else if (GifDecoder.Test(stream))
		{
			result = GifDecoder.Decode(stream, requiredComponents);
		}
		else if (PsdDecoder.Test(stream))
		{
			result = PsdDecoder.Decode(stream, requiredComponents);
		}
		else if (TgaDecoder.Test(stream))
		{
			result = TgaDecoder.Decode(stream, requiredComponents);
		}*/

		if (result == null)
		{
			throw new Exception("unknown image type");
		}

		if (use8BitsPerChannel && result.bitsPerChannel != 8)
		{
/*			result.data = Utility.stbi__convert_16_to_8(result.data, result.width, result.height, (int)result.colorComponents);*/
			throw new UnsupportedOperationException();
		}

		return result;
	}
}
