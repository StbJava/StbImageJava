package stb.image.Utility;

import java.util.stream.Stream;

class IOUtils
{
	public static void Rewind(SeekableStream stream)
	{
		stream.Seek(0, SeekOrigin.Begin);
	}

	public static byte stbi__get8(Stream s)
	{
		int b = s.ReadByte();
		if (b == -1)
		{
			throw new Exception("EOF");
		}

		return (byte)b;
	}

	public static int stbi__get16be(Stream s)
	{
		int z = s.stbi__get8();
		return (z << 8) + s.stbi__get8();
	}

	public static uint stbi__get32be(Stream s)
	{
		uint z = (uint)stbi__get16be(s);
		return (uint)((z << 16) + stbi__get16be(s));
	}

	public static int stbi__get16le(Stream s)
	{
		int z = s.stbi__get8();
		return z + (s.stbi__get8() << 8);
	}

	public static uint stbi__get32le(Stream s)
	{
		uint z = (uint)(stbi__get16le(s));
		return (uint)(z + (stbi__get16le(s) << 16));
	}

	public static void stbi__skip(Stream s, int skip)
	{
		s.Seek(skip, SeekOrigin.Current);
	}
}
