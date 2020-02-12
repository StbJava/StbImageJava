package org.nothings.stb.image.decoding;

import org.nothings.stb.image.ColorComponents;
import org.nothings.stb.image.ImageInfo;
import org.nothings.stb.image.ImageResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class PsdDecoder extends Decoder {
	private PsdDecoder(InputStream stream) {
		super(stream);
	}

	private int stbi__psd_decode_rle(ShortFakePtr po, int pixelCount) throws Exception {
		ShortFakePtr p = po.clone();
		int count = 0;
		int nleft = 0;
		int len = 0;
		count = 0;
		while ((nleft = pixelCount - count) > 0) {
			len = stbi__get8();
			if (len == 128) {
			} else if (len < 128) {
				len++;
				if (len > nleft)
					return 0;
				count += len;
				while (len != 0) {
					p.set(stbi__get8());
					p.move(4);
					len--;
				}
			} else if (len > 128) {
				short val = 0;
				len = 257 - len;
				if (len > nleft)
					return 0;
				val = stbi__get8();
				count += len;
				while (len != 0) {
					p.set(val);
					p.move(4);
					len--;
				}
			}
		}

		return 1;
	}

	private ImageResult InternalDecode(ColorComponents requiredComponents, int bpc) throws Exception {
		int pixelCount = 0;
		int channelCount = 0;
		int compression = 0;
		int channel = 0;
		int i = 0;
		int bitdepth = 0;
		int w = 0;
		int h = 0;
		short[] _out_;
		if (stbi__get32be() != 0x38425053)
			stbi__err("not PSD");
		if (stbi__get16be() != 1)
			stbi__err("wrong version");
		stbi__skip(6);
		channelCount = stbi__get16be();
		if (channelCount < 0 || channelCount > 16)
			stbi__err("wrong channel count");
		h = (int) stbi__get32be();
		w = (int) stbi__get32be();
		bitdepth = stbi__get16be();
		if (bitdepth != 8 && bitdepth != 16)
			stbi__err("unsupported bit depth");
		if (stbi__get16be() != 3)
			stbi__err("wrong color format");
		stbi__skip((int) stbi__get32be());
		stbi__skip((int) stbi__get32be());
		stbi__skip((int) stbi__get32be());
		compression = stbi__get16be();
		if (compression > 1)
			stbi__err("bad compression");

		int bits_per_channel = 8;
		if (compression == 0 && bitdepth == 16 && bpc == 16) {
			_out_ = new short[8 * w * h];
			bits_per_channel = 16;
		} else {
			_out_ = new short[4 * w * h];
		}

		pixelCount = w * h;

		ShortFakePtr ptr = new ShortFakePtr(_out_);
		if (compression != 0) {
			stbi__skip(h * channelCount * 2);
			for (channel = 0; channel < 4; channel++) {
				ShortFakePtr p = new ShortFakePtr(ptr, channel);
				if (channel >= channelCount) {
					for (i = 0; i < pixelCount; i++, p.move(4)) p.set((short) (channel == 3 ? 255 : 0));
				} else {
					if (stbi__psd_decode_rle(p, pixelCount) == 0) stbi__err("corrupt");
				}
			}
		} else {
			for (channel = 0; channel < 4; channel++)
				if (channel >= channelCount) {
					if (bitdepth == 16 && bpc == 16)
						throw new UnsupportedOperationException("16-bit images are not supported yet");
					/*							int* q = ((int*)(ptr)) + channel;
												int val = (int)((channel) == (3) ? 65535 : 0);
												for (i = (int)(0); (i) < (pixelCount); i++, q += 4)
												{
													*q = (int)(val);
												}*/

					ShortFakePtr p = new ShortFakePtr(ptr, channel);
					short val = (short) (channel == 3 ? 255 : 0);
					for (i = 0; i < pixelCount; i++, p.move(4)) p.set(val);
				} else {
					if (bits_per_channel == 16)
						throw new UnsupportedOperationException("16-bit images are not supported yet");
					/*							int* q = ((int*)(ptr)) + channel;
												for (i = (int)(0); (i) < (pixelCount); i++, q += 4)
												{
													*q = ((int)(stbi__get16be()));
												}*/

					ShortFakePtr p = new ShortFakePtr(ptr, channel);
					if (bitdepth == 16)
						for (i = 0; i < pixelCount; i++, p.move(4))
							p.set((short) (stbi__get16be() >> 8));
					else
						for (i = 0; i < pixelCount; i++, p.move(4))
							p.set(stbi__get8());
				}
		}

		if (channelCount >= 4) {
			if (bits_per_channel == 16)
				throw new UnsupportedOperationException("16-bit images are not supported yet");
			/*					for (i = (int)(0); (i) < (w * h); ++i)
								{
									int* pixel = (int*)(ptr) + 4 * i;
									if ((pixel[3] != 0) && (pixel[3] != 65535))
									{
										float a = (float)(pixel[3] / 65535.0f);
										float ra = (float)(1.0f / a);
										float inv_a = (float)(65535.0f * (1 - ra));
										pixel[0] = ((int)(pixel[0] * ra + inv_a));
										pixel[1] = ((int)(pixel[1] * ra + inv_a));
										pixel[2] = ((int)(pixel[2] * ra + inv_a));
									}
								}*/
			for (i = 0; i < w * h; ++i) {
				ShortFakePtr pixel = new ShortFakePtr(ptr, 4 * i);
				if (pixel.getAt(3) != 0 && pixel.getAt(3) != 255) {
					float a = pixel.getAt(3) / 255.0f;
					float ra = 1.0f / a;
					float inv_a = 255.0f * (1 - ra);
					pixel.setAt(0, (short) (pixel.getAt(0) * ra + inv_a));
					pixel.setAt(1, (short) (pixel.getAt(1) * ra + inv_a));
					pixel.setAt(2, (short) (pixel.getAt(2) * ra + inv_a));
				}
			}
		}

		int req_comp = ColorComponents.toReqComp(requiredComponents);
		if (req_comp != 0 && req_comp != 4) {
			if (bits_per_channel == 16)
				_out_ = Utility.stbi__convert_format16(_out_, 4, req_comp, w, h);
			else
				_out_ = Utility.stbi__convert_format(_out_, 4, req_comp, w, h);
		}

		return new ImageResult(w,
				h,
				ColorComponents.RedGreenBlueAlpha,
				requiredComponents != null
						? requiredComponents
						: ColorComponents.RedGreenBlueAlpha,
				bits_per_channel,
				Utility.toByteArray(_out_)
		);
	}

	public static boolean Test(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			return Utility.stbi__get32be(stream) == 0x38425053;
		} catch (Exception ex) {
			return false;
		}
	}

	public static ImageInfo Info(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			if (Utility.stbi__get32be(stream) != 0x38425053) return null;

			if (Utility.stbi__get16be(stream) != 1) return null;

			Utility.stbi__skip(stream, 6);
			int channelCount = Utility.stbi__get16be(stream);
			if (channelCount < 0 || channelCount > 16) return null;

			int height = (int) Utility.stbi__get32be(stream);
			int width = (int) Utility.stbi__get32be(stream);
			int depth = Utility.stbi__get16be(stream);
			if (depth != 8 && depth != 16) return null;

			if (Utility.stbi__get16be(stream) != 3) return null;

			return new ImageInfo(width, height, ColorComponents.RedGreenBlueAlpha, depth);
		} catch (Exception ex) {
			return null;
		}
	}

	public static ImageResult Decode(byte[] data, ColorComponents requiredComponents, int bpc) throws Exception {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		PsdDecoder decoder = new PsdDecoder(stream);
		return decoder.InternalDecode(requiredComponents, bpc);
	}

	public static ImageResult Decode(byte[] data, ColorComponents requiredComponents) throws Exception {
		return Decode(data, requiredComponents, 8);
	}

	public static ImageResult Decode(byte[] data) throws Exception {
		return Decode(data, null);
	}
}