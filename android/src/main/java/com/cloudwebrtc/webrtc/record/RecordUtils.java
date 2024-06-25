/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

 package com.cloudwebrtc.webrtc.record;

 import android.graphics.Bitmap;
 import android.graphics.YuvImage;
 import android.media.MediaCodecInfo;
 import android.media.MediaCodecList;
 import android.media.MediaFormat;
 import android.os.Build;
 import android.text.TextUtils;
 import android.util.Log;
 import com.google.android.gms.common.util.ArrayUtils;
 import java.nio.ByteBuffer;
 
 import static android.graphics.ImageFormat.NV21;
 
 /**
  * RecordUtils provides helper functions for managing thread safety.
  */
 public final class RecordUtils {
   static String TAG = "RecordUtils";
 //  static {
 //    System.loadLibrary("native-lib");
 //  }
 
 //  public native static void ConvertRGBToGRAY(unsigned char in[], unsigned char out[],int w,int h,int type);
 
 //  public static native int createFifo(String path);
 
 //  public static int CreateFifoFile(String path){
 //    File file = new File(path);
 //    if(file.exists() == false)
 //      return createFifo(path);
 //    return 0;
 //  }
 
   private RecordUtils() {}
 
   /** Helper method which throws an exception  when an assertion has failed. */
   public static void assertIsTrue(boolean condition) {
     if (!condition) {
       throw new AssertionError("Expected condition to be true");
     }
   }
 
   /** Helper method for building a string of thread information.*/
   public static String getThreadInfo() {
     return "@[name=" + Thread.currentThread().getName() + ", id=" + Thread.currentThread().getId()
         + "]";
   }
 
   /** Information about the current build, taken from system properties. */
   public static void logDeviceInfo(String tag) {
     Log.d(tag, "Android SDK: " + Build.VERSION.SDK_INT + ", "
             + "Release: " + Build.VERSION.RELEASE + ", "
             + "Brand: " + Build.BRAND + ", "
             + "Device: " + Build.DEVICE + ", "
             + "Id: " + Build.ID + ", "
             + "Hardware: " + Build.HARDWARE + ", "
             + "Manufacturer: " + Build.MANUFACTURER + ", "
             + "Model: " + Build.MODEL + ", "
             + "Product: " + Build.PRODUCT);
   }
 
   /** Returns the consumer friendly device name */
   public static String getDeviceName() {
     String manufacturer = Build.MANUFACTURER;
     String model = Build.MODEL;
     if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
       return capitalize(model);
     }
     return capitalize(manufacturer) + " " + model;
   }
 
   private static String capitalize(String str) {
     if (TextUtils.isEmpty(str)) {
       return str;
     }
     char[] arr = str.toCharArray();
     boolean capitalizeNext = true;
     String phrase = "";
     for (char c : arr) {
       if (capitalizeNext && Character.isLetter(c)) {
         phrase += Character.toUpperCase(c);
         capitalizeNext = false;
         continue;
       } else if (Character.isWhitespace(c)) {
         capitalizeNext = true;
       }
       phrase += c;
     }
     return phrase;
   }

 //  public static Bitmap captureBitmapFromYuvFrame(I420Frame i420Frame) {
 //    YuvImage yuvImage = i420ToYuvImage(i420Frame.yuvPlanes,
 //            i420Frame.yuvStrides,
 //            i420Frame.width,
 //            i420Frame.height);
 //    ByteArrayOutputStream stream = new ByteArrayOutputStream();
 //    Rect rect = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());
 //
 //    // Compress YuvImage to jpeg
 //    yuvImage.compressToJpeg(rect, 100, stream);
 //
 //    // Convert jpeg to Bitmap
 //    byte[] imageBytes = stream.toByteArray();
 //    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
 //    Matrix matrix = new Matrix();
 //
 //    // Apply any needed rotation
 //    matrix.postRotate(i420Frame.rotationDegree);
 //    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
 //            true);
 //
 //    return bitmap;
 //  }
 
   public static YuvImage i420ToYuvImage(ByteBuffer[] yuvPlanes,
                                   int[] yuvStrides,
                                   int width,
                                   int height) {
     if (yuvStrides[0] != width) {
       return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
     }
     if (yuvStrides[1] != width / 2) {
       return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
     }
     if (yuvStrides[2] != width / 2) {
       return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
     }
 
     byte[] bytes = new byte[yuvStrides[0] * height +
             yuvStrides[1] * height / 2 +
             yuvStrides[2] * height / 2];
     ByteBuffer tmp = ByteBuffer.wrap(bytes, 0, width * height);
     copyPlane(yuvPlanes[0], tmp);
 
     byte[] tmpBytes = new byte[width / 2 * height / 2];
     tmp = ByteBuffer.wrap(tmpBytes, 0, width / 2 * height / 2);
 
     copyPlane(yuvPlanes[2], tmp);
     for (int row = 0 ; row < height / 2 ; row++) {
       for (int col = 0 ; col < width / 2 ; col++) {
         bytes[width * height + row * width + col * 2]
                 = tmpBytes[row * width / 2 + col];
       }
     }
     copyPlane(yuvPlanes[1], tmp);
     for (int row = 0 ; row < height / 2 ; row++) {
       for (int col = 0 ; col < width / 2 ; col++) {
         bytes[width * height + row * width + col * 2 + 1] =
                 tmpBytes[row * width / 2 + col];
       }
     }
     return new YuvImage(bytes, NV21, width, height, null);
   }
 
   public static  YuvImage fastI420ToYuvImage(ByteBuffer[] yuvPlanes,
                                       int[] yuvStrides,
                                       int width,
                                       int height) {
     byte[] bytes = new byte[width * height * 3 / 2];
     int i = 0;
     for (int row = 0 ; row < height ; row++) {
       for (int col = 0 ; col < width ; col++) {
         bytes[i++] = yuvPlanes[0].get(col + row * yuvStrides[0]);
       }
     }
     for (int row = 0 ; row < height / 2 ; row++) {
       for (int col = 0 ; col < width / 2; col++) {
         bytes[i++] = yuvPlanes[2].get(col + row * yuvStrides[2]);
         bytes[i++] = yuvPlanes[1].get(col + row * yuvStrides[1]);
       }
     }
     return new YuvImage(bytes, NV21, width, height, null);
   }
 
   public static void copyPlane(ByteBuffer src, ByteBuffer dst) {
     src.position(0).limit(src.capacity());
     dst.put(src);
     dst.position(0).limit(dst.capacity());
   }
 
   public static byte[] YV12toYUV420PackedSemiPlanar(final byte[] input, final byte[] output, final int width, final int height) {
         /*
          * COLOR_TI_FormatYUV420PackedSemiPlanar is NV12
          * We convert by putting the corresponding U and V bytes together (interleaved).
          */
     final int frameSize = width * height;
     final int qFrameSize = frameSize/4;
 
     System.arraycopy(input, 0, output, 0, frameSize); // Y
 
     for (int i = 0; i < qFrameSize; i++) {
       output[frameSize + i*2] = input[frameSize + i + qFrameSize]; // Cb (U)
       output[frameSize + i*2 + 1] = input[frameSize + i]; // Cr (V)
     }
     return output;
   }
 
   public static byte[] YV12toYUV420Planar(byte[] input, byte[] output, int width, int height) {
         /*
          * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
          * So we just have to reverse U and V.
          */
     final int frameSize = width * height;
     final int qFrameSize = frameSize/4;
 
     System.arraycopy(input, 0, output, 0, frameSize); // Y
     System.arraycopy(input, frameSize, output, frameSize + qFrameSize, qFrameSize); // Cr (V)
     System.arraycopy(input, frameSize + qFrameSize, output, frameSize, qFrameSize); // Cb (U)
 
     return output;
   }
 
   public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
     byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
     // Rotate the Y luma
     int i = 0;
     for (int x = 0; x < imageWidth; x++) {
       for (int y = imageHeight - 1; y >= 0; y--) {
         yuv[i] = data[y * imageWidth + x];
         i++;
       }
     }
     // Rotate the U and V color components
     i = imageWidth * imageHeight * 3 / 2 - 1;
     for (int x = imageWidth - 1; x > 0; x = x - 2) {
       for (int y = 0; y < imageHeight / 2; y++) {
         yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
         i--;
         yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
                 + (x - 1)];
         i--;
       }
     }
     return yuv;
   }
 
   public static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
     byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
     int i = 0;
     int count = 0;
     for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
       yuv[count] = data[i];
       count++;
     }
     i = imageWidth * imageHeight * 3 / 2 - 1;
     for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
             * imageHeight; i -= 2) {
       yuv[count++] = data[i - 1];
       yuv[count++] = data[i];
     }
     return yuv;
   }
 
   public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth, int imageHeight) {
     byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
     int nWidth = 0, nHeight = 0;
     int wh = 0;
     int uvHeight = 0;
     if (imageWidth != nWidth || imageHeight != nHeight) {
       nWidth = imageWidth;
       nHeight = imageHeight;
       wh = imageWidth * imageHeight;
       uvHeight = imageHeight >> 1;// uvHeight = height / 2
     }
     // ??Y
     int k = 0;
     for (int i = 0; i < imageWidth; i++) {
       int nPos = 0;
       for (int j = 0; j < imageHeight; j++) {
         yuv[k] = data[nPos + i];
         k++;
         nPos += imageWidth;
       }
     }
     for (int i = 0; i < imageWidth; i += 2) {
       int nPos = wh;
       for (int j = 0; j < uvHeight; j++) {
         yuv[k] = data[nPos + i];
         yuv[k + 1] = data[nPos + i + 1];
         k += 2;
         nPos += imageWidth;
       }
     }
     return rotateYUV420Degree180(yuv, imageWidth, imageHeight);
   }
 
   public static byte[] bitmapToByteArray(Bitmap bm) {
         // Create the buffer with the correct size
         int iBytes = bm.getWidth() * bm.getHeight() ;
         byte[] res = new byte[iBytes];
         Bitmap.Config format = bm.getConfig();
         if (format == Bitmap.Config.ARGB_8888)
         {
             ByteBuffer buffer = ByteBuffer.allocate(iBytes*4);
             // Log.e("DBG", buffer.remaining()+""); -- Returns a correct number based on dimensions
             // Copy to buffer and then into byte array
             bm.copyPixelsToBuffer(buffer);
             byte[] arr = buffer.array();
             for(int i=0;i<iBytes;i++)
             {
                 int A,R,G,B;
                 R=(int)(arr[i*4+0]) & 0xff;
                 G=(int)(arr[i*4+1]) & 0xff;
                 B=(int)(arr[i*4+2]) & 0xff;
                 //A=arr[i*4+3];
                 byte r = (byte)(0.2989 * R + 0.5870 * G + 0.1140 * B) ;
                 res[i] = r;
             }
         }
         if (format == Bitmap.Config.RGB_565)
         {
             ByteBuffer buffer = ByteBuffer.allocate(iBytes*2);
             // Log.e("DBG", buffer.remaining()+""); -- Returns a correct number based on dimensions
             // Copy to buffer and then into byte array
             bm.copyPixelsToBuffer(buffer);
             byte[] arr = buffer.array();
             for(int i=0;i<iBytes;i++)
             {
                 float A,R,G,B;
                 R = ((arr[i*2+0] & 0xF8) );
                 G = ((arr[i*2+0] & 0x7) << 5) + ((arr[i*2+1] & 0xE0) >> 5);
                 B = ((arr[i*2+1] & 0x1F) << 3 );
                 byte r = (byte)(0.2989 * R + 0.5870 * G + 0.1140 * B) ;
                 res[i] = r;
             }
 
 //            uint16_t *parr = (uint16_t*)in;
 //>           for(int i=0;i<sz;i++)
 //>           {
 //>               float A,R,G,B;
 //>               R = ((parr[i] & 0xF800)>> 11) *8;
 //>               G = ((parr[i] & 0x07e0)>> 5) *4;
 //>               B = ((parr[i] & 0x001F)>> 0) *8;
 //>
 //>               unsigned char ir = 0.2989 * R + 0.5870 * G + 0.1140 * B;
 //>               out[i] = ir;
 //>           }
         }
         // Log.e("DBG", buffer.remaining()+""); -- Returns 0
         return res;
     }
 
   public static void findSupportedCodec(){
     MediaCodecInfo[] list = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
       for (MediaCodecInfo info : list) {
           if (!info.isEncoder())
               continue;
           String name = info.isEncoder() ? "encoder" : "decoder";
           for (String mimeType : info.getSupportedTypes()) {
               Log.e(TAG, name + " " + info.getName() + " MimeType " + mimeType);
               // Check if HW codec supports known color format.
               MediaCodecInfo.CodecCapabilities capabilities;
               try {
                   capabilities = info.getCapabilitiesForType(mimeType);
               } catch (IllegalArgumentException e) {
                   Log.e(TAG, "Cannot retrieve encoder capabilities " + mimeType);
                   continue;
               }

               for (int codecColorFormat : capabilities.colorFormats) {

                   //          Log.e(TAG, "Found target " + name + " for mime " + mimeType + ". Color: 0x"
                   //                  + Integer.toHexString(codecColorFormat) );
                   Log.e(TAG, "Found target " + name + " for mime " + mimeType + ". Color: " + codecColorFormat);

               }
           }
       }
   }
 
   public static boolean checkSupportRecording(){
     String mimeTypeVideo = MediaFormat.MIMETYPE_VIDEO_MPEG4;
     String mimeTypeAudio = MediaFormat.MIMETYPE_AUDIO_AAC;
     int result = 0;
     MediaCodecInfo[] list = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
     for (MediaCodecInfo info : list) {

       if(!info.isEncoder())
         continue;
 
       for (String mimeType : info.getSupportedTypes()) {
 
         if(mimeType.equals(mimeTypeAudio)){
           result++;
           continue;
         }
 
         if(mimeType.equals(mimeTypeVideo)){
           // Check if HW codec supports known color format.
           MediaCodecInfo.CodecCapabilities capabilities;
           try {
             capabilities = info.getCapabilitiesForType(mimeType);
           } catch (IllegalArgumentException e) {
             continue;
           }
 
           int compareFormat = (Build.VERSION.SDK_INT > Build.VERSION_CODES.M?MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
           for (int codecColorFormat : capabilities.colorFormats) {
             if(codecColorFormat == compareFormat)
               result++;
 
 
           }
         }
       }
     }
 
     return result >= 2;
   }
 
   public static String getMp4VideoFormatSupport(){
     String[] mp4Type = new String[] {"video/avc", "video/mp4v-es", "video/hevc", "video/mpeg2"};
 
     for(String mimeType : mp4Type){
       MediaCodecInfo[] list = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
       for (MediaCodecInfo info : list) {
         if(!info.isEncoder())
           continue;
 
         if(ArrayUtils.contains(info.getSupportedTypes(),mimeType)){
           MediaCodecInfo.CodecCapabilities capabilities;
           try {
             capabilities = info.getCapabilitiesForType(mimeType);
           } catch (IllegalArgumentException e) {
             continue;
           }
 
           int compareFormat = (Build.VERSION.SDK_INT > Build.VERSION_CODES.M?MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
           for (int codecColorFormat : capabilities.colorFormats) {
             if(codecColorFormat == compareFormat)
               return mimeType;
           }
         }
       }
     }
     return null;
   }
 
   public static String getMp4AudioFormatSupport(){
     String[] audioList = new String[]{"audio/mp4a-latm"};
 
     for(String mimeType : audioList) {
       MediaCodecInfo[] list = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
       for (MediaCodecInfo info : list) {
         if(!info.isEncoder())
           continue;
         if(ArrayUtils.contains(info.getSupportedTypes(),mimeType)){
           return mimeType;
         }
       }
     }
     return null;
   }
 
}
 