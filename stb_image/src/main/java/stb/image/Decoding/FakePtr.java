package stb.image.Decoding;

import java.util.Arrays;

public class FakePtr<T> {
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

	public void memcpy(FakePtr<T> b, int count) {
		for (int i = 0; i < count; ++i) {
			setAt(i, b.getAt(i));
		}
	}

	public void memcpy(T[] b, int count) {
		for (int i = 0; i < count; ++i) {
			setAt(i, b[i]);
		}
	}
}