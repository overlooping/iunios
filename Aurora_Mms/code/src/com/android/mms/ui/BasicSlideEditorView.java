/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.ui;

import com.android.mms.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import aurora.widget.AuroraEditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.Map;
//Gionee <zhouyj> <2013-05-10> add for CR00809441 begin
import android.graphics.Color;
//Gionee <zhouyj> <2013-05-10> add for CR00809441 end

/**
 * This is a basic view to show and edit a slide.
 */
public class BasicSlideEditorView extends LinearLayout implements
        SlideViewInterface {
    private static final String TAG = "BasicSlideEditorView";

    private ImageView mImageView;
    private View mAudioView;
    private TextView mAudioNameView;
    private AuroraEditText mEditText;
    private boolean mOnTextChangedListenerEnabled = true;
    private OnTextChangedListener mOnTextChangedListener;

    public BasicSlideEditorView(Context context) {
        super(context);
    }

    public BasicSlideEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        mImageView = (ImageView) findViewById(R.id.image);
        mAudioView = findViewById(R.id.audio);
        mAudioNameView = (TextView) findViewById(R.id.audio_name);
        //Gionee <zhouyj> <2013-05-10> add for CR00809441 begin
        mAudioNameView.setTextColor(Color.BLACK);
        //Gionee <zhouyj> <2013-05-10> add for CR00809441 end
        mEditText = (AuroraEditText) findViewById(R.id.text_message);
        mEditText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub
            }

            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                if (mOnTextChangedListenerEnabled && (mOnTextChangedListener != null)) {
                    mOnTextChangedListener.onTextChanged(s.toString());
                }
            }

            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });
    }

    public void startAudio() {
        // TODO Auto-generated method stub
    }

    public void startVideo() {
        // TODO Auto-generated method stub
    }

    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        mAudioView.setVisibility(View.VISIBLE);
        mAudioNameView.setText(name);
    }

    public void setImage(String name, Bitmap bitmap) {
        try {
            if (null == bitmap) {
                bitmap = BitmapFactory.decodeResource(getResources(),
                        -1);
            }
            mImageView.setImageBitmap(bitmap);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setImage: out of memory: ", e);
        }
    }

    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    public void setImageVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setText(String name, String text) {
        mOnTextChangedListenerEnabled = false;
        if ((text != null) && !text.equals(mEditText.getText().toString())) {
            mEditText.setText(text);
            mEditText.setSelection(text.length());
        }
        mOnTextChangedListenerEnabled = true;
    }

    public void setTextVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setVideo(String name, Uri video) {
        try {
            Bitmap bitmap = VideoAttachmentView.createVideoThumbnail(mContext, video);
            if (null == bitmap) {
                bitmap = BitmapFactory.decodeResource(getResources(),
                        -1);
            }
            mImageView.setImageBitmap(bitmap);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setVideo: out of memory: ", e);
        }
    }

    public void setVideoVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void stopAudio() {
        // TODO Auto-generated method stub
    }

    public void stopVideo() {
        // TODO Auto-generated method stub
    }

    public void reset() {
        mImageView.setImageDrawable(null);
        mAudioView.setVisibility(View.GONE);
        mOnTextChangedListenerEnabled = false;
        mEditText.setText("");
        mOnTextChangedListenerEnabled = true;
    }

    public void setVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setOnTextChangedListener(OnTextChangedListener l) {
        mOnTextChangedListener = l;
    }

    public interface OnTextChangedListener {
        void onTextChanged(String s);
    }

    public void pauseAudio() {
        // TODO Auto-generated method stub

    }

    public void pauseVideo() {
        // TODO Auto-generated method stub

    }

    public void seekAudio(int seekTo) {
        // TODO Auto-generated method stub

    }

    public void seekVideo(int seekTo) {
        // TODO Auto-generated method stub

    }
   // Aurora xuyong 2014-04-25 added for bug #4301 start
    @Override
    public void setImage(String name, Uri uri) {
        // TODO Auto-generated method stub
        
    }
   // Aurora xuyong 2014-04-25 added for bug #4301 end
}