package com.example.administrator.streamingdemo.control.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;

import com.example.administrator.streamingdemo.R;
import com.example.administrator.streamingdemo.control.activity.StreamingCameraActivity;
import com.example.administrator.streamingdemo.control.activity.StreamingScreenActivity;
import com.example.administrator.streamingdemo.model.BasicInfo;
import com.example.administrator.streamingdemo.model.StreamSettingInfo;
import com.example.administrator.streamingdemo.utils.Constants;

/**
 * Created by linhtruong on 5/19/2016.
 */
public class MoreSettingFragment extends DialogFragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private AppCompatCheckBox mChkArchiving;
    private AppCompatCheckBox mChkMakeArchive;
    private AppCompatCheckBox mChkLiveChat;
    private AppCompatCheckBox mChkRestriction;

    private Button mBtnCancel;
    private Button mBtnOk;

    public static MoreSettingFragment newInstance() {
        return new MoreSettingFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.more_setting_fragment, container, false);
        initData(v);
        return v;
    }

    private void initData(View v) {
        mChkArchiving = (AppCompatCheckBox) v.findViewById(R.id.chkBoxArchiving);
        mChkMakeArchive = (AppCompatCheckBox) v.findViewById(R.id.chkBoxMakeArchive);
        mChkLiveChat = (AppCompatCheckBox) v.findViewById(R.id.chkBoxLiveChat);
        mChkRestriction = (AppCompatCheckBox) v.findViewById(R.id.chkBoxRestriction);

        mBtnCancel = (Button) v.findViewById(R.id.btnCancel);
        mBtnOk = (Button) v.findViewById(R.id.btnOk);

        mBtnOk.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        mChkArchiving.setOnCheckedChangeListener(this);
        mChkMakeArchive.setOnCheckedChangeListener(this);
        mChkLiveChat.setOnCheckedChangeListener(this);
        mChkRestriction.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        switch (id) {
            case R.id.chkBoxArchiving:

                break;

            case R.id.chkBoxMakeArchive:

                break;

            case R.id.chkBoxLiveChat:

                break;

            case R.id.chkBoxRestriction:

                break;
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btnCancel:
                if (isAdded()) {
                    dismiss();
                }
                break;

            case R.id.btnOk:
                BasicInfo basicInfo = BasicInfo.getInstance();
                StreamSettingInfo info = basicInfo.getStreamInfo();
                info.setArchiving(mChkArchiving.isChecked());
                info.setMakeArhieve(mChkMakeArchive.isChecked());
                info.setLiveChat(mChkLiveChat.isChecked());
                info.setRestriction(mChkRestriction.isChecked());

                if (info.getType() == Constants.STREAM_TYPE_CAMERA) {
                    Intent i = new Intent(getActivity(), StreamingCameraActivity.class);
                    startActivity(i);
                } else if (info.getType() == Constants.STREAM_TYPE_SCREEN) {
                    Intent i = new Intent(getActivity(), StreamingScreenActivity.class);
                    startActivity(i);
             /*       final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    startActivity(mainIntent);*/
                }
                break;
        }

    }
}
