# StbImageJava
StbImageJava is Java library that can load images in JPG, PNG, BMP, TGA, PSD and GIF formats.

It is based on stb_image.h 2.22 code.

It is important to note, that this project is **port**(not **wrapper**). Original C code had been ported to Java. Therefore StbImageJava doesnt require any native binaries.

# Usage
Following code loads image from byte array and converts it to 32-bit RGBA:
```java
  byte[] bytes = Files.readAllBytes(new File("image.jpg").toPath());
  ImageResult image = ImageResult.FromData(bytes, ColorComponents.RedGreenBlueAlpha);
```

# License
Public Domain

# Credits
* [stb](https://github.com/nothings/stb)
