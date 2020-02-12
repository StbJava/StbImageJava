package org.nothings.stb.image.decoding;

import org.nothings.stb.image.ColorComponents;
import org.nothings.stb.image.ImageInfo;
import org.nothings.stb.image.ImageResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class JpgDecoder extends Decoder {
	private static class img_comp {
		public int id;
		public int h, v;
		public int tq;
		public int hd, ha;
		public int dc_pred;

		public int x, y, w2, h2;
		public short[] raw_data;
		public ShortFakePtr data;
		public short[] raw_coeff;
		public ShortFakePtr coeff; // progressive only
		public short[] linebuf;
		public int coeff_w, coeff_h; // number of 8x8 coefficient blocks
	}

	private interface idct_block_kernel {
		void Do(ShortFakePtr output, int out_stride, ShortFakePtr data);
	}

	private interface YCbCr_to_RGB_kernel {
		void Do(ShortFakePtr output, ShortFakePtr y, ShortFakePtr pcb,
				ShortFakePtr pcr, int count, int step);
	}

	private interface Resampler {
		ShortFakePtr Do(ShortFakePtr a, ShortFakePtr b, ShortFakePtr c, int d, int e);
	}

	private static class stbi__resample {
		public int hs;
		public ShortFakePtr line0;
		public ShortFakePtr line1;
		public Resampler resample;
		public int vs;
		public int w_lores;
		public int ypos;
		public int ystep;
	}

	private static class stbi__huffman {
		public final int[] code = new int[256];
		public final int[] delta = new int[17];
		public final short[] fast = new short[1 << 9];
		public final long[] maxcode = new long[18];
		public final short[] size = new short[257];
		public final short[] values = new short[256];
	}

	private static final long MAX_UNSIGNED_INT = (((long) 1 << 32) - 1);

	private static final int STBI__ZFAST_BITS = 9;

	private static final int[] stbi__bmask =
			{0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535};

	private static final int[] stbi__jbias =
			{0, -1, -3, -7, -15, -31, -63, -127, -255, -511, -1023, -2047, -4095, -8191, -16383, -32767};

	private static final short[] stbi__jpeg_dezigzag =
			{
					0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13, 6, 7,
					14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51, 58, 59, 52, 45, 38, 31, 39, 46,
					53, 60, 61, 54, 47, 55, 62, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63
			};

	private final stbi__huffman[] huff_dc = new stbi__huffman[4];
	private final stbi__huffman[] huff_ac = new stbi__huffman[4];
	private int[][] dequant;

	private short[][] fast_ac;

	// sizes for components, interleaved MCUs
	private int img_h_max, img_v_max;
	private int img_mcu_x, img_mcu_y;
	private int img_mcu_w, img_mcu_h;

	// definition of jpeg image component
	private final img_comp[] img_comp = new img_comp[4];

	private long code_buffer; // jpeg entropy-coded buffer
	private int code_bits; // number of valid bits
	private int marker; // marker seen while filling entropy buffer
	private int nomore; // flag if we saw a marker so must stop

	private int progressive;
	private int spec_start;
	private int spec_end;
	private int succ_high;
	private int succ_low;
	private int eob_run;
	private int jfif;
	private int app14_color_transform; // Adobe APP14 tag
	private int rgb;

	private int scan_n;
	private final int[] order = new int[4];
	private int restart_interval, todo;

	// kernels
	private idct_block_kernel idct_block_kernel;
	private YCbCr_to_RGB_kernel YCbCr_to_RGB_kernel;
	private Resampler resample_row_hv_2_kernel;

	private JpgDecoder(InputStream stream) {
		super(stream);
		for (int i = 0; i < 4; ++i) {
			huff_ac[i] = new stbi__huffman();
			huff_dc[i] = new stbi__huffman();
		}

		for (int i = 0; i < img_comp.length; ++i) img_comp[i] = new img_comp();

		fast_ac = new short[4][];
		for (int i = 0; i < fast_ac.length; ++i) fast_ac[i] = new short[1 << STBI__ZFAST_BITS];

		dequant = new int[4][];
		for (int i = 0; i < dequant.length; ++i) dequant[i] = new int[64];
	}

	private static int stbi__build_huffman(stbi__huffman h, int[] count) throws Exception {
		int i = 0;
		int j = 0;
		int k = 0;
		long code = 0;
		for (i = 0; i < 16; ++i)
			for (j = 0; j < count[i]; ++j)
				h.size[k++] = (short) (i + 1);
		h.size[k] = 0;
		code = 0;
		k = 0;
		for (j = 1; j <= 16; ++j) {
			h.delta[j] = (int) (k - code);
			if (h.size[k] == j) {
				while (h.size[k] == j) h.code[k++] = (int) code++;
				if (code - 1 >= 1 << j)
					stbi__err("bad code lengths");
			}

			h.maxcode[j] = code << (16 - j);
			code <<= 1;
		}

		h.maxcode[j] = 0xffffffff;

		Arrays.fill(h.fast, (short) 255);
		for (i = 0; i < k; ++i) {
			int s = (int) h.size[i];
			if (s <= 9) {
				int c = h.code[i] << (9 - s);
				int m = 1 << (9 - s);
				for (j = 0; j < m; ++j) h.fast[c + j] = (short) i;
			}
		}

		return 1;
	}

	private static void stbi__build_fast_ac(short[] fast_ac, stbi__huffman h) {
		int i = 0;
		for (i = 0; i < 1 << 9; ++i) {
			int fast = h.fast[i];
			fast_ac[i] = 0;
			if (fast < 255) {
				int rs = h.values[fast];
				int run = (rs >> 4) & 15;
				int magbits = rs & 15;
				int len = (int) h.size[fast];
				if (magbits != 0 && len + magbits <= 9) {
					int k = ((i << len) & ((1 << 9) - 1)) >> (9 - magbits);
					int m = 1 << (magbits - 1);
					if (k < m)
						k += (int) ((~0 << magbits) + 1);
					if (k >= -128 && k <= 127)
						fast_ac[i] = (short) (k * 256 + run * 16 + len + magbits);
				}
			}
		}
	}

	private void clamp_code_buffer() {
		code_buffer &= MAX_UNSIGNED_INT;
	}

	private void stbi__grow_buffer_unsafe() throws Exception {
		do {
			long b = (long) (nomore != 0 ? 0 : stbi__get8());
			if (b == 0xff) {
				int c = (int) stbi__get8();
				while (c == 0xff) c = stbi__get8();
				if (c != 0) {
					marker = (short) c;
					nomore = 1;
					return;
				}
			}

			code_buffer |= b << (24 - code_bits);
			clamp_code_buffer();

			code_bits += 8;
		} while (code_bits <= 24);
	}

	private int stbi__jpeg_huff_decode(stbi__huffman h) throws Exception {
		long temp = 0;
		int c = 0;
		int k = 0;
		if (code_bits < 16)
			stbi__grow_buffer_unsafe();
		c = (int) ((code_buffer >> (32 - 9)) & ((1 << 9) - 1));
		k = h.fast[c];
		if (k < 255) {
			int s = (int) h.size[k];
			if (s > code_bits)
				return -1;
			code_buffer <<= s;
			clamp_code_buffer();
			code_bits -= s;
			return h.values[k];
		}

		temp = code_buffer >> 16;
		for (k = 9 + 1; ; ++k)
			if (temp < h.maxcode[k])
				break;
		if (k == 17) {
			code_bits -= 16;
			return -1;
		}

		if (k > code_bits)
			return -1;
		c = (int) (((code_buffer >> (32 - k)) & stbi__bmask[k]) + h.delta[k]);
		code_bits -= k;
		code_buffer <<= k;
		clamp_code_buffer();

		return h.values[c];
	}

	private int stbi__extend_receive(int n) throws Exception {
		int sgn = 0;
		if (code_bits < n)
			stbi__grow_buffer_unsafe();
		sgn = (int) code_buffer >> 31;
		long k = Utility._lrotl(code_buffer, n);
		code_buffer = k & ~stbi__bmask[n];
		clamp_code_buffer();

		k &= stbi__bmask[n];
		code_bits -= n;
		return (int) (k + (stbi__jbias[n] & ~sgn));
	}

	private int stbi__jpeg_get_bits(int n) throws Exception {
		if (code_bits < n)
			stbi__grow_buffer_unsafe();
		long k = Utility._lrotl(code_buffer, n);
		code_buffer = k & ~stbi__bmask[n];
		clamp_code_buffer();

		k &= stbi__bmask[n];
		code_bits -= n;
		return (int) k;
	}

	private int stbi__jpeg_get_bit() throws Exception {
		if (code_bits < 1)
			stbi__grow_buffer_unsafe();
		long k = code_buffer;
		code_buffer <<= 1;
		clamp_code_buffer();

		--code_bits;
		return (int) (k & 0x80000000);
	}

	private int stbi__jpeg_decode_block(short[] data, stbi__huffman hdc, stbi__huffman hac, short[] fac, int b,
										int[] dequant) throws Exception {
		int diff = 0;
		int dc = 0;
		int k = 0;
		int t = 0;
		if (code_bits < 16)
			stbi__grow_buffer_unsafe();
		t = stbi__jpeg_huff_decode(hdc);
		if (t < 0)
			stbi__err("bad huffman code");

		Arrays.fill(data, (short) 0);
		diff = t != 0 ? stbi__extend_receive(t) : 0;
		dc = img_comp[b].dc_pred + diff;
		img_comp[b].dc_pred = dc;
		data[0] = (short) (dc * dequant[0]);
		k = 1;
		do {
			int zig = 0;
			int c = 0;
			int r = 0;
			int s = 0;
			if (code_bits < 16)
				stbi__grow_buffer_unsafe();
			c = (int) ((code_buffer >> (32 - 9)) & ((1 << 9) - 1));
			r = fac[c];
			if (r != 0) {
				k += (r >> 4) & 15;
				s = r & 15;
				code_buffer <<= s;
				clamp_code_buffer();
				code_bits -= s;
				zig = stbi__jpeg_dezigzag[k++];
				data[zig] = (short) ((r >> 8) * dequant[zig]);
			} else {
				int rs = stbi__jpeg_huff_decode(hac);
				if (rs < 0)
					stbi__err("bad huffman code");
				s = rs & 15;
				r = rs >> 4;
				if (s == 0) {
					if (rs != 0xf0)
						break;
					k += 16;
				} else {
					k += r;
					zig = stbi__jpeg_dezigzag[k++];
					data[zig] = (short) (stbi__extend_receive(s) * dequant[zig]);
				}
			}
		} while (k < 64);

		return 1;
	}

	private int stbi__jpeg_decode_block_prog_dc(ShortFakePtr data, stbi__huffman hdc, int b) throws Exception {
		int diff = 0;
		int dc = 0;
		int t = 0;
		if (spec_end != 0)
			stbi__err("can't merge dc and ac");
		if (code_bits < 16)
			stbi__grow_buffer_unsafe();
		if (succ_high == 0) {
			data.fill((short) 0, 64);
			t = stbi__jpeg_huff_decode(hdc);
			diff = t != 0 ? stbi__extend_receive(t) : 0;
			dc = img_comp[b].dc_pred + diff;
			img_comp[b].dc_pred = dc;
			data.setAt(0, (short) (dc << succ_low));
		} else {
			if (stbi__jpeg_get_bit() != 0) {
				short val = data.getAt(0);
				data.setAt(0, (short) (val + (short) (1 << succ_low)));
			}
		}

		return 1;
	}

	private int stbi__jpeg_decode_block_prog_ac(ShortFakePtr data, stbi__huffman hac, short[] fac) throws Exception {
		int k = 0;
		if (spec_start == 0)
			stbi__err("can't merge dc and ac");
		if (succ_high == 0) {
			int shift = succ_low;
			if (eob_run != 0) {
				--eob_run;
				return 1;
			}

			k = spec_start;
			do {
				int zig = 0;
				int c = 0;
				int r = 0;
				int s = 0;
				if (code_bits < 16)
					stbi__grow_buffer_unsafe();
				c = (int) ((code_buffer >> (32 - 9)) & ((1 << 9) - 1));
				r = fac[c];
				if (r != 0) {
					k += (r >> 4) & 15;
					s = r & 15;
					code_buffer <<= s;
					clamp_code_buffer();
					code_bits -= s;
					zig = stbi__jpeg_dezigzag[k++];
					data.setAt(zig, (short) ((r >> 8) << shift));
				} else {
					int rs = stbi__jpeg_huff_decode(hac);
					if (rs < 0)
						stbi__err("bad huffman code");
					s = rs & 15;
					r = rs >> 4;
					if (s == 0) {
						if (r < 15) {
							eob_run = 1 << r;
							if (r != 0)
								eob_run += stbi__jpeg_get_bits(r);
							--eob_run;
							break;
						}

						k += 16;
					} else {
						k += r;
						zig = stbi__jpeg_dezigzag[k++];
						data.setAt(zig, (short) (stbi__extend_receive(s) << shift));
					}
				}
			} while (k <= spec_end);
		} else {
			short bit = (short) (1 << succ_low);
			if (eob_run != 0) {
				--eob_run;
				for (k = spec_start; k <= spec_end; ++k) {
					short idx = stbi__jpeg_dezigzag[k];
					short value = data.getAt(idx);
					if (value != 0)
						if (stbi__jpeg_get_bit() != 0)
							if ((value & bit) == 0) {
								short val = data.getAt(idx);
								if (value > 0)
									data.setAt(idx, (short) (val + bit));
								else
									data.setAt(idx, (short) (val - bit));
							}
				}
			} else {
				k = spec_start;
				do {
					int r = 0;
					int s = 0;
					int rs = stbi__jpeg_huff_decode(hac);
					if (rs < 0)
						stbi__err("bad huffman code");
					s = rs & 15;
					r = rs >> 4;
					if (s == 0) {
						if (r < 15) {
							eob_run = (1 << r) - 1;
							if (r != 0)
								eob_run += stbi__jpeg_get_bits(r);
							r = 64;
						}
					} else {
						if (s != 1)
							stbi__err("bad huffman code");
						if (stbi__jpeg_get_bit() != 0)
							s = bit;
						else
							s = -bit;
					}

					while (k <= spec_end) {
						short idx = stbi__jpeg_dezigzag[k++];
						short value = data.getAt(idx);
						if (value != 0) {
							if (stbi__jpeg_get_bit() != 0)
								if ((value & bit) == 0) {
									short val = data.getAt(idx);
									if (value > 0)
										data.setAt(idx, (short) (val + bit));
									else
										data.setAt(idx, (short) (val - bit));
								}
						} else {
							if (r == 0) {
								data.setAt(idx, (short) s);
								break;
							}

							--r;
						}
					}
				} while (k <= spec_end);
			}
		}

		return 1;
	}

	private static short stbi__clamp(int x) {
		if (x < 0)
			return 0;
		if (x > 255)
			return (short) 255;

		return (short) x;
	}

	private static void stbi__idct_block(ShortFakePtr _out_, int out_stride, ShortFakePtr data) {
		int i = 0;
		Integer[] val = new Integer[64];
		FakePtr<Integer> v = new FakePtr<>(val);
		ShortFakePtr o;
		ShortFakePtr d = data;
		for (i = 0; i < 8; ++i, d.increase(), v.increase())
			if (d.getAt(8) == 0 && d.getAt(16) == 0 && d.getAt(24) == 0 && d.getAt(32) == 0 && d.getAt(40) == 0 && d.getAt(48) == 0 && d.getAt(56) == 0) {
				int dcterm = d.getAt(0) * 4;
				v.setAt(0, dcterm);
				v.setAt(8, dcterm);
				v.setAt(16, dcterm);
				v.setAt(24, dcterm);
				v.setAt(32, dcterm);
				v.setAt(40, dcterm);
				v.setAt(48, dcterm);
				v.setAt(56, dcterm);
			} else {
				int t0 = 0;
				int t1 = 0;
				int t2 = 0;
				int t3 = 0;
				int p1 = 0;
				int p2 = 0;
				int p3 = 0;
				int p4 = 0;
				int p5 = 0;
				int x0 = 0;
				int x1 = 0;
				int x2 = 0;
				int x3 = 0;
				p2 = d.getAt(16);
				p3 = d.getAt(48);
				p1 = (p2 + p3) * (int) (0.5411961f * 4096 + 0.5);
				t2 = p1 + p3 * (int) (-1.847759065f * 4096 + 0.5);
				t3 = p1 + p2 * (int) (0.765366865f * 4096 + 0.5);
				p2 = d.getAt(0);
				p3 = d.getAt(32);
				t0 = (p2 + p3) * 4096;
				t1 = (p2 - p3) * 4096;
				x0 = t0 + t3;
				x3 = t0 - t3;
				x1 = t1 + t2;
				x2 = t1 - t2;
				t0 = d.getAt(56);
				t1 = d.getAt(40);
				t2 = d.getAt(24);
				t3 = d.getAt(8);
				p3 = t0 + t2;
				p4 = t1 + t3;
				p1 = t0 + t3;
				p2 = t1 + t2;
				p5 = (p3 + p4) * (int) (1.175875602f * 4096 + 0.5);
				t0 = t0 * (int) (0.298631336f * 4096 + 0.5);
				t1 = t1 * (int) (2.053119869f * 4096 + 0.5);
				t2 = t2 * (int) (3.072711026f * 4096 + 0.5);
				t3 = t3 * (int) (1.501321110f * 4096 + 0.5);
				p1 = p5 + p1 * (int) (-0.899976223f * 4096 + 0.5);
				p2 = p5 + p2 * (int) (-2.562915447f * 4096 + 0.5);
				p3 = p3 * (int) (-1.961570560f * 4096 + 0.5);
				p4 = p4 * (int) (-0.390180644f * 4096 + 0.5);
				t3 += p1 + p4;
				t2 += p2 + p3;
				t1 += p2 + p4;
				t0 += p1 + p3;
				x0 += 512;
				x1 += 512;
				x2 += 512;
				x3 += 512;
				v.setAt(0, (x0 + t3) >> 10);
				v.setAt(56, (x0 - t3) >> 10);
				v.setAt(8, (x1 + t2) >> 10);
				v.setAt(48, (x1 - t2) >> 10);
				v.setAt(16, (x2 + t1) >> 10);
				v.setAt(40, (x2 - t1) >> 10);
				v.setAt(24, (x3 + t0) >> 10);
				v.setAt(32, (x3 - t0) >> 10);
			}

		for (i = 0, v = new FakePtr<Integer>(val), o = _out_.clone(); i < 8; ++i, v.move(8), o.move(out_stride)) {
			int t0 = 0;
			int t1 = 0;
			int t2 = 0;
			int t3 = 0;
			int p1 = 0;
			int p2 = 0;
			int p3 = 0;
			int p4 = 0;
			int p5 = 0;
			int x0 = 0;
			int x1 = 0;
			int x2 = 0;
			int x3 = 0;
			p2 = v.getAt(2);
			p3 = v.getAt(6);
			p1 = (p2 + p3) * (int) (0.5411961f * 4096 + 0.5);
			t2 = p1 + p3 * (int) (-1.847759065f * 4096 + 0.5);
			t3 = p1 + p2 * (int) (0.765366865f * 4096 + 0.5);
			p2 = v.getAt(0);
			p3 = v.getAt(4);
			t0 = (p2 + p3) * 4096;
			t1 = (p2 - p3) * 4096;
			x0 = t0 + t3;
			x3 = t0 - t3;
			x1 = t1 + t2;
			x2 = t1 - t2;
			t0 = v.getAt(7);
			t1 = v.getAt(5);
			t2 = v.getAt(3);
			t3 = v.getAt(1);
			p3 = t0 + t2;
			p4 = t1 + t3;
			p1 = t0 + t3;
			p2 = t1 + t2;
			p5 = (p3 + p4) * (int) (1.175875602f * 4096 + 0.5);
			t0 = t0 * (int) (0.298631336f * 4096 + 0.5);
			t1 = t1 * (int) (2.053119869f * 4096 + 0.5);
			t2 = t2 * (int) (3.072711026f * 4096 + 0.5);
			t3 = t3 * (int) (1.501321110f * 4096 + 0.5);
			p1 = p5 + p1 * (int) (-0.899976223f * 4096 + 0.5);
			p2 = p5 + p2 * (int) (-2.562915447f * 4096 + 0.5);
			p3 = p3 * (int) (-1.961570560f * 4096 + 0.5);
			p4 = p4 * (int) (-0.390180644f * 4096 + 0.5);
			t3 += p1 + p4;
			t2 += p2 + p3;
			t1 += p2 + p4;
			t0 += p1 + p3;
			x0 += 65536 + (128 << 17);
			x1 += 65536 + (128 << 17);
			x2 += 65536 + (128 << 17);
			x3 += 65536 + (128 << 17);
			o.setAt(0, stbi__clamp((x0 + t3) >> 17));
			o.setAt(7, stbi__clamp((x0 - t3) >> 17));
			o.setAt(1, stbi__clamp((x1 + t2) >> 17));
			o.setAt(6, stbi__clamp((x1 - t2) >> 17));
			o.setAt(2, stbi__clamp((x2 + t1) >> 17));
			o.setAt(5, stbi__clamp((x2 - t1) >> 17));
			o.setAt(3, stbi__clamp((x3 + t0) >> 17));
			o.setAt(4, stbi__clamp((x3 - t0) >> 17));
		}
	}

	private int stbi__get_marker() throws Exception {
		int x = 0;
		if (marker != 0xff) {
			x = marker;
			marker = 0xff;
			return x;
		}

		x = stbi__get8();
		if (x != 0xff)
			return 0xff;
		while (x == 0xff) x = stbi__get8();
		return x;
	}

	private void stbi__jpeg_reset() {
		code_bits = 0;
		code_buffer = 0;
		nomore = 0;
		img_comp[0].dc_pred = img_comp[1].dc_pred = img_comp[2].dc_pred = img_comp[3].dc_pred = 0;
		marker = 0xff;
		todo = restart_interval != 0 ? restart_interval : 0x7fffffff;
		eob_run = 0;
	}

	private int stbi__parse_entropy_coded_data() throws Exception {
		stbi__jpeg_reset();
		if (progressive == 0) {
			if (scan_n == 1) {
				int i = 0;
				int j = 0;
				short[] data = new short[64];
				int n = order[0];
				int w = (img_comp[n].x + 7) >> 3;
				int h = (img_comp[n].y + 7) >> 3;
				for (j = 0; j < h; ++j)
					for (i = 0; i < w; ++i) {
						int ha = img_comp[n].ha;
						if (stbi__jpeg_decode_block(data, huff_dc[img_comp[n].hd], huff_ac[ha], fast_ac[ha], n,
								dequant[img_comp[n].tq]) == 0)
							return 0;
						idct_block_kernel.Do(new ShortFakePtr(img_comp[n].data, img_comp[n].w2 * j * 8 + i * 8),
								img_comp[n].w2,
								new ShortFakePtr(data));
						if (--todo <= 0) {
							if (code_bits < 24)
								stbi__grow_buffer_unsafe();
							if (!(marker >= 0xd0 && marker <= 0xd7))
								return 1;
							stbi__jpeg_reset();
						}
					}

				return 1;
			} else {
				int i = 0;
				int j = 0;
				int k = 0;
				int x = 0;
				int y = 0;
				short[] data = new short[64];
				for (j = 0; j < img_mcu_y; ++j)
					for (i = 0; i < img_mcu_x; ++i) {
						for (k = 0; k < scan_n; ++k) {
							int n = order[k];
							for (y = 0; y < img_comp[n].v; ++y)
								for (x = 0; x < img_comp[n].h; ++x) {
									int x2 = (i * img_comp[n].h + x) * 8;
									int y2 = (j * img_comp[n].v + y) * 8;
									int ha = img_comp[n].ha;
									if (stbi__jpeg_decode_block(data, huff_dc[img_comp[n].hd], huff_ac[ha], fast_ac[ha], n,
											dequant[img_comp[n].tq]) == 0)
										return 0;
									idct_block_kernel.Do(new ShortFakePtr(img_comp[n].data,
													img_comp[n].w2 * y2 + x2), img_comp[n].w2,
											new ShortFakePtr(data));
								}
						}

						if (--todo <= 0) {
							if (code_bits < 24)
								stbi__grow_buffer_unsafe();
							if (!(marker >= 0xd0 && marker <= 0xd7))
								return 1;
							stbi__jpeg_reset();
						}
					}

				return 1;
			}
		}

		if (scan_n == 1) {
			int i = 0;
			int j = 0;
			int n = order[0];
			int w = (img_comp[n].x + 7) >> 3;
			int h = (img_comp[n].y + 7) >> 3;
			for (j = 0; j < h; ++j)
				for (i = 0; i < w; ++i) {
					ShortFakePtr data = new ShortFakePtr(img_comp[n].coeff, 64 * (i + j * img_comp[n].coeff_w));
					if (spec_start == 0) {
						if (stbi__jpeg_decode_block_prog_dc(data, huff_dc[img_comp[n].hd], n) == 0)
							return 0;
					} else {
						int ha = img_comp[n].ha;
						if (stbi__jpeg_decode_block_prog_ac(data, huff_ac[ha], fast_ac[ha]) == 0)
							return 0;
					}

					if (--todo <= 0) {
						if (code_bits < 24)
							stbi__grow_buffer_unsafe();
						if (!(marker >= 0xd0 && marker <= 0xd7))
							return 1;
						stbi__jpeg_reset();
					}
				}

			return 1;
		} else {
			int i = 0;
			int j = 0;
			int k = 0;
			int x = 0;
			int y = 0;
			for (j = 0; j < img_mcu_y; ++j)
				for (i = 0; i < img_mcu_x; ++i) {
					for (k = 0; k < scan_n; ++k) {
						int n = order[k];
						for (y = 0; y < img_comp[n].v; ++y)
							for (x = 0; x < img_comp[n].h; ++x) {
								int x2 = i * img_comp[n].h + x;
								int y2 = j * img_comp[n].v + y;
								ShortFakePtr data = new ShortFakePtr(img_comp[n].coeff, 64 * (x2 + y2 * img_comp[n].coeff_w));
								if (stbi__jpeg_decode_block_prog_dc(data, huff_dc[img_comp[n].hd], n) == 0)
									return 0;
							}
					}

					if (--todo <= 0) {
						if (code_bits < 24)
							stbi__grow_buffer_unsafe();
						if (!(marker >= 0xd0 && marker <= 0xd7))
							return 1;
						stbi__jpeg_reset();
					}
				}

			return 1;
		}
	}

	private static void stbi__jpeg_dequantize(ShortFakePtr data, int[] dequant) {
		int i = 0;
		for (i = 0; i < 64; ++i) {
			short val = data.getAt(i);
			val *= (short) dequant[i];
			data.setAt(i, val);
		}
	}

	private void stbi__jpeg_finish() {
		if (progressive != 0) {
			int i = 0;
			int j = 0;
			int n = 0;
			for (n = 0; n < img_n; ++n) {
				int w = (img_comp[n].x + 7) >> 3;
				int h = (img_comp[n].y + 7) >> 3;
				for (j = 0; j < h; ++j)
					for (i = 0; i < w; ++i) {
						ShortFakePtr data = new ShortFakePtr(img_comp[n].coeff, 64 * (i + j * img_comp[n].coeff_w));
						stbi__jpeg_dequantize(data, dequant[img_comp[n].tq]);
						idct_block_kernel.Do(new ShortFakePtr(img_comp[n].data, img_comp[n].w2 * j * 8 + i * 8),
								img_comp[n].w2,
								data);
					}
			}
		}
	}

	private int stbi__process_marker(int m) throws Exception {
		int L = 0;
		switch (m) {
			case 0xff:
				stbi__err("expected marker");
				break;
			case 0xDD:
				if (stbi__get16be() != 4)
					stbi__err("bad DRI len");
				restart_interval = stbi__get16be();
				return 1;
			case 0xDB:
				L = stbi__get16be() - 2;
				while (L > 0) {
					int q = (int) stbi__get8();
					int p = q >> 4;
					int sixteen = p != 0 ? 1 : 0;
					int t = q & 15;
					int i = 0;
					if (p != 0 && p != 1)
						stbi__err("bad DQT type");
					if (t > 3)
						stbi__err("bad DQT table");
					for (i = 0; i < 64; ++i)
						dequant[t][stbi__jpeg_dezigzag[i]] =
								(int) (sixteen != 0 ? stbi__get16be() : stbi__get8());
					L -= sixteen != 0 ? 129 : 65;
				}

				return L == 0 ? 1 : 0;
			case 0xC4:
				L = stbi__get16be() - 2;
				while (L > 0) {
					short[] v;
					int[] sizes = new int[16];
					int i = 0;
					int n = 0;
					int q = (int) stbi__get8();
					int tc = q >> 4;
					int th = q & 15;
					if (tc > 1 || th > 3)
						stbi__err("bad DHT header");
					for (i = 0; i < 16; ++i) {
						sizes[i] = stbi__get8();
						n += sizes[i];
					}

					L -= 17;
					if (tc == 0) {
						if (stbi__build_huffman(huff_dc[th], sizes) == 0)
							return 0;
						v = huff_dc[th].values;
					} else {
						if (stbi__build_huffman(huff_ac[th], sizes) == 0)
							return 0;
						v = huff_ac[th].values;
					}

					for (i = 0; i < n; ++i) v[i] = (short) stbi__get8();
					if (tc != 0)
						stbi__build_fast_ac(fast_ac[th], huff_ac[th]);
					L -= n;
				}

				return L == 0 ? 1 : 0;
		}

		if (m >= 0xE0 && m <= 0xEF || m == 0xFE) {
			L = stbi__get16be();
			if (L < 2) {
				if (m == 0xFE)
					stbi__err("bad COM len");
				else
					stbi__err("bad APP len");
			}

			L -= 2;
			if (m == 0xE0 && L >= 5) {
				short[] tag = new short[5];
				tag[0] = (short) 'J';
				tag[1] = (short) 'F';
				tag[2] = (short) 'I';
				tag[3] = (short) 'F';
				tag[4] = (short) '\0';
				int ok = 1;
				int i = 0;
				for (i = 0; i < 5; ++i)
					if (stbi__get8() != tag[i])
						ok = 0;
				L -= 5;
				if (ok != 0)
					jfif = 1;
			} else if (m == 0xEE && L >= 12) {
				short[] tag = new short[6];
				tag[0] = (short) 'A';
				tag[1] = (short) 'd';
				tag[2] = (short) 'o';
				tag[3] = (short) 'b';
				tag[4] = (short) 'e';
				tag[5] = (short) '\0';
				int ok = 1;
				int i = 0;
				for (i = 0; i < 6; ++i)
					if (stbi__get8() != tag[i])
						ok = 0;
				L -= 6;
				if (ok != 0) {
					stbi__get8();
					stbi__get16be();
					stbi__get16be();
					app14_color_transform = stbi__get8();
					L -= 6;
				}
			}

			stbi__skip(L);
			return 1;
		}

		return 0;
	}

	private int stbi__process_scan_header() throws Exception {
		int i = 0;
		int Ls = stbi__get16be();
		scan_n = stbi__get8();
		if (scan_n < 1 || scan_n > 4 || scan_n > img_n)
			stbi__err("bad SOS component count");
		if (Ls != 6 + 2 * scan_n)
			stbi__err("bad SOS len");
		for (i = 0; i < scan_n; ++i) {
			int id = (int) stbi__get8();
			int which = 0;
			int q = (int) stbi__get8();
			for (which = 0; which < img_n; ++which)
				if (img_comp[which].id == id)
					break;
			if (which == img_n)
				return 0;
			img_comp[which].hd = q >> 4;
			if (img_comp[which].hd > 3)
				stbi__err("bad DC huff");
			img_comp[which].ha = q & 15;
			if (img_comp[which].ha > 3)
				stbi__err("bad AC huff");
			order[i] = which;
		}

		{
			int aa = 0;
			spec_start = stbi__get8();
			spec_end = stbi__get8();
			aa = stbi__get8();
			succ_high = aa >> 4;
			succ_low = aa & 15;
			if (progressive != 0) {
				if (spec_start > 63 || spec_end > 63 || spec_start > spec_end || succ_high > 13 || succ_low > 13)
					stbi__err("bad SOS");
			} else {
				if (spec_start != 0)
					stbi__err("bad SOS");
				if (succ_high != 0 || succ_low != 0)
					stbi__err("bad SOS");
				spec_end = 63;
			}
		}

		return 1;
	}

	private int stbi__free_jpeg_components(int ncomp, int why) {
		int i = 0;
		for (i = 0; i < ncomp; ++i) {
			if (img_comp[i].raw_data != null) {
				img_comp[i].raw_data = null;
				img_comp[i].data = null;
			}

			if (img_comp[i].raw_coeff != null) {
				img_comp[i].raw_coeff = null;
				img_comp[i].coeff = null;
			}

			if (img_comp[i].linebuf != null) img_comp[i].linebuf = null;
		}

		return why;
	}

	private int stbi__process_frame_header(int scan) throws Exception {
		int Lf = 0;
		int p = 0;
		int i = 0;
		int q = 0;
		int h_max = 1;
		int v_max = 1;
		int c = 0;
		Lf = stbi__get16be();
		if (Lf < 11)
			stbi__err("bad SOF len");
		p = stbi__get8();
		if (p != 8)
			stbi__err("only 8-bit");
		img_y = stbi__get16be();
		if (img_y == 0)
			stbi__err("no header height");
		img_x = stbi__get16be();
		if (img_x == 0)
			stbi__err("0 width");
		c = stbi__get8();
		if (c != 3 && c != 1 && c != 4)
			stbi__err("bad component count");
		img_n = c;
		for (i = 0; i < c; ++i) {
			img_comp[i].data = null;
			img_comp[i].linebuf = null;
		}

		if (Lf != 8 + 3 * img_n)
			stbi__err("bad SOF len");
		rgb = 0;
		for (i = 0; i < img_n; ++i) {
			short[] rgb = new short[3];
			rgb[0] = (short) 'R';
			rgb[1] = (short) 'G';
			rgb[2] = (short) 'B';
			img_comp[i].id = stbi__get8();
			if (img_n == 3 && img_comp[i].id == rgb[i])
				++this.rgb;
			q = stbi__get8();
			img_comp[i].h = q >> 4;
			if (img_comp[i].h == 0 || img_comp[i].h > 4)
				stbi__err("bad H");
			img_comp[i].v = q & 15;
			if (img_comp[i].v == 0 || img_comp[i].v > 4)
				stbi__err("bad V");
			img_comp[i].tq = stbi__get8();
			if (img_comp[i].tq > 3)
				stbi__err("bad TQ");
		}

		if (scan != STBI__SCAN_load)
			return 1;
		for (i = 0; i < img_n; ++i) {
			if (img_comp[i].h > h_max)
				h_max = img_comp[i].h;
			if (img_comp[i].v > v_max)
				v_max = img_comp[i].v;
		}

		img_h_max = h_max;
		img_v_max = v_max;
		img_mcu_w = h_max * 8;
		img_mcu_h = v_max * 8;
		img_mcu_x = (img_x + img_mcu_w - 1) / img_mcu_w;
		img_mcu_y = (img_y + img_mcu_h - 1) / img_mcu_h;
		for (i = 0; i < img_n; ++i) {
			img_comp[i].x = (img_x * img_comp[i].h + h_max - 1) / h_max;
			img_comp[i].y = (img_y * img_comp[i].v + v_max - 1) / v_max;
			img_comp[i].w2 = img_mcu_x * img_comp[i].h * 8;
			img_comp[i].h2 = img_mcu_y * img_comp[i].v * 8;
			img_comp[i].coeff = null;
			img_comp[i].raw_coeff = null;
			img_comp[i].linebuf = null;
			img_comp[i].raw_data = new short[img_comp[i].w2 * img_comp[i].h2 + 15];
			img_comp[i].data = new ShortFakePtr(img_comp[i].raw_data);
			if (progressive != 0) {
				img_comp[i].coeff_w = img_comp[i].w2 / 8;
				img_comp[i].coeff_h = img_comp[i].h2 / 8;
				img_comp[i].raw_coeff = new short[img_comp[i].w2 * img_comp[i].h2 + 15];
				img_comp[i].coeff = new ShortFakePtr(img_comp[i].raw_coeff);
			}
		}

		return 1;
	}

	private boolean stbi__decode_jpeg_header(int scan) throws Exception {
		jfif = 0;
		app14_color_transform = -1;
		marker = 0xff;
		int m = stbi__get_marker();
		if (!(m == 0xd8)) {
			if (scan == STBI__SCAN_type)
				return false;
			stbi__err("no SOI");
		}

		if (scan == STBI__SCAN_type)
			return true;
		m = stbi__get_marker();
		while (!(m == 0xc0 || m == 0xc1 || m == 0xc2)) {
			if (stbi__process_marker(m) == 0)
				return false;
			m = stbi__get_marker();
			while (m == 0xff) {
				if (stbi__at_eof())
					stbi__err("no SOF");
				m = stbi__get_marker();
			}
		}

		progressive = m == 0xc2 ? 1 : 0;
		if (stbi__process_frame_header(scan) == 0)
			return false;
		return true;
	}

	private int stbi__decode_jpeg_image() throws Exception {
		int m = 0;
		for (m = 0; m < 4; m++) {
			img_comp[m].raw_data = null;
			img_comp[m].raw_coeff = null;
		}

		restart_interval = 0;
		if (!stbi__decode_jpeg_header(STBI__SCAN_load))
			return 0;
		m = stbi__get_marker();
		while (!(m == 0xd9)) {
			if (m == 0xda) {
				if (stbi__process_scan_header() == 0)
					return 0;
				if (stbi__parse_entropy_coded_data() == 0)
					return 0;
				if (marker == 0xff)
					while (!stbi__at_eof()) {
						int x = (int) stbi__get8();
						if (x == 255) {
							marker = stbi__get8();
							break;
						}
					}
			} else if (m == 0xdc) {
				int Ld = stbi__get16be();
				long NL = (long) stbi__get16be();
				if (Ld != 4)
					stbi__err("bad DNL len");
				if (NL != img_y)
					stbi__err("bad DNL height");
			} else {
				if (stbi__process_marker(m) == 0)
					return 0;
			}

			m = stbi__get_marker();
		}

		if (progressive != 0)
			stbi__jpeg_finish();
		return 1;
	}

	private static ShortFakePtr resample_row_1(ShortFakePtr _out_, ShortFakePtr in_near, ShortFakePtr in_far,
												 int w, int hs) {
		return in_near.clone();
	}

	private static ShortFakePtr stbi__resample_row_v_2(ShortFakePtr _out_, ShortFakePtr in_near,
														 ShortFakePtr in_far, int w, int hs) {
		int i = 0;
		for (i = 0; i < w; ++i) {
			short value = (short) ((3 * in_near.getAt(i) + in_far.getAt(i) + 2) >> 2);
			_out_.setAt(i, value);
		}
		return _out_.clone();
	}

	private static ShortFakePtr stbi__resample_row_h_2(ShortFakePtr _out_, ShortFakePtr in_near,
														 ShortFakePtr in_far, int w, int hs) {
		int i = 0;
		ShortFakePtr input = in_near;
		if (w == 1) {
			short value = input.getAt(0);
			_out_.setAt(0, value);
			_out_.setAt(1, value);
			return _out_;
		}

		_out_.setAt(0, input.getAt(0));
		_out_.setAt(1, (short) ((input.getAt(0) * 3 + input.getAt(1) + 2) >> 2));
		for (i = 1; i < w - 1; ++i) {
			int n = 3 * input.getAt(i) + 2;
			_out_.setAt(i * 2 + 0, (short) ((n + input.getAt(i - 1)) >> 2));
			_out_.setAt(i * 2 + 1, (short) ((n + input.getAt(i + 1)) >> 2));
		}

		_out_.setAt(i * 2 + 0, (short) ((input.getAt(w - 2) * 3 + input.getAt(w - 1) + 2) >> 2));
		_out_.setAt(i * 2 + 1, input.getAt(w - 1));
		return _out_.clone();
	}

	private static ShortFakePtr stbi__resample_row_hv_2(ShortFakePtr _out_, ShortFakePtr in_near,
														  ShortFakePtr in_far, int w, int hs) {
		int i = 0;
		int t0 = 0;
		int t1 = 0;
		if (w == 1) {
			short value = (short) ((3 * in_near.getAt(0) + in_far.getAt(0) + 2) >> 2);
			_out_.setAt(0, value);
			_out_.setAt(1, value);
			return _out_;
		}

		t1 = 3 * in_near.getAt(0) + in_far.getAt(0);
		_out_.setAt(0, (short) ((t1 + 2) >> 2));
		for (i = 1; i < w; ++i) {
			t0 = t1;
			t1 = 3 * in_near.getAt(i) + in_far.getAt(i);
			_out_.setAt(i * 2 - 1, (short) ((3 * t0 + t1 + 8) >> 4));
			_out_.setAt(i * 2, (short) ((3 * t1 + t0 + 8) >> 4));
		}

		_out_.setAt(w * 2 - 1, (short) ((t1 + 2) >> 2));
		return _out_.clone();
	}

	private static ShortFakePtr stbi__resample_row_generic(ShortFakePtr _out_, ShortFakePtr in_near,
															 ShortFakePtr in_far, int w, int hs) {
		int i = 0;
		int j = 0;
		for (i = 0; i < w; ++i)
			for (j = 0; j < hs; ++j)
				_out_.setAt(i * hs + j, in_near.getAt(i));
		return _out_.clone();
	}

	private static void stbi__YCbCr_to_RGB_row(ShortFakePtr out, ShortFakePtr y, ShortFakePtr pcb,
											   ShortFakePtr pcr, int count, int step) {
		ShortFakePtr _out_ = out.clone();
		int i = 0;
		for (i = 0; i < count; ++i) {
			int y_fixed = (y.getAt(i) << 20) + (1 << 19);
			int r = 0;
			int g = 0;
			int b = 0;
			int cr = pcr.getAt(i) - 128;
			int cb = pcb.getAt(i) - 128;
			r = y_fixed + cr * ((int) (1.40200f * 4096.0f + 0.5f) << 8);
			g = (int) (y_fixed + cr * -((int) (0.71414f * 4096.0f + 0.5f) << 8) +
					((cb * -((int) (0.34414f * 4096.0f + 0.5f) << 8)) & 0xffff0000));
			b = y_fixed + cb * ((int) (1.77200f * 4096.0f + 0.5f) << 8);
			r >>= 20;
			g >>= 20;
			b >>= 20;

			_out_.setAt(0, stbi__clamp(r));
			_out_.setAt(1, stbi__clamp(g));
			_out_.setAt(2, stbi__clamp(b));
			_out_.setAt(3, (short) 255);
			_out_.move(step);
		}
	}

	private void stbi__setup_jpeg() {
		idct_block_kernel = (output, out_stride, data) -> stbi__idct_block(output, out_stride, data);
		YCbCr_to_RGB_kernel = (output, y, pcb, pcr, count, step) -> stbi__YCbCr_to_RGB_row(output, y, pcb, pcr, count, step);
		resample_row_hv_2_kernel = (a, b, c, d, e) -> stbi__resample_row_hv_2(a, b, c, d, e);
	}

	private void stbi__cleanup_jpeg() {
		stbi__free_jpeg_components(img_n, 0);
	}

	private static short stbi__blinn_8x8(short x, short y) {
		long t = (long) (x * y + 128);
		return (short) ((t + (t >> 8)) >> 8);
	}

	private ImageResult load_jpeg_image(ColorComponents req_comp) throws Exception {
		int out_x, out_y, comp;
		out_x = out_y = comp = 0;

		int n = 0;
		int decode_n = 0;
		int is_rgb = 0;
		img_n = 0;

		if (stbi__decode_jpeg_image() == 0) {
			stbi__cleanup_jpeg();
			return null;
		}

		n = req_comp != null ? req_comp.getValue() : img_n >= 3 ? 3 : 1;
		is_rgb = img_n == 3 && (rgb == 3 || app14_color_transform == 0 && jfif == 0) ? 1 : 0;
		if (img_n == 3 && n < 3 && is_rgb == 0)
			decode_n = 1;
		else
			decode_n = img_n;
		int k = 0;
		int i = 0;
		int j = 0;
		short[] output;
		ArrayList<ShortFakePtr> coutput = new ArrayList<ShortFakePtr>();
		coutput.add(null);
		coutput.add(null);
		coutput.add(null);
		coutput.add(null);
		stbi__resample[] res_comp = new stbi__resample[4];
		for (int kkk = 0; kkk < res_comp.length; ++kkk)
			res_comp[kkk] = new stbi__resample();
		for (k = 0; k < decode_n; ++k) {
			stbi__resample r = res_comp[k];
			img_comp[k].linebuf = new short[img_x + 3];
			r.hs = img_h_max / img_comp[k].h;
			r.vs = img_v_max / img_comp[k].v;
			r.ystep = r.vs >> 1;
			r.w_lores = (img_x + r.hs - 1) / r.hs;
			r.ypos = 0;
			r.line0 = img_comp[k].data.clone();
			r.line1 = img_comp[k].data.clone();
			if (r.hs == 1 && r.vs == 1)
				r.resample = (a, b, c, d, e) -> resample_row_1(a, b, c, d, e);
			else if (r.hs == 1 && r.vs == 2)
				r.resample = (a, b, c, d, e) -> stbi__resample_row_v_2(a, b, c, d, e);
			else if (r.hs == 2 && r.vs == 1)
				r.resample = (a, b, c, d, e) -> stbi__resample_row_h_2(a, b, c, d, e);
			else if (r.hs == 2 && r.vs == 2)
				r.resample = resample_row_hv_2_kernel;
			else
				r.resample = (a, b, c, d, e) -> stbi__resample_row_generic(a, b, c, d, e);
		}

		output = new short[n * img_x * img_y];
		ShortFakePtr ptr = new ShortFakePtr(output);
		for (j = 0; j < img_y; ++j) {
			ShortFakePtr _out_ = ptr.cloneAdd(n * img_x * j);
			for (k = 0; k < decode_n; ++k) {
				stbi__resample r = res_comp[k];
				int y_bot = r.ystep >= r.vs >> 1 ? 1 : 0;

				coutput.set(k, r.resample.Do(new ShortFakePtr(img_comp[k].linebuf),
						y_bot != 0 ? r.line1 : r.line0,
						y_bot != 0 ? r.line0 : r.line1,
						r.w_lores, r.hs));
				if (++r.ystep >= r.vs) {
					r.ystep = 0;
					r.line0 = r.line1.clone();
					if (++r.ypos < img_comp[k].y)
						r.line1.move(img_comp[k].w2);
				}
			}

			if (n >= 3) {
				ShortFakePtr y = coutput.get(0);
				if (img_n == 3) {
					if (is_rgb != 0)
						for (i = 0; i < img_x; ++i) {
							_out_.setAt(0, y.getAt(i));
							_out_.setAt(1, coutput.get(1).getAt(i));
							_out_.setAt(2, coutput.get(2).getAt(i));
							_out_.setAt(3, (short) 255);
							_out_.move(n);
						}
					else
						YCbCr_to_RGB_kernel.Do(_out_, y, coutput.get(1), coutput.get(2), img_x, n);
				} else if (img_n == 4) {
					if (app14_color_transform == 0) {
						for (i = 0; i < img_x; ++i) {
							short m = coutput.get(3).getAt(i);
							_out_.setAt(0, stbi__blinn_8x8(coutput.get(0).getAt(i), m));
							_out_.setAt(1, stbi__blinn_8x8(coutput.get(1).getAt(i), m));
							_out_.setAt(2, stbi__blinn_8x8(coutput.get(2).getAt(i), m));
							_out_.setAt(3, (short) 255);
							_out_.move(n);
						}
					} else if (app14_color_transform == 2) {
						YCbCr_to_RGB_kernel.Do(_out_, y, coutput.get(1), coutput.get(2), img_x, n);
						for (i = 0; i < img_x; ++i) {
							short m = coutput.get(3).getAt(i);
							_out_.setAt(0, stbi__blinn_8x8((short) (255 - _out_.getAt(0)), m));
							_out_.setAt(1, stbi__blinn_8x8((short) (255 - _out_.getAt(1)), m));
							_out_.setAt(2, stbi__blinn_8x8((short) (255 - _out_.getAt(2)), m));
							_out_.move(n);
						}
					} else {
						YCbCr_to_RGB_kernel.Do(_out_, y, coutput.get(1), coutput.get(2), img_x, n);
					}
				} else {
					for (i = 0; i < img_x; ++i) {
						short v = y.getAt(i);
						_out_.setAt(0, v);
						_out_.setAt(1, v);
						_out_.setAt(2, v);
						_out_.setAt(3, (short) 255);
						_out_.move(n);
					}
				}
			} else {
				if (is_rgb != 0) {
					if (n == 1)
						for (i = 0; i < img_x; ++i) {
							_out_.setAndIncrease(Utility.stbi__compute_y(coutput.get(0).getAt(i), coutput.get(1).getAt(i), coutput.get(2).getAt(i)));
						}
					else
						for (i = 0; i < img_x; ++i, _out_.move(2)) {
							_out_.setAt(0, Utility.stbi__compute_y(coutput.get(0).getAt(i), coutput.get(1).getAt(i), coutput.get(2).getAt(i)));
							_out_.setAt(1, (short) 255);
						}
				} else if (img_n == 4 && app14_color_transform == 0) {
					for (i = 0; i < img_x; ++i) {
						short m = coutput.get(3).getAt(i);
						short r = stbi__blinn_8x8(coutput.get(0).getAt(i), m);
						short g = stbi__blinn_8x8(coutput.get(1).getAt(i), m);
						short b = stbi__blinn_8x8(coutput.get(2).getAt(i), m);
						_out_.setAt(0, Utility.stbi__compute_y(r, g, b));
						_out_.setAt(1, (short) 255);
						_out_.move(n);
					}
				} else if (img_n == 4 && app14_color_transform == 2) {
					for (i = 0; i < img_x; ++i) {
						_out_.setAt(0, stbi__blinn_8x8((short) (255 - coutput.get(0).getAt(i)), coutput.get(3).getAt(i)));
						_out_.setAt(1, (short) 255);
						_out_.move(n);
					}
				} else {
					ShortFakePtr y = coutput.get(0);
					if (n == 1)
						for (i = 0; i < img_x; ++i)
							_out_.setAt(i, y.getAt(i));
					else
						for (i = 0; i < img_x; ++i) {
							_out_.setAndIncrease(y.getAt(i));
							_out_.setAndIncrease((short) 255);
						}
				}
			}
		}

		stbi__cleanup_jpeg();
		out_x = img_x;
		out_y = img_y;
		comp = img_n >= 3 ? 3 : 1;

		return new ImageResult(out_x,
				out_y,
				ColorComponents.fromInt(comp),
				req_comp != null ? req_comp : ColorComponents.fromInt(comp),
				8,
				Utility.toByteArray(output));
	}

	private ImageResult InternalDecode(ColorComponents requiredComponents) throws Exception {
		stbi__setup_jpeg();

		return load_jpeg_image(requiredComponents);
	}

	public static boolean Test(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			JpgDecoder decoder = new JpgDecoder(stream);
			decoder.stbi__setup_jpeg();
			return decoder.stbi__decode_jpeg_header(STBI__SCAN_type);
		} catch (Exception ex) {
			return false;
		}
	}

	public static ImageInfo Info(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			JpgDecoder decoder = new JpgDecoder(stream);

			decoder.stbi__setup_jpeg();
			boolean r = decoder.stbi__decode_jpeg_header(STBI__SCAN_header);
			if (!r) return null;

			return new ImageInfo(decoder.img_x,
					decoder.img_y,
					decoder.img_n >= 3 ? ColorComponents.RedGreenBlue : ColorComponents.Grey,
					8);
		} catch (Exception ex) {
			return null;
		}
	}

	public static ImageResult Decode(byte[] data, ColorComponents requiredComponents) throws Exception {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		JpgDecoder decoder = new JpgDecoder(stream);
		return decoder.InternalDecode(requiredComponents);
	}

	public static ImageResult Decode(byte[] data) throws Exception {
		return Decode(data, null);
	}
}