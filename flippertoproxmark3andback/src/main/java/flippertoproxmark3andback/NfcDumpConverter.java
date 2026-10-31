package flippertoproxmark3andback;

import flippertoproxmark3andback.Constants.FileType;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;

public class NfcDumpConverter {

  private static final Logger LOGGER = Logger.getLogger(
    NfcDumpConverter.class.getName()
  );

  public static void main(String[] args) throws IOException, ParseException {
    runProgramWithArguments(args);
  }

  private static void runProgramWithArguments(String[] args)
    throws ParseException, IOException {
    if (args.length == 0 || "help".equals(args[0])) {
      help();
      return;
    }

    ConversionRequest request;
    try {
      request = parseArguments(args);
    } catch (IllegalArgumentException e) {
      LOGGER.severe(e.getMessage());
      help();
      return;
    }

    File inputFile = request.inputFile();
    LOGGER.info(() -> "Converting input file " + inputFile.getPath());
    if (!inputFile.isFile()) {
      LOGGER.severe(
        "Input file does not exist or is not a regular file: " +
          inputFile.getPath()
      );
      return;
    }

    NFC nfc = NFCFactory.createNFCFromFile(inputFile);
    export(nfc, request.outputType(), request.customName());
  }

  static ConversionRequest parseArguments(String[] args) {
    if (args.length == 1) {
      return parseInputOnlyArgument(args[0]);
    }

    if (
      args.length < 4 || !"convert".equals(args[0]) || !"export".equals(args[2])
    ) {
      throw new IllegalArgumentException(
        "Expected: convert <input> export <output> [nfc|json]"
      );
    }

    String outputArgument = args[3];
    if ("default".equals(outputArgument)) {
      if (args.length != 5) {
        throw new IllegalArgumentException(
          "Default output requires a format: nfc or json"
        );
      }

      FileType outputType;
      if ("nfc".equals(args[4])) {
        outputType = FileType.flipper;
      } else if ("json".equals(args[4])) {
        outputType = FileType.proxmark3;
      } else {
        throw new IllegalArgumentException(
          "Unsupported default output format: " + args[4]
        );
      }

      return new ConversionRequest(new File(args[1]), outputType, "");
    }

    if (args.length != 4) {
      throw new IllegalArgumentException(
        "A custom output path must be the final argument"
      );
    }
    int extensionIndex = outputArgument.lastIndexOf('.');
    int fileNameStart = Math.max(
      outputArgument.lastIndexOf('/'),
      outputArgument.lastIndexOf('\\')
    );
    if (
      extensionIndex <= fileNameStart ||
      extensionIndex == outputArgument.length() - 1
    ) {
      throw new IllegalArgumentException(
        "Output path must have a .nfc or .json extension: " + outputArgument
      );
    }

    String extension = outputArgument
      .substring(extensionIndex + 1)
      .toLowerCase(Locale.ROOT);
    FileType outputType = Constants.fileExtensionToFileType.get(extension);
    if (outputType == null) {
      throw new IllegalArgumentException(
        "Unsupported output file extension: " + extension
      );
    }

    return new ConversionRequest(
      new File(args[1]),
      outputType,
      outputArgument.substring(0, extensionIndex)
    );
  }

  private static ConversionRequest parseInputOnlyArgument(
    String inputArgument
  ) {
    File inputFile = new File(inputArgument);
    FileType inputType = Constants.fileTypeFromFile(inputFile);
    if (inputType == null) {
      throw new IllegalArgumentException(
        "Input file must have a .nfc or .json extension: " + inputArgument
      );
    }

    int extensionIndex = inputArgument.lastIndexOf('.');
    int fileNameStart = Math.max(
      inputArgument.lastIndexOf('/'),
      inputArgument.lastIndexOf('\\')
    );
    if (extensionIndex <= fileNameStart) {
      throw new IllegalArgumentException(
        "Input path must include a filename before its extension: " +
          inputArgument
      );
    }

    FileType outputType =
      inputType == FileType.flipper ? FileType.proxmark3 : FileType.flipper;
    return new ConversionRequest(
      inputFile,
      outputType,
      inputArgument.substring(0, extensionIndex)
    );
  }

  record ConversionRequest(
    File inputFile,
    FileType outputType,
    String customName
  ) {}

  private static void export(NFC nfc, FileType outputType, String customName)
    throws IOException {
    File targetFile = outputFileFor(nfc, outputType, customName);
    if (targetFile.exists() && !confirmOverwrite(targetFile)) {
      LOGGER.info(
        "Cancelled export; existing file was left unchanged: " +
          targetFile.getAbsolutePath()
      );
      return;
    }

    File outputFile;
    String formatName;
    if (outputType == FileType.flipper) {
      outputFile = nfc.exportAsFlipperNfc(customName);
      formatName = "Flipper NFC";
    } else {
      outputFile = nfc.exportAsProxmark3Dump(customName);
      formatName = "Proxmark3 JSON";
    }

    LOGGER.info(
      () ->
        "Exported " + formatName + " file to " + outputFile.getAbsolutePath()
    );
  }

  private static File outputFileFor(
    NFC nfc,
    FileType outputType,
    String customName
  ) {
    String extension = outputType == FileType.flipper ? ".nfc" : ".json";
    String outputName = customName;
    if (outputName.isEmpty()) {
      outputName =
        nfc.getCreatedBy() +
        "-" +
        Constants.arrToHexString(nfc.getUID(), false, true);
      if (outputType == FileType.proxmark3) {
        outputName += "-dump";
      }
    }
    return new File(outputName + extension);
  }

  private static boolean confirmOverwrite(File targetFile) throws IOException {
    LOGGER.info(
      "Output file already exists: " +
        targetFile.getPath() +
        ". Reply yes to replace it; default is no."
    );
    String response = new BufferedReader(
      new InputStreamReader(System.in, StandardCharsets.UTF_8)
    ).readLine();
    return (
      response != null &&
      ("y".equalsIgnoreCase(response.trim()) ||
        "yes".equalsIgnoreCase(response.trim()))
    );
  }

  private static void help() {
    LOGGER.info(
      """
      This tool switches NFC file formats between .nfc (Flipper NFC Format) and .json (Proxmark3 NFC Dump Format).
      Currently supports MIFARE 1k, 4k, Mini, and Mifare Ultralight/NTAG cards.
      Convert with a custom output path: convert <input> export <output.nfc|output.json>
      Convert with a generated output name: convert <input> export default <nfc|json>
      Convert to the other format beside the input: <input.nfc|input.json>
      """
    );
  }
}
