/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

//New file added by delong.liu@archermind.com

package com.android.phone;

import android.content.Context;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.util.Log;

import com.android.phone.PhoneGlobals;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
//Gionee fangbin 20121114 added for CR00728364 start
import com.android.phone.GnPhoneRecordHelper;
import com.android.phone.PhoneApp;
import android.widget.Toast;
import com.android.phone.R;
//Gionee fangbin 20121114 added for CR00728364 end
import gionee.os.storage.GnStorageManager;
import android.os.Build;

public class Recorder implements OnErrorListener {

    private static final String TAG = "Recorder";

    private static final String SAMPLE_PATH_KEY = "sample_path";
    private static final String SAMPLE_LENGTH_KEY = "sample_length";
    private static final String RECORDING_FILE_PREFIX = "Audio_";

    public static final int IDLE_STATE = 0;
    public static final int RECORDING_STATE = 1;

    int mState = IDLE_STATE;

    public static final int NO_ERROR = 0;
    public static final int SDCARD_ACCESS_ERROR = 1;
    public static final int INTERNAL_ERROR = 2;

    public interface OnStateChangedListener {
        /**
         * 
         * @param state 
         */
        void onStateChanged(int state);

        void onError(int error);
    }

    OnStateChangedListener mOnStateChangedListener;

    long mSampleStart; // time at which latest record or play operation
    // started
    long mSampleLength; // length of current sample
    File mSampleFile;
    //aurora add liguangyu 20140419 for BUG #4384 start
    File mLocalFile;
    //aurora add liguangyu 20140419 for BUG #4384 end
    long mPrivateId;
    
    MediaRecorder mRecorder;

    public Recorder() {
    }

    /**
     * 
     * @param recorderState 
     */
    public void saveState(Bundle recorderState) {
        recorderState.putString(SAMPLE_PATH_KEY, mSampleFile.getAbsolutePath());
        recorderState.putLong(SAMPLE_LENGTH_KEY, mSampleLength);
    }

    /**
     * 
     * @return int 
     */
    public int getMaxAmplitude() {
        if (mState != RECORDING_STATE) {
            return 0;
        }
        return mRecorder.getMaxAmplitude();
    }

    /**
     * 
     * @param recorderState 
     */
    public void restoreState(Bundle recorderState) {
        String samplePath = recorderState.getString(SAMPLE_PATH_KEY);
        if (samplePath == null) {
            return;
        }
        long sampleLength = recorderState.getLong(SAMPLE_LENGTH_KEY, -1);
        if (sampleLength == -1) {
            return;
        }
        File file = new File(samplePath);
        if (!file.exists()) {
            return;
        }
        if (mSampleFile != null
                && mSampleFile.getAbsolutePath().compareTo(file.getAbsolutePath()) == 0) {
            return;
        }
        delete();
        mSampleFile = file;
        mSampleLength = sampleLength;

        signalStateChanged(IDLE_STATE);
    }

    /**
     * 
     * @param listener 
     */
    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    /**
     * 
     * @return 
     */
    public int state() {
        return mState;
    }

    /**
     * 
     * @return 
     */
    public int progress() {
        if (mState == RECORDING_STATE) {
            return (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
        }
        return 0;
    }

    /**
     * 
     * @return 
     */
    public long sampleLength() {
        return mSampleLength;
    }

    /**
     * 
     * @return 
     */
    public File sampleFile() {
        return mSampleFile;
    }

    /**
     * Resets the recorder state. If a sample was recorded, the file is deleted.
     */
    public void delete() {
        stop();

        if (mSampleFile != null) {
            mSampleFile.delete();
        }
        mSampleFile = null;
        mSampleLength = 0l;

        signalStateChanged(IDLE_STATE);
    }

    /**
     * Resets the recorder state. If a sample was recorded, the file is left on
     * disk and will be reused for a new recording.
     */
    public void clear() {
        stop();

        mSampleLength = 0l;

        signalStateChanged(IDLE_STATE);
    }

    /**
     * 
     * @param outputfileformat 
     * @param extension 
     * @throws IOException 
     */
    public void startRecording(int outputfileformat, String extension) throws IOException {
        log("startRecording");
        stop();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_kk.mm.ss.SSS");
        String date = dateFormat.format(new Date());
        StorageManager storageManager = (StorageManager) PhoneGlobals.getInstance().getSystemService(
                Context.STORAGE_SERVICE);
        if (null == storageManager) {
            log("-----story manager is null----");
            return;
        }
        File sampleDir = new File(GnStorageManager.getDefaultPath());

        if (!sampleDir.canWrite()) {
            Log.i(TAG, "----- file can't write!! ---");
            // Workaround for broken sdcard support on the device.
            sampleDir = new File("/sdcard/sdcard");
        }

        sampleDir = new File(sampleDir.getAbsolutePath() + "/PhoneRecord");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }

        try {
            mSampleFile = new File(sampleDir.getAbsolutePath() + "/" + RECORDING_FILE_PREFIX + date + extension);
            if (!mSampleFile.createNewFile()) {
                log("can not create new empty file for recording because same name file already exists.");
            }
        } catch (IOException e) {
            setError(SDCARD_ACCESS_ERROR);
            log("----***------- can't access sdcard !!");
            throw e;
        }

        log("finish creating temp file, start to record");

        mRecorder = new MediaRecorder();
        mRecorder.setOnErrorListener(this);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(outputfileformat);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mSampleFile.getAbsolutePath());

        try {
            mRecorder.prepare();
            mRecorder.start();
            mSampleStart = System.currentTimeMillis();
            setState(RECORDING_STATE);
        } catch (IOException exception) {
            log("startRecording, IOException");
            setError(INTERNAL_ERROR);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            throw exception;
        }
    }

    public void stopRecording() {
        log("stopRecording");
        if (mRecorder == null) {
            return;
        }
        mSampleLength = System.currentTimeMillis() - mSampleStart;

        
        try {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder = null;
        }
        // Gionee fangbin 20121114 added for CR00728364 start
//        if (PhoneApp.ISGNPHONE) {
            GnPhoneRecordHelper.gnRecordPrefixPath = null;
//        }
        // Gionee fangbin 20121114 added for CR00728364 end
        //aurora add liguangyu 20140419 for BUG #4384 start
        if(mLocalFile != null && mLocalFile.exists()) {
        	if(mSampleFile != null) {
        		File sampleDir = null;
        		if(mPrivateId > 0) {
        			sampleDir = new File(GnPhoneRecordHelper.getPrivateRecordPath(PhoneGlobals.getInstance(), mPrivateId)); 
        		  	  if(!sampleDir.exists()){
        			      sampleDir.mkdirs();
        		  	  }
        		} else {
        			sampleDir = new File(GnPhoneRecordHelper.getRecordPath(PhoneGlobals.getInstance())); 
        		}
        		File result = new File(sampleDir, GnPhoneRecordHelper.changeAuroraRecordFileName(mSampleFile.getName(), mAddedDate + "_" + mSampleLength));
//        		GnPhoneRecordHelper.copyFileContent(mLocalFile, mSampleFile);
        		GnPhoneRecordHelper.copyFileContent(mLocalFile, result);
        	}
        	mLocalFile.delete();
        	mLocalFile = null;
        }
        //aurora add liguangyu 20140419 for BUG #4384 end
        mPrivateId = 0;

        setState(IDLE_STATE);
    }

    public void stop() {
        log("stop");
        stopRecording();
    }

    /**
     * @param state 
     */
    private void setState(int state) {
        if (state == mState) {
            return;
        }
        mState = state;
        signalStateChanged(mState);
    }

    /**
     * 
     * @param state 
     */
    private void signalStateChanged(int state) {
        if (mOnStateChangedListener != null) {
            mOnStateChangedListener.onStateChanged(state);
        }
    }

    /**
     * 
     * @param error 
     */
    private void setError(int error) {
        if (mOnStateChangedListener != null) {
            mOnStateChangedListener.onError(error);
        }
    }

    /**
     * error listener 
     */
    public void onError(MediaRecorder mp, int what, int extra) {
        log("onError");
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            stop();
            // TODO show hint view
        }
        return;
    }

    public void log(String msg) {
        Log.d(TAG, msg);
    }
    
    // Gionee fangbin 20121114 added for CR00728364 start
    public void deleteRecordErrorFile(){
        log("delete error record file !");
        File templeFile = this.sampleFile();
        if(!templeFile.exists()){
            return;
        }
        templeFile.delete();
        templeFile = null;
    }
    
    public void startRecording(Context context, int outputfileformat, String extension, String contacts, String number) throws IOException {
        log("startRecording");
        stop();

        String prefix = "";
        File sampleDir = null;
        try {
            // Gionee fangbin 20121231 added for CR00756041 start
            GnPhoneRecordHelper.reSetRecorderPrefixPath();
            // Gionee fangbin 20121231 added for CR00756041 end
            GnPhoneRecordHelper.gnRecordPrefixPath = GnPhoneRecordHelper.getSDCardPath(context);
            if (null == GnPhoneRecordHelper.gnRecordPrefixPath) {
                Toast.makeText(context, context.getString(R.string.aurora_no_storage), Toast.LENGTH_LONG).show();
                return;
            } else if (null != GnPhoneRecordHelper.getLastSDCardPath(context) && 
                    !GnPhoneRecordHelper.getLastSDCardPath(context).equals(GnPhoneRecordHelper.gnRecordPrefixPath)) {
                if (GnPhoneRecordHelper.gnRecordPrefixPath.equals(GnPhoneRecordHelper.getSecondPath(context)) && GnPhoneRecordHelper.hasExternalSdcardStorage(context)) {
                    Toast.makeText(context, context.getString(R.string.gn_internal_memory), Toast.LENGTH_LONG).show();
                } else if (GnPhoneRecordHelper.gnRecordPrefixPath.equals(GnPhoneRecordHelper.SDCARD_DEFAULT_PATH)) {
                    Toast.makeText(context, context.getString(R.string.gn_save_sdcard), Toast.LENGTH_LONG).show();
                }
            }
            GnPhoneRecordHelper.setLastSDCardPath(context, GnPhoneRecordHelper.gnRecordPrefixPath);
//            prefix = GnPhoneRecordHelper.getRecordFileName(context, contacts, number);
            prefix = GnPhoneRecordHelper.getAuroraRecordFileName(context, number);            
            sampleDir = new File(GnPhoneRecordHelper.getRecordPath(context));
            mSampleFile = new File(sampleDir, prefix + extension);
            
            //aurora add liguangyu 20140419 for BUG #4384 start
            File file = PhoneGlobals.getInstance().getDir ("localrecord", Context.MODE_PRIVATE);
            if (file.exists() == false) {
                file.mkdirs();
            }
            String baseName = file.getPath() + File.separator + "record";
            if(Build.VERSION.SDK_INT >= 21) {
               	mLocalFile = new File(sampleDir, "recordtemp");
            } else {
            	mLocalFile = new File(baseName);     
            }
            mLocalFile.createNewFile(); 
            mPrivateId = GnPhoneRecordHelper.getPrivateId();
            //aurora add liguangyu 20140419 for BUG #4384 end
        } catch (Exception e) {
            setError(SDCARD_ACCESS_ERROR);
            Log.i(TAG, "----***------- can't access sdcard !!");
            e.printStackTrace();
        }
        
        log("finish creating temp file, start to record");

        mRecorder = new MediaRecorder();
        mRecorder.setOnErrorListener(this);
        
        if(Build.VERSION.SDK_INT >= 21) {
        	 mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
             mRecorder.setOutputFormat(outputfileformat);
             mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
             mRecorder.setAudioChannels(1);
             mRecorder.setAudioEncodingBitRate(24000);
             mRecorder.setAudioSamplingRate(16000);
        } else {
        	 mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            mRecorder.setAudioSource(PhoneUtils.isMtk() || AuroraPhoneUtils.isNexus5()? MediaRecorder.AudioSource.MIC : MediaRecorder.AudioSource.VOICE_CALL);
            mRecorder.setOutputFormat(outputfileformat);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }
        

        //aurora modify liguangyu 20140419 for BUG #4384 start
//        mRecorder.setOutputFile(mSampleFile.getAbsolutePath());
        mRecorder.setOutputFile(mLocalFile.getAbsolutePath());
        //aurora modify liguangyu 20140419 for BUG #4384 end

        try {
            mRecorder.prepare();
            mRecorder.start();
            mSampleStart = System.currentTimeMillis();
            setState(RECORDING_STATE);
        } catch (IOException exception) {
            log("startRecording, IOException");
            setError(INTERNAL_ERROR);
            deleteRecordErrorFile();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            throw exception;
        } catch (RuntimeException exception) {
            log("startRecording, RuntimeException");
            setError(INTERNAL_ERROR);
            deleteRecordErrorFile();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            throw exception;
        }
    }
    
    public String getName() {
        if (null != mSampleFile) {
            String name = mSampleFile.getName();
            name = name.substring(0, name.lastIndexOf("."));
            return name;
        } else {
            return null;
        }
    }
    
    public long getSize() {
        if (null != mSampleFile) {
            long size = mSampleFile.length();
            return size;
        } else {
            return 0;
        }
    }
    // Gionee fangbin 20121114 added for CR00728364 end
    
    protected long mAddedDate;
}
