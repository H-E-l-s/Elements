package com.hels.elements;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

public class BLELogger  {

    public final UUID uuidLogRecord = UUID.fromString("f000b131-0451-4000-b000-000000000000");
    public final UUID uuidLogControl = UUID.fromString("f000b132-0451-4000-b000-000000000000");

    public static final byte READ_ALL_REQ = 0x01;

    public static final byte GET_REC_NUM = 0x11;
    public static final byte GET_RECORDS = 0x12;

    public static final byte CHAR_CNTRL_LEN    = 16;
    public static final byte CHAR_DATA_LEN    = (32+4);

    public final byte STATE_IDLE = 0;
    public final byte STATE_WAITING_RECORDS_NUM = 1;               // waiting indication from device with number of available records
    public final byte STATE_READING_RECORDS_DATA = 2;         // waiting for indications with the records

    public final int STATUS_READING_ALL = 0x01;
    public final int STATUS_READING_24H = 0x02;

    //---- Log Service ----------------------------------------------------------------------------
    //private volatile int logServiceStatus = 0;
    private final static int LSS_REC_NUM_RCVD = 0x01;
    private final static int LSS_REC_NUM_CONFIRMED = 0x02;
    private final static int LSS_RECORD_RCVD = 0x04;
    private volatile int recordsAvailable = 0;
    private volatile int recordsDownload = 0;
    //---------------------------------------------------------------------------------------------

    private final int WRITE_STRING = 0x01;
    private final int WRITE_UINT16 = 0x02;
    private final int WRITE_ARRAY = 0x03;

    public byte state = STATE_IDLE;
    public int status = 0;

    public boolean readLogRecords(int beginTimeStamp, int endTimeStamp) {
        return false;
    }

    String fileName;

    BLECharacteristicIO bleCharacteristicIO;

    //ArrayBlockingQueue<byte[]> recordsBuffer;
    ArrayList<byte[]> recordsBuf;
    byte[] controlResponse = new byte[CHAR_CNTRL_LEN];

    public interface BLECharacteristicIO {
        boolean writeCharacteristic(UUID id, Object value, int type);
    }

    public BLELogger(BLECharacteristicIO bleCharacteristicIO) {
        status = 0;
        state = STATE_IDLE;
        this.bleCharacteristicIO = bleCharacteristicIO;
    }

    boolean initLogReading(String fileName) {
        recordsAvailable = 0;
        state = STATE_IDLE;
        status = 0;
        this.fileName = fileName;
        recordsBuf = new ArrayList<byte[]>();

        // log Service: request {0x01, 0x11, 0..0 } to find ALL records and return the number of available records.
        byte[] controlData = new byte[16];
        controlData[0] = READ_ALL_REQ;       // all records
        controlData[1] = GET_REC_NUM;        // how many records available

        if (bleCharacteristicIO.writeCharacteristic(uuidLogControl, controlData, WRITE_ARRAY)) {
            MLogger.logToFile(null, "", String.format("BLL: Writing Log req - Read ALL - OK"), true);
            state = STATE_WAITING_RECORDS_NUM;
            return true;
        } else {
            MLogger.logToFile(null, "", "BLL: Writing Log req - Read ALL - ERROR", true);
            return false;
        }
    }

    boolean processingLogReading() {

        switch (state) {
            case STATE_WAITING_RECORDS_NUM:
                //Log.d("RLOG", "STATE_WAITING_RECORDS_NUM");
                // returned number of available records
                if((controlResponse[0] == READ_ALL_REQ) && ( controlResponse[1] == GET_REC_NUM) ) {
                    recordsAvailable = Utils.intFromByteArray(controlResponse, 2, 4);
                    recordsDownload = recordsAvailable - 5;
                    MLogger.logToFile(null, "", String.format("BLL: Number received %d", recordsAvailable), true);
                    //recordsAvailable = 3;
                    byte[] controlData = new byte[16];
                    controlData[0] = READ_ALL_REQ;       // all records
                    controlData[1] = GET_RECORDS;        // get records
                    controlData[2] = 0; controlData[3] = 0; controlData[4] = 0; controlData[5] = 0; // first record to read
                    controlData[6] = (byte)((recordsAvailable >> 24) & 0xFF) ;
                    controlData[7] = (byte)((recordsAvailable >> 16) & 0xFF) ;
                    controlData[8] = (byte)((recordsAvailable >> 8) & 0xFF) ;
                    controlData[9] = (byte)((recordsAvailable) & 0xFF) ;
                    if (bleCharacteristicIO.writeCharacteristic(uuidLogControl, controlData, WRITE_ARRAY)) {
                        MLogger.logToFile(null, "", String.format("BLL: Writing Data req - OK"), true);
                    } else {
                        MLogger.logToFile(null, "", "BLL: Writing Data req - ERROR", true);
                        return false;
                    }

                    state = STATE_READING_RECORDS_DATA;
                }
            break;
            case STATE_READING_RECORDS_DATA:
                //Log.d("RLOG", "STATE_READING_RECORDS_DATA");
                //MLogger.logToFile(null, "", String.format("BLL: waiting for records %d", recordsAvailable), true);
                if(recordsAvailable == 0 ) {
                    MLogger.logToFile(null, "", "All records received", true);
                    return false;
                }
            break;
        }
        return true;
    }

    public void putToBuffer(byte[] a) {
        recordsBuf.add(a);
        if( recordsAvailable > 0) {
            recordsAvailable--;
            MLogger.logToFile(null, "", String.format("BLL: Records left %d", recordsAvailable), true);
        }
        else MLogger.logToFile(null, "", "BLL: Too many records", true);
    }

    public void controlResponseSet(byte[] a) {
        System.arraycopy(a, 0, controlResponse, 0, a.length);
    }

    boolean appendLogFile(byte[] data) {

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//        File newDir = new File(path + "/" + fileName);
        try{
//            if (!newDir.exists()) {
//                newDir.mkdir();
//            }
            FileOutputStream writer = new FileOutputStream(new File(path, fileName));

            writer.write(data);
            writer.close();
            Log.e("TAG", "Wrote to file: "+ fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    boolean appendCSVFile(byte[] data) {

        String s = "";

        long dateTime = (long) data[4] & 0xFF;
        dateTime <<= 8;
        dateTime |= (long) data[5] & 0xFF;
        dateTime <<= 8;
        dateTime |= (long) data[6] & 0xFF;
        dateTime <<= 8;
        dateTime |= (long) data[7] & 0xFF;

        long vBatBkp = (long) data[22] & 0xFF;
        vBatBkp <<= 8;
        vBatBkp |= (long) data[23] & 0xFF;

        long vBat = (long) data[10] & 0xFF;
        vBat <<= 8;
        vBat |= (long) data[11] & 0xFF;

        long iBat = (long) data[12];
        iBat <<= 8;
        iBat |= (long) data[13] & 0xFF;



        long wBat = (long) data[10+4];
        wBat <<= 8;
        wBat |= (long) data[11+4] & 0xFF;

        long vLoad = (long) data[12+4] & 0xFF;
        vLoad <<= 8;
        vLoad |= (long) data[13+4] & 0xFF;

        long iLoad = (long) data[14+4];
        iLoad <<= 8;
        iLoad |= (long) data[15+4] & 0xFF;

        long eLoad = (long) data[16+4];
        eLoad <<= 8;
        eLoad |= (long) data[17+4] & 0xFF;

        long tc = (long) data[22+4];
        tc <<= 8;
        tc |= (long) data[23+4] & 0xFF;

        String dateString = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date(dateTime * 1000));

        s += String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d\r\n", dateString, vBat, iBat, wBat, vLoad, iLoad, eLoad, vBatBkp, tc );

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        try {
            FileOutputStream writer = new FileOutputStream(new File(path, fileName), true);
            //FileOutputStream writer = openFileOutput("savedData.txt",  MODE_APPEND);
            //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(writer);
            //outputStreamWriter.write(s);
            outputStreamWriter.append(s);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }


        return true;
    }

}
