package org.nothings.stb.image.decoding;

import java.util.Arrays;

public class FakePtrInt {
	private int[] array;
	public int offset;

	public FakePtrInt(FakePtrInt ptr, int offset) {
		array = ptr.array;
		this.offset = ptr.offset + offset;
	}

	public FakePtrInt(int[] data, int offset) {
		array = data;
		this.offset = offset;
	}

	public FakePtrInt(int[] data) {
		this(data, 0);
	}

	public void clear(int count) {
		Arrays.fill(array, offset, offset + count, 0);
	}

	public int get() {
		return array[offset];
	}

	public void set(int value) {
		array[offset] = value;
	}

	public int getAt(int offset) {
		return array[this.offset + offset];
	}

	public void setAt(int offset, int value) {
		array[this.offset + offset] = value;
	}

	public void move(int offset) {
		this.offset += offset;
	}

	public void increase() {
		move(1);
	}

	public int getAndIncrease() {
		int result = array[offset];
		++offset;
		return result;
	}

	public void setAndIncrease(int value) {
		array[offset] = value;
		++offset;
	}

	public FakePtrInt cloneAdd(int offset) {
		return new FakePtrInt(array, this.offset + offset);
	}

	public FakePtrInt clone() {
		return new FakePtrInt(array, offset);
	}

	public void fill(int value, int count) {
		Arrays.fill(array, offset, offset + count, value);
	}

	public void memcpy(FakePtrInt b, int count) {
		System.arraycopy(b.array, b.offset, array, offset, count);
	}
}
