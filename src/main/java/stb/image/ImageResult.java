package stb.image;

import stb.image.decoding.*;

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

	public static ImageResult FromData(byte[] data, ColorComponents  requiredComponents) throws Exception
	{
		ImageResult result = null;
		if (JpgDecoder.Test(data))
		{
			result = JpgDecoder.Decode(data, requiredComponents);
		}
		else if (PngDecoder.Test(data))
		{
			result = PngDecoder.Decode(data, requiredComponents);
		}
		else if (BmpDecoder.Test(data))
		{
			result = BmpDecoder.Decode(data, requiredComponents);
		}
		else if (GifDecoder.Test(data))
		{
			result = GifDecoder.Decode(data, requiredComponents);
		}
		else if (PsdDecoder.Test(data))
		{
			result = PsdDecoder.Decode(data, requiredComponents);
		}
		else if (TgaDecoder.Test(data))
		{
			result = TgaDecoder.Decode(data, requiredComponents);
		}

		if (result == null)
		{
			throw new Exception("unknown image type");
		}

/*		if (use8BitsPerChannel && result.bitsPerChannel != 8)
		{
			result.data = Utility.stbi__convert_16_to_8(result.data, result.width, result.height, (int)result.colorComponents);
			throw new UnsupportedOperationException();
		}*/

		return result;
	}
}
