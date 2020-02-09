package stb.image;

public enum ColorComponents {
	Grey(1),
	GreyAlpha(2),
	RedGreenBlue(3),
	RedGreenBlueAlpha(4);

	private int value;

	ColorComponents(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static ColorComponents fromInt(int value) {
		return values()[value - 1];
	}

	public static int toReqComp(ColorComponents cc) {
		return cc != null ? cc.value : 0;
	}
}