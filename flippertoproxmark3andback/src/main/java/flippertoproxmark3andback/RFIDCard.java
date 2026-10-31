package flippertoproxmark3andback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import flippertoproxmark3andback.Constants.FileType;
import flippertoproxmark3andback.Constants.KeyType;
import flippertoproxmark3andback.Constants.NfcType;

public class RFIDCard extends NFC {
    private static final Logger LOGGER = Logger.getLogger(RFIDCard.class.getName());

    //ISO14443(ID cards rfid) Specific fields//TODO: potentially split nfc into different classes and make a detector method that returns the type of nfc object required
    private int[] ATQA;//Saved in Proxmark format (Note: remember to switch to flipper format for flipper stuff) 
    private int SAK;

    public int[] getATQA() {
        return ATQA;
    }
    public int getSAK() {
        return SAK;
    }

    public RFIDCard(File file) throws FileNotFoundException, org.json.simple.parser.ParseException {
        super(file);
        LOGGER.fine(() -> "Reading ISO14443 card data from " + file.getName());
        if (FileType.flipper == this.getImportFileType()) {
            ArrayList<String> lines = Constants.flipperFileToCleanedListOfLines(file);
            //System.out.println(scan.nextLine());// ISO14443 specific fields

            String[] ATQA = lines.get(4).substring(6).split(" ");//ATQA: inserthere
            // for (String s : ATQA) {
            //     System.out.println(s);
            // }
            this.ATQA = new int[]{Integer.decode("0x" + ATQA[1]), Integer.decode("0x" + ATQA[0])};//again formatted proxmark3 style
            this.SAK = Integer.decode("0x"+lines.get(5).substring(5));//SAK: inserthere
        } else {
            JSONParser jsonParser = new JSONParser();

            try (FileReader reader = new FileReader(file))
            {
                //Read JSON file
                Object obj = jsonParser.parse(reader);
                
                JSONObject nfcArray = (JSONObject) obj;
                JSONObject card = (JSONObject) nfcArray.get("Card");
                if (card.containsKey("SAK")) {
                    this.SAK = Constants.hexStrToInt((String) card.get("SAK"));
                    this.ATQA = Constants.hexStrToIntArr((String) card.get("ATQA"));
                } else {//Ultralight
                    this.SAK = Constants.hexStrToInt("00");
                    this.ATQA = new int[]{Constants.hexStrToInt("44"), Constants.hexStrToInt("00")};
                }
                
                reader.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Unable to read RFID JSON file " + file.getAbsolutePath(), e);
            }
        }
    }
    public File exportAsFlipperNfc(String customName) throws IOException {
        File flipperNfcFile;
        if (customName.equals("")) {
            flipperNfcFile = new File(this.getCreatedBy() + "-" + Constants.arrToHexString(this.getUID(), false, true) + ".nfc");
        } else {
            flipperNfcFile = new File(customName + ".nfc");
        }
        PrintStream fileStream = new PrintStream(flipperNfcFile);
        fileStream.println("Filetype: Flipper NFC device");//Placeholders maybe for future verisons
        fileStream.println("Version: 4");
        fileStream.println("# Device type can be ISO14443-3A, ISO14443-3B, ISO14443-4A, ISO14443-4B, ISO15693-3, FeliCa, NTAG/Ultralight, Mifare Classic, Mifare DESFire, SLIX, ST25TB, EMV");
        fileStream.println("Device type: " + "ISO14443");
        fileStream.println("# UID is common for all formats");
        fileStream.println("UID: " + Constants.arrToHexString(this.getUID(), true, true));
        fileStream.println("# ISO14443 specific fields");
        fileStream.println("ATQA: " + Constants.intToHexString(ATQA[1], true, 2) + " " + Constants.intToHexString(ATQA[0], true, 2));
        fileStream.println("SAK: " + Constants.intToHexString(SAK, true, 2));
        fileStream.close();
        return flipperNfcFile;


        
        
    }
    public File exportAsProxmark3Dump(String customName) throws IOException {
        File proxmarkJsonFile;
        if (customName.equals("")) {
            proxmarkJsonFile = new File(this.getCreatedBy() + "-" + Constants.arrToHexString(this.getUID(), false, true) + "-" + "dump.json");
        } else {
            proxmarkJsonFile = new File(customName + ".json");
        }
        PrintStream fileStream = new PrintStream(proxmarkJsonFile);
        fileStream.println("{");
        fileStream.println("  \"Created\": \""+this.getCreatedBy()+"\",");
        fileStream.println("  \"FileType\": \"" + "rfidcard" + "\",");
        fileStream.println("  \"Card\": {");
        fileStream.println("    \"UID\": \"" + Constants.arrToHexString(this.getUID(), false, true) + "\",");
        fileStream.println("    \"ATQA\": \"" + Constants.arrToHexString(ATQA, false, true) + "\",");
        fileStream.println("    \"SAK\": \"" + Constants.intToHexString(SAK, true, 2) + "\"");
        fileStream.println("  }");
        fileStream.print("}");
        fileStream.close();
        return proxmarkJsonFile;

        


    }

}
