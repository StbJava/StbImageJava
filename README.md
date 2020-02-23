# StbImageJava
[ ![Download](https://api.bintray.com/packages/rds1983/maven/stb.image/images/download.svg) ](https://bintray.com/rds1983/maven/stb.image/_latestVersion) [![Chat](https://img.shields.io/discord/628186029488340992.svg)](https://discord.gg/ZeHxhCY)

StbImageJava is Java port of stb_image.h 2.22. Or - in other words - it's a Java library that can load images in JPG, PNG, BMP, TGA, PSD and GIF formats.

# Adding Reference
StbImageJava is available at bintray/jcenter: https://bintray.com/rds1983/maven/stb.image/_latestVersion

# Usage
Following code loads image from byte array and converts it to 32-bit RGBA:
```java
  byte[] bytes = Files.readAllBytes(new File("image.jpg").toPath());
  ImageResult image = ImageResult.FromData(bytes, ColorComponents.RedGreenBlueAlpha);
```

# Building from Source
1. Clone this repo.
2. `gradlew idea`
3. Open generated project in the Intellij IDEA.

# Reliability & Performance
There is special app to measure reliability & performance of StbImageSharp in comparison to the original stb_image.h: https://github.com/StbJava/StbImageJava/blob/master/testing/src/main/java/stb/image/testing/Testing.java

It could be built through command `gradlew jar`(jar will appear in the folder testing/build/libs) and ran through: `java -jar testing-1.0.jar <path_to_folder_with_images>`(i.e. `java -jar testing-1.0.jar D:/Projects/TestImages`).

It goes through every image file in the specified folder and tries to load it 10 times with StbImageJava, then 10 times with ljwgl-stb(which is JNI wrapper over original stb_image.h). Then it compares whether the results are byte-wise similar and also calculates loading times. Also it sums up and reports loading times for each method.

Moreover ImageIO is included in the testing too.

I've used it over following set of images: https://github.com/StbSharp/TestImages

The byte-wise comprarison results are similar for StbImageSharp and Stb.Native(except a few 16-bit PNGs and PSDs that arent supported yet by StbImageJava).

And performance comparison results are(times are total loading times):
```
13 -- StbImageJava - jpg: 2890, psd: 9, bmp: 101, png: 12785, tga: 830, Total: 16615 ms
13 -- Stb.Native - jpg: 846, psd: 0, bmp: 35, png: 9350, tga: 587, Total: 10818 ms
13 -- ImageIO - jpg: 2161, bmp: 17, png: 14760, Total: 16938 ms
13 -- Total files processed - jpg: 170, psd: 1, bmp: 7, png: 564, tga: 41, Total: 783
```

# License
Public Domain

# Credits
* [stb](https://github.com/nothings/stb)
