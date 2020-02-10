package org.nothings.stb.image.decoding;

import java.util.Arrays;

class ShortFakePtr {
	private short[] array;
	public int offset;

	public ShortFakePtr(ShortFakePtr ptr, int offset) {
		array = ptr.array;
		this.offset = ptr.offset + offset;
	}

	public ShortFakePtr(short[] data, int offset) {
		array = data;
		this.offset = offset;
	}

	public ShortFakePtr(short[] data) {
		this(data, 0);
	}

	public void clear(int count) {
		Arrays.fill(array, offset, offset + count, (short)0);
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

	public ShortFakePtr cloneAdd(int offset) {
		return new ShortFakePtr(array, this.offset + offset);
	}

	public ShortFakePtr clone() {
		return new ShortFakePtr(array, offset);
	}

	public void fill(short value, int count) {
		Arrays.fill(array, offset, offset + count, value);
	}

	public void fillAndIncrease(short value, int count) {
		fill(value, count);
		offset += count;
	}

	public void memcpy(ShortFakePtr b, int count) {
		System.arraycopy(b.array, b.offset, array, offset, count);
	}

	public void memcpyAndIncrease(ShortFakePtr b, int count) {
		memcpy(b, count);
		offset += count;
	}

	public void memcpy(short[] b, int count) {
		for (int i = 0; i < count; ++i) {
			setAt(i, b[i]);
		}
	}	
}
