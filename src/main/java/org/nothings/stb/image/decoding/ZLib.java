package org.nothings.stb.image.decoding;

import java.util.Arrays;

class ZLib {
	private static class stbi__zhuffman {
		public final int[] fast = new int[1 << 9];
		public final int[] firstcode = new int[16];
		public final int[] firstsymbol = new int[16];
		public final int[] maxcode = new int[17];
		public final short[] size = new short[288];
		public final int[] value = new int[288];
	}

	private static final int[] stbi__zlength_base =
			{
					3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195,
					227, 258, 0, 0
			};

	private static final int[] stbi__zlength_extra =
			{0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 0, 0};

	private static final int[] stbi__zdist_base =
			{
					1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097,
					6145, 8193, 12289, 16385, 24577, 0, 0
			};

	private static final int[] stbi__zdist_extra =
			{0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13};

	private static final short[] stbi__zdefault_length =
			{
					8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
					8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
					8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
					8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
					9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
					9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
					9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
					9, 9, 9, 9, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8
			};

	private static final short[] stbi__zdefault_distance =
			{5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5};

	private static final short[] length_dezigzag =
			{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};

	private long code_buffer;
	private int num_bits;
	private final stbi__zhuffman z_distance = new stbi__zhuffman();
	private int z_expandable;
	private final stbi__zhuffman z_length = new stbi__zhuffman();

	private ShortFakePtr zbuffer;
	private ShortFakePtr zbuffer_end;
	private ShortFakePtr zout;
	private ShortFakePtr zout_end;
	private short[] zout_start;

	private short stbi__zget8() {
		if (zbuffer.offset >= zbuffer_end.offset)
			return 0;
		return zbuffer.getAndIncrease();
	}

	private void stbi__fill_bits() {
		do {
			code_buffer |= (long) stbi__zget8() << num_bits;
			num_bits += 8;
		} while (num_bits <= 24);
	}

	private long stbi__zreceive(int n) {
		long k = 0;
		if (num_bits < n)
			stbi__fill_bits();
		k = (long) (code_buffer & ((1 << n) - 1));
		code_buffer >>= n;
		num_bits -= n;
		return k;
	}

	private int stbi__zhuffman_decode_slowpath(stbi__zhuffman z) {
		int b = 0;
		int s = 0;
		int k = 0;
		k = Utility.stbi__bit_reverse((int) code_buffer, 16);
		for (s = 9 + 1; ; ++s)
			if (k < z.maxcode[s])
				break;
		if (s == 16)
			return -1;
		b = (k >> (16 - s)) - z.firstcode[s] + z.firstsymbol[s];
		code_buffer >>= s;
		num_bits -= s;
		return z.value[b];
	}

	private int stbi__zhuffman_decode(stbi__zhuffman z) {
		int b = 0;
		int s = 0;
		if (num_bits < 16)
			stbi__fill_bits();
		b = z.fast[(int) code_buffer & ((1 << 9) - 1)];
		if (b != 0) {
			s = b >> 9;
			code_buffer >>= s;
			num_bits -= s;
			return b & 511;
		}

		return stbi__zhuffman_decode_slowpath(z);
	}

	private int stbi__zexpand(ShortFakePtr zout, int n) throws Exception {
		int cur = 0;
		int limit = 0;
		int old_limit = 0;
		this.zout = zout.clone();
		if (z_expandable == 0)
			Decoder.stbi__err("output buffer limit");
		cur = this.zout.offset;
		limit = old_limit = zout_end.offset;
		while (cur + n > limit) limit *= 2;

		zout_start = Arrays.copyOf(zout_start, limit);
		this.zout = new ShortFakePtr(zout_start, cur);
		zout_end = new ShortFakePtr(zout_start, limit);
		return 1;
	}

	private int stbi__parse_huffman_block() throws Exception {
		ShortFakePtr zout = this.zout.clone();
		for (; ; ) {
			int z = stbi__zhuffman_decode(z_length);
			if (z < 256) {
				if (z < 0)
					Decoder.stbi__err("bad huffman code");
				if (zout.offset >= zout_end.offset) {
					if (stbi__zexpand(zout, 1) == 0)
						return 0;
					zout = this.zout.clone();
				}

				zout.setAndIncrease((short) z);
			} else {
				int len = 0;
				int dist = 0;
				if (z == 256) {
					this.zout = zout;
					return 1;
				}

				z -= 257;
				len = stbi__zlength_base[z];
				if (stbi__zlength_extra[z] != 0)
					len += (int) stbi__zreceive(stbi__zlength_extra[z]);
				z = stbi__zhuffman_decode(z_distance);
				if (z < 0)
					Decoder.stbi__err("bad huffman code");
				dist = stbi__zdist_base[z];
				if (stbi__zdist_extra[z] != 0)
					dist += (int) stbi__zreceive(stbi__zdist_extra[z]);
				if (zout.offset < dist)
					Decoder.stbi__err("bad dist");
				if (zout.offset + len > zout_end.offset) {
					if (stbi__zexpand(zout, len) == 0)
						return 0;
					zout = this.zout.clone();
				}

				if (dist == 1) {
					if (len > 0) {
						short v = zout.getAt(-dist);
						zout.fillAndIncrease(v, len);
					}
				} else {
					if (len > 0) {
						ShortFakePtr p = new ShortFakePtr(zout, -dist);
						do {
							zout.setAndIncrease(p.getAndIncrease());
						} while (--len != 0);
//						zout.memcpyAndIncrease(p, len);
					}
				}
			}
		}
	}

	private static int stbi__zbuild_huffman(stbi__zhuffman z, ShortFakePtr sizelist, int num) throws Exception {
		int i = 0;
		int k = 0;
		int code = 0;
		int[] next_code = new int[16];
		int[] sizes = new int[17];
		Arrays.fill(sizes, 0);
		Arrays.fill(z.fast, 0);
		for (i = 0; i < num; ++i) ++sizes[sizelist.getAt(i)];
		sizes[0] = 0;
		for (i = 1; i < 16; ++i)
			if (sizes[i] > 1 << i)
				Decoder.stbi__err("bad sizes");
		code = 0;
		for (i = 1; i < 16; ++i) {
			next_code[i] = code;
			z.firstcode[i] = (int) code;
			z.firstsymbol[i] = (int) k;
			code = code + sizes[i];
			if (sizes[i] != 0)
				if (code - 1 >= 1 << i)
					Decoder.stbi__err("bad codelengths");
			z.maxcode[i] = code << (16 - i);
			code <<= 1;
			k += sizes[i];
		}

		z.maxcode[16] = 0x10000;
		for (i = 0; i < num; ++i) {
			int s = (int) sizelist.getAt(i);
			if (s != 0) {
				int c = next_code[s] - z.firstcode[s] + z.firstsymbol[s];
				int fastv = (int) ((s << 9) | i);
				z.size[c] = (short) s;
				z.value[c] = (int) i;
				if (s <= 9) {
					int j = Utility.stbi__bit_reverse(next_code[s], s);
					while (j < 1 << 9) {
						z.fast[j] = fastv;
						j += 1 << s;
					}
				}

				++next_code[s];
			}
		}

		return 1;
	}

	private int stbi__compute_huffman_codes() throws Exception {
		stbi__zhuffman z_codelength = new stbi__zhuffman();
		short[] lencodes = new short[286 + 32 + 137];
		short[] codelength_sizes = new short[19];
		int i = 0;
		int n = 0;
		int hlit = (int) (stbi__zreceive(5) + 257);
		int hdist = (int) (stbi__zreceive(5) + 1);
		int hclen = (int) (stbi__zreceive(4) + 4);
		int ntot = hlit + hdist;
		Arrays.fill(codelength_sizes, (short) 0);
		for (i = 0; i < hclen; ++i) {
			int s = (int) stbi__zreceive(3);
			codelength_sizes[length_dezigzag[i]] = (short) s;
		}

		if (stbi__zbuild_huffman(z_codelength, new ShortFakePtr(codelength_sizes), 19) == 0)
			return 0;
		n = 0;
		while (n < ntot) {
			int c = stbi__zhuffman_decode(z_codelength);
			if (c < 0 || c >= 19)
				Decoder.stbi__err("bad codelengths");
			if (c < 16) {
				lencodes[n++] = (short) c;
			} else {
				short fill = (short) 0;
				if (c == 16) {
					c = (int) (stbi__zreceive(2) + 3);
					if (n == 0)
						Decoder.stbi__err("bad codelengths");
					fill = lencodes[n - 1];
				} else if (c == 17) {
					c = (int) (stbi__zreceive(3) + 3);
				} else {
					c = (int) (stbi__zreceive(7) + 11);
				}

				if (ntot - n < c)
					Decoder.stbi__err("bad codelengths");

				Arrays.fill(lencodes, n, n + c, fill);
				n += c;
			}
		}

		if (n != ntot)
			Decoder.stbi__err("bad codelengths");
		if (stbi__zbuild_huffman(z_length, new ShortFakePtr(lencodes), hlit) == 0)
			return 0;
		if (stbi__zbuild_huffman(z_distance, new ShortFakePtr(lencodes, hlit), hdist) == 0)
			return 0;
		return 1;
	}

	private int stbi__parse_uncompressed_block() throws Exception {
		short[] header = new short[4];
		int len = 0;
		int nlen = 0;
		int k = 0;
		if ((num_bits & 7) != 0)
			stbi__zreceive(num_bits & 7);
		k = 0;
		while (num_bits > 0) {
			header[k++] = (short) (code_buffer & 255);
			code_buffer >>= 8;
			num_bits -= 8;
		}

		while (k < 4) header[k++] = stbi__zget8();
		len = header[1] * 256 + header[0];
		nlen = header[3] * 256 + header[2];
		if (nlen != (len ^ 0xffff))
			Decoder.stbi__err("zlib corrupt");
		if (zbuffer.offset + len > zbuffer_end.offset)
			Decoder.stbi__err("read past buffer");
		if (zout.offset + len > zout_end.offset)
			if (stbi__zexpand(zout, len) == 0)
				return 0;
		for (int i = 0; i < len; i++) zout.setAt(i, zbuffer.getAt(i));
		zbuffer.move(len);
		zout.move(len);
		return 1;
	}

	private int stbi__parse_zlib_header() throws Exception {
		int cmf = (int) stbi__zget8();
		int cm = cmf & 15;
		int flg = (int) stbi__zget8();
		if ((cmf * 256 + flg) % 31 != 0)
			Decoder.stbi__err("bad zlib header");
		if ((flg & 32) != 0)
			Decoder.stbi__err("no preset dict");
		if (cm != 8)
			Decoder.stbi__err("bad compression");
		return 1;
	}

	private int stbi__parse_zlib(int parse_header) throws Exception {
		int _final_ = 0;
		int type = 0;
		if (parse_header != 0)
			if (stbi__parse_zlib_header() == 0)
				return 0;
		num_bits = 0;
		code_buffer = 0;
		do {
			_final_ = (int) stbi__zreceive(1);
			type = (int) stbi__zreceive(2);
			if (type == 0) {
				if (stbi__parse_uncompressed_block() == 0)
					return 0;
			} else if (type == 3) {
				return 0;
			} else {
				if (type == 1) {
					if (stbi__zbuild_huffman(z_length, new ShortFakePtr(stbi__zdefault_length), 288) == 0)
						return 0;
					if (stbi__zbuild_huffman(z_distance, new ShortFakePtr(stbi__zdefault_distance), 32) == 0)
						return 0;
				} else {
					if (stbi__compute_huffman_codes() == 0)
						return 0;
				}

				if (stbi__parse_huffman_block() == 0)
					return 0;
			}
		} while (_final_ == 0);

		return 1;
	}

	private int stbi__do_zlib(short[] obuf, int olen, int exp, int parse_header) throws Exception {
		zout_start = obuf;
		zout = new ShortFakePtr(obuf);
		zout_end = new ShortFakePtr(obuf, olen);
		z_expandable = exp;
		return stbi__parse_zlib(parse_header);
	}

	public static Pair<short[], Integer> stbi_zlib_decode_malloc_guesssize_headerflag(short[] buffer, int len,
																					  int initial_size, int parse_header) throws Exception {
		ZLib a = new ZLib();
		short[] p = new short[initial_size];
		a.zbuffer = new ShortFakePtr(buffer);
		a.zbuffer_end = new ShortFakePtr(buffer, +len);
		if (a.stbi__do_zlib(p, initial_size, 1, parse_header) != 0) {
			return new Pair<>(a.zout_start, a.zout.offset);
		}

		return null;
	}
}