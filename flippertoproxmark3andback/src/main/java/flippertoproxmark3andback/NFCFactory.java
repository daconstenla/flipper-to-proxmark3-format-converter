package flippertoproxmark3andback;

import flippertoproxmark3andback.Constants.FileType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class NFCFactory {

  private static final Logger LOGGER = Logger.getLogger(
    NFCFactory.class.getName()
  );

  public static NFC createNFCFromFile(File file)
    throws FileNotFoundException, ParseException {
    LOGGER.fine(() -> "Inspecting NFC dump " + file.getAbsolutePath());
    FileType fileType = Constants.fileTypeFromFile(file);
    if (fileType == null) {
      throw new IllegalArgumentException(
        "Unsupported NFC dump file extension: " + file.getName()
      );
    }
    if (fileType == FileType.flipper) {
      ArrayList<String> lines = Constants.flipperFileToCleanedListOfLines(file);
      // Filetype: Flipper NFC Device
      // Version: 3
      // Nfc device type can be UID, Mifare Ultralight, Mifare CLassic or ISO15693
      // createdBy = "FlipperZero";
      // System.out.println();
      String deviceType = lines.get(2).substring(13); // Device type: "inserthere"
      LOGGER.fine(() -> "Detected Flipper device type " + deviceType);
      // System.out.println(deviceType);
      // System.out.println(scan.nextLine());// UID is common for all formats
      // String[] UID = scan.nextLine().substring(5).split(" ");//UID: "inserthere"
      if (deviceType.equals("Mifare Classic")) {
        return new MifareClassic(file);
      } else if (deviceType.equals("UID")) {
        if (lines.size() > 4) {
          return new RFIDCard(file);
        } else {
          return new NFC(file);
        }
      } else if (Constants.flipperDeviceToMfuType.containsKey(deviceType)) {
        return new MifareUltralight(file);
      } else {
        return new RFIDCard(file);
      }
      // for (String s : UID) {
      // System.out.println(s);
      // }
      // this.UID = new int[]{Integer.decode("0x" + UID[0]), Integer.decode("0x" +
      // UID[1]),
      // Integer.decode("0x" + UID[2]), Integer.decode("0x" + UID[3])};
    } else {
      JSONParser jsonParser = new JSONParser();

      try (FileReader reader = new FileReader(file)) {
        // Read JSON file
        Object obj = jsonParser.parse(reader);

        JSONObject nfcArray = (JSONObject) obj;
        // System.out.println(nfcArray);
        // this.createdBy = (String) nfcArray.get("Created");
        // System.out.println(this.createdBy);
        // this.UID = hexStrToIntArr(hashMap.get("UID"));
        String nfcStr = Constants.proxmarkFiletypeToFlipperDevice.get(
          nfcArray.get("FileType")
        );
        if (nfcStr == null) {
          throw new IllegalArgumentException(
            "Unsupported Proxmark3 file type: " + nfcArray.get("FileType")
          );
        }
        LOGGER.fine(() -> "Detected Proxmark card type " + nfcStr);
        if (nfcStr.equals("Mifare Classic")) {
          return new MifareClassic(file);
        } else if (nfcStr.equals("UID")) {
          // Any other type of nfc tag
          JSONObject card = (JSONObject) nfcArray.get("Card");
          return card.containsKey("SAK") ? new RFIDCard(file) : new NFC(file);
        } else if (nfcStr.equals("Mifare Ultralight")) {
          return new MifareUltralight(file);
        } else {
          return new NFC(file);
        }
      } catch (IOException e) {
        LOGGER.log(
          Level.WARNING,
          "Unable to inspect JSON dump " + file.getAbsolutePath(),
          e
        );
      }
    }
    return new NFC(file);
  }
}
