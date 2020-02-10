# StbImageJava
[ ![download](https://api.bintray.com/packages/rds1983/maven/stb.image/images/download.svg) ](https://bintray.com/rds1983/maven/stb.image/_latestVersion) [![Build status](https://ci.appveyor.com/api/projects/status/d4o6dbixbps691y3?svg=true)](https://ci.appveyor.com/project/RomanShapiro/stbimagejava) [![Chat](https://img.shields.io/discord/628186029488340992.svg)](https://discord.gg/ZeHxhCY)

StbImageJava is Java port of stb_image.h 2.22. Or - in other words - it's a Java library that can load images in JPG, PNG, BMP, TGA, PSD and GIF formats.

# Adding Reference
Download the latest jar from [Releases](https://github.com/StbJava/StbImageJava/releases) and add it to the project references.

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
