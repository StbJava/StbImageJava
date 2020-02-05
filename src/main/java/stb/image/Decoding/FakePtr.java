package stb.image.Decoding;

import java.util.Arrays;

public class FakePtr<T>
{
	private T[] _array;

	public int Offset;

	public FakePtr(FakePtr<T> ptr, int offset)
	{
		_array = ptr._array;
		Offset = ptr.Offset + offset;
	}

	public FakePtr(T[] data, int offset)
	{
		_array = data;
		Offset = offset;
	}

	public FakePtr(T[] data)
	{
		this(data, 0);
	}

	public void clear(int count)
	{
		Arrays.fill(_array, Offset, Offset + count, 0);
	}

	public T getAt(int offset)
	{
		return _array[Offset + offset];
	}

	public void setAt(int offset, T value)
	{
		_array[Offset + offset] = value;
	}

	public void move(int offset)
    {
        Offset += offset;
    }

    public void increase()
    {
        move(1);
    }

	public T getAndIncrease()
	{
		var result = _array[Offset];
		++Offset;

		return result;
	}

	public void setAndIncrease(T value)
	{
		_array[Offset] = value;
		++Offset;
	}

	public void set(T value)
	{
		_array[Offset] = value;
	}

	public FakePtr<T> cloneAdd(int offset)
	{
		return new FakePtr<T>(_array, Offset + offset );
	}

	public static FakePtr<T> CreateWithSize(int size)
	{
		var result = new FakePtr<T>(new T[size]);

		for (int i = 0; i < size; ++i)
		{
			result[i] = new T();
		}

		return result;
	}

	public static FakePtr<T> CreateWithSize(long size)
	{
		return CreateWithSize((int)size);
	}

	public static FakePtr<T> Create()
	{
		return CreateWithSize(1);
	}

	public static void memcpy(FakePtr<T> a, FakePtr<T> b, int count)
	{
		for (long i = 0; i < count; ++i)
		{
			a[i] = b[i];
		}
	}

	public static void memcpy(T[] a, FakePtr<T> b, int count)
	{
		for (long i = 0; i < count; ++i)
		{
			a[i] = b[i];
		}
	}

	public static void memcpy(FakePtr<T> a, T[] b, int count)
	{
		for (long i = 0; i < count; ++i)
		{
			a[i] = b[i];
		}
	}
}
