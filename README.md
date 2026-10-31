# Flipper To Proxmark3 And Back

This tool is for switching nfc file formats between .nfc (Flipper NFC Format) and .json (Proxmark3 NFC Dump Format)

**Works for MIFARE 1k, 4k, Mini cards and Mifare Ultralight/NTAGS**

## Build from source

Install Java 21 or newer. From the repository root, run:

```sh
./gradlew clean test jar
```

On Windows, run:

```bat
gradlew.bat clean test jar
```

Run `./gradlew spotlessApply` to format Java sources. The `check` task runs Spotless validation automatically.

The executable jar, including its runtime dependencies, is created at
`flippertoproxmark3andback/build/libs/flippertoproxmark3andback.jar`.

## How to use?

1. Download the latest jar file from the latest github release
2. Move the jar file to the file you wish to convert
3. Pass only the input file to automatically create the other format beside it:

```sh
java -jar flippertoproxmark3andback.jar "flipper.nfc"        # creates flipper.json
java -jar flippertoproxmark3andback.jar "proxmark3-dump.json" # creates proxmark3-dump.nfc
```

If the output file already exists, the app asks before replacing it. The default response is no.

To choose a different output filename or use the default generated filename, run:

```
java -jar flippertoproxmark3andback.jar convert "flipper.nfc" export "proxmark3-dump.json"
java -jar flippertoproxmark3andback.jar convert "proxmark3-dump.json" export default nfc
```
Default mode allows for the corresponding automatic name generation format:

for .json: Proxmark3 | FlipperZero-(insert-uid-here)-dump.json

for .nfc: Proxmark3 | FlipperZero-(insert-uid-here).nfc

Import and export files have to have either .nfc or .json as the file extension otherwise the tool will not work

```
Some examples would be:
java -jar flippertoproxmark3andback.jar convert "flipper.nfc" export "proxmark3-dump.json"
java -jar flippertoproxmark3andback.jar convert "proxmark3-dump.json" export default nfc
```
<img width="743" alt="How to screenshot for program" src="https://github.com/tjamesw123/flipper-to-proxmark3-and-back/assets/94910672/3de79b31-d810-4bfe-be45-ab05a7574e92">
