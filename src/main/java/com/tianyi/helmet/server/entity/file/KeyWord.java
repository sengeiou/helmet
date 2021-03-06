package com.tianyi.helmet.server.entity.file;

import com.tianyi.helmet.server.entity.IdEntity;

/**
 * 关键词
 * Created by liuhanc on 2017/12/28.
 */
public class KeyWord extends IdEntity {
    private String keywordName;//标签名称

    public KeyWord() {
    }

    public KeyWord(int id, String keywordName) {
        this.setId(id);
        this.setKeywordName(keywordName);
    }


    public String getKeywordName() {
        return keywordName;
    }

    public void setKeywordName(String keywordName) {
        this.keywordName = keywordName;
    }

}
