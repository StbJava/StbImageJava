package stb.image.Decoding;

import stb.image.ColorComponents;
import stb.image.ImageInfo;
import stb.image.ImageResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class BmpDecoder extends Decoder {
	private static class stbi__bmp_data {
		public int bpp;
		public int offset;
		public int hsz;
		public long mr;
		public long mg;
		public long mb;
		public long ma;
		public long all_a;
	}

	private static final long[] mul_table = {0, 0xff, 0x55, 0x49, 0x11, 0x21, 0x41, 0x81, 0x01};
	private static final long[] shift_table = {0, 0, 0, 1, 0, 2, 4, 6, 0};

	private BmpDecoder(java.io.InputStream stream) {
		super(stream);
	}

	private static int stbi__high_bit(long z) {
		int n = 0;
		if (z == 0)
			return -1;
		if (z >= 0x10000) {
			n += 16;
			z >>= 16;
		}

		if (z >= 0x00100) {
			n += 8;
			z >>= 8;
		}

		if (z >= 0x00010) {
			n += 4;
			z >>= 4;
		}

		if (z >= 0x00004) {
			n += 2;
			z >>= 2;
		}

		if (z >= 0x00002) {
			n += 1;
			z >>= 1;
		}

		return n;
	}

	private static int stbi__bitcount(long a) {
		a = (a & 0x55555555) + ((a >> 1) & 0x55555555);
		a = (a & 0x33333333) + ((a >> 2) & 0x33333333);
		a = (a + (a >> 4)) & 0x0f0f0f0f;
		a = a + (a >> 8);
		a = a + (a >> 16);
		return (int) (a & 0xff);
	}

	private static int stbi__shiftsigned(long v, int shift, int bits) {
		if (shift < 0)
			v <<= -shift;
		else
			v >>= shift;
		v >>= 8 - bits;
		return (int) (v * (int) mul_table[bits]) >> (int) shift_table[bits];
	}

	private void stbi__bmp_parse_header(stbi__bmp_data info) throws Exception {
		int hsz = 0;
		if (stbi__get8() != 'B' || stbi__get8() != 'M')
			stbi__err("not BMP");
		stbi__get32le();
		stbi__get16le();
		stbi__get16le();
		info.offset = (int) stbi__get32le();
		info.hsz = hsz = (int) stbi__get32le();
		info.mr = info.mg = info.mb = info.ma = 0;
		if (hsz != 12 && hsz != 40 && hsz != 56 && hsz != 108 && hsz != 124)
			stbi__err("unknown BMP");
		if (hsz == 12) {
			img_x = stbi__get16le();
			img_y = stbi__get16le();
		} else {
			img_x = (int) stbi__get32le();
			img_y = (int) stbi__get32le();
		}

		if (stbi__get16le() != 1)
			stbi__err("bad BMP");
		info.bpp = stbi__get16le();
		if (hsz != 12) {
			int compress = (int) stbi__get32le();
			if (compress == 1 || compress == 2)
				stbi__err("BMP RLE");
			stbi__get32le();
			stbi__get32le();
			stbi__get32le();
			stbi__get32le();
			stbi__get32le();
			if (hsz == 40 || hsz == 56) {
				if (hsz == 56) {
					stbi__get32le();
					stbi__get32le();
					stbi__get32le();
					stbi__get32le();
				}

				if (info.bpp == 16 || info.bpp == 32) {
					if (compress == 0) {
						if (info.bpp == 32) {
							info.mr = 0xff << 16;
							info.mg = 0xff << 8;
							info.mb = 0xff << 0;
							info.ma = 0xff << 24;
							info.all_a = 0;
						} else {
							info.mr = 31 << 10;
							info.mg = 31 << 5;
							info.mb = 31 << 0;
						}
					} else if (compress == 3) {
						info.mr = stbi__get32le();
						info.mg = stbi__get32le();
						info.mb = stbi__get32le();
						if (info.mr == info.mg && info.mg == info.mb) stbi__err("bad BMP");
					} else {
						stbi__err("bad BMP");
					}
				}
			} else {
				int i = 0;
				if (hsz != 108 && hsz != 124)
					stbi__err("bad BMP");
				info.mr = stbi__get32le();
				info.mg = stbi__get32le();
				info.mb = stbi__get32le();
				info.ma = stbi__get32le();
				stbi__get32le();
				for (i = 0; i < 12; ++i) stbi__get32le();
				if (hsz == 124) {
					stbi__get32le();
					stbi__get32le();
					stbi__get32le();
					stbi__get32le();
				}
			}
		}
	}

	private ImageResult InternalDecode(ColorComponents requiredComponents) throws Exception {
		Short[] _out_;
		long mr = 0;
		long mg = 0;
		long mb = 0;
		long ma = 0;
		long all_a = 0;
		short[] pal = new short[256 * 4];
		int psize = 0;
		int i = 0;
		int j = 0;
		int width = 0;
		int flip_vertically = 0;
		int pad = 0;
		int target = 0;
		stbi__bmp_data info = new stbi__bmp_data();
		info.all_a = 255;
		stbi__bmp_parse_header(info);
		flip_vertically = img_y > 0 ? 1 : 0;
		img_y = Math.abs(img_y);
		mr = info.mr;
		mg = info.mg;
		mb = info.mb;
		ma = info.ma;
		all_a = info.all_a;
		if (info.hsz == 12) {
			if (info.bpp < 24)
				psize = (info.offset - 14 - 24) / 3;
		} else {
			if (info.bpp < 16)
				psize = (info.offset - 14 - info.hsz) >> 2;
		}

		img_n = ma != 0 ? 4 : 3;
		if (requiredComponents != null && (int) requiredComponents.getValue() >= 3)
			target = (int) requiredComponents.getValue();
		else
			target = img_n;
		_out_ = new Short[target * img_x * img_y];
		if (info.bpp < 16) {
			int z = 0;
			if (psize == 0 || psize > 256) stbi__err("invalid");
			for (i = 0; i < psize; ++i) {
				pal[i * 4 + 2] = (short) stbi__get8();
				pal[i * 4 + 1] = (short) stbi__get8();
				pal[i * 4 + 0] = (short) stbi__get8();
				if (info.hsz != 12)
					stbi__get8();
				pal[i * 4 + 3] = (short) 255;
			}

			stbi__skip(info.offset - 14 - info.hsz - psize * (info.hsz == 12 ? 3 : 4));
			if (info.bpp == 1)
				width = (img_x + 7) >> 3;
			else if (info.bpp == 4)
				width = (img_x + 1) >> 1;
			else if (info.bpp == 8)
				width = img_x;
			else
				stbi__err("bad bpp");
			pad = -width & 3;
			if (info.bpp == 1)
				for (j = 0; j < img_y; ++j) {
					int bit_offset = 7;
					int v = (int) stbi__get8();
					for (i = 0; i < img_x; ++i) {
						int color = (v >> bit_offset) & 0x1;
						_out_[z++] = pal[color * 4 + 0];
						_out_[z++] = pal[color * 4 + 1];
						_out_[z++] = pal[color * 4 + 2];
						if (target == 4)
							_out_[z++] = (short) 255;
						if (i + 1 == img_x)
							break;
						if (--bit_offset < 0) {
							bit_offset = 7;
							v = stbi__get8();
						}
					}

					stbi__skip(pad);
				}
			else
				for (j = 0; j < img_y; ++j) {
					for (i = 0; i < img_x; i += 2) {
						int v = (int) stbi__get8();
						int v2 = 0;
						if (info.bpp == 4) {
							v2 = v & 15;
							v >>= 4;
						}

						_out_[z++] = pal[v * 4 + 0];
						_out_[z++] = pal[v * 4 + 1];
						_out_[z++] = pal[v * 4 + 2];
						if (target == 4)
							_out_[z++] = (short) 255;
						if (i + 1 == img_x)
							break;
						v = info.bpp == 8 ? stbi__get8() : v2;
						_out_[z++] = pal[v * 4 + 0];
						_out_[z++] = pal[v * 4 + 1];
						_out_[z++] = pal[v * 4 + 2];
						if (target == 4)
							_out_[z++] = (short) 255;
					}

					stbi__skip(pad);
				}
		} else {
			int rshift = 0;
			int gshift = 0;
			int bshift = 0;
			int ashift = 0;
			int rcount = 0;
			int gcount = 0;
			int bcount = 0;
			int acount = 0;
			int z = 0;
			int easy = 0;
			stbi__skip(info.offset - 14 - info.hsz);
			if (info.bpp == 24)
				width = 3 * img_x;
			else if (info.bpp == 16)
				width = 2 * img_x;
			else
				width = 0;
			pad = -width & 3;
			if (info.bpp == 24)
				easy = 1;
			else if (info.bpp == 32)
				if (mb == 0xff && mg == 0xff00 && mr == 0x00ff0000 && ma == 0xff000000)
					easy = 2;
			if (easy == 0) {
				if (mr == 0 || mg == 0 || mb == 0) stbi__err("bad masks");
				rshift = stbi__high_bit(mr) - 7;
				rcount = stbi__bitcount(mr);
				gshift = stbi__high_bit(mg) - 7;
				gcount = stbi__bitcount(mg);
				bshift = stbi__high_bit(mb) - 7;
				bcount = stbi__bitcount(mb);
				ashift = stbi__high_bit(ma) - 7;
				acount = stbi__bitcount(ma);
			}

			for (j = 0; j < img_y; ++j) {
				if (easy != 0) {
					for (i = 0; i < img_x; ++i) {
						short a = 0;
						_out_[z + 2] = (short) stbi__get8();
						_out_[z + 1] = (short) stbi__get8();
						_out_[z + 0] = (short) stbi__get8();
						z += 3;
						a = (short) (easy == 2 ? stbi__get8() : 255);
						all_a |= a;
						if (target == 4)
							_out_[z++] = a;
					}
				} else {
					int bpp = info.bpp;
					for (i = 0; i < img_x; ++i) {
						long v = bpp == 16 ? (long) stbi__get16le() : stbi__get32le();
						long a = 0;
						_out_[z++] = (short) (stbi__shiftsigned(v & mr, rshift, rcount) & 255);
						_out_[z++] = (short) (stbi__shiftsigned(v & mg, gshift, gcount) & 255);
						_out_[z++] = (short) (stbi__shiftsigned(v & mb, bshift, bcount) & 255);
						a = (long) (ma != 0 ? stbi__shiftsigned(v & ma, ashift, acount) : 255);
						all_a |= a;
						if (target == 4)
							_out_[z++] = (short) (a & 255);
					}
				}

				stbi__skip(pad);
			}
		}

		if (target == 4 && all_a == 0)
			for (i = 4 * img_x * img_y - 1; i >= 0; i -= 4)
				_out_[i] = (short) 255;
		if (flip_vertically != 0) {
			short t = 0;
			FakePtr<Short> ptr = new FakePtr<>(_out_);
			for (j = 0; j < img_y >> 1; ++j) {
				FakePtr<Short> p1 = ptr.cloneAdd(j * img_x * target);
				FakePtr<Short> p2 = ptr.cloneAdd((img_y - 1 - j) * img_x * target);
				for (i = 0; i < img_x * target; ++i) {
					t = p1.getAt(i);
					p1.setAt(i, p2.getAt(i));
					p2.setAt(i, t);
				}
			}
		}

		if (requiredComponents != null && (int) requiredComponents.getValue() != target)
			_out_ = Utility.stbi__convert_format(_out_, target, requiredComponents.getValue(), img_x, img_y);

		return new ImageResult(
				img_x,
				img_y,
				ColorComponents.fromInt(img_n),
				requiredComponents != null ? requiredComponents : ColorComponents.fromInt(img_n),
				8,
				Utility.toResultArray(_out_)
		);
	}

	private static boolean TestInternal(InputStream stream) throws Exception {
		if (stream.read() != 'B')
			return false;
		if (stream.read() != 'M')
			return false;

		Utility.stbi__get32le(stream);
		Utility.stbi__get16le(stream);
		Utility.stbi__get16le(stream);
		Utility.stbi__get32le(stream);
		int sz = (int) Utility.stbi__get32le(stream);
		boolean r = sz == 12 || sz == 40 || sz == 56 || sz == 108 || sz == 124;
		return r;
	}

	public static boolean Test(byte[] data) {
		try {
			return TestInternal(new ByteArrayInputStream(data));
		} catch (Exception ex) {
			return false;
		}
	}

	public static ImageInfo Info(byte[] data) {
		try {
			stbi__bmp_data info = new stbi__bmp_data();
			info.all_a = 255;

			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			BmpDecoder decoder = new BmpDecoder(stream);
			decoder.stbi__bmp_parse_header(info);
			return new ImageInfo(decoder.img_x,
					decoder.img_y,
					info.ma != 0 ? ColorComponents.RedGreenBlueAlpha : ColorComponents.RedGreenBlue,
					8);
		} catch (Exception ex) {
			return null;
		}
	}

	public static ImageResult Decode(byte[] data, ColorComponents requiredComponents) throws Exception {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		BmpDecoder decoder = new BmpDecoder(stream);
		return decoder.InternalDecode(requiredComponents);
	}

	public static ImageResult Decode(byte[] data) throws Exception {
		return Decode(data, null);
	}
}