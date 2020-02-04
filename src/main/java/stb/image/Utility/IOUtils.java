package stb.image.Utility;

import java.io.InputStream;

public class IOUtils
{
	public static byte stbi__get8(InputStream s) throws Exception
	{
		int b = s.read();
		if (b == -1)
		{
			throw new Exception("EOF");
		}

		return (byte)b;
	}

	public static int stbi__get16be(InputStream s) throws Exception
	{
		int z = stbi__get8(s);
		return (z << 8) + stbi__get8(s);
	}

	public static long stbi__get32be(InputStream s) throws Exception
	{
		long z = (long)stbi__get16be(s);
		return (long)((z << 16) + stbi__get16be(s));
	}

	public static int stbi__get16le(InputStream s) throws Exception
	{
		int z = stbi__get8(s);
		return z + (stbi__get8(s) << 8);
	}

	public static long stbi__get32le(InputStream s) throws Exception
	{
		long z = (long)(stbi__get16le(s));
		return (long)(z + (stbi__get16le(s) << 16));
	}
}
