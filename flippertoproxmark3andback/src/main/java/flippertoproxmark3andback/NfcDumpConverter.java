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

        if (!"convert".equals(args[0]) || args.length < 4 || !"export".equals(args[2])) {
            help();
            return;
        }

        File inputFile = new File(args[1]);
        LOGGER.info(() -> "Converting input file " + inputFile.getPath());
        if (!inputFile.isFile()) {
            LOGGER.severe("Input file does not exist or is not a regular file: " + inputFile.getPath());
            return;
        }

        NFC nfc = NFCFactory.createNFCFromFile(inputFile);
        String outputArgument = args[3];
        if ("default".equals(outputArgument)) {
            if (args.length != 5) {
                help();
                return;
            }

            FileType outputType;
            if ("nfc".equals(args[4])) {
                outputType = FileType.flipper;
            } else if ("json".equals(args[4])) {
                outputType = FileType.proxmark3;
            } else {
                help();
                return;
            }

            export(nfc, outputType, "");
            return;
        }

        if (args.length != 4) {
            help();
            return;
        }

        int extensionIndex = outputArgument.lastIndexOf('.');
        int fileNameStart = Math.max(outputArgument.lastIndexOf('/'), outputArgument.lastIndexOf('\\'));
        if (extensionIndex <= fileNameStart || extensionIndex == outputArgument.length() - 1) {
            LOGGER.severe("Output path must have a .nfc or .json extension: " + outputArgument);
            return;
        }

        String extension = outputArgument.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        FileType outputType = Constants.fileExtensionToFileType.get(extension);
        if (outputType == null) {
            LOGGER.severe("Unsupported output file extension: " + extension);
            return;
        }

        export(nfc, outputType, outputArgument.substring(0, extensionIndex));
    }

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
