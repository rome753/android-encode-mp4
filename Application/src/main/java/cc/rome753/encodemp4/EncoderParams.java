package cc.rome753.encodemp4;

/** 音、视频编码参数
 *
 * Created by chao on 2017/9/19.
 */

public class EncoderParams {
    private String videoPath;
    private int frameWidth;     // 图像宽度
    private int frameHeight;    // 图像高度
    private int bitRate;
    private int frameRate;
    private boolean isVertical;

    private String picPath;     // 图片抓拍路径
    private int audioBitrate;   // 音频编码比特率
    private int audioChannelCount; // 通道数据
    private int audioSampleRate;   // 采样率

    private int audioChannelConfig; // 单声道或立体声
    private int audioFormat;    // 采样精度
    private int audioSouce;     // 音频来源

    public EncoderParams(){}

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public int getAudioChannelConfig() {
        return audioChannelConfig;
    }

    public void setAudioChannelConfig(int audioChannelConfig) {
        this.audioChannelConfig = audioChannelConfig;
    }

    public boolean isVertical() {
        return isVertical;
    }

    public void setVertical(boolean vertical) {
        isVertical = vertical;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(int audioFormat) {
        this.audioFormat = audioFormat;
    }

    public int getAudioSouce() {
        return audioSouce;
    }

    public void setAudioSouce(int audioSouce) {
        this.audioSouce = audioSouce;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    public int getAudioChannelCount() {
        return audioChannelCount;
    }

    public void setAudioChannelCount(int audioChannelCount) {
        this.audioChannelCount = audioChannelCount;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }
}
