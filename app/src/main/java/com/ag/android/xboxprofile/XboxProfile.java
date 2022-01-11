package com.ag.android.xboxprofile;

public class XboxProfile {
    private String mGamerTag;
    private int mScore;
    private String mPicUrl;


    public XboxProfile(String gamerTag) {
        mGamerTag = gamerTag;
    }


    public String getGamerTag() {
        return mGamerTag;
    }

    public void setGamerTag(String gamerTag) {
        mGamerTag = gamerTag;
    }

    public int getScore() {
        return mScore;
    }

    public void setScore(int score) {
        mScore = score;
    }

    public String getPicUrl() {
        return mPicUrl;
    }

    public void setPicUrl(String picUrl) {
        mPicUrl = picUrl;
    }
}
