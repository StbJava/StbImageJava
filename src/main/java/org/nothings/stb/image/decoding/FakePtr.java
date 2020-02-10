package org.nothings.stb.image.decoding;

import java.util.Arrays;

class FakePtr<T> {
	private T[] _array;
	public int Offset;

	public FakePtr(FakePtr<T> ptr, int offset) {
		_array = ptr._array;
		Offset = ptr.Offset + offset;
	}

	public FakePtr(T[] data, int offset) {
		_array = data;
		Offset = offset;
	}

	public FakePtr(T[] data) {
		this(data, 0);
	}

	public void clear(int count) {
		Arrays.fill(_array, Offset, Offset + count, 0);
	}

	public T get() {
		return _array[Offset];
	}

	public void set(T value) {
		_array[Offset] = value;
	}

	public T getAt(int offset) {
		return _array[Offset + offset];
	}

	public void setAt(int offset, T value) {
		_array[Offset + offset] = value;
	}

	public void move(int offset) {
		Offset += offset;
	}

	public void increase() {
		move(1);
	}

	public T getAndIncrease() {
		T result = _array[Offset];
		++Offset;
		return result;
	}

	public void setAndIncrease(T value) {
		_array[Offset] = value;
		++Offset;
	}

	public FakePtr<T> cloneAdd(int offset) {
		return new FakePtr<T>(_array, Offset + offset);
	}

	public FakePtr<T> clone() {
		return new FakePtr<T>(_array, Offset);
	}

	public void fill(T value, int count) {
		Arrays.fill(_array, Offset, Offset + count, value);
	}

	public void fillAndIncrease(T value, int count) {
		fill(value, count);
		Offset += count;
	}

	public void memcpy(FakePtr<T> b, int count) {
		System.arraycopy(b._array, b.Offset, _array, Offset, count);
	}

	public void memcpyAndIncrease(FakePtr<T> b, int count) {
		memcpy(b, count);
		Offset += count;
	}

	public void memcpy(T[] b, int count) {
		for (int i = 0; i < count; ++i) {
			setAt(i, b[i]);
		}
	}
}