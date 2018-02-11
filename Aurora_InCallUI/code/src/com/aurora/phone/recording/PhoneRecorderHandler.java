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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.incallui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.widget.Toast;
import com.android.phone.IPhoneRecordStateListener;
import com.android.phone.IPhoneRecorder;


public class PhoneRecorderHandler {

    private static final String LOG_TAG = "PhoneRecorderHandler";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private Intent mRecorderServiceIntent = new Intent(InCallApp.getInstance(),
            PhoneRecorderServices.class);
    private IPhoneRecorder mPhoneRecorder;
    private int mPhoneRecorderState = PhoneRecorder.IDLE_STATE;
    private int mCustomValue;
    private int mRecordType;
    private Listener mListener;
    private String mRecordStoragePath;

    //private boolean mIsStorageMounted = true;

    public interface Listener {
        /**
         * 
         * @param state 
         * @param customValue 
         */
        void requestUpdateRecordState(final int state, final int customValue);

        void onStorageFull();
    }

    private PhoneRecorderHandler() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addDataScheme("file");
        InCallApp.getInstance().registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private static PhoneRecorderHandler sInstance = new PhoneRecorderHandler();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };

    private Runnable mRecordDiskCheck = new Runnable() {
        public void run() {
            checkRecordDisk();
        }
    };

    public static synchronized PhoneRecorderHandler getInstance() {
        return sInstance;
    }

    /**
     * 
     * @param listener 
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * 
     * @param listener 
     */
    public void clearListener(Listener listener) {
        if (listener == mListener) {
            mListener = null;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mPhoneRecorder = IPhoneRecorder.Stub.asInterface(service);
            try {
                log("onServiceConnected");
                if (null != mPhoneRecorder) {
                    mPhoneRecorder.listen(mPhoneRecordStateListener);
                    mPhoneRecorder.startRecord();
                    // Gionee fangbin 20121114 added for CR00728364 start
//                    if (PhoneApp.ISGNPHONE) {
                        mRecordStartTime = System.currentTimeMillis();
                        if (null != mRecordTimeCallBack) {
                            mRecordTimeCallBack.callBack(mRecordTime, true);
                        }
//                    }
                    // Gionee fangbin 20121114 added for CR00728364 end
                    mHandler.postDelayed(mRecordDiskCheck, 500);
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "onServiceConnected: couldn't register to record service",
                        new IllegalStateException());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mPhoneRecorder = null;
        }
    };

    private IPhoneRecordStateListener mPhoneRecordStateListener = new IPhoneRecordStateListener.Stub() {
        /**
         * 
         * @param state 
         */
        public void onStateChange(int state) {
            log("onStateChange, state is " + state);
            mPhoneRecorderState = state;
            if (null != mListener) {
                mListener.requestUpdateRecordState(state, mCustomValue);
            }
        }

        /**
         * 
         * @param iError 
         */
        public void onError(int iError) {
            String message = null;
            switch (iError) {
                case Recorder.SDCARD_ACCESS_ERROR:
                    message = InCallApp.getInstance().getResources().getString(
                            R.string.error_sdcard_access);
                    break;
                case Recorder.INTERNAL_ERROR:
                    message = InCallApp.getInstance().getResources().getString(
                            R.string.alert_device_error);
                    break;
                default:
                    break;
            }
            if (null != mPhoneRecorder) {
                Toast.makeText(InCallApp.getInstance(), message, Toast.LENGTH_LONG).show();
            }
            // Gionee fangbin 20121114 added for CR00728364 start
//            if (PhoneApp.ISGNPHONE) {
                stopVoiceRecord();
                if (null != mRecordTimeCallBack) {
                    mRecordTimeCallBack.onError();
                }
//            }
            // Gionee fangbin 20121114 added for CR00728364 end
        }
    };

    /**
     * 
     * @param customValue
     */
    public void startVoiceRecord(final int customValue) {
        mCustomValue = customValue;
        mRecordType = AuroraConstants.PHONE_RECORDING_TYPE_ONLY_VOICE;
        mRecordStoragePath = GnPhoneRecordHelper.getExternalStorageDefaultPath();
        if (null != mRecorderServiceIntent && null == mPhoneRecorder) {
            InCallApp.getInstance().bindService(mRecorderServiceIntent, mConnection,
                    Context.BIND_AUTO_CREATE);
        } else if (null != mRecorderServiceIntent && null != mPhoneRecorder) {
            try {
                mPhoneRecorder.startRecord();
                // Gionee fangbin 20121114 added for CR00728364 start
//                if (PhoneApp.ISGNPHONE) {
                    mRecordStartTime = System.currentTimeMillis();
                    if (null != mRecordTimeCallBack) {
                        mRecordTimeCallBack.callBack(mRecordTime, true);
                    }
//                }
                // Gionee fangbin 20121114 added for CR00728364 end
                mHandler.postDelayed(mRecordDiskCheck, 500);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "start Record failed", new IllegalStateException());
            }
        }
    }

    public void stopVoiceRecord() {
        stopVoiceRecord(true);
    }

    private void stopVoiceRecord(boolean isMount) {
        try {
            log("stopRecord");
            if (null != mPhoneRecorder) {
                mPhoneRecorder.stopRecord(isMount);
                // Gionee fangbin 20121114 added for CR00728364 start
//                if (PhoneApp.ISGNPHONE) {
                    // Gionee fangbin 20121208 added for CR00739122 start
                    mHandler.removeCallbacks(mRecordDiskCheck);
                    // Gionee fangbin 20121208 added for CR00739122 end
                    mRecordStartTime = 0l;
                    mRecordTime = 0l;
                    if (null != mRecordTimeCallBack) {
                        mRecordTimeCallBack.callBack(mRecordTime, false);
                    }
//                }
                // Gionee fangbin 20121114 added for CR00728364 end
                mPhoneRecorder.remove();
                if (null != mConnection) {
                    InCallApp.getInstance().unbindService(mConnection);
                }
                mPhoneRecorder = null;
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "stopRecord: couldn't call to record service",
                    new IllegalStateException());
        }
    }

    /**
     * 
     * @param type 
     * @param sdMaxSize 
     * @param customValue 
     */
    public void startVideoRecord(final int type, final long sdMaxSize, final int customValue) {
//        mRecordType = type;
//        mCustomValue = customValue;
//        mRecordStoragePath = PhoneUtils.getExternalStorageDefaultPath();
//        log("- start call VTManager.startRecording() : type = " + type + " sd max size = "
//                + sdMaxSize);
//        VTManager.getInstance().startRecording(type, sdMaxSize);
//        // Gionee fangbin 20121114 added for CR00728364 start
//        if (PhoneApp.ISGNPHONE) {
//            mRecordStartTime = System.currentTimeMillis();
//            if (null != mRecordTimeCallBack) {
//                mRecordTimeCallBack.callBack(mRecordTime, true);
//            }
//        }
//        // Gionee fangbin 20121114 added for CR00728364 end
//        log("- end call VTManager.startRecording()");
//        mPhoneRecorderState = PhoneRecorder.RECORDING_STATE;
//        if (null != mListener) {
//            mListener.requestUpdateRecordState(mPhoneRecorderState, mCustomValue);
//        }
//        mHandler.postDelayed(mRecordDiskCheck, 500);
    }

    public void stopVideoRecord() {
//        log("- start call VTManager.stopRecording() : " + mRecordType);
//        VTManager.getInstance().stopRecording(mRecordType);
//        // Gionee fangbin 20121114 added for CR00728364 start
//        if (PhoneApp.ISGNPHONE) {
//            // Gionee fangbin 20121208 added for CR00739122 start
//            mHandler.removeCallbacks(mRecordDiskCheck);
//            // Gionee fangbin 20121208 added for CR00739122 end
//            mRecordStartTime = 0l;
//            mRecordTime = 0l;
//            if (null != mRecordTimeCallBack) {
//                mRecordTimeCallBack.callBack(mRecordTime, false);
//            }
//        }
//        // Gionee fangbin 20121114 added for CR00728364 end
//        log("- end call VTManager.stopRecording() : " + mRecordType);
//        mPhoneRecorderState = PhoneRecorder.IDLE_STATE;
//        if (null != mListener) {
//            mListener.requestUpdateRecordState(mPhoneRecorderState, mCustomValue);
//        }
    }

    /**
     * 
     * @return 
     */
    public int getPhoneRecorderState() {
        return mPhoneRecorderState;
    }

    /**
     * 
     * @param state 
     */
    public void setPhoneRecorderState(final int state) {
        mPhoneRecorderState = state;
    }

    public int getCustomValue() {
        return mCustomValue;
    }

    /**
     * 
     * @param customValue 
     */
    public void setCustomValue(final int customValue) {
        mCustomValue = customValue;
    }

    public int getRecordType() {
        return mRecordType;
    }

    /**
     * 
     * @param recordType
     */
    public void setRecordType(final int recordType) {
        mRecordType = recordType;
    }

    /**
     * 
     * @return 
     */
    public boolean isVTRecording() {
        return AuroraConstants.PHONE_RECORDING_VIDEO_CALL_CUSTOM_VALUE == mCustomValue
                && PhoneRecorder.RECORDING_STATE == mPhoneRecorderState;
    }

    private void checkRecordDisk() {
        // Gionee fangbin 20121114 added for CR00728364 start
//        if (PhoneApp.ISGNPHONE) {
            if (!GnPhoneRecordHelper.diskSpaceAvailable(AuroraConstants.PHONE_RECORD_LOW_STORAGE_THRESHOLD)) {
                Log.e("AN: ", "Checking result, disk is full, stop recording...");
                if (PhoneRecorder.isRecording() || isVTRecording()) {
                    if(GnPhoneRecordHelper.getDiskAvailableSize() != 0){
                        Toast.makeText(InCallApp.getInstance(), InCallApp.getInstance().getResources().getString(R.string.gn_vt_recording_saved_sd_full), Toast.LENGTH_SHORT).show();
                    }
                    if (PhoneRecorder.isRecording()) {
                        stopVoiceRecord();
                    } else if (isVTRecording()) {
                        stopVideoRecord();
                    }
                    mRecordStartTime = 0l;
                    mRecordTime = 0l;
                    if (null != mRecordTimeCallBack) {
                        mRecordTimeCallBack.callBack(mRecordTime, false);
                    }
                    if (null != mListener) {
                        mListener.onStorageFull();
                    }
                }
            } else {
                if (mRecordStartTime != 0) {
                    mRecordTime = System.currentTimeMillis() - mRecordStartTime;
                }
                if (null != mRecordTimeCallBack) {
                    mRecordTimeCallBack.callBack(mRecordTime, true);
                }
                mHandler.postDelayed(mRecordDiskCheck, 50);
            }
//        } else {
//        // Gionee fangbin 20121114 added for CR00728364 end
//            if (!PhoneUtils.diskSpaceAvailable(mRecordStoragePath, Constants.PHONE_RECORD_LOW_STORAGE_THRESHOLD)) {
//                Log.e("AN: ", "Checking result, disk is full, stop recording...");
//                if (PhoneRecorder.isRecording() || isVTRecording()) {
//                    if (PhoneRecorder.isRecording()) {
//                        stopVoiceRecord();
//                    } else if (isVTRecording()) {
//                        stopVideoRecord();
//                    }
//                    if (null != mListener) {
//                        mListener.onStorageFull();
//                    }
//                }
//            } else {
//                mHandler.postDelayed(mRecordDiskCheck, 50);
//            }
//        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())
                    || Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())) {
                StorageVolume storageVolume =
                    (StorageVolume) intent.getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
                if (null == storageVolume) {
                    log("storageVolume is null");
                    return;
                }
                String currentPath = storageVolume.getPath();
                if (null == mRecordStoragePath || !currentPath.equals(mRecordStoragePath)) {
                    log("not current used storage unmount or eject");
                    return;
                }
                if (PhoneRecorder.RECORDING_STATE == mPhoneRecorderState) {
                    if (AuroraConstants.PHONE_RECORDING_TYPE_ONLY_VOICE == mRecordType) {
                        log("Current used sd card is ejected, stop voice record");
                        stopVoiceRecord(false);
                    } else if (AuroraConstants.PHONE_RECORDING_TYPE_VOICE_AND_PEER_VIDEO == mRecordType
                            || AuroraConstants.PHONE_RECORDING_TYPE_ONLY_PEER_VIDEO == mRecordType) {
                        log("Current used sd card is ejected, stop video record");
                        stopVideoRecord();
                    }
                }
            } /*else if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
                mIsStorageMounted = true;
            }*/
        }
    };

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
    
    // Gionee fangbin 20121114 added for CR00728364 start
    private long mRecordTime = 0l;
    private long mRecordStartTime = 0l;
    private RecordTimeCallBack mRecordTimeCallBack = null;
    
    public interface RecordTimeCallBack {
        public void callBack(long time, boolean visible);
        public void onError();
    }
    public void setRecordTimeCallBack(RecordTimeCallBack callBack) {
        mRecordTimeCallBack = callBack;
    }
    
    public void startVoiceRecord(final int customValue, String contacts, String name, String number) {
        mCustomValue = customValue;
        mRecordType = AuroraConstants.PHONE_RECORDING_TYPE_ONLY_VOICE;
        if (null != mRecorderServiceIntent && null == mPhoneRecorder) {
            mRecorderServiceIntent.putExtra("contacts", contacts);
            mRecorderServiceIntent.putExtra("name", name);
            mRecorderServiceIntent.putExtra("number", number);
            InCallApp.getInstance().bindService(mRecorderServiceIntent, mConnection, 
                                               Context.BIND_AUTO_CREATE);
        } else if (null != mRecorderServiceIntent && null != mPhoneRecorder) {
            try{
                mPhoneRecorder.startRecord();
                mRecordStartTime = System.currentTimeMillis();
                if (null != mRecordTimeCallBack) {
                    mRecordTimeCallBack.callBack(mRecordTime, true);
                }
                mHandler.postDelayed(mRecordDiskCheck, 500);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "start Record failed", new IllegalStateException());
            }
        }
    }
    // Gionee fangbin 20121114 added for CR00728364 end
}