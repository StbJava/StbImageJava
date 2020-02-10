package org.nothings.stb.image.decoding;

import org.nothings.stb.image.ColorComponents;
import org.nothings.stb.image.ImageInfo;
import org.nothings.stb.image.ImageResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

public class GifDecoder extends Decoder {
	private static class stbi__gif_lzw {
		public short prefix;
		public short first;
		public short suffix;
	}

	private int w;
	private int h;
	private short[] _out_;
	private short[] background;
	private short[] history;
	private int flags;
	private int bgindex;
	private int ratio;
	private int transparent;
	private int eflags;
	private int delay;
	private final short[] pal;
	private final short[] lpal;
	private final stbi__gif_lzw[] codes = new stbi__gif_lzw[8192];
	private short[] color_table;
	private int parse;
	private int step;
	private int lflags;
	private int start_x;
	private int start_y;
	private int max_x;
	private int max_y;
	private int cur_x;
	private int cur_y;
	private int line_size;

	private GifDecoder(InputStream stream) {
		super(stream);
		for (int i = 0; i < codes.length; ++i) {
			codes[i] = new stbi__gif_lzw();
		}
		pal = new short[256 * 4];
		lpal = new short[256 * 4];
	}

	private void stbi__gif_parse_colortable(short[] pal, int num_entries, int transp) throws Exception {
		int i;
		for (i = 0; i < num_entries; ++i) {
			pal[i * 4 + 2] = stbi__get8();
			pal[i * 4 + 1] = stbi__get8();
			pal[i * 4] = stbi__get8();
			pal[i * 4 + 3] = (short) (transp == i ? 0 : 255);
		}
	}

	private int stbi__gif_header(int is_info) throws Exception {
		short version = 0;
		if (stbi__get8() != 'G' || stbi__get8() != 'I' || stbi__get8() != 'F' || stbi__get8() != '8')
			stbi__err("not GIF");
		version = stbi__get8();
		if (version != '7' && version != '9')
			stbi__err("not GIF");
		if (stbi__get8() != 'a')
			stbi__err("not GIF");
		w = stbi__get16le();
		h = stbi__get16le();
		flags = stbi__get8();
		bgindex = stbi__get8();
		ratio = stbi__get8();
		transparent = -1;

		int comp = 4;
		if (is_info != 0)
			return comp;
		if ((flags & 0x80) != 0)
			stbi__gif_parse_colortable(pal, 2 << (flags & 7), -1);
		return comp;
	}

	private void stbi__out_gif_code(int code) {
		int idx = 0;
		if (codes[code].prefix >= 0)
			stbi__out_gif_code((int) codes[code].prefix);
		if (cur_y >= max_y)
			return;
		idx = cur_x + cur_y;
		history[idx / 4] = 1;
		ShortFakePtr c = new ShortFakePtr(color_table, codes[code].suffix * 4);
		if (c.getAt(3) > 128) {
			ShortFakePtr p = new ShortFakePtr(_out_, idx);
			p.setAt(0, c.getAt(2));
			p.setAt(1, c.getAt(1));
			p.setAt(2, c.getAt(0));
			p.setAt(3, c.getAt(3));
		}

		cur_x += 4;
		if (cur_x >= max_x) {
			cur_x = start_x;
			cur_y += step;
			while (cur_y >= max_y && parse > 0) {
				step = (1 << parse) * line_size;
				cur_y = start_y + (step >> 1);
				--parse;
			}
		}
	}

	private short[] stbi__process_gif_raster() throws Exception {
		short lzw_cs = 0;
		int len = 0;
		int init_code = 0;
		long first = 0;
		int codesize = 0;
		int codemask = 0;
		int avail = 0;
		int oldcode = 0;
		int bits = 0;
		int valid_bits = 0;
		int clear = 0;
		lzw_cs = stbi__get8();
		if (lzw_cs > 12)
			return null;
		clear = 1 << lzw_cs;
		first = 1;
		codesize = lzw_cs + 1;
		codemask = (1 << codesize) - 1;
		bits = 0;
		valid_bits = 0;
		for (init_code = 0; init_code < clear; init_code++) {
			codes[init_code].prefix = -1;
			codes[init_code].first = (short) init_code;
			codes[init_code].suffix = (short) init_code;
		}

		avail = clear + 2;
		oldcode = -1;
		len = 0;
		for (; ; )
			if (valid_bits < codesize) {
				if (len == 0) {
					len = stbi__get8();
					if (len == 0)
						return _out_;
				}

				--len;
				bits |= stbi__get8() << valid_bits;
				valid_bits += 8;
			} else {
				int code = bits & codemask;
				bits >>= codesize;
				valid_bits -= codesize;
				if (code == clear) {
					codesize = lzw_cs + 1;
					codemask = (1 << codesize) - 1;
					avail = clear + 2;
					oldcode = -1;
					first = 0;
				} else if (code == clear + 1) {
					stbi__skip(len);
					while ((len = stbi__get8()) > 0) stbi__skip(len);
					return _out_;
				} else if (code <= avail) {
					if (first != 0) stbi__err("no clear code");
					if (oldcode >= 0) {
						int idx = avail++;
						if (avail > 8192) stbi__err("too many codes");
						codes[idx].prefix = (short) oldcode;
						codes[idx].first = codes[oldcode].first;
						codes[idx].suffix = code == avail ? codes[idx].first : codes[code].first;
					} else if (code == avail) {
						stbi__err("illegal code in raster");
					}

					stbi__out_gif_code((int) code);
					if ((avail & codemask) == 0 && avail <= 0x0FFF) {
						codesize++;
						codemask = (1 << codesize) - 1;
					}

					oldcode = code;
				} else {
					stbi__err("illegal code in raster");
				}
			}
	}

	private Pair<short[], Integer> stbi__gif_load_next(ShortFakePtr two_back) throws Exception {
		int dispose = 0;
		int first_frame = 0;
		int pi = 0;
		int pcount = 0;
		first_frame = 0;

		int comp = 0;
		if (_out_ == null) {
			comp = stbi__gif_header(0);
			if (comp == 0)
				return null;
			pcount = w * h;
			_out_ = new short[4 * pcount];
			Arrays.fill(_out_, (short) 0);
			background = new short[4 * pcount];
			Arrays.fill(background, (short) 0);
			history = new short[pcount];
			Arrays.fill(history, (short) 0);
			first_frame = 1;
		} else {
			ShortFakePtr ptr = new ShortFakePtr(_out_);
			dispose = (eflags & 0x1C) >> 2;
			pcount = w * h;
			if (dispose == 3 && two_back == null) dispose = 2;
			if (dispose == 3) {
				for (pi = 0; pi < pcount; ++pi) {
					if (history[pi] != 0) {
						new ShortFakePtr(ptr, pi * 4).memcpy(new ShortFakePtr(two_back, pi * 4), 4);
					}
				}
			} else if (dispose == 2) {
				for (pi = 0; pi < pcount; ++pi)
					if (history[pi] != 0)
						new ShortFakePtr(ptr, pi * 4).memcpy(new ShortFakePtr(background, pi * 4), 4);
			}

			new ShortFakePtr(background).memcpy(ptr, 4 * w * h);
		}

		Arrays.fill(history, 0, w * h, (short) 0);
		for (; ; ) {
			int tag = (int) stbi__get8();
			switch (tag) {
				case 0x2C: {
					int x = 0;
					int y = 0;
					int w = 0;
					int h = 0;
					short[] o;
					x = stbi__get16le();
					y = stbi__get16le();
					w = stbi__get16le();
					h = stbi__get16le();
					if (x + w > w || y + h > h)
						stbi__err("bad Image Descriptor");
					line_size = w * 4;
					start_x = x * 4;
					start_y = y * line_size;
					max_x = start_x + w * 4;
					max_y = start_y + h * line_size;
					cur_x = start_x;
					cur_y = start_y;
					if (w == 0)
						cur_y = max_y;
					lflags = stbi__get8();
					if ((lflags & 0x40) != 0) {
						step = 8 * line_size;
						parse = 3;
					} else {
						step = line_size;
						parse = 0;
					}

					if ((lflags & 0x80) != 0) {
						stbi__gif_parse_colortable(lpal, 2 << (lflags & 7),
								(eflags & 0x01) != 0 ? transparent : -1);
						color_table = lpal;
					} else if ((flags & 0x80) != 0) {
						color_table = pal;
					} else {
						stbi__err("missing color table");
					}

					o = stbi__process_gif_raster();
					if (o == null)
						return null;
					pcount = w * h;
					if (first_frame != 0 && bgindex > 0)
						for (pi = 0; pi < pcount; ++pi)
							if (history[pi] == 0) {
								pal[bgindex * 4 + 3] = 255;
								new ShortFakePtr(_out_, pi * 4).memcpy(new ShortFakePtr(pal, bgindex), 4);
							}

					return new Pair<>(o, comp);
				}
				case 0x21: {
					int len = 0;
					int ext = (int) stbi__get8();
					if (ext == 0xF9) {
						len = stbi__get8();
						if (len == 4) {
							eflags = stbi__get8();
							delay = 10 * stbi__get16le();
							if (transparent >= 0) pal[transparent * 4 + 3] = 255;
							if ((eflags & 0x01) != 0) {
								transparent = stbi__get8();
								if (transparent >= 0) pal[transparent * 4 + 3] = 0;
							} else {
								stbi__skip(1);
								transparent = -1;
							}
						} else {
							stbi__skip(len);
							break;
						}
					}

					while ((len = stbi__get8()) != 0) stbi__skip(len);
					break;
				}
				case 0x3B:
					return null;
				default:
					stbi__err("unknown code");
					break;
			}
		}
	}

/*		private void* stbi__load_gif_main(int** delays, int* x, int* y, int* z, int* comp, int req_comp)
		{
			if ((IsGif(InputStream)))
			{
				int layers = (int)(0);
				short* u = null;
				short* _out_ = null;
				short* two_back = null;
				int stride = 0;
				if ((delays) != null)
				{
					*delays = null;
				}
				do
				{
					u = stbi__gif_load_next(comp, (int)(req_comp), two_back);
					if ((u) != null)
					{
						*x = (int)(w);
						*y = (int)(h);
						++layers;
						stride = (int)(w * h * 4);
						if ((_out_) != null)
						{
							_out_ = (short*)(CRuntime.realloc(_out_, (ulong)(layers * stride)));
							if ((delays) != null)
							{
								*delays = (int*)(CRuntime.realloc(*delays, (ulong)(sizeof(int) * layers)));
							}
						}
						else
						{
							_out_ = (short*)(Utility.stbi__malloc((ulong)(layers * stride)));
							if ((delays) != null)
							{
								*delays = (int*)(Utility.stbi__malloc((ulong)(layers * sizeof(int))));
							}
						}
						CRuntime.memcpy(_out_ + ((layers - 1) * stride), u, (ulong)(stride));
						if ((layers) >= (2))
						{
							two_back = _out_ - 2 * stride;
						}
						if ((delays) != null)
						{
							(*delays)[layers - 1U] = (int)(delay);
						}
					}
				}
				while (u != null);
				CRuntime.free(_out_);
				CRuntime.free(history);
				CRuntime.free(background);
				if (((req_comp) != 0) && (req_comp != 4))
					_out_ = stbi__convert_format(_out_, (int)(4), (int)(req_comp), (long)(layers * w), (long)(h));
				*z = (int)(layers);
				return _out_;
			}
			else
			{
				stbi__err("not GIF");
			}

		}*/

	private ImageResult InternalDecode(ColorComponents requiredComponents) throws Exception {
		int comp;

		Pair<short[], Integer> u = stbi__gif_load_next(null);
		if (u == null) throw new Exception("could not decode gif");

		short[] data = u.value1;
		if (requiredComponents != null && requiredComponents != ColorComponents.RedGreenBlueAlpha)
			data = Utility.stbi__convert_format(data, 4, ColorComponents.toReqComp(requiredComponents), w, h);

		return new ImageResult(w,
				h,
				ColorComponents.fromInt(u.value2),
				requiredComponents != null ? requiredComponents : ColorComponents.fromInt(u.value2),
				8,
				data);
	}

	private static boolean InternalTest(InputStream stream) throws Exception {
		int sz = 0;
		if (Utility.stbi__get8(stream) != 'G' || Utility.stbi__get8(stream) != 'I' || Utility.stbi__get8(stream) != 'F' ||
				Utility.stbi__get8(stream) != '8')
			return false;
		sz = Utility.stbi__get8(stream);
		if (sz != '9' && sz != '7')
			return false;
		if (Utility.stbi__get8(stream) != 'a')
			return false;
		return true;
	}

	public static boolean Test(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			return InternalTest(stream);
		} catch (Exception ex) {
			return false;
		}
	}

	public static ImageInfo Info(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			GifDecoder decoder = new GifDecoder(stream);

			int comp = decoder.stbi__gif_header(1);
			if (comp == 0) return null;

			return new ImageInfo(decoder.w, decoder.h, ColorComponents.fromInt(comp), 8);
		} catch (Exception ex) {
			return null;
		}
	}

	public static ImageResult Decode(byte[] data, ColorComponents requiredComponents) throws Exception {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		GifDecoder decoder = new GifDecoder(stream);
		return decoder.InternalDecode(requiredComponents);
	}

	public static ImageResult Decode(byte[] data) throws Exception {
		return Decode(data, null);
	}
}