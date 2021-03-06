package com.tianyi.helmet.server.service.baidu;

import com.baidu.aip.speech.AipSpeech;
import com.tianyi.helmet.server.service.support.ConfigService;
import org.apache.commons.fileupload.util.Streams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 *  百度语音识别服务
 *
 * Created by liuhanc on 2018/4/3.
 */
@Service
public class BaiduSpeechService {
    @Autowired
    private ConfigService configService;
    private AipSpeech client;

    private Logger logger = LoggerFactory.getLogger(BaiduSpeechService.class);

    @PostConstruct
    public void init() {
        // 初始化一个AipSpeech
        logger.debug("初始化百度语音引擎.id=" + configService.getBaiduSpeechAppId() + ",key=" + configService.getBaiduSpeechAppKey() + ",secret=" + configService.getBaiduSpeechAppSecret());
        client = new AipSpeech(configService.getBaiduSpeechAppId(), configService.getBaiduSpeechAppKey(), configService.getBaiduSpeechAppSecret());
        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(20000);
        client.setSocketTimeoutInMillis(600000);
        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
//        client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
//        client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理
        // 可选：设置log4j日志输出格式，若不设置，则使用默认配置
        // 也可以直接通过jvm启动参数设置此环境变量
//        System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");
    }

    public String asrAudioToText(File audioFile, String format) {
        ByteArrayOutputStream byos = new ByteArrayOutputStream();
        try {
            Streams.copy(new FileInputStream(audioFile), byos, false);
        } catch (Exception e) {
            logger.error("读取声音文件异常." + audioFile.getAbsolutePath(), e);
            return null;
        }

        return asrAudioToText(byos.toByteArray(), format);
    }

    public String asrAudioToText(byte[] data, String format) {
        JSONObject asrRes = client.asr(data, format, 16000, null);
        logger.debug("调用语音识别接口反馈:" + asrRes.toString());
        int errNo = asrRes.getInt("err_no");//error_code
        if (errNo == 0) {
            //success
            JSONArray array = asrRes.getJSONArray("result");
            int size = array.length();
            for (int i = 0; i < size; i++) {
                String str = array.getString(i);
                logger.debug("百度识别语音结果:" + i + ":" + str);
            }

            if (size > 0)
                return array.getString(0);
            return null;
        } else {
            logger.error("语音识别失败.code=" + errNo + "" + asrRes.getString("err_msg"));
        }
        return null;
    }

}
