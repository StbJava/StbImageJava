package org.nothings.stb.image.decoding;

import java.util.Arrays;

class FakePtrShort {
	private short[] array;
	public int offset;

	public FakePtrShort(FakePtrShort ptr, int offset) {
		array = ptr.array;
		this.offset = ptr.offset + offset;
	}

	public FakePtrShort(short[] data, int offset) {
		array = data;
		this.offset = offset;
	}

	public FakePtrShort(short[] data) {
		this(data, 0);
	}

	public void clear(int count) {
		Arrays.fill(array, offset, offset + count, (short) 0);
	}

	public short get() {
		return array[offset];
	}

	public void set(short value) {
		array[offset] = value;
	}

	public short getAt(int offset) {
		return array[this.offset + offset];
	}

	public void setAt(int offset, short value) {
		array[this.offset + offset] = value;
	}

	public void move(int offset) {
		this.offset += offset;
	}

	public void increase() {
		move(1);
	}

	public short getAndIncrease() {
		short result = array[offset];
		++offset;
		return result;
	}

	public void setAndIncrease(short value) {
		array[offset] = value;
		++offset;
	}

	public FakePtrShort cloneAdd(int offset) {
		return new FakePtrShort(array, this.offset + offset);
	}

	public FakePtrShort clone() {
		return new FakePtrShort(array, offset);
	}

	public void fill(short value, int count) {
		Arrays.fill(array, offset, offset + count, value);
	}

	public void memcpy(FakePtrShort b, int count) {
		System.arraycopy(b.array, b.offset, array, offset, count);
	}
}