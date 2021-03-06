package com.tianyi.helmet.server.entity.file;

/**
 * 音频信息
 */
public class Audio extends UploadEntity {
    private long seconds;
    private int state = VideoStateEnum.SUCCESS.getState();//@see VideoStateEnum
    private String src;//来源 upload,netease

    @Override
    public UploadEntityTypeEnum getType(){
        return UploadEntityTypeEnum.audio;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
    public long getSeconds() {
        return seconds;
    }

    public void setSeconds(long seconds) {
        this.seconds = seconds;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
