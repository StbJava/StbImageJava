package stb.image;

public enum ColorComponents
{
	Grey(1),
	GreyAlpha(2),
	RedGreenBlue(3),
	RedGreenBlueAlpha(4);

	public int value;

	private ColorComponents(int value)
	{
		this.value = value;
	}
}
