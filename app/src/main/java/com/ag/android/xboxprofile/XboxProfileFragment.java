package com.ag.android.xboxprofile;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class XboxProfileFragment extends Fragment
    implements XboxConnector.Listener {

    private static final String TAG = "XboxProfileFragment";

    private boolean mIsAuthenticating;
    private XboxConnector mConnector;
    private XboxProfile mProfile;
    private List<XboxAchievement> mAchievements = new ArrayList<>();
    private ThumbnailDownloader<AchievementHolder> mThumbnailDownloader;

    private TextView mGamerTagView;
    private TextView mScoreView;
    private ImageView mPhotoView;
    private RecyclerView mAchievementRecyclerView;
    private ConstraintLayout mProgress;


    public static XboxProfileFragment newInstance() {
        return new XboxProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String email = prefs.getString("email", null);
        String password = prefs.getString("password", null);

        mIsAuthenticating = true;
        mProfile = null;
        mConnector = new XboxConnector(this);
        mConnector.authenticate(email, password);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_xbox_profile, container, false);

        mGamerTagView = v.findViewById(R.id.gamer_tag);
        mScoreView = v.findViewById(R.id.score);
        mPhotoView = v.findViewById(R.id.profile_photo);
        mPhotoView.setClipToOutline(true);

        mProgress = v.findViewById(R.id.progress_layout);

        mAchievementRecyclerView = v.findViewById(R.id.achievement_recycler_view);
        mAchievementRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloaderListener(
                new ThumbnailDownloader.ThumbnailDownloaderListener<AchievementHolder>() {
                    @Override
                    public void onThumbnailDownloaded(AchievementHolder target, Bitmap thumbnail) {
                        Drawable drawable = new BitmapDrawable(thumbnail);
                        target.bindDrawable(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

        updateUI();

        return v;
    }

    @Override
    public void onAuthenticationOK() {
        Log.i(TAG, "Got authenticated");
        mIsAuthenticating = false;
        updateUI();

        mConnector.getProfile();
        mConnector.getAchievements();
    }

    @Override
    public void onAuthenticationKO() {
        Log.i(TAG, "Didnt get authenticated");
        mIsAuthenticating = false;
        Toast.makeText(getActivity(), "Authentication failed", Toast.LENGTH_LONG).show();

        Activity a = getActivity();
        if (a != null)
            a.finish();
    }

    @Override
    public void onProfileReceived(XboxProfile profile) {
        Log.i(TAG, "Got profile");
        mProfile = profile;

        updateUI();
        new PictureDownloader(picture -> mPhotoView.setImageBitmap(picture)).execute(mProfile.getPicUrl());
    }

    @Override
    public void onAchievementsReceived(List<XboxAchievement> achievements) {
        Log.i(TAG, "Got achievements");
        mAchievements = achievements;

        updateUI();
    }

    private void updateUI() {
        if (mIsAuthenticating) {
            mProgress.setVisibility(View.VISIBLE);
        } else {
            mProgress.setVisibility(View.GONE);

            if (mProfile != null) {
                mGamerTagView.setText(mProfile.getGamerTag());
                mScoreView.setText(getString(R.string.score_text, mProfile.getScore()));
            }

            if (mAchievements != null) {
                if (isAdded()) {
                    mAchievementRecyclerView.setAdapter(new AchievementAdapter(mAchievements));
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }


    private void setupAdapter() {
        if (isAdded()) {
            mAchievementRecyclerView.setAdapter(new AchievementAdapter(mAchievements));
        }
    }


    private class AchievementHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private XboxAchievement mAchievement;

        private TextView mDateView;
        private ImageView mIconView;
        private TextView mGameTitleView;
        //private TextView mScoreView;
        //private TextView mNameView;


        public AchievementHolder(@NonNull View itemView) {
            super(itemView);

            mDateView = itemView.findViewById(R.id.achievement_date);
            mIconView = itemView.findViewById(R.id.achievement_icon);
            mGameTitleView = itemView.findViewById(R.id.achievement_game_title);
            //mScoreView = itemView.findViewById(R.id.achievement_score);
            //mNameView = itemView.findViewById(R.id.achievement_name);

            itemView.setOnClickListener(this);
        }

        public void bindAchievement(XboxAchievement achievement) {
            mAchievement = achievement;

            final DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
            Date d = mAchievement.getDate();
            if (d != null)
                mDateView.setText(dateFormat.format(d));
            else
                mDateView.setText("--");

            mGameTitleView.setText(mAchievement.getGameTitle());
            mScoreView.setText(Integer.toString(mAchievement.getScoreReward()));
            //mNameView.setText(mAchievement.getName());
        }

        public void bindDrawable(Drawable drawable) {
            mIconView.setImageDrawable(drawable);
        }

        @Override
        public void onClick(View view) {
            Toast.makeText(getActivity(), mAchievement.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private class AchievementAdapter extends RecyclerView.Adapter<AchievementHolder> {
        private List<XboxAchievement> mAchievements;

        public AchievementAdapter(List<XboxAchievement> achievements) {
            mAchievements = achievements;
        }

        @NonNull
        @Override
        public AchievementHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_achievements, parent, false);

            return new AchievementHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AchievementHolder achievementHolder, int position) {
            XboxAchievement achievement = mAchievements.get(position);
            achievementHolder.bindAchievement(achievement);
            //Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            //photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(achievementHolder, achievement.getIconUrl());
        }

        @Override
        public int getItemCount() {
            return mAchievements.size();
        }
    }

}
