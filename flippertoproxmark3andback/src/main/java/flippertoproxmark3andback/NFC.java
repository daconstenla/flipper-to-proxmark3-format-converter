package flippertoproxmark3andback;

import flippertoproxmark3andback.Constants.FileType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class NFC {

  private static final Logger LOGGER = Logger.getLogger(NFC.class.getName());

  // This class does only UIDs but branches out into all other types of NFC
  // devices supported by
  // flipper zero
  private String createdBy;
  private FileType importFileType;
  private int[] UID;

  // 30 60 20 6F 5B 0A 78 77 88 C1 F1 B9 F5 66 9C C8
  // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
  public String getCreatedBy() {
    return createdBy;
  }

  public FileType getImportFileType() {
    return importFileType;
  }

  public int[] getUID() {
    return UID;
  }

  public NFC() {}

  public NFC(File file) throws FileNotFoundException, ParseException {
    this.importFileType = Constants.fileTypeFromFile(file);
    if (this.importFileType == null) {
      throw new IllegalArgumentException(
        "Unsupported NFC dump file extension: " + file.getName()
      );
    }
    FileType fileType = importFileType;
    LOGGER.fine(() -> "Reading NFC data from " + file.getName());
    // System.out.println(importFileType);

    if (FileType.flipper == fileType) {
      ArrayList<String> lines = Constants.flipperFileToCleanedListOfLines(file);
      // Filetype: Flipper NFC Device
      // Version: 3
      // Nfc device type can be UID, Mifare Ultralight, Mifare CLassic or ISO15693
      createdBy = "FlipperZero";
      // System.out.println();
      String deviceType = lines.get(2).substring(13); // Device type: "inserthere"
      // System.out.println(deviceType);
      // System.out.println(scan.nextLine());// UID is common for all formats
      String[] UID = lines.get(3).substring(5).split(" "); // UID: "inserthere"
      // for (String s : UID) {
      // System.out.println(s);
      // }
      this.UID = Constants.hexStrArrtoIntArr(UID);
    } else {
      JSONParser jsonParser = new JSONParser();

      try (FileReader reader = new FileReader(file)) {
        // Read JSON file
        Object obj = jsonParser.parse(reader);

        JSONObject nfcArray = (JSONObject) obj;
        // System.out.println(nfcArray);
        this.createdBy = (String) nfcArray.get("Created");
        // System.out.println(this.createdBy);
        JSONObject card = (JSONObject) nfcArray.get("Card");
        this.UID = Constants.hexStrToIntArr((String) card.get("UID"));
        reader.close();
      } catch (IOException e) {
        LOGGER.log(
          Level.WARNING,
          "Unable to read NFC JSON file " + file.getAbsolutePath(),
          e
        );
      }
    }
  }

  public File exportAsFlipperNfc(String customName) throws IOException {
    File flipperNfcFile;
    if (customName.isEmpty()) {
      flipperNfcFile = new File(
        createdBy + "-" + Constants.arrToHexString(UID, false, true) + ".nfc"
      );
    } else {
      flipperNfcFile = new File(customName + ".nfc");
    }
    PrintStream fileStream = new PrintStream(flipperNfcFile);
    fileStream.println("Filetype: Flipper NFC device"); // Placeholders maybe for future verisons
    fileStream.println("Version: 4");
    fileStream.println(
      "# Device type can be ISO14443-3A, ISO14443-3B, ISO14443-4A, ISO14443-4B, ISO15693-3, FeliCa, NTAG/Ultralight, Mifare Classic, Mifare DESFire, SLIX, ST25TB, EMV"
    );
    fileStream.println("Device type: " + "UID");
    fileStream.println("# UID is common for all formats");
    fileStream.println("UID: " + Constants.arrToHexString(UID, true, true));
    fileStream.close();
    return flipperNfcFile;
  }

  public File exportAsProxmark3Dump(String customName) throws IOException {
    File proxmarkJsonFile;
    if (customName.isEmpty()) {
      proxmarkJsonFile = new File(
        this.getCreatedBy() +
          "-" +
          Constants.arrToHexString(UID, false, true) +
          "-" +
          "dump.json"
      );
    } else {
      proxmarkJsonFile = new File(customName + ".json");
    }
    PrintStream fileStream = new PrintStream(proxmarkJsonFile);
    fileStream.println("{");
    fileStream.println("  \"Created\": \"" + createdBy + "\",");
    fileStream.println("  \"FileType\": \"" + "uid" + "\",");
    fileStream.println("  \"Card\": {");
    fileStream.println(
      "    \"UID\": \"" + Constants.arrToHexString(UID, false, true) + "\""
    );
    fileStream.println("  }");
    fileStream.print("}");
    fileStream.close();
    return proxmarkJsonFile;
  }
}
