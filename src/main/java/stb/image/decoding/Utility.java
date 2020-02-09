package stb.image.decoding;

import java.io.IOException;
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

	protected static void stbi__skip(InputStream s, int count) throws IOException {
		s.skip(count);
	}

	public static short stbi__get8(InputStream s) throws Exception
	{
		int b = s.read();
		if (b == -1)
		{
			throw new Exception("EOF");
		}

		return (short)b;
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

	public static short stbi__compute_y(int r, int g, int b)
	{
		return (short)(((r * 77) + (g * 150) + (29 * b)) >> 8);
	}

	public static int stbi__compute_y_16(int r, int g, int b)
	{
		return (int)(((r * 77) + (g * 150) + (29 * b)) >> 8);
	}

	public static short[] stbi__convert_format16(short[] data, int img_n, int req_comp, long x, long y)
	{
		throw new UnsupportedOperationException("16-bit images are not supported yet");
/*			int i = 0;
		int j = 0;
		if ((req_comp) == (img_n))
			return data;

		short[] good = new short[req_comp * x * y * 2];
		ShortFakePtr dataPtr = new ShortFakePtr(data);
		ShortFakePtr goodPtr = new ShortFakePtr(good);
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

	public static short[] stbi__convert_format(short[] data, int img_n, int req_comp, int x, int y) throws Exception
	{
		int i = 0;
		int j = 0;
		if ((req_comp) == (img_n))
			return data;

		short[] good = new short[req_comp * x * y];
		for (j = (int)(0); (j) < ((int)(y)); ++j)
		{
			ShortFakePtr src = new ShortFakePtr(data, (int) (j * x * img_n));
			ShortFakePtr dest = new ShortFakePtr(good, (int) (j * x * req_comp));
			switch (((img_n) * 8 + (req_comp)))
			{
				case ((1) * 8 + (2)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.increase(), dest.move(2))
					{
						dest.setAt(0, src.getAt(0));
						dest.setAt(1, (short) 255);
					}
					break;
				case ((1) * 8 + (3)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.increase(), dest.move(3))
					{
						short val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
					}
					break;
				case ((1) * 8 + (4)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.increase(), dest.move(4))
					{
						short val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
						dest.setAt(3, (short)255);
					}
					break;
				case ((2) * 8 + (1)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(2), dest.move(1))
					{
						short val = src.getAt(0);
						dest.setAt(0, val);
					}
					break;
				case ((2) * 8 + (3)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(2), dest.move(3))
					{
						short val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
					}
					break;
				case ((2) * 8 + (4)):
					for (i = (int)(x - 1); (i) >= (0); --i, src.move(2), dest.move(4))
					{
						short val = src.getAt(0);
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
						dest.setAt(3, (short)255);
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
						dest.setAt(1, (short)(255));
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

	public static short[] stbi__convert_16_to_8(short[] orig, int w, int h, int channels)
	{
		throw new UnsupportedOperationException("16-bit images are not supported yet");

/*			int i = 0;
		int img_len = (int)(w * h * channels);
		short[] reduced = new short[img_len];

		fixed (short* ptr2 = &orig[0])
		{
			int* ptr = (int*)ptr2;
			for (i = (int)(0); (i) < (img_len); ++i)
			{
				reduced[i] = ((short)((ptr[i] >> 8) & 0xFF));
			}
		}
		return reduced;*/
	}

	public static int[] stbi__convert_8_to_16(short[] orig, int w, int h, int channels)
	{
		int i = 0;
		int img_len = (int)(w * h * channels);
		int[] enlarged = new int[img_len];
		for (i = (int)(0); (i) < (img_len); ++i)
		{
			enlarged[i] = ((int)((orig[i] << 8) + orig[i]));
		}

		return enlarged;
	}

/*	public static void stbi__vertical_flip(short[] image, int w, int h, int shorts_per_pixel)
	{
		int row = 0;
		int shorts_per_row = w * shorts_per_pixel;
		short[] temp = new short[2048];
		for (row = (int)(0); (row) < (h >> 1); row++)
		{
			ShortFakePtr row0 = new ShortFakePtr(image, (int)(row * shorts_per_row));
			ShortFakePtr row1 = new ShortFakePtr(image, (int)((h - row - 1) * shorts_per_row));
			int shorts_left = shorts_per_row;
			while ((shorts_left) != 0)
			{
				int shorts_copy = (((shorts_left) < (2048)) ? shorts_left : 2048);
				Utility.memcpy(temp, row0, shorts_copy);
				Utility.memcpy(row0, row1, shorts_copy);
				Utility.memcpy(row1, temp, shorts_copy);
				row0 += shorts_copy;
				row1 += shorts_copy;
				shorts_left -= shorts_copy;
			}
		}
	}*/
}
