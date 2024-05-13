package com.pax.pay.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import com.pax.device.Device;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.settings.SysParam;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import th.co.bkkps.utils.Log;

public class BitmapImageConverterUtils {
    private static final String TAG = BitmapImageConverterUtils.class.getSimpleName();
    private static final int BMP_WIDTH_OF_TIMES = 4;
    private static final int BYTE_PER_PIXEL = 3;

    private static final byte[] BMP_HEADER = new byte[]{0x42, 0x4D};

    /**
     * Android Bitmap Object to Window's v3 24bit Bmp Format File
     * @param orgBitmap
     * @param filePath
     * @return file saved result
     */
    private static BitmapImageConverterUtils instance = null;




    public static BitmapImageConverterUtils getInstance()  {
        if (instance == null) {
            instance = new BitmapImageConverterUtils();
        }
        return instance;
    }

    public static boolean save(Bitmap orgBitmap, String filePath) throws IOException {
        long start = System.currentTimeMillis();
        if(orgBitmap == null){
            return false;
        }

        if(filePath == null){
            return false;
        }

        boolean isSaveSuccess = true;

        //image size
        int width = orgBitmap.getWidth();
        int height = orgBitmap.getHeight();

        //image dummy data size
        //reason : the amount of bytes per image row must be a multiple of 4 (requirements of bmp format)
        byte[] dummyBytesPerRow = null;
        boolean hasDummy = false;
        int rowWidthInBytes = BYTE_PER_PIXEL * width; //source image width * number of bytes to encode one pixel.
        if(rowWidthInBytes%BMP_WIDTH_OF_TIMES>0){
            hasDummy=true;
            //the number of dummy bytes we need to add on each row
            dummyBytesPerRow = new byte[(BMP_WIDTH_OF_TIMES-(rowWidthInBytes%BMP_WIDTH_OF_TIMES))];
            //just fill an array with the dummy bytes we need to append at the end of each row
            for(int i = 0; i < dummyBytesPerRow.length; i++){
                dummyBytesPerRow[i] = (byte)0xFF;
            }
        }

        //an array to receive the pixels from the source image
        int[] pixels = new int[width * height];

        //the number of bytes used in the file to store raw image data (excluding file headers)
        int imageSize = (rowWidthInBytes+(hasDummy?dummyBytesPerRow.length:0)) * height;
        //file headers size
        int imageDataOffset = 0x36;

        //final size of the file
        int fileSize = imageSize + imageDataOffset;

        //Android Bitmap Image Data
        orgBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);

        /**
         * BITMAP FILE HEADER Write Start
         **/
        buffer.put((byte)0x42);
        buffer.put((byte)0x4D);

        //size
        buffer.put(writeInt(fileSize));

        //reserved
        buffer.put(writeShort((short)0));
        buffer.put(writeShort((short)0));

        //image data start offset
        buffer.put(writeInt(imageDataOffset));

        /** BITMAP FILE HEADER Write End */

        //*******************************************

        /** BITMAP INFO HEADER Write Start */
        //size
        buffer.put(writeInt(0x28));

        //width, height
        //if we add 3 dummy bytes per row : it means we add a pixel (and the image width is modified.
        buffer.put(writeInt(width+(hasDummy?(dummyBytesPerRow.length==3?1:0):0)));
        buffer.put(writeInt(height));

        //planes
        buffer.put(writeShort((short)1));

        //bit count
        buffer.put(writeShort((short)24));

        //bit compression
        buffer.put(writeInt(0));

        //image data size
        buffer.put(writeInt(imageSize));

        //horizontal resolution in pixels per meter
        buffer.put(writeInt(0));

        //vertical resolution in pixels per meter (unreliable)
        buffer.put(writeInt(0));

        buffer.put(writeInt(0));

        buffer.put(writeInt(0));

        /** BITMAP INFO HEADER Write End */

        int row = height;
        int col = width;
        int startPosition = (row - 1) * col;
        int endPosition = row * col;
        while( row > 0 ){
            for(int i = startPosition; i < endPosition; i++ ){
                buffer.put((byte)(pixels[i] & 0x000000FF));
                buffer.put((byte)((pixels[i] & 0x0000FF00) >> 8));
                buffer.put((byte)((pixels[i] & 0x00FF0000) >> 16));
            }
            if(hasDummy){
                buffer.put(dummyBytesPerRow);
            }
            row--;
            endPosition = startPosition;
            startPosition = startPosition - col;
        }

        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(buffer.array());
        fos.close();
        Log.v("AndroidBmpUtil" ,System.currentTimeMillis()-start+" ms");

        return isSaveSuccess;
    }

    /**
     * Write integer to little-endian
     * @param value
     * @return
     * @throws IOException
     */
    private static byte[] writeInt(int value) throws IOException {
        byte[] b = new byte[4];

        b[0] = (byte)(value & 0x000000FF);
        b[1] = (byte)((value & 0x0000FF00) >> 8);
        b[2] = (byte)((value & 0x00FF0000) >> 16);
        b[3] = (byte)((value & 0xFF000000) >> 24);

        return b;
    }

    /**
     * Write short to little-endian byte array
     * @param value
     * @return
     * @throws IOException
     */
    private static byte[] writeShort(short value) throws IOException {
        byte[] b = new byte[2];

        b[0] = (byte)(value & 0x00FF);
        b[1] = (byte)((value & 0xFF00) >> 8);

        return b;
    }









    private static Bitmap reSize(Bitmap origBitmap) {
//        int expect_width = 192 ;
//        int curr_width  = origBitmap.getWidth();
//        int curr_height = origBitmap.getHeight();
//
//        double scale_ratio = 0.00 ;
//        int scale_width = 0 ;
//        int scale_height = 0 ;
//
//        if (curr_width > expect_width) {
//            scale_ratio = (((100 * curr_width) / expect_width) / 100) -1 ;
//            scale_width = expect_width ;
//            scale_height = Integer.parseInt(String.valueOf(Math.round(curr_height * scale_ratio)).replace(".0","")) ;
//        } else {
//            scale_width = curr_width;
//            scale_height = curr_height;
//        }

        int scale_width   = Integer.parseInt(String.valueOf((Math.round((double)(origBitmap.getWidth() * 0.4)))).replace(".0",""));
        int scale_height  = Integer.parseInt(String.valueOf((Math.round((double)(origBitmap.getHeight() * 0.4)))).replace(".0",""));

        Bitmap bitmap = FinancialApplication.getGl().getImgProcessing().scale(origBitmap, scale_width, scale_height);
        return bitmap;
    }

    public static int CalculateDecimalfromByteArray(byte[] hexByte) {
        return (Tools.byte2Int(hexByte[0]))  + (Tools.byte2Int(hexByte[1])*(256)) + (Tools.byte2Int(hexByte[2])*(4096))+ (Tools.byte2Int(hexByte[3])*(65536));
    }




    /*==================================================================================
        This function use by Signature data before send by ERCM-UPLOAD-ERECEIPT
      ==================================================================================*/
    public static byte[] CreateVerifoneFormat(byte[] exSignatureMonoBitmap){

        int iDataOffset = Convert.getInstance().intFromByteArray(exSignatureMonoBitmap, 10, Convert.EEndian.LITTLE_ENDIAN);
        int iWidth =     Convert.getInstance().intFromByteArray(exSignatureMonoBitmap, 18, Convert.EEndian.LITTLE_ENDIAN);
        int iHeight =    Convert.getInstance().intFromByteArray(exSignatureMonoBitmap, 22, Convert.EEndian.LITTLE_ENDIAN);
        //int bmpSize =    Convert.getInstance().intFromByteArray(exSignatureMonoBitmap, 34, Convert.EEndian.LITTLE_ENDIAN);
        int bmpSize =  (iWidth * iHeight) / 8 ;

        byte[] rawBmp = new byte[bmpSize];
        Utils.SaveArrayCopy(exSignatureMonoBitmap, iDataOffset, rawBmp, 0, bmpSize);

        ByteArrayOutputStream rawStream = new ByteArrayOutputStream();
        // invert image
        for (int i=0; i<rawBmp.length; i++){
            byte invert = rawBmp[i] ^= (byte)0xff;
            rawStream.write(invert);
        }

        rawBmp = rawStream.toByteArray();

        // flip bmp-data-without header
        byte[] flippedRaw_NoHeader = FlipBmpImageVertically(rawBmp, iWidth);
        //byte[] flippedRaw_WithHeader= EReceiptUtils.getInstance().MergeArray(bmpHeader,flippedRaw_NoHeader);
        //saveDataToFile(flippedRaw_WithHeader,"/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_flipped.bmp");

        ByteArrayOutputStream opStream = new ByteArrayOutputStream();
        // VF Header
        byte[] VF_HEADER = {0x50, 0x1B, 0x47, 0x4C, 0x31, 0x2C, 0x36, 0x2C};
        opStream.write(VF_HEADER, 0, VF_HEADER.length);
        VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, (String.valueOf(iWidth) + ",").getBytes());
        VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, (String.valueOf(iHeight) + ";").getBytes());
        VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, flippedRaw_NoHeader);//gzipCompressionByte(flippedRaw_NoHeader));
        //saveDataToFile(VF_HEADER,"/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data", "/signature_flipped.vff");

        byte[] w_h = Integer.toString(iWidth)
                .concat(",")
                .concat(Integer.toString(iHeight))
                .concat(";")
                .getBytes();
        opStream.write(w_h, 0, w_h.length);

        opStream.write(rawBmp, 0, rawBmp.length);
        saveDataToFile(opStream.toByteArray(),"/data/data/" + FinancialApplication.getApp().getApplicationContext().getPackageName() + "/files/output_slip_data", "/signature_flipped.vff");


        return opStream.toByteArray();
    }

    public static byte[] CreateBmpMonoChromeAndCompress(Bitmap bitmap) {
        byte[] bmpByteArray = FinancialApplication.getGl().getImgProcessing().bitmapToMonoBmp(bitmap, Constants.rgb2MonoAlgo);
        byte[] VF_HEADER = bmpByteArray;
        VF_HEADER = gzipCompressionByte(VF_HEADER);

        Log.i(EReceiptUtils.TAG,"After compressed size = "+ VF_HEADER.length) ;
        return VF_HEADER;


        //if (enableVFformat==false ) {

        //} else {
//            //bmpByteArray = compressZip(bmpByteArray);
//
//            byte[] bDataOffSet = new  byte[4];
//            byte[] bWidth = new  byte[4];
//            byte[] bHeight = new  byte[4];
//
//            System.arraycopy(bmpByteArray, 10 , bDataOffSet, 0 , bDataOffSet.length);
//            System.arraycopy(bmpByteArray, 18 , bWidth,      0 , bWidth.length);
//            System.arraycopy(bmpByteArray, 22 , bHeight,     0 , bHeight.length);
//
//            int iDataOffset = CalculateDecimalfromByteArray(bDataOffSet);
//            int iWidth = CalculateDecimalfromByteArray(bWidth);
//            int iHeight = CalculateDecimalfromByteArray(bHeight);
//            Log.i("Online","               RAW-BMP-IMAGE : " + Tools.bcd2Str(bmpByteArray)) ;
//
//            byte[] bmpheader = new byte[iDataOffset];                                                                       // --> removable
//            byte[] rawImageData = new byte[bmpByteArray.length - iDataOffset];
//            System.arraycopy(bmpByteArray, 0 , bmpheader, 0 ,iDataOffset);                                  // --> removable
//            System.arraycopy(bmpByteArray, iDataOffset  , rawImageData, 0 , rawImageData.length);
//            String imageDataStr = Tools.bcd2Str(bmpByteArray);
//            imageDataStr = imageDataStr.replace(Tools.bcd2Str(rawImageData),"#00#");
//            Log.i("Online","               RAW-UNFLIP-IMAGE : " + Tools.bcd2Str(rawImageData)) ;
//            saveDataToFile(rawImageData,                          "/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_bmp_without_header_unflip.bmp");
//            saveDataToFile(Tools.bcd2Str(rawImageData).getBytes(),"/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_bmp_without_header_unflip.txt");
//            saveDataToFile(imageDataStr.getBytes(),               "/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_bmp_without_header_unflip_replacer.txt");
//
//            byte[] FlippedNoHeader = FlipBmpImageVertically(rawImageData,iWidth);
//            Log.i("Online","               RAW-FLIPED-IMAGE : " + Tools.bcd2Str(FlippedNoHeader)) ;
//            saveDataToFile(FlippedNoHeader,                          "/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_bmp_without_header_flipped.bmp");
//            saveDataToFile(Tools.bcd2Str(FlippedNoHeader).getBytes(),"/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_bmp_without_header_flipped.txt");
//
//            // ---> removable
//            byte[] BMPFlipped = EReceiptUtils.getInstance().MergeArray(bmpheader, FlippedNoHeader);
//            Log.i("Online","               RECONSTURCT-BMP : " + Tools.bcd2Str(BMPFlipped)) ;
//            saveDataToFile(BMPFlipped,                          "/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_flipped_with_bmp_header.bmp");
//            saveDataToFile(Tools.bcd2Str(BMPFlipped).getBytes(),"/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_flipped_with_bmp_header.txt");
//
           // byte[] VF_HEADER = {0x50, 0x1B, 0x47, 0x4C, 0x31, 0x2C, 0x36, 0x2C};
            //byte[] VF_HEADER2 = {0x50, 0x1B, 0x47, 0x4C, 0x31, 0x2C, 0x36, 0x2C};
            //int iWidth  =384;
            //int iHeight =120;
//            VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, (String.valueOf(iWidth) + ",").getBytes());
//            VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, (String.valueOf(iHeight) + ";").getBytes());
//            //VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, rawImageData);
//            VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, gzipCompressionByte(rawImageData));
//            VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, gzipCompressionByte(bmpByteArray));
//
            //VF_HEADER2 = EReceiptUtils.getInstance().MergeArray(VF_HEADER2, (String.valueOf(iWidth) + ",").getBytes());
//            VF_HEADER2 = EReceiptUtils.getInstance().MergeArray(VF_HEADER2, (String.valueOf(iHeight) + ";").getBytes());
//            VF_HEADER2 = EReceiptUtils.getInstance().MergeArray(VF_HEADER2, bmpByteArray);
//
//            saveDataToFile(VF_HEADER, "/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_with_vf_format_compressed.vff");
//            saveDataToFile(VF_HEADER2,"/data/data/th.co.bps.kasetpay.sandbox/app_data/tmp_signature_img", "/signature_with_vf_format_nocompress.vff");
//            Log.i("Online","               VF FORMAT (COMPRESSED) : " + Tools.bcd2Str(VF_HEADER)) ;
//            Log.i("Online","               VF FORMAT (NOCOMPRESS): "  + Tools.bcd2Str(VF_HEADER2)) ;
//
//            //VF_HEADER = gzipCompressionByte(VF_HEADER);
//
//            Log.i(EReceiptUtils.TAG,"After compressed size = "+ VF_HEADER.length) ;
//            return VF_HEADER;
//        }
    }

    public static byte[] FlipBmpImageVertically (byte[] originalImage, int pixWidth) {
        int bWidth = pixWidth/8;
        int nb_row = originalImage.length / bWidth;

        ByteArrayOutputStream bAops = new ByteArrayOutputStream();
        int dataCopyLen =0 ;
        byte[] tmpByte = new byte[bWidth];

        int img_len = originalImage.length;
        for (int idx = img_len ; idx > 0;) {
            try {
                if (idx - bWidth < 0) {dataCopyLen = idx;} else {dataCopyLen=bWidth;}
                tmpByte = new byte[dataCopyLen];
                System.arraycopy(originalImage, idx - dataCopyLen, tmpByte, 0, dataCopyLen);
                bAops.write(tmpByte);
                idx -= (dataCopyLen) ;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bAops.toByteArray();
    }
//    utils::bytes FlipBmpImageVertically(const utils::bytes& bmp_pixel_array,
//                                        unsigned width_px) {
//
//        unsigned width_byte = width_px / 8;
//        unsigned nb_row = bmp_pixel_array.size() / width_byte;
//
//        utils::bytes output;
//        for (unsigned int i = 0; i < nb_row; i++) {
//            int pos = (nb_row - i - 1) * width_byte;
//            output.insert(output.end(),
//                    bmp_pixel_array.begin() + pos,
//                    bmp_pixel_array.begin() + pos + width_byte);
//        }
//
//        return output;
//    }

    public static void saveDataToFile (byte[] data, String path, String FileName){
        File file = new File(path);
        if ((file.exists() && file.isDirectory()) == false) {file.mkdir();}
        file = new File(path + FileName);
        if (file.exists() == true) {file.delete();}
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path+FileName);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static byte[] CreateBmpMonoChromeAndCompress2(Bitmap bitmap) {
        byte[] bmpByteArray = FinancialApplication.getGl().getImgProcessing().bitmapToMonoBmp(bitmap, Constants.rgb2MonoAlgo);
        byte[] VF_HEADER = new byte[0]; //{0x50, 0x1B, 0x47, 0x4C, 0x31, 0x2C, 0x36, 0x2C};
        VF_HEADER = gzipCompressionByte(bmpByteArray);;
//
//        //bmpByteArray = compressZip(bmpByteArray);
//
//        byte[] bDataOffSet = new  byte[4];
//        byte[] bWidth = new  byte[4];
//        byte[] bHeight = new  byte[4];
//
//        System.arraycopy(bmpByteArray, 10 , bDataOffSet, 0 , bDataOffSet.length);
//        System.arraycopy(bmpByteArray, 18 , bWidth, 0 , bWidth.length);
//        System.arraycopy(bmpByteArray, 22 , bHeight, 0 , bHeight.length);
//
//        int iDataOffset = CalculateDecimalfromByteArray(bDataOffSet);
//        int iWidth = CalculateDecimalfromByteArray(bWidth);
//        int iHeight = CalculateDecimalfromByteArray(bHeight);
//
//        byte[] rawImageData = new byte[bmpByteArray.length - iDataOffset-1];
//        System.arraycopy(bmpByteArray, iDataOffset-1 , rawImageData, 0 , rawImageData.length);
//
//
//        VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, (String.valueOf(iWidth) + ",").getBytes());
//        VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, (String.valueOf(iHeight) + ";").getBytes());
//        VF_HEADER = EReceiptUtils.getInstance().MergeArray(VF_HEADER, rawImageData);
//
//
//        VF_HEADER = gzipCompressionByte(VF_HEADER);
//
//        Log.i(EReceiptUtils.TAG,"After compressed size = "+ VF_HEADER.length) ;
        return VF_HEADER;
    }

    public static byte[] compressZip(byte[] bmpArray) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        ZipEntry zipEntry = new ZipEntry("zSign" + Device.getTime(Constants.TIME_PATTERN_TRANS));
        zipEntry.setSize(bmpArray.length);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(bmpArray);
        zipOutputStream.closeEntry();
        zipOutputStream.close();

        return outputStream.toByteArray();
    }












    public static boolean isFileExisting(Context context, Acquirer acquirer) {
        boolean isFoundFile =false;
        String rootLogoDir = EReceiptUtils.getERM_LogoDirectory(context);
        String FileName = File.separator + acquirer.getNii() + "_" + acquirer.getName() + ".bmp";
        File pImg;
        if (new File(rootLogoDir).exists() == true
                && new File(rootLogoDir).isDirectory() == true) {
            if (new File(rootLogoDir + FileName).exists() == true
                    && new File(rootLogoDir + FileName).isFile() == true) {
                isFoundFile=true;
            }
        }

        return isFoundFile;
    }

    public static String getLastLogoFileName = null;
    public static byte[] getSlipHeaderLogoFilename(Context context, Acquirer acquirer) {
        String rootLogoDir = EReceiptUtils.getERM_LogoDirectory(context) ;
        String FileName = File.separator + acquirer.getNii() + "_" + acquirer.getName() + ".bmp";
        File pImg ;
        getLastLogoFileName=null;
        if (new File(rootLogoDir).exists() != true) {
            pImg = new File(rootLogoDir);
            pImg.mkdir();
        }

        if (new File(rootLogoDir + FileName).exists() == true
                && new File(rootLogoDir + FileName).isFile()==true) {
            getLastLogoFileName = rootLogoDir + FileName;
            return getLogoByteArrayWithoutCompression(rootLogoDir, FileName, false,false);
        }
        else  {
            getLastLogoFileName = "kBank_Default.bmp";
            if(acquirer.getName().toUpperCase().contains(Constants.ACQ_SMRTPAY)) {
                getLastLogoFileName = "logo_kbank_smartpay.bmp";
            }
            if(acquirer.getName().toUpperCase().equals(Constants.ACQ_KPLUS)) {
                getLastLogoFileName = "logo_kbank_kplus.bmp";
            }
            if(acquirer.getName().toUpperCase().equals(Constants.ACQ_DOLFIN_INSTALMENT)) {
                getLastLogoFileName = "logo_kbank_dolfin_smartpay.bmp";
            }

            return getLogoByteArrayWithoutCompression("", getLastLogoFileName, true,false);
        }

        //return null;


//        String LogoFileName =Component.loadString(Constants.DN_PARAM_SLIP_LOGO_PATH) + File.separator + Constants.SLIP_LOGO_NAME;
//        File pImg ;
//        boolean toggle_use_asset_logo =false;
//        if (LogoFileName != null) {
//            pImg = new File(LogoFileName) ;
//            if (pImg.isFile()) {
//                return getImageData(Constants.DN_PARAM_SLIP_LOGO_PATH,Constants.SLIP_LOGO_NAME);
//            } else {
//                toggle_use_asset_logo=true;
//            }
//        } else {
//            toggle_use_asset_logo=true;
//        }
//
//        if (toggle_use_asset_logo) {
//
//            try {
//                return getImageData("logo_kbank_black.jpg");
//            } catch (Exception ex) {
//                // do nothing
//            }
//
//        }
//        return null;
    }
    // default path output of converted image to BMP
    private static String localAppFilePath = FinancialApplication.getApp().getFilesDir().getPath().toString();
    private static String localAppFileName = "ercm_hlogo.bmp";
    private static String localAppFileAbsolutePath = localAppFilePath + File.separator + localAppFileName;
    private static String localAppGZIPCompressedFileName = "ercm_hlogo.gz";
    private static String localAppGZIPCompressedAbsolutePath = localAppFilePath + File.separator + localAppGZIPCompressedFileName;





    public static byte[] getSignatureVerifoneFormat (byte[] data) {

        return CreateVerifoneFormat(data);
        //return rawSignature;
    }

    /*=========================================================================================
       This static function use by read-logo data for [ERM-INITIAL-TERMINAL] Only
      =========================================================================================*/
    public static byte[] getLogoByteArrayWithoutCompression (String path, String fName, Boolean getFromAssets, boolean enableVFformat) {
        ByteArrayOutputStream bArrOpS = new ByteArrayOutputStream() ;
        File f ;
        try {
            Bitmap bmp = null;
            if(getFromAssets==true) {
                InputStream inputStream = FinancialApplication.getApp().getAssets().open(fName);
                bmp  = BitmapFactory.decodeStream(inputStream);
                bmp.compress(Bitmap.CompressFormat.JPEG, 50, bArrOpS );
            } else {
                f = new File(path + fName);
                bmp = BitmapFactory.decodeStream(new FileInputStream(f));
                bmp.compress(Bitmap.CompressFormat.JPEG, 50, bArrOpS );
            }
            Log.i(EReceiptUtils.TAG,"Filename : " + localAppFilePath + File.separator + localAppFileName + " (Sizes " + bArrOpS.size() + " bytes.) dimension(H x W) = " + bmp.getHeight() +" x " + bmp.getWidth() +" pixels.") ;

            return CreateBmpMonoChromeAndCompress(bmp);

//            File targFile = new File(path +fName);
//            byte[] bmpRawData = new byte[(int)targFile.length()];
//            FileInputStream stream = new FileInputStream(targFile);
//            stream.read(bmpRawData,0,bmpRawData.length);
//            stream.close();
//            return CreateBmpMonoChromeAndCompress(bmpRawData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

//        ByteArrayOutputStream bArrOpS = new ByteArrayOutputStream() ;
//        File f ;
//        try {
//            boolean ret = false ;
//            if (new File(localAppFileAbsolutePath).exists() == false) {
//                if (new File(localAppFilePath).exists() == false ) {
//                    new File(localAppFilePath).mkdir();
//                }
//                f = new File(Component.loadString(path), fName);
//                Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(f));
//                bmp.compress(Bitmap.CompressFormat.JPEG, 100, bArrOpS );
//                Log.i(EReceiptUtils.TAG,"Filename : " + localAppFilePath + File.separator + localAppFileName + " (Sizes " + bArrOpS.size() + " bytes.) dimension(H x W) = " + bmp.getHeight() +" x " + bmp.getWidth() +" pixels.") ;
//
//                // convert Bitmap(JPEG) to (BMP File)
//                ret =  BitmapImageConverterUtils.save(bmp,localAppFileAbsolutePath);
//                Log.i(EReceiptUtils.TAG,"File : " + localAppFileAbsolutePath + " | image converter result : " + ret) ;
//            } else {
//                ret=true;
//            }
//
//            if(ret==true) {
//                if (gzipCompression(localAppFilePath,localAppFileName)) {
//                    return readImageData(localAppFilePath, localAppGZIPCompressedFileName);
//                }
//            }
//        } catch (FileNotFoundException ex) {
//            Log.e(EReceiptUtils.TAG,"Error during convert JPEG to BMP : " + ex.getMessage());
//        } catch (IOException ex) {
//            Log.e(EReceiptUtils.TAG,"Error during convert JPEG to BMP : " + ex.getMessage());
//        }
//        return null;
    }

//    private static byte[] getImageData (String path, String fName) {
//        ByteArrayOutputStream bArrOpS = new ByteArrayOutputStream() ;
//        File f ;
//        try {
//            boolean ret = false ;
//            if (new File(localAppFileAbsolutePath).exists() == false) {
//                if (new File(localAppFilePath).exists() == false ) {
//                    new File(localAppFilePath).mkdir();
//                }
//                f = new File(Component.loadString(path), fName);
//                Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(f));
//                bmp.compress(Bitmap.CompressFormat.JPEG, 100, bArrOpS );
//                Log.i(EReceiptUtils.TAG,"Filename : " + localAppFilePath + File.separator + localAppFileName + " (Sizes " + bArrOpS.size() + " bytes.) dimension(H x W) = " + bmp.getHeight() +" x " + bmp.getWidth() +" pixels.") ;
//
//                // convert Bitmap(JPEG) to (BMP File)
//                ret =  BitmapImageConverterUtils.save(bmp,localAppFileAbsolutePath);
//                Log.i(EReceiptUtils.TAG,"File : " + localAppFileAbsolutePath + " | image converter result : " + ret) ;
//            } else {
//                ret=true;
//            }
//
//            if(ret==true) {
//                if (gzipCompression(localAppFilePath,localAppFileName)) {
//                    return readImageData(localAppFilePath, localAppGZIPCompressedFileName);
//                }
//            }
//        } catch (FileNotFoundException ex) {
//            Log.e(EReceiptUtils.TAG,"Error during convert JPEG to BMP : " + ex.getMessage());
//        } catch (IOException ex) {
//            Log.e(EReceiptUtils.TAG,"Error during convert JPEG to BMP : " + ex.getMessage());
//        }
//        return null;
//    }

//    private static byte[] getImageData (String FileName) {
//        ByteArrayOutputStream bArrOpS = new ByteArrayOutputStream() ;
//        if (FileName != null) {
//            InputStream inputStream ;
//            try {
//                boolean ret =false ;
//                if (new File(localAppFileAbsolutePath).exists() == false) {
//                    if (new File(localAppFilePath).exists() == false ) {
//                        new File(localAppFilePath).mkdir();
//                    }
//                    inputStream = FinancialApplication.getApp().getAssets().open(FileName);
//                    Bitmap bmp = BitmapFactory.decodeStream(inputStream);
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 50, bArrOpS );
//                    Log.i(EReceiptUtils.TAG,"Filename : " + FileName + " (Sizes " + bArrOpS.size() + " bytes.) dimension(H x W) = " + bmp.getHeight() +" x " + bmp.getWidth() +" pixels.") ;
//
//                    //Resize image
//                    Log.i(EReceiptUtils.TAG,"Before resize size = " + bmp.getByteCount()) ;
//                    bmp = reSize(bmp) ;
//
//
//                    return CreateBmpMonoChromeAndCompress(bmp);
//
//
//                    // convert Bitmap(JPEG) to (BMP File)
//                    //ret =  BitmapImageConverterUtils.save(bmp,localAppFileAbsolutePath);
//                    //Log.i(EReceiptUtils.TAG,"File : " + localAppFileAbsolutePath + " | image converter result : " + ret) ;
//                }
//
//                else {
//                    ret = true;
//                }
//
//                if (ret==true) {
//                    File tmpLogo = new File(localAppFilePath + File.separator + localAppFileName);
//                    Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(tmpLogo));
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, bArrOpS );
//
//                    return CreateBmpMonoChromeAndCompress(bmp);
//                }
////                else {
////                    ret=true;
////                }
////
////                if(ret==true) {
////                    if (gzipCompression()) {
////                        return readImageData(localAppGZIPCompressedAbsolutePath);
////                    }
////                }
//
//            } catch (Exception ex ) {
//                Log.e(EReceiptUtils.TAG,"Error during convert JPEG to BMP : " + ex.getMessage());
//            }
//        }
//        return null;
//    }
    private static byte[] doConvertMonoChromeBitmap (int width, int height) {
        ByteArrayOutputStream bArrOpS = new ByteArrayOutputStream() ;
        Bitmap bmpMonochrome = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpMonochrome);
        ColorMatrix ma = new ColorMatrix();
        ma.setSaturation(0);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(ma));
        canvas.drawBitmap(bmpMonochrome, 0, 0, paint);
        bmpMonochrome.compress(Bitmap.CompressFormat.JPEG,50,bArrOpS);
        return bArrOpS.toByteArray();
    }
    private static byte[] readImageData (String FPath , String FName) {
        return  readImageData(FPath +File.separator + FName);
    }
    private static byte[] readImageData (String absolutePathFileName) {
        ByteArrayOutputStream bArrOpS = new ByteArrayOutputStream() ;
        File targetfile = new File(absolutePathFileName);
        Bitmap bmp = null;
        try {
            bmp = BitmapFactory.decodeStream(new FileInputStream(targetfile));
            bmp.compress(Bitmap.CompressFormat.JPEG, 50, bArrOpS );

            return bArrOpS.toByteArray();
        } catch (FileNotFoundException e) {
            Log.e(EReceiptUtils.TAG,"Error during read image data from (" + absolutePathFileName + ") : " + e.getMessage());
        }
        return null;
    }
    private static boolean gzipCompression (String path, String Name) {
        Boolean gZipResult =false;
        byte[] dataToCompress = readImageData(path,Name);
        gZipResult = gzipCompression(dataToCompress);

        return gZipResult;
    }
    private static byte[] gzipCompressionEx (String path, String Name) {
        byte[] compressedData = gzipCompressionEx(readImageData(path,Name));
        return compressedData;
    }
    private static boolean gzipCompression (byte[] rawData) {
        Boolean gZipResult =false;

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(rawData.length);
        try {
            GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
            try {
                zipStream.write(rawData);
            } finally {
                zipStream.close();
            }
        } catch (IOException ex){
            try {
                byteStream.close();
            } catch (IOException e) {
                Log.e(EReceiptUtils.TAG,"Error GZIP compression ZipStream.write : " + e.getMessage());
            }
        }

        byte[] compressedData = byteStream.toByteArray();
        FileOutputStream fileStream = null;
        try {
            fileStream = new FileOutputStream(localAppGZIPCompressedAbsolutePath);
            fileStream.write(compressedData);
            gZipResult=true;
        } catch (FileNotFoundException e) {
            Log.e(EReceiptUtils.TAG,"Error during write compressed data to file : " + e.getMessage());
        } catch (IOException e) {
            Log.e(EReceiptUtils.TAG,"Error during write compressed data to file : " + e.getMessage());
        } finally {
            try {
                fileStream.close();
            } catch (Exception ex) {
                Log.e(EReceiptUtils.TAG,"Error during close compressed file : " + ex.getMessage());
            }
        }

        return gZipResult;
    }
    private static byte[] gzipCompressionEx (byte[] rawData) {
        Boolean gZipResult =false;

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(rawData.length);
        try {
            GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
            try {
                zipStream.write(rawData);
            } finally {
                zipStream.close();
            }
        } catch (IOException ex){
            try {
                byteStream.close();
            } catch (IOException e) {
                Log.e(EReceiptUtils.TAG,"Error GZIP compression ZipStream.write : " + e.getMessage());
            }
        }

        byte[] compressedData = byteStream.toByteArray();
        return compressedData;
    }
    private static byte[] gzipCompressionByte (byte[] rawData) {
        Boolean gZipResult =false;

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(rawData.length);
        try {

            GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
            try {

                zipStream.write(rawData);
            } finally {
                zipStream.close();
            }
        } catch (IOException ex){
            try {
                byteStream.close();
            } catch (IOException e) {
                Log.e(EReceiptUtils.TAG,"Error GZIP compression ZipStream.write : " + e.getMessage());
            }
        }

        byte[] compressedData = byteStream.toByteArray();
        return compressedData;
    }
    private static byte[] fixSizeArray(byte[] imageData, int len) {
        if (imageData.length > len) {
            byte[] tmpImagData = new byte[len];
            System.arraycopy(imageData, 0 , tmpImagData, 0,len);
            return tmpImagData;
        }
        return imageData;
    }

    public void saveFileERCMPBK(String paramFileName, String paramPath, Context context) {
        try {
            if(paramFileName != null && !paramFileName.isEmpty()
                    && paramPath != null && !paramPath.isEmpty() ) {
                if ( ! (new File(paramPath.concat(File.separator).concat(paramFileName)).exists()) ) {return;}
                File file = new File(paramPath, paramFileName);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                byte[] dataBytes = outputStream.toByteArray();
                saveToInternalStorage(dataBytes, context, "kbnk_ercm_pbk.dat");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    public void saveBmpImageParam(String paramFileName, String paramPath, Context context, String nii, String acqName){
        if (nii == null || nii.isEmpty() || acqName == null || acqName.isEmpty()) {
            return;
        }

        String savedFileName = nii + "_" + acqName + ".bmp";
        try {
            if(!paramFileName.isEmpty()){
                File f = new File(paramPath, paramFileName);
                if(f.exists()) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    FileInputStream fis = new FileInputStream(f);
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    byte[] imageBytes = outputStream.toByteArray();

                    byte[] headerBytes = Arrays.copyOf(imageBytes, 2);
                    if (imageBytes.length > 0) {
                        if (Arrays.equals(headerBytes, BMP_HEADER)) { // BMP format
                            saveToInternalStorage(imageBytes, context, savedFileName);
                        } else { // none BMP format
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            byte[] bmpBytes = FinancialApplication.getGl().getImgProcessing().bitmapToMonoBmp(bitmap, Constants.rgb2MonoAlgo);
                            saveToInternalStorage(bmpBytes, context, savedFileName);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    private void saveToInternalStorage(byte[] dataBytes, Context context, String savedFileName){
        ContextWrapper cw = new ContextWrapper(context);
        // path to /data/data/myapp/app_imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, savedFileName);

        if (mypath.exists() && !mypath.delete()) {
            return;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            fos.write(dataBytes);
            fos.flush();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }

}
