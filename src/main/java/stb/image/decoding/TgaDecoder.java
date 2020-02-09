package stb.image.decoding;

import stb.image.ColorComponents;
import stb.image.ImageInfo;
import stb.image.ImageResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class TgaDecoder extends Decoder {
	private TgaDecoder(InputStream stream) {
		super(stream);
	}

	private static Pair<Integer, Boolean> stbi__tga_get_comp(int bits_per_pixel, int is_grey) {
		switch (bits_per_pixel) {
			case 8:
				return new Pair<>(1, false);
			case 15:
			case 16:
				if (bits_per_pixel == 16 && is_grey != 0)
					return new Pair<>(2, false);
				;
				return new Pair<>(3, true);
			case 24:
			case 32:
				return new Pair<>(bits_per_pixel / 8, false);
			default:
				return null;
		}
	}

	private void stbi__tga_read_rgb16(ShortFakePtr _out_) throws Exception {
		int px = (int) stbi__get16le();
		int fiveBitMask = (int) 31;
		int r = (px >> 10) & fiveBitMask;
		int g = (px >> 5) & fiveBitMask;
		int b = px & fiveBitMask;
		_out_.setAt(0, (short) (r * 255 / 31));
		_out_.setAt(1, (short) (g * 255 / 31));
		_out_.setAt(2, (short) (b * 255 / 31));
	}

	private ImageResult InternalDecode(ColorComponents requiredComponents) throws Exception {
		int tga_offset = (int) stbi__get8();
		int tga_indexed = (int) stbi__get8();
		int tga_image_type = (int) stbi__get8();
		int tga_is_RLE = 0;
		int tga_palette_start = stbi__get16le();
		int tga_palette_len = stbi__get16le();
		int tga_palette_bits = (int) stbi__get8();
		int tga_x_origin = stbi__get16le();
		int tga_y_origin = stbi__get16le();
		int tga_width = stbi__get16le();
		int tga_height = stbi__get16le();
		int tga_bits_per_pixel = (int) stbi__get8();
		int tga_comp = 0;
		int tga_rgb16 = 0;
		int tga_inverted = (int) stbi__get8();
		short[] tga_data;
		short[] tga_palette = null;
		int i = 0;
		int j = 0;
		short[] raw_data = new short[4];
		raw_data[0] = 0;

		int RLE_count = 0;
		int RLE_repeating = 0;
		int read_next_pixel = 1;
		if (tga_image_type >= 8) {
			tga_image_type -= 8;
			tga_is_RLE = 1;
		}

		tga_inverted = 1 - ((tga_inverted >> 5) & 1);

		Pair<Integer, Boolean> get_comp;

		if (tga_indexed != 0)
			get_comp = stbi__tga_get_comp(tga_palette_bits, 0);
		else
			get_comp = stbi__tga_get_comp(tga_bits_per_pixel, tga_image_type == 3 ? 1 : 0);

		tga_comp = get_comp.value1;
		if (tga_comp == 0)
			stbi__err("bad format");

		tga_data = new short[tga_width * tga_height * tga_comp];
		stbi__skip(tga_offset);
		if (tga_indexed == 0 && tga_is_RLE == 0 && tga_rgb16 == 0) {
			for (i = 0; i < tga_height; ++i) {
				int row = tga_inverted != 0 ? tga_height - i - 1 : i;
				stbi__getn(tga_data, row * tga_width * tga_comp, tga_width * tga_comp);
			}
		} else {
			if (tga_indexed != 0) {
				stbi__skip(tga_palette_start);
				tga_palette = new short[tga_palette_len * tga_comp];
				if (tga_rgb16 != 0) {
					ShortFakePtr pal_entry = new ShortFakePtr(tga_palette);
					for (i = 0; i < tga_palette_len; ++i) {
						stbi__tga_read_rgb16(pal_entry);
						pal_entry.move(tga_comp);
					}
				} else if (!stbi__getn(tga_palette, 0, tga_palette_len * tga_comp)) {
					stbi__err("bad palette");
				}
			}

			for (i = 0; i < tga_width * tga_height; ++i) {
				if (tga_is_RLE != 0) {
					if (RLE_count == 0) {
						int RLE_cmd = (int) stbi__get8();
						RLE_count = 1 + (RLE_cmd & 127);
						RLE_repeating = RLE_cmd >> 7;
						read_next_pixel = 1;
					} else if (RLE_repeating == 0) {
						read_next_pixel = 1;
					}
				} else {
					read_next_pixel = 1;
				}

				if (read_next_pixel != 0) {
					if (tga_indexed != 0) {
						int pal_idx = tga_bits_per_pixel == 8 ? stbi__get8() : stbi__get16le();
						if (pal_idx >= tga_palette_len) pal_idx = 0;
						pal_idx *= tga_comp;
						for (j = 0; j < tga_comp; ++j) raw_data[j] = tga_palette[pal_idx + j];
					} else if (tga_rgb16 != 0) {
						stbi__tga_read_rgb16(new ShortFakePtr(raw_data));
					} else {
						for (j = 0; j < tga_comp; ++j) raw_data[j] = stbi__get8();
					}

					read_next_pixel = 0;
				}

				for (j = 0; j < tga_comp; ++j) tga_data[i * tga_comp + j] = raw_data[j];
				--RLE_count;
			}

			if (tga_inverted != 0)
				for (j = 0; j * 2 < tga_height; ++j) {
					int index1 = j * tga_width * tga_comp;
					int index2 = (tga_height - 1 - j) * tga_width * tga_comp;
					for (i = tga_width * tga_comp; i > 0; --i) {
						Short temp = tga_data[index1];
						tga_data[index1] = tga_data[index2];
						tga_data[index2] = temp;
						++index1;
						++index2;
					}
				}
		}

		if (tga_comp >= 3 && tga_rgb16 == 0) {
			ShortFakePtr tga_pixel = new ShortFakePtr(tga_data);
			for (i = 0; i < tga_width * tga_height; ++i) {
				Short temp = tga_pixel.getAt(0);
				tga_pixel.setAt(0, tga_pixel.getAt(2));
				tga_pixel.setAt(2, temp);
				tga_pixel.move(tga_comp);
			}
		}

		int req_comp = ColorComponents.toReqComp(requiredComponents);
		if (req_comp != 0 && req_comp != tga_comp)
			tga_data = Utility.stbi__convert_format(tga_data, tga_comp, req_comp, tga_width, tga_height);
		tga_palette_start = tga_palette_len = tga_palette_bits = tga_x_origin = tga_y_origin = 0;

		Utility.clampResult(tga_data);
		return new ImageResult(tga_width,
				tga_height,
				ColorComponents.fromInt(tga_comp),
				requiredComponents != null ? requiredComponents : ColorComponents.fromInt(tga_comp),
				8,
				tga_data);
	}

	public static boolean Test(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);

			Utility.stbi__get8(stream);
			int tga_color_type = (int) Utility.stbi__get8(stream);
			if (tga_color_type > 1)
				return false;
			int sz = (int) Utility.stbi__get8(stream);
			if (tga_color_type == 1) {
				if (sz != 1 && sz != 9)
					return false;
				Utility.stbi__skip(stream, 4);
				sz = Utility.stbi__get8(stream);
				if (sz != 8 && sz != 15 && sz != 16 && sz != 24 && sz != 32)
					return false;
				Utility.stbi__skip(stream, 4);
			} else {
				if (sz != 2 && sz != 3 && sz != 10 && sz != 11)
					return false;
				Utility.stbi__skip(stream, 9);
			}

			if (Utility.stbi__get16le(stream) < 1)
				return false;
			if (Utility.stbi__get16le(stream) < 1)
				return false;
			sz = Utility.stbi__get8(stream);
			if (tga_color_type == 1 && sz != 8 && sz != 16)
				return false;
			if (sz != 8 && sz != 15 && sz != 16 && sz != 24 && sz != 32)
				return false;

			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public static ImageInfo Info(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);

			int tga_w = 0;
			int tga_h = 0;
			int tga_comp = 0;
			int tga_image_type = 0;
			int tga_bits_per_pixel = 0;
			int tga_colormap_bpp = 0;
			int sz = 0;
			int tga_colormap_type = 0;
			Utility.stbi__get8(stream);
			tga_colormap_type = Utility.stbi__get8(stream);
			if (tga_colormap_type > 1) return null;

			tga_image_type = Utility.stbi__get8(stream);
			if (tga_colormap_type == 1) {
				if (tga_image_type != 1 && tga_image_type != 9) return null;
				Utility.stbi__skip(stream, 4);
				sz = Utility.stbi__get8(stream);
				if (sz != 8 && sz != 15 && sz != 16 && sz != 24 && sz != 32) return null;
				Utility.stbi__skip(stream, 4);
				tga_colormap_bpp = sz;
			} else {
				if (tga_image_type != 2 && tga_image_type != 3 && tga_image_type != 10 && tga_image_type != 11)
					return null;
				Utility.stbi__skip(stream, 9);
				tga_colormap_bpp = 0;
			}

			tga_w = Utility.stbi__get16le(stream);
			if (tga_w < 1) return null;

			tga_h = Utility.stbi__get16le(stream);
			if (tga_h < 1) return null;

			tga_bits_per_pixel = Utility.stbi__get8(stream);
			Utility.stbi__get8(stream);

			Pair<Integer, Boolean> get_comp;

			if (tga_colormap_bpp != 0) {
				if (tga_bits_per_pixel != 8 && tga_bits_per_pixel != 16) return null;
				get_comp = stbi__tga_get_comp(tga_colormap_bpp, 0);
			} else {
				get_comp = stbi__tga_get_comp(tga_bits_per_pixel,
						tga_image_type == 3 || tga_image_type == 11 ? 1 : 0);
			}

			tga_comp = get_comp.value1;

			if (tga_comp == 0) return null;

			return new ImageInfo(tga_w, tga_h, ColorComponents.fromInt(tga_comp), tga_bits_per_pixel);
		} catch (Exception ex) {
			return null;
		}
	}

	public static ImageResult Decode(byte[] data, ColorComponents requiredComponents) throws Exception {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		TgaDecoder decoder = new TgaDecoder(stream);
		return decoder.InternalDecode(requiredComponents);
	}

	public static ImageResult Decode(byte[] data) throws Exception {
		return Decode(data, null);
	}
}
