package cc.rome753.encodemp4;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Mp4封装混合器
 * <p>
 * Created by chao on 2017/7/28.
 */

public class MediaMuxerUtil {
    private static final String TAG = MediaMuxerUtil.class.getSimpleName();
    private MediaMuxer mMuxer;
    private final long durationMillis;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private long mBeginMillis;

    // 文件路径；文件时长
    public MediaMuxerUtil(String path, long durationMillis) {
        this.durationMillis = durationMillis;
        try {
            mMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addTrack(MediaFormat format, boolean isVideo) {
        if(mMuxer == null) {
            return;
        }
        if (mAudioTrackIndex != -1 && mVideoTrackIndex != -1) {
            return;
        }

        int track = mMuxer.addTrack(format);
        Log.i(TAG, String.format("addTrack %s result %d", isVideo ? "video" : "audio", track));
        if (isVideo) {
            mVideoTrackIndex = track;
            if (mAudioTrackIndex != -1) {
                Log.i(TAG, "both audio and video added,and muxer is started");
                mMuxer.start();
                mBeginMillis = System.currentTimeMillis();
            }
        } else {
            mAudioTrackIndex = track;
            if (mVideoTrackIndex != -1) {
                mMuxer.start();
                mBeginMillis = System.currentTimeMillis();
            }
        }
    }

    static class MuxData {
        public ByteBuffer outputBuffer;
        public MediaCodec.BufferInfo bufferInfo;
        public boolean isVideo;

        public MuxData(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
            this.outputBuffer = outputBuffer;
            this.bufferInfo = bufferInfo;
            this.isVideo = isVideo;
        }
    }

    // 缓存未启动前的数据
    private Queue<MuxData> bufQueue = new LinkedList<>();

    public synchronized void pumpStream(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
        if (mBeginMillis == 0) {
            bufQueue.add(new MuxData(outputBuffer, bufferInfo, isVideo));
        } else {
            while (!bufQueue.isEmpty()) {
                MuxData data = bufQueue.remove();
                pump(data.outputBuffer, data.bufferInfo, data.isVideo);
            }
            try {
                pump(outputBuffer, bufferInfo, isVideo);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void pump(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
        if(mMuxer == null) {
            return;
        }
        if (mAudioTrackIndex == -1 || mVideoTrackIndex == -1) {
            return;
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
        } else if (bufferInfo.size != 0) {

            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

            mMuxer.writeSampleData(isVideo ? mVideoTrackIndex : mAudioTrackIndex, outputBuffer, bufferInfo);
            Log.d(TAG, String.format("sent %s [" + bufferInfo.size + "] with timestamp:[%d] to muxer", isVideo ? "video" : "audio", bufferInfo.presentationTimeUs / 1000));
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.i(TAG, "BUFFER_FLAG_END_OF_STREAM received");
        }

        if (System.currentTimeMillis() - mBeginMillis >= durationMillis) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
            mVideoTrackIndex = mAudioTrackIndex = -1;
        }
    }

    public synchronized void release() {
        if (mMuxer != null) {
            if (mAudioTrackIndex != -1 && mVideoTrackIndex != -1) {
                Log.i(TAG, String.format("muxer is started. now it will be stoped."));
                try {
                    mMuxer.stop();
                    mMuxer.release();
                } catch (IllegalStateException ex) {
                    ex.printStackTrace();
                }

                mAudioTrackIndex = mVideoTrackIndex = -1;
            } else {
                Log.i(TAG, String.format("muxer is failed to be stoped."));
            }
        }
    }
}
