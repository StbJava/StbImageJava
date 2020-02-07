package stb.image.Decoding;

import stb.image.ColorComponents;
import stb.image.ImageInfo;
import stb.image.ImageResult;

import java.io.InputStream;

/*public class PsdDecoder extends Decoder
{
	private PsdDecoder(InputStream stream)
	{
		super(stream);
	}

	private int stbi__psd_decode_rle(FakePtr<Short> p, int pixelCount)
	{
		var count = 0;
		var nleft = 0;
		var len = 0;
		count = 0;
		while ((nleft = pixelCount - count) > 0)
		{
			len = stbi__get8();
			if (len == 128)
			{
			}
			else if (len < 128)
			{
				len++;
				if (len > nleft)
					return 0;
				count += len;
				while (len != 0)
				{
					p.Value = stbi__get8();
					p += 4;
					len--;
				}
			}
			else if (len > 128)
			{
				short val = 0;
				len = 257 - len;
				if (len > nleft)
					return 0;
				val = stbi__get8();
				count += len;
				while (len != 0)
				{
					p.Value = val;
					p += 4;
					len--;
				}
			}
		}

		return 1;
	}

	private ImageResult InternalDecode(ColorComponents  requiredComponents, int bpc)
	{
		var pixelCount = 0;
		var channelCount = 0;
		var compression = 0;
		var channel = 0;
		var i = 0;
		var bitdepth = 0;
		var w = 0;
		var h = 0;
		short[] _out_;
		if (stbi__get32be() != 0x38425053)
			stbi__err("not PSD");
		if (stbi__get16be() != 1)
			stbi__err("wrong version");
		stbi__skip(6);
		channelCount = stbi__get16be();
		if (channelCount < 0 || channelCount > 16)
			stbi__err("wrong channel count");
		h = (int)stbi__get32be();
		w = (int)stbi__get32be();
		bitdepth = stbi__get16be();
		if (bitdepth != 8 && bitdepth != 16)
			stbi__err("unsupported bit depth");
		if (stbi__get16be() != 3)
			stbi__err("wrong color format");
		stbi__skip((int)stbi__get32be());
		stbi__skip((int)stbi__get32be());
		stbi__skip((int)stbi__get32be());
		compression = stbi__get16be();
		if (compression > 1)
			stbi__err("bad compression");

		var bits_per_channel = 8;
		if (compression == 0 && bitdepth == 16 && bpc == 16)
		{
			_out_ = new short[8 * w * h];
			bits_per_channel = 16;
		}
		else
		{
			_out_ = new short[4 * w * h];
		}

		pixelCount = w * h;

		var ptr = new FakePtr<Short>(_out_);
		if (compression != 0)
		{
			stbi__skip(h * channelCount * 2);
			for (channel = 0; channel < 4; channel++)
			{
				FakePtr<Short> p;
				p = ptr + channel;
				if (channel >= channelCount)
				{
					for (i = 0; i < pixelCount; i++, p += 4) p.set((short)(channel == 3 ? 255 : 0));
				}
				else
				{
					if (stbi__psd_decode_rle(p, pixelCount) == 0) stbi__err("corrupt");
				}
			}
		}
		else
		{
			for (channel = 0; channel < 4; channel++)
				if (channel >= channelCount)
				{
					if (bitdepth == 16 && bpc == 16)
						throw new UnsupportedOperationException();
					/*							int* q = ((int*)(ptr)) + channel;
												int val = (int)((channel) == (3) ? 65535 : 0);
												for (i = (int)(0); (i) < (pixelCount); i++, q += 4)
												{
													*q = (int)(val);
												}*/

/*					var p = ptr + channel;
					var val = (short)(channel == 3 ? 255 : 0);
					for (i = 0; i < pixelCount; i++, p += 4) p.set(val);
				}
				else
				{
					if (bits_per_channel == 16)
						throw new UnsupportedOperationException();
					/*							int* q = ((int*)(ptr)) + channel;
												for (i = (int)(0); (i) < (pixelCount); i++, q += 4)
												{
													*q = ((int)(stbi__get16be()));
												}*/

/*					var p = ptr + channel;
					if (bitdepth == 16)
						for (i = 0; i < pixelCount; i++, p += 4)
							p.set((short)(stbi__get16be() >> 8));
					else
						for (i = 0; i < pixelCount; i++, p += 4)
							p.set(stbi__get8());
				}
		}

		if (channelCount >= 4)
		{
			if (bits_per_channel == 16)
				throw new UnsupportedOperationException();
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
/*			for (i = 0; i < w * h; ++i)
			{
				var pixel = ptr + 4 * i;
				if (pixel[3] != 0 && pixel[3] != 255)
				{
					var a = pixel[3] / 255.0f;
					var ra = 1.0f / a;
					var inv_a = 255.0f * (1 - ra);
					pixel[0] = (short)(pixel[0] * ra + inv_a);
					pixel[1] = (short)(pixel[1] * ra + inv_a);
					pixel[2] = (short)(pixel[2] * ra + inv_a);
				}
			}
		}

		var req_comp = requiredComponents.ToReqComp();
		if (req_comp != 0 && req_comp != 4)
		{
			if (bits_per_channel == 16)
				_out_ = Utility.stbi__convert_format16(_out_, 4, req_comp, (long)w, (long)h);
			else
				_out_ = Utility.stbi__convert_format(_out_, 4, req_comp, (long)w, (long)h);
		}

		return new ImageResult
		{
			Width = w,
			Height = h,
			SourceComponents = ColorComponents.RedGreenBlueAlpha,
			ColorComponents = requiredComponents != null
				? requiredComponents.Value
				: ColorComponents.RedGreenBlueAlpha,
			BitsPerChannel = bits_per_channel,
			Data = _out_
		};
	}

	public static boolean Test(InputStream stream)
	{
		var r = stream.stbi__get32be() == 0x38425053;
		stream.Rewind();

		return r;
	}

	public static ImageInfo Info(InputStream stream)
	{
		try
		{
			if (stream.stbi__get32be() != 0x38425053) return null;

			if (stream.stbi__get16be() != 1) return null;

			stream.stbi__skip(6);
			var channelCount = stream.stbi__get16be();
			if (channelCount < 0 || channelCount > 16) return null;

			var height = (int)stream.stbi__get32be();
			var width = (int)stream.stbi__get32be();
			var depth = stream.stbi__get16be();
			if (depth != 8 && depth != 16) return null;

			if (stream.stbi__get16be() != 3) return null;

			return new ImageInfo
			{
				Width = width,
				Height = height,
				ColorComponents = ColorComponents.RedGreenBlueAlpha,
				BitsPerChannel = depth
			};
		}
		finally
		{
			stream.Rewind();
		}
	}

	public static ImageResult Decode(InputStream stream, ColorComponents  requiredComponents, int bpc)
	{
		var decoder = new PsdDecoder(stream);
		return decoder.InternalDecode(requiredComponents, bpc);
	}

	public static ImageResult Decode(InputStream stream, ColorComponents  requiredComponents)
	{
		return Decode(stream, requiredComponents, 8);
	}

	public static ImageResult Decode(InputStream stream)
	{
		return Decode(stream, null);
	}
}*/
