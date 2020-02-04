package stb.image;

import java.io.InputStream;

public	class ImageResult
{
	public int Width;
	public int Height;
	public ColorComponents ColorComponents;
	public ColorComponents SourceComponents;

	/// <summary>
	/// Either 8 or 16
	/// </summary>
	public int BitsPerChannel;
	public byte[] Data;

	public static ImageResult FromInputStream(InputStream stream, ColorComponents? requiredComponents = null, boolean use8BitsPerChannel = true)
	{
		ImageResult result = null;
		if (JpgDecoder.Test(stream))
		{
			result = JpgDecoder.Decode(stream, requiredComponents);
		}
		else if (PngDecoder.Test(stream))
		{
			result = PngDecoder.Decode(stream, requiredComponents);
		}
		else if (BmpDecoder.Test(stream))
		{
			result = BmpDecoder.Decode(stream, requiredComponents);
		}
		else if (GifDecoder.Test(stream))
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
		}

		if (result == null)
		{
			Decoder.stbi__err("unknown image type");
		}

		if (use8BitsPerChannel && result.BitsPerChannel != 8)
		{
			result.Data = Conversion.stbi__convert_16_to_8(result.Data, result.Width, result.Height, (int)result.ColorComponents);
		}

		return result;
	}
}
