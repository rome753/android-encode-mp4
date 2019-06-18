package cc.rome753.encodemp4;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;


import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 对YUV视频流进行编码
 * Created by chao on 2017/5/6.
 */

public class H264EncodeConsumer extends Thread {
    private static final String TAG = "EncodeVideo";
    private static final String MIME_TYPE = "video/avc";
    // 间隔1s插入一帧关键帧
    private static final int FRAME_INTERVAL = 1;
    // 绑定编码器缓存区超时时间为10s
    private static final int TIMES_OUT = 10000;

    // 硬编码器
    private MediaCodec mVideoEncodec;
    private boolean isExit = false;
    private boolean isEncoderStart = false;

    private boolean isAddKeyFrame = false;
    private EncoderParams mParams;
    private MediaFormat newFormat;
    private WeakReference<MediaMuxerUtil> mMuxerRef;
    private int mColorFormat;
    private long nanoTime = 0;//System.nanoTime();

    synchronized void setTmpuMuxer(MediaMuxerUtil mMuxer, EncoderParams mParams) {
        this.mMuxerRef = new WeakReference<>(mMuxer);
        this.mParams = mParams;
        MediaMuxerUtil muxer = mMuxerRef.get();

        if (muxer != null && newFormat != null) {
            muxer.addTrack(newFormat, true);
        }
    }

    private void startCodec() {
        try {
            MediaCodecInfo mCodecInfo = selectSupportCodec(MIME_TYPE);
            if (mCodecInfo == null) {
                Log.d(TAG, "startCodec fail" + MIME_TYPE);
                return;
            }
            mColorFormat = selectSupportColorFormat(mCodecInfo, MIME_TYPE);
            mVideoEncodec = MediaCodec.createByCodecName(mCodecInfo.getName());
            MediaFormat mFormat = MediaFormat.createVideoFormat(MIME_TYPE, mParams.getFrameHeight(), mParams.getFrameWidth());
            mFormat.setInteger(MediaFormat.KEY_BIT_RATE, mParams.getBitRate());
            mFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mParams.getFrameRate());
            mFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);         // 颜色格式
            mFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);
            if (mVideoEncodec != null) {
                mVideoEncodec.configure(mFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mVideoEncodec.start();
                isEncoderStart = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startCodec" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopCodec() {
        if (mVideoEncodec != null) {
            mVideoEncodec.stop();
            mVideoEncodec.release();
            mVideoEncodec = null;
            isAddKeyFrame = false;
            isEncoderStart = false;

            Log.d(TAG, "stopCodec");
        }
    }

//    private long lastPush = 0;

    private LinkedBlockingQueue<RawData> queue = new LinkedBlockingQueue<>();

    static class RawData {
        byte[] buf;
        long timeStamp;

        RawData(byte[] buf, long timeStamp) {
            this.buf = buf;
            this.timeStamp = timeStamp;
        }
    }

    public void addData(byte[] yuvData) {
        Log.e("chao", "**********add video" + System.nanoTime() / 1000 / 1000);
        queue.offer(new RawData(yuvData, System.nanoTime()));
    }

    private RawData removeData() {
        return queue.poll();
    }

    private void handleData(byte[] yuvData, long timeStamp) {
        if (!isEncoderStart || mParams == null)
            return;
        try {
            int mWidth = mParams.getFrameWidth();
            int mHeight = mParams.getFrameHeight();

            byte[] tempBytes = new byte[yuvData.length];
            byte[] resultBytes = new byte[yuvData.length];
            if(mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                YUVTools.nv21ToYv12(yuvData, tempBytes, mWidth, mHeight);
                YUVTools.rotateP90(tempBytes, resultBytes, mWidth, mHeight);
            } else /*if(mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)*/{ //nv12
                YUVTools.rotateSP90(yuvData, resultBytes, mWidth, mHeight);
            }

            feedMediaCodecData(resultBytes, timeStamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(21)
    private void feedMediaCodecData(byte[] data, long timeStamp) {
        int inputBufferIndex = mVideoEncodec.dequeueInputBuffer(TIMES_OUT);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mVideoEncodec.getInputBuffer(inputBufferIndex);
            if (inputBuffer != null) {
                inputBuffer.clear();
                inputBuffer.put(data);
            }
            Log.e("chao", "video set pts......." + (timeStamp) / 1000 / 1000);
            mVideoEncodec.queueInputBuffer(inputBufferIndex, 0, data.length, System.nanoTime() / 1000
                    , MediaCodec.BUFFER_FLAG_KEY_FRAME);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        try {
            if (!isEncoderStart) {
                Thread.sleep(200);

                startCodec();
            }
            while (!isExit && isEncoderStart) {
                RawData rawData = removeData();
                if(rawData != null) {
                    handleData(rawData.buf, rawData.timeStamp);
                }

                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex;
                do {
                    outputBufferIndex = mVideoEncodec.dequeueOutputBuffer(mBufferInfo, TIMES_OUT);
                    if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                        Log.i(TAG, "INFO_TRY_AGAIN_LATER");
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        synchronized (H264EncodeConsumer.this) {
                            newFormat = mVideoEncodec.getOutputFormat();
                            if (mMuxerRef != null) {
                                MediaMuxerUtil muxer = mMuxerRef.get();
                                if (muxer != null) {
                                    muxer.addTrack(newFormat, true);
                                }
                            }
                        }

                        Log.i(TAG, "编码器输出缓存区格式改变，添加视频轨道到混合器");
                    } else {
                        ByteBuffer outputBuffer = mVideoEncodec.getOutputBuffer(outputBufferIndex);
                        int type = outputBuffer.get(4) & 0x1F;

                        Log.d(TAG, "------还有数据---->" + type);
                        if (type == 7 || type == 8) {

                            Log.e(TAG, "------PPS、SPS帧(非图像数据)，忽略-------");
                            mBufferInfo.size = 0;
                        } else if (type == 5) {
                            if (mMuxerRef != null) {
                                MediaMuxerUtil muxer = mMuxerRef.get();
                                if (muxer != null) {
                                    Log.i(TAG, "------编码混合  视频关键帧数据-----" + mBufferInfo.presentationTimeUs / 1000);
                                    muxer.pumpStream(outputBuffer, mBufferInfo, true);
                                }
                                isAddKeyFrame = true;
                            }
                        } else {
                            if (isAddKeyFrame) {
                                if (isAddKeyFrame && mMuxerRef != null) {
                                    MediaMuxerUtil muxer = mMuxerRef.get();
                                    if (muxer != null) {
                                        Log.i(TAG, "------编码混合  视频普通帧数据-----" + mBufferInfo.presentationTimeUs / 1000);
                                        muxer.pumpStream(outputBuffer, mBufferInfo, true);
                                    }
                                }
                            }
                        }
                        mVideoEncodec.releaseOutputBuffer(outputBufferIndex, false);
                    }
                } while (outputBufferIndex >= 0);
            }
            stopCodec();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exit() {
        isExit = true;
    }

    /**
     * 遍历所有编解码器，返回第一个与指定MIME类型匹配的编码器
     * 判断是否有支持指定mime类型的编码器
     */
    private MediaCodecInfo selectSupportCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * 根据mime类型匹配编码器支持的颜色格式
     */
    private int selectSupportColorFormat(MediaCodecInfo mCodecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = mCodecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isCodecRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        return 0;
    }

    private boolean isCodecRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
//            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                return true;
            default:
                return false;
        }
    }

}
