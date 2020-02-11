# StbImageJava
[ ![download](https://api.bintray.com/packages/rds1983/maven/stb.image/images/download.svg) ](https://bintray.com/rds1983/maven/stb.image/_latestVersion) [![Chat](https://img.shields.io/discord/628186029488340992.svg)](https://discord.gg/ZeHxhCY)

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
This repo contains special app that was written to measure reliability & performance of StbImageJava in comparison to the original stb_image.h: https://github.com/StbJava/StbImageJava/blob/master/testing/src/main/java/stb/image/testing/Testing.java

It could be built through command `gradlew jar`(jar will appear in the folder testing/build/libs) and ran through - 'java -jar testing-1.0.jar <path_to_folder_with_images>`(i.e. 'java -jar testing-1.0.jar D:/Projects/TestImages`).

It goes through every image file in the specified folder and tries to load it 10 times with StbImageJava, then 10 times with ljwgl-stb(which is JNI wrapper over native stb_image.h). Then it compares whether the results are byte-wise similar and also calculates loading times. Also it sums up and reports loading times for each method.

I've used it over following set of images: https://github.com/StbSharp/TestImages
The results are similar for both methods(except a few 16-bit PNGs and PSDs that arent supported yet by StbImageJava).
And performance comparison result is:
```
13 -- Total StbJava Loading From memory Time: 23915 ms
13 -- Total Stb.Native Loading From memory Time: 18547 ms
```

# License
Public Domain

# Credits
* [stb](https://github.com/nothings/stb)
