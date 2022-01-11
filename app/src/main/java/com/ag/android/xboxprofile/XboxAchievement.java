package com.ag.android.xboxprofile;

import java.util.Date;

public class XboxAchievement {
    private String mName;
    private String mGameTitle;
    private String mProgressState;
    private Date mDate;
    private String mIconUrl;
    private String mDescription;
    private int mScoreReward;


    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getGameTitle() {
        return mGameTitle;
    }
    public void setGameTitle(String gameTitle) {
        mGameTitle = gameTitle;
    }


    public String getProgressState() {
        return mProgressState;
    }

    public void setProgressState(String progressState) {
        mProgressState = progressState;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public String getIconUrl() {
        return mIconUrl;
    }

    public void setIconUrl(String iconUrl) {
        mIconUrl = iconUrl;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public int getScoreReward() {
        return mScoreReward;
    }

    public void setScoreReward(int scoreReward) {
        mScoreReward = scoreReward;
    }


}
