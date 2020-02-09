package stb.image;

import stb.image.Decoding.*;

import java.io.IOException;
import java.io.InputStream;

public class ImageInfo
{
	private int width;
    private int height;
    private ColorComponents colorComponents;
    private int bitsPerChannel;

    public ImageInfo(int width, int height, ColorComponents colorComponents, int bitsPerChannel)
    {
        this.width = width;
        this.height = height;
        this.colorComponents = colorComponents;
        this.bitsPerChannel = bitsPerChannel;
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

    public int getBitsPerChannel() {
        return bitsPerChannel;
    }

    public static ImageInfo FromInputStream(byte[] data)
	{
		ImageInfo info = JpgDecoder.Info(data);
		if (info != null)
		{
			return info;
		}

		info = PngDecoder.Info(data);
		if (info != null)
		{
			return info;
		}

		info = GifDecoder.Info(data);
		if (info != null)
		{
			return info;
		}

		info = BmpDecoder.Info(data);
		if (info != null)
		{
			return info;
		}

		info = PsdDecoder.Info(data);
		if (info != null)
		{
			return info;
		}

		info = TgaDecoder.Info(data);
		if (info != null)
		{
			return info;
		}

		return null;
	}
}
