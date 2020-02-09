# StbImageJava
StbImageJava is Java library that can load images in JPG, PNG, BMP, TGA, PSD and GIF formats.

It is based on the code of stb_image.h(version 2.22).

# Adding Reference
Download latest jar from [Releases](https://github.com/StbJava/StbImageJava/releases) and reference it from the project.

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
