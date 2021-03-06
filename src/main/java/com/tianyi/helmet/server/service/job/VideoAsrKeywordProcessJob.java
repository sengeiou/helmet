package com.tianyi.helmet.server.service.job;


import com.tianyi.helmet.server.entity.file.Video;
import com.tianyi.helmet.server.entity.file.VideoDataExtend;
import com.tianyi.helmet.server.entity.file.VideoKeyword;
import com.tianyi.helmet.server.entity.file.VideoStateEnum;
import com.tianyi.helmet.server.service.baidu.BaiduSpeechService;
import com.tianyi.helmet.server.service.fastdfs.FastDfsClient;
import com.tianyi.helmet.server.service.file.KeyWordService;
import com.tianyi.helmet.server.service.file.VideoDataExtendService;
import com.tianyi.helmet.server.service.file.VideoKeywordService;
import com.tianyi.helmet.server.service.file.VideoService;
import com.tianyi.helmet.server.service.support.ConfigService;
import com.tianyi.helmet.server.service.support.FfmpegService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;

/**
 *  视频扩展操作job
 *
 * 1 提取音频文件
 * 2 音频文件识别成文字
 */
@Component //在applicationContext.xml中配置了此类接收某个pattern的订阅
public class VideoAsrKeywordProcessJob implements MessageListener {
    private Logger logger = LoggerFactory.getLogger(VideoAsrKeywordProcessJob.class);

    @Autowired
    private VideoService videoService;
    @Autowired
    private VideoKeywordService videoKeywordService;
    @Autowired
    private FfmpegService ffmpegService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private VideoDataExtendService videoDataExtendService;
    @Autowired
    private KeyWordService keyWordService;
    @Autowired
    private FastDfsClient fastDfsClient;
    @Autowired
    private BaiduSpeechService baiduSpeechService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String patternStr = new String(pattern);
            String body = new String(message.getBody());
            logger.debug("VideoAsrKeywordProcessJob 收到消息:patternStr=" + patternStr + ",channel=" + new String(message.getChannel()) + ",body=" + body);
            Integer videoId = Integer.parseInt(body);
            Video v = videoService.selectById(videoId);
            processOneData(v);
        } catch (Exception e) {
            logger.error("处理视频语音识别队列消息异常.", e);
        }
    }

    /**
     * 处理某个视频
     *
     * @param v
     */
    public void processOneData(Video v) {
        int oldState = v.getState();
        boolean prepareOk = doPrepare(v);
        if (!prepareOk)
            return;

        //服务数据视频.则提取视频中声音，识别并拆分出关键词.20180709 改为所有视频都自动识别其中的声音
        try {
            doProcessVideoAsr(v);
        } catch (Exception e) {
            logger.error("服务数据视频声音提取出文字发生异常.id=" + v.getId(), e);
        } finally {
            videoService.updateState(v.getId(), VideoStateEnum.get(oldState));
        }
    }

    /**
     * 提取视频声音，再解析声音文件得到文字
     *
     * @param v
     */
    public boolean doProcessVideoAsr(Video v) {
        VideoDataExtend extend = videoDataExtendService.selectByVideoId(v.getId());
        boolean insertExtData = false;
        if (extend == null) {
            insertExtData = true;
            extend = new VideoDataExtend();
            extend.setVideoId(v.getId());
        } else if (!StringUtils.isEmpty(extend.getAudioOrigText())) {
            logger.debug("视频声音对应文字已识别过,不再识别.v.id=" + v.getId());
            return true;
        }

        int allSeconds = (int) v.getSeconds();
        if (allSeconds <= 0) {
            logger.debug("视频时长为0,不再识别声音.v.id=" + v.getId());
            return false;
        }

        //得到声音文件
        String audioOssPath = extend.getAudioOssPath();
        String mp3AudioFilePath = null;
        File videoDir = new File(configService.getUploadFileSaveDir(), configService.getUploadVideoDir());
        if (StringUtils.isEmpty(audioOssPath)) {
            File tempVideoFile = videoService.getVideoFile(v);
            if (tempVideoFile == null) {
                logger.error("提取视频声音时，临时视频拉取失败.v.id=" + v.getId());
                return false;
            }

            String videoFilePath = tempVideoFile.getAbsolutePath();
            mp3AudioFilePath = videoFilePath + ".mp3";
            File audioFile = new File(mp3AudioFilePath);
            if (!audioFile.exists() || audioFile.length() == 0) {
                boolean exactSuccess = ffmpegService.extractMp3FromVideo(videoFilePath, mp3AudioFilePath);
                if (!exactSuccess) {
                    logger.error("提取视频声音时，提取失败.v.id=" + v.getId());
                    return false;
                }
            }

            audioOssPath = fastDfsClient.uploadFile(audioFile, "mp3");
            extend.setAudioOssPath(audioOssPath);
        } else {
            mp3AudioFilePath = videoDir + File.separator + "video-audio-" + v.getId() + ".mp3";
            try {
                fastDfsClient.downloadFile(audioOssPath, new FileOutputStream(mp3AudioFilePath));
            } catch (Exception e) {
                logger.error("下载声音文件到本地异常.v.id=" + v.getId() + ",ext.id=" + extend.getId(), e);
                return false;
            }
        }

        //切分声音
        String[] splitedAudioPaths = ffmpegService.audioSplitToMp3(mp3AudioFilePath, videoDir.getAbsolutePath(), 55, allSeconds);
        logger.info("视频时长:" + allSeconds + ",将视频的声音进行切分,得到" + splitedAudioPaths.length + "份");
        if (splitedAudioPaths.length == 0) {
            logger.error("声音文件切分异常.v.id=" + v.getId() + ",ext.id=" + extend.getId());
            return false;
        }

        //将每个切分的声音从mp3格式转换成wav格式，再调用语音解析服务解析声音
        StringBuffer audioTextBuffer = new StringBuffer();
        String toFmtExt = "pcm";
        for (String splitMp3Path : splitedAudioPaths) {
            String splitWavPath = splitMp3Path + "." + toFmtExt;
            boolean success;
            try {
                success = ffmpegService.audioTranscode(splitMp3Path, splitWavPath, 1, "pcm_s16le", "s16le");
            } catch (Exception e) {
                success = false;
                logger.error("mp3转成其他格式" + toFmtExt + "异常." + splitMp3Path, e);
            }

            File splitWavFile = new File(splitWavPath);
            if (success) {
                String text = baiduSpeechService.asrAudioToText(splitWavFile, toFmtExt);
                logger.debug("声音识别得到文字:" + text);
                audioTextBuffer.append(text == null ? "" : text).append("\r\n");
            }

            new File(splitMp3Path).delete();
            splitWavFile.delete();
        }

        new File(mp3AudioFilePath).delete();//删除本地mp3声音文件

        String audioText = audioTextBuffer.toString();
        if (StringUtils.isEmpty(audioText)) {
            audioText = "声音识别失败";
        } else {
            //识别出来了，则进行关键词检测
            final String checkAudioText = audioText;
            keyWordService.selectKeyWordList().stream().filter(keyWord -> {
                return checkAudioText.contains(keyWord.getKeywordName());
            }).forEach(kw -> {
                try {
                    videoKeywordService.insert(new VideoKeyword(v.getId(), kw.getId()));
                } catch (Exception e) {
                    logger.error("记录自动识别出的视频关键词异常.v.id=" + v.getId() + ",kw.id=" + kw.getId() + ",keyword=" + kw.getKeywordName(), e);
                }
            });
        }

        extend.setAudioOrigText(audioText);
        extend.setAudioEditText("");//默认空
        if (insertExtData)
            videoDataExtendService.insert(extend);
        else
            videoDataExtendService.updateOrigTextByVideoId(extend.getAudioOrigText(), extend.getVideoId());

        return true;
    }

    /**
     * 工作准备
     * 锁定视频处理中状态
     *
     * @param v
     * @return
     */
    private boolean doPrepare(Video v) {
        //核查数据
        int oldState = v.getState();
        if (oldState == VideoStateEnum.PROCING.getState()) {
            logger.error("视频的状态是正在处理,所以不予处理.id=" + v.getId());
            return false;
        }
        if (StringUtils.isEmpty(v.getOssPath())) {
            logger.error("视频的oss存储路径为空无法处理.id=" + v.getId());
            return false;
        }

        //设置状态
        int result = videoService.updateState(v.getId(), VideoStateEnum.PROCING);
        if (result != 1) {
            logger.debug("设置视频处理中状态失败...id=" + v.getId());
            return false;
        } else {
            logger.debug("设置视频处理中状态成功...id=" + v.getId());
            return true;
        }
    }
}
