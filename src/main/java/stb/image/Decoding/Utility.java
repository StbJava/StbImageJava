package stb.image.Decoding;

import java.io.InputStream;

class Utility
{
	public static int stbi__bitreverse16(int n) {
		n = ((n & 0xAAAA) >> 1) | ((n & 0x5555) << 1);
		n = ((n & 0xCCCC) >> 2) | ((n & 0x3333) << 2);
		n = ((n & 0xF0F0) >> 4) | ((n & 0x0F0F) << 4);
		n = ((n & 0xFF00) >> 8) | ((n & 0x00FF) << 8);
		return n;
	}

	public static int stbi__bit_reverse(int v, int bits) {
		return stbi__bitreverse16(v) >> (16 - bits);
	}

	public static long _lrotl(long x, int y) {
		return (x << y) | (x >> (32 - y));
	}

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
		long z = stbi__get16be(s);
		return (z << 16) + stbi__get16be(s);
	}

	public static int stbi__get16le(InputStream s) throws Exception
	{
		int z = stbi__get8(s);
		return z + (stbi__get8(s) << 8);
	}

	public static long stbi__get32le(InputStream s) throws Exception
	{
		long z = stbi__get16le(s);
		return z + (stbi__get16le(s) << 16);
	}

	public static byte stbi__compute_y(int r, int g, int b)
	{
		return (byte)(((r * 77) + (g * 150) + (29 * b)) >> 8);
	}

	public static int stbi__compute_y_16(int r, int g, int b)
	{
		return (int)(((r * 77) + (g * 150) + (29 * b)) >> 8);
	}

	public static byte[] stbi__convert_format16(byte[] data, int img_n, int req_comp, long x, long y)
	{
		throw new UnsupportedOperationException();
/*			int i = 0;
		int j = 0;
		if ((req_comp) == (img_n))
			return data;

		var good = new byte[req_comp * x * y * 2];
		FakePtr<Byte> dataPtr = new FakePtr<Byte>(data);
		FakePtr<Byte> goodPtr = new FakePtr<Byte>(good);
		for (j = (int)(0); (j) < ((int)(y)); ++j)
		{
			int* src = (int*)dataPtr + j * x * img_n;
			int* dest = (int*)goodPtr + j * x * req_comp;
			switch (((img_n) * 8 + (req_comp)))
			{
				case ((1) * 8 + (2)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 1, dest += 2)
					{
						dest[0] = src.getAt(0);
						dest[1] = (int)(0xffff);
					}
					break;
				case ((1) * 8 + (3)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 1, dest += 3)
					{
						dest[0] = (int)(dest[1] = (int)(dest[2] = src.getAt(0)));
					}
					break;
				case ((1) * 8 + (4)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 1, dest += 4)
					{
						dest[0] = (int)(dest[1] = (int)(dest[2] = src.getAt(0)));
						dest[3] = (int)(0xffff);
					}
					break;
				case ((2) * 8 + (1)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 2, dest += 1)
					{
						dest[0] = src.getAt(0);
					}
					break;
				case ((2) * 8 + (3)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 2, dest += 3)
					{
						dest[0] = (int)(dest[1] = (int)(dest[2] = src.getAt(0)));
					}
					break;
				case ((2) * 8 + (4)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 2, dest += 4)
					{
						dest[0] = (int)(dest[1] = (int)(dest[2] = src.getAt(0)));
						dest[3] = src.getAt(1);
					}
					break;
				case ((3) * 8 + (4)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 3, dest += 4)
					{
						dest[0] = src.getAt(0);
						dest[1] = src.getAt(1);
						dest[2] = src.getAt(2);
						dest[3] = (int)(0xffff);
					}
					break;
				case ((3) * 8 + (1)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 3, dest += 1)
					{
						dest[0] = (int)(stbi__compute_y_16(src.getAt(0), src.getAt(1), src.getAt(2)));
					}
					break;
				case ((3) * 8 + (2)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 3, dest += 2)
					{
						dest[0] = (int)(stbi__compute_y_16(src.getAt(0), src.getAt(1), src.getAt(2)));
						dest[1] = (int)(0xffff);
					}
					break;
				case ((4) * 8 + (1)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 4, dest += 1)
					{
						dest[0] = (int)(stbi__compute_y_16(src.getAt(0), src.getAt(1), src.getAt(2)));
					}
					break;
				case ((4) * 8 + (2)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 4, dest += 2)
					{
						dest[0] = (int)(stbi__compute_y_16(src.getAt(0), src.getAt(1), src.getAt(2)));
						dest[1] = (int)(src[3]);
					}
					break;
				case ((4) * 8 + (3)):
					for (i = (int)(x - 1); (i) >= (0); --i, src += 4, dest += 3)
					{
						dest[0] = src.getAt(0);
						dest[1] = src.getAt(1);
						dest[2] = src.getAt(2);
					}
					break;
				default:
					Decoder.stbi__err("0");
					break;
			}
		}

		return good;*/
	}

	public static byte[] toByteArray(Byte[] data)
	{
		byte[] result = new byte[data.length];
		for(int i = 0; i < data.length; ++i)
		{
			result[i] = data[i];
		}

		return result;
	}

	public static Byte[] stbi__convert_format(Byte[] data, int img_n, int req_comp, int x, int y) throws Exception
	{
		int i = 0;
		int j = 0;
		if ((req_comp) == (img_n))
			return data;

		var good = new Byte[req_comp * x * y];
		for (j = (int)(0); (j) < ((int)(y)); ++j)
		{
			FakePtr<Byte> src = new FakePtr<>(data, (int) (j * x * img_n));
			FakePtr<Byte> dest = new FakePtr<>(good, (int) (j * x * req_comp));
			switch (((img_n) * 8 + (req_comp)))
			{
				case ((1) * 8 + (2)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.increase(), dest.move(2))
					{
						dest.setAt(0, src.getAt(0));
						dest.setAt(1, (byte) 255);
					}
					break;
				case ((1) * 8 + (3)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.increase(), dest.move(3))
					{
						byte val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
					}
					break;
				case ((1) * 8 + (4)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.increase(), dest.move(4))
					{
						byte val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
						dest.setAt(3, (byte)255);
					}
					break;
				case ((2) * 8 + (1)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(2), dest.move(1))
					{
						byte val = src.getAt(0);
						dest.setAt(0, val);
					}
					break;
				case ((2) * 8 + (3)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(2), dest.move(3))
					{
						byte val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
					}
					break;
				case ((2) * 8 + (4)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(2), dest.move(4))
					{
						byte val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
						dest.setAt(3, src.getAt(1));
					}
					break;
				case ((3) * 8 + (4)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(3), dest.move(4))
					{
						dest.setAt(0, src.getAt(0));
						dest.setAt(1, src.getAt(1));
						dest.setAt(2, src.getAt(2));
						dest.setAt(3, (byte)255);
					}
					break;
				case ((3) * 8 + (1)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(3), dest.move(1))
					{
						dest.setAt(0, stbi__compute_y(src.getAt(0), src.getAt(1), src.getAt(2)));
					}
					break;
				case ((3) * 8 + (2)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(3), dest.move(2))
					{
						dest.setAt(0, stbi__compute_y(src.getAt(0), src.getAt(1), src.getAt(2)));
						dest.setAt(1, (byte)(255));
					}
					break;
				case ((4) * 8 + (1)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(4), dest.move(1))
					{
						dest.setAt(0, stbi__compute_y(src.getAt(0), src.getAt(1), src.getAt(2)));
					}
					break;
				case ((4) * 8 + (2)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(4), dest.move(2))
					{
						dest.setAt(0, stbi__compute_y(src.getAt(0), src.getAt(1), src.getAt(2)));
						dest.setAt(1, src.getAt(3));
					}
					break;
				case ((4) * 8 + (3)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(4), dest.move(3))
					{
						dest.setAt(0, src.getAt(0));
						dest.setAt(1, src.getAt(1));
						dest.setAt(2, src.getAt(2));
					}
					break;
				default:
					Decoder.stbi__err("0");
					break;
			}
		}

		return good;
	}

	public static byte[] stbi__convert_16_to_8(byte[] orig, int w, int h, int channels)
	{
		throw new UnsupportedOperationException();

/*			int i = 0;
		int img_len = (int)(w * h * channels);
		var reduced = new byte[img_len];

		fixed (byte* ptr2 = &orig[0])
		{
			int* ptr = (int*)ptr2;
			for (i = (int)(0); (i) < (img_len); ++i)
			{
				reduced[i] = ((byte)((ptr[i] >> 8) & 0xFF));
			}
		}
		return reduced;*/
	}

	public static int[] stbi__convert_8_to_16(byte[] orig, int w, int h, int channels)
	{
		int i = 0;
		int img_len = (int)(w * h * channels);
		var enlarged = new int[img_len];
		for (i = (int)(0); (i) < (img_len); ++i)
		{
			enlarged[i] = ((int)((orig[i] << 8) + orig[i]));
		}

		return enlarged;
	}

	public static void stbi__vertical_flip(byte[] image, int w, int h, int bytes_per_pixel)
	{
		int row = 0;
		int bytes_per_row = w * bytes_per_pixel;
		byte[] temp = new byte[2048];
		for (row = (int)(0); (row) < (h >> 1); row++)
		{
			FakePtr<Byte> row0 = new FakePtr<Byte>(image, (int)(row * bytes_per_row));
			FakePtr<Byte> row1 = new FakePtr<Byte>(image, (int)((h - row - 1) * bytes_per_row));
			int bytes_left = bytes_per_row;
			while ((bytes_left) != 0)
			{
				int bytes_copy = (((bytes_left) < (2048)) ? bytes_left : 2048);
				FakePtr<Byte>.memcpy(temp, row0, bytes_copy);
				FakePtr<Byte>.memcpy(row0, row1, bytes_copy);
				FakePtr<Byte>.memcpy(row1, temp, bytes_copy);
				row0 += bytes_copy;
				row1 += bytes_copy;
				bytes_left -= bytes_copy;
			}
		}
	}
}
