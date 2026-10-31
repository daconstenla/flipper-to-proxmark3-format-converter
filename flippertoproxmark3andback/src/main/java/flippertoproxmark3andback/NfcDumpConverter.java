package flippertoproxmark3andback;

import flippertoproxmark3andback.Constants.FileType;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

import org.json.simple.parser.ParseException;

public class NfcDumpConverter {
    private static final Logger LOGGER = Logger.getLogger(NfcDumpConverter.class.getName());
    
    public static void main(String[] args) throws IOException, ParseException {
        runProgramWithArguments(args);
    }

    private static void runProgramWithArguments(String[] args) throws ParseException, IOException {
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
            LOGGER.severe("Input file does not exist or is not a regular file: " + inputFile.getPath());
            return;
        }

        NFC nfc = NFCFactory.createNFCFromFile(inputFile);
        export(nfc, request.outputType(), request.customName());
    }

    static ConversionRequest parseArguments(String[] args) {
        if (args.length < 4 || !"convert".equals(args[0]) || !"export".equals(args[2])) {
            throw new IllegalArgumentException("Expected: convert <input> export <output> [nfc|json]");
        }

        String outputArgument = args[3];
        if ("default".equals(outputArgument)) {
            if (args.length != 5) {
                throw new IllegalArgumentException("Default output requires a format: nfc or json");
            }

            FileType outputType;
            if ("nfc".equals(args[4])) {
                outputType = FileType.flipper;
            } else if ("json".equals(args[4])) {
                outputType = FileType.proxmark3;
            } else {
                throw new IllegalArgumentException("Unsupported default output format: " + args[4]);
            }

            return new ConversionRequest(new File(args[1]), outputType, "");
        }

        if (args.length != 4) {
            throw new IllegalArgumentException("A custom output path must be the final argument");
        }
        int extensionIndex = outputArgument.lastIndexOf('.');
        int fileNameStart = Math.max(outputArgument.lastIndexOf('/'), outputArgument.lastIndexOf('\\'));
        if (extensionIndex <= fileNameStart || extensionIndex == outputArgument.length() - 1) {
            throw new IllegalArgumentException("Output path must have a .nfc or .json extension: " + outputArgument);
        }

        String extension = outputArgument.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        FileType outputType = Constants.fileExtensionToFileType.get(extension);
        if (outputType == null) {
            throw new IllegalArgumentException("Unsupported output file extension: " + extension);
        }

        return new ConversionRequest(new File(args[1]), outputType, outputArgument.substring(0, extensionIndex));
    }

    record ConversionRequest(File inputFile, FileType outputType, String customName) {}

    private static void export(NFC nfc, FileType outputType, String customName) throws IOException {
        File outputFile;
        String formatName;
        if (outputType == FileType.flipper) {
            outputFile = nfc.exportAsFlipperNfc(customName);
            formatName = "Flipper NFC";
        } else {
            outputFile = nfc.exportAsProxmark3Dump(customName);
            formatName = "Proxmark3 JSON";
        }

        LOGGER.info(() -> "Exported " + formatName + " file to " + outputFile.getAbsolutePath());
    }

    private static void help() {
        System.out.println("This tool is for switching nfc file formats between .nfc (Flipper NFC Format) and .json (Proxmark3 NFC Dump Format)");
        System.out.println("Currently only works for MIFARE 1k, 4k, Mini cards and Mifare Ultralight/NTAGS");
        System.out.println("convert \"flipper.nfc\" | \"proxmark3-dump.json\" export \"enter-file-name-here-with-extension-you-want-to-convert-to\" (Either .nfc or .json)");
        System.out.println("Also\nconvert \"flipper.nfc\" | \"proxmark3-dump.json\" export \"default\" nfc(.nfc) | json(.json)");
    }
}
