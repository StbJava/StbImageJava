package org.nothings.stb.image.decoding;

import java.io.IOException;
import java.io.InputStream;

public class Decoder {
	protected static final int STBI__SCAN_load = 0;
	protected static final int STBI__SCAN_type = 1;
	protected static final int STBI__SCAN_header = 2;

	protected int img_x = 0;
	protected int img_y = 0;
	protected int img_n = 0;

	protected InputStream InputStream;

	protected Decoder(java.io.InputStream stream) {
		if (stream == null) {
			throw new NullPointerException("stream");
		}

		InputStream = stream;
	}

	protected long stbi__get32be() throws Exception {
		return Utility.stbi__get32be(InputStream);
	}

	protected int stbi__get16be() throws Exception {
		return Utility.stbi__get16be(InputStream);
	}

	protected long stbi__get32le() throws Exception {
		return Utility.stbi__get32le(InputStream);
	}

	protected int stbi__get16le() throws Exception {
		return Utility.stbi__get16le(InputStream);
	}

	protected short stbi__get8() throws Exception {
		return Utility.stbi__get8(InputStream);
	}

	protected boolean stbi__getn(byte[] buffer, int offset, int count) throws IOException {
		int read = InputStream.read(buffer, offset, count);

		return read == count;
	}

	protected void stbi__skip(int count) throws IOException {
		InputStream.skip(count);
	}

	protected boolean stbi__at_eof() throws IOException {
		return InputStream.available() == 0;
	}

	static void stbi__err(String message) throws Exception {
		throw new Exception(message);
	}
}