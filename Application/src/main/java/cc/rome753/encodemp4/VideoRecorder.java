package cc.rome753.encodemp4;

import java.nio.ByteBuffer;

/**
 * Created by chao on 19-4-10.
 */

public class VideoRecorder {

    private AACEncodeConsumer mAacConsumer;
    private H264EncodeConsumer mH264Consumer;
    private MediaMuxerUtil mMuxer;

    private String filePath;


    public VideoRecorder(String filePath) {
        this.filePath = filePath;
    }


    private EncoderParams setEncodeParams() {
        EncoderParams params = new EncoderParams();
        params.setVideoPath(filePath);    // 视频文件路径
        params.setFrameWidth(640);             // 分辨率
        params.setFrameHeight(480);
        params.setBitRate(600000);   // 视频编码码率
        params.setFrameRate(30);// 视频编码帧率
        params.setAudioBitrate(44100);        // 音频比特率
        params.setAudioSampleRate(AACEncodeConsumer.DEFAULT_SAMPLE_RATE);  // 音频采样率
        params.setAudioChannelConfig(AACEncodeConsumer.CHANNEL_IN_MONO);// 单声道
        params.setAudioChannelCount(AACEncodeConsumer.CHANNEL_COUNT_MONO);       // 单声道通道数量
        params.setAudioFormat(AACEncodeConsumer.ENCODING_PCM_16BIT);       // 采样精度为16位
        return params;
    }


    public void addAudioData(byte[] buffer) {
        if (mAacConsumer != null) {
            try {
                mAacConsumer.addData(ByteBuffer.wrap(buffer), buffer.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addVideoData(byte[] frame) {
        if(mH264Consumer != null) {
            try {
                mH264Consumer.addData(frame);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        EncoderParams params = setEncodeParams();
        // 创建音视频编码线程
        mH264Consumer = new H264EncodeConsumer();
        mAacConsumer = new AACEncodeConsumer();
        mMuxer = new MediaMuxerUtil(params.getVideoPath(), 1000000);
        if (mH264Consumer != null) {
            mH264Consumer.setTmpuMuxer(mMuxer,params);
        }
        if (mAacConsumer != null) {
            mAacConsumer.setTmpuMuxer(mMuxer,params);
        }
        // 配置好混合器后启动线程
        mH264Consumer.start();
        mAacConsumer.start();

    }

    public void stop() {
        // 停止混合器
        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }
        if (mH264Consumer != null) {
            mH264Consumer.setTmpuMuxer(null,null);
        }
        if (mAacConsumer != null) {
            mAacConsumer.setTmpuMuxer(null,null);
        }
        // 停止视频编码线程
        if (mH264Consumer != null) {
            mH264Consumer.exit();
            try {
                Thread t2 = mH264Consumer;
                mH264Consumer = null;
                if (t2 != null) {
                    t2.interrupt();
                    t2.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 停止音频编码线程
        if (mAacConsumer != null) {
            mAacConsumer.exit();
            try {
                Thread t1 = mAacConsumer;
                mAacConsumer = null;
                if (t1 != null) {
                    t1.interrupt();
                    t1.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
