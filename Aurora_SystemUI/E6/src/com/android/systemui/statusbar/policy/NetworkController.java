/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wimax.WimaxManagerConstants;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Slog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.util.AsyncChannel;
import com.android.server.am.BatteryStatsService;
import com.android.systemui.statusbar.util.SIMHelper;

import com.android.systemui.R;
import com.mediatek.systemui.ext.PluginFactory;
import com.mediatek.systemui.ext.IconIdWrapper;

import com.android.systemui.Xlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

//gionee fengxb 2012-11-22 add for CR00735378 start
import java.util.Timer;
import java.util.TimerTask;
import android.os.Bundle;
import android.os.SystemProperties;
//gionee fengxb 2012-11-22 add for CR00735378 end

import com.gionee.featureoption.FeatureOption;

public class NetworkController extends BroadcastReceiver {
    // debug
    static final String TAG = "StatusBar.NetworkController";
    static final boolean DEBUG = true;
    static final boolean CHATTY = true; // additional diagnostics, but not logspew

    // telephony
    boolean mHspaDataDistinguishable;
    final TelephonyManager mPhone;
    boolean mDataConnected;
    IccCardConstants.State mSimState = IccCardConstants.State.READY;
    int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    int mDataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    int mDataState = TelephonyManager.DATA_DISCONNECTED;
    int mDataActivity = TelephonyManager.DATA_ACTIVITY_NONE;
    ServiceState mServiceState;
    SignalStrength mSignalStrength;
    int[] mDataIconList = TelephonyIcons.DATA_G[0];
    String mNetworkName;
    String mNetworkNameDefault;
    String mNetworkNameSeparator;
    IconIdWrapper mPhoneSignalIconId = new IconIdWrapper();
    int mDataDirectionIconId; // data + data direction on phones
    IconIdWrapper mDataSignalIconId = new IconIdWrapper();
    int mDataTypeIconId;
	// Aurora <Steve.Tang> 2014-12-18. For Dual-Sim. start
	int mQSPhoneSignalIconId;
    int mQSDataTypeIconId;
    int mNoSimIconId;
    int mLastSimIconId;
	// Aurora <Steve.Tang> 2014-12-18. For Dual-Sim. end
	
    int mAirplaneIconId;
    boolean mDataActive;
    IconIdWrapper mMobileActivityIconId = new IconIdWrapper(); // overlay arrows for data direction
    int mLastSignalLevel;
    boolean mShowPhoneRSSIForData = false;
    boolean mShowAtLeastThreeGees = false;
    boolean mAlwaysShowCdmaRssi = false;

    String mContentDescriptionPhoneSignal;
    String mContentDescriptionWifi;
    String mContentDescriptionWimax;
    String mContentDescriptionCombinedSignal;
    String mContentDescriptionDataType;
    
    //gionee fengxb 2012-11-22 add for CR00735378 start
    //Gionee huangshuiqiang 2013-12-16 for CR00968639 add start
    private boolean mGNDelayUpdate = true;//SystemProperties.get("ro.gn.delayupdateSignalStrength").equals("yes");
    //Gionee huangshuiqiang 2013-12-16 for CR00968639 add end
    private SignalStrength mLastSignalStrength;
    private Timer mTimer = null;
    private TimerTask mTimertask = null;
    private static final int DELAY = 1;
    private static long DELAY_TIME = 10000;//10s 
    //gionee fengxb 2012-11-22 add for CR00735378 end

    // wifi
    WifiManager mWifiManager;
    AsyncChannel mWifiChannel;
    boolean mWifiEnabled, mWifiConnected;
    int mWifiRssi, mWifiLevel;
    String mWifiSsid;
    int mWifiIconId = 0;
    int mWifiActivityIconId = 0; // overlay arrows for wifi direction
    int mWifiActivity = WifiManager.DATA_ACTIVITY_NONE;

    // bluetooth
    protected boolean mBluetoothTethered = false;
    protected int mBluetoothTetherIconId =
        com.aurora.R.drawable.stat_sys_tether_bluetooth;

    //wimax
    protected boolean mWimaxSupported = false;
    protected boolean mIsWimaxEnabled = false;
    protected boolean mWimaxConnected = false;
    protected boolean mWimaxIdle = false;
    protected int mWimaxIconId = 0;
    protected int mWimaxSignal = 0;
    protected int mWimaxState = 0;
    protected int mWimaxExtraState = 0;
    protected int mDataServiceState = ServiceState.STATE_OUT_OF_SERVICE;

    // data connectivity (regardless of state, can we access the internet?)
    // state of inet connection - 0 not connected, 100 connected
    protected boolean mConnected = false;
    protected int mConnectedNetworkType = ConnectivityManager.TYPE_NONE;
    protected String mConnectedNetworkTypeName;
    protected int mInetCondition = 0;
    protected static final int INET_CONDITION_THRESHOLD = 50;

    protected boolean mAirplaneMode = false;
    protected boolean mLastAirplaneMode = true;

    // our ui
    Context mContext;
    ArrayList<ImageView> mPhoneSignalIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mDataDirectionIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mDataDirectionOverlayIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mWifiIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mWimaxIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mCombinedSignalIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mDataTypeIconViews = new ArrayList<ImageView>();
    ArrayList<TextView> mCombinedLabelViews = new ArrayList<TextView>();
    ArrayList<TextView> mMobileLabelViews = new ArrayList<TextView>();
    ArrayList<TextView> mWifiLabelViews = new ArrayList<TextView>();
    ArrayList<TextView> mEmergencyLabelViews = new ArrayList<TextView>();
    ArrayList<SignalCluster> mSignalClusters = new ArrayList<SignalCluster>();
    int mLastPhoneSignalIconId = -1;
    int mLastDataDirectionIconId = -1;
    int mLastDataDirectionOverlayIconId = -1;
    int mLastWifiIconId = -1;
    int mLastWimaxIconId = -1;
    int mLastCombinedSignalIconId = -1;
    int mLastDataTypeIconId = -1;
    String mLastCombinedLabel = "";

    protected boolean mHasMobileDataFeature;

    boolean mDataAndWifiStacked = false;

    // yuck -- stop doing this here and put it in the framework
    IBatteryStats mBatteryStats;

    public interface SignalCluster {
        void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
                String contentDescription);
        void setMobileDataIndicators(boolean visible, IconIdWrapper strengthIcon, IconIdWrapper activityIcon,
                int typeIcon, String contentDescription, String typeContentDescription);
        void setIsAirplaneMode(boolean is, int airplaneIcon);
        /// M: [SystemUI] Support "SIM indicator".
        void setShowSimIndicator(boolean showSimIndicator, int simIndicatorResId);
        /// M: Support Roam Data Icon both show.
        void setRoamingFlagandResource(boolean roaming, int roamingId);
    }

    /**
     * Construct this controller object and register for updates.
     */
    public NetworkController(Context context) {
        mContext = context;
        final Resources res = context.getResources();

        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mHasMobileDataFeature = cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);

        mShowPhoneRSSIForData = res.getBoolean(R.bool.config_showPhoneRSSIForData);
        mShowAtLeastThreeGees = res.getBoolean(R.bool.config_showMin3G);
        mAlwaysShowCdmaRssi = res.getBoolean(
                com.aurora.R.bool.config_alwaysUseCdmaRssi);

        // set up the default wifi icon, used when no radios have ever appeared
        updateWifiIcons();
        updateWimaxIcons();

        // telephony
        mPhone = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mPhone.listen(mPhoneStateListener,
                          PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY);
		// Aurora <zhanggp> <2013-10-17> modified for systemui begin				
        mHspaDataDistinguishable = mContext.getResources().getBoolean(
                R.bool.config_hspa_data_distinguishable);
                //&& PluginFactory.getStatusBarPlugin(mContext)
                //        .isHspaDataDistinguishable();
		// Aurora <zhanggp> <2013-10-17> modified for systemui end
        mNetworkNameSeparator = mContext.getString(R.string.status_bar_network_name_separator);
        mNetworkNameDefault = mContext.getString(
                com.aurora.R.string.lockscreen_carrier_default);
        mNetworkName = mNetworkNameDefault;

        // wifi
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Handler handler = new WifiHandler();
        mWifiChannel = new AsyncChannel();
        Messenger wifiMessenger = mWifiManager.getWifiServiceMessenger();
        if (wifiMessenger != null) {
            mWifiChannel.connect(mContext, handler, wifiMessenger);
        }

        // broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(FeatureOption.SPN_STRINGS_UPDATED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        /// M: [SystemUI] Support "SIM Color Change".
        filter.addAction(FeatureOption.SIM_SETTINGS_INFO_CHANGED);
        mWimaxSupported = mContext.getResources().getBoolean(
                com.aurora.R.bool.config_wimaxEnabled);
        if(mWimaxSupported) {
            filter.addAction(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION);
            filter.addAction(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION);
        }
        context.registerReceiver(this, filter);

        // AIRPLANE_MODE_CHANGED is sent at boot; we've probably already missed it
        updateAirplaneMode();

        // yuck
        mBatteryStats = BatteryStatsService.getService();
    }

    public boolean hasMobileDataFeature() {
        return mHasMobileDataFeature;
    }

    public boolean isEmergencyOnly() {
        return (mServiceState != null && mServiceState.isEmergencyOnly());
    }

    public void addPhoneSignalIconView(ImageView v) {
        mPhoneSignalIconViews.add(v);
    }

    public void addDataDirectionIconView(ImageView v) {
        mDataDirectionIconViews.add(v);
    }

    public void addDataDirectionOverlayIconView(ImageView v) {
        mDataDirectionOverlayIconViews.add(v);
    }

    public void addWifiIconView(ImageView v) {
        mWifiIconViews.add(v);
    }
    public void addWimaxIconView(ImageView v) {
        mWimaxIconViews.add(v);
    }

    public void addCombinedSignalIconView(ImageView v) {
        mCombinedSignalIconViews.add(v);
    }

    public void addDataTypeIconView(ImageView v) {
        mDataTypeIconViews.add(v);
    }

    public void addCombinedLabelView(TextView v) {
        mCombinedLabelViews.add(v);
    }

    public void addMobileLabelView(TextView v) {
        mMobileLabelViews.add(v);
    }

    public void addWifiLabelView(TextView v) {
        mWifiLabelViews.add(v);
    }

    public void addEmergencyLabelView(TextView v) {
        mEmergencyLabelViews.add(v);
    }

    public void addSignalCluster(SignalCluster cluster) {
        mSignalClusters.add(cluster);
        refreshSignalCluster(cluster);
    }

    public void refreshSignalCluster(SignalCluster cluster) {
        cluster.setWifiIndicators(
                // only show wifi in the cluster if connected or if wifi-only
                mWifiEnabled && (mWifiConnected || !mHasMobileDataFeature),
                mWifiIconId,
                mWifiActivityIconId,
                mContentDescriptionWifi);

        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is special
            cluster.setMobileDataIndicators(
                    true,
                    mAlwaysShowCdmaRssi ? mPhoneSignalIconId : new IconIdWrapper(mWimaxIconId),
                    mMobileActivityIconId,
                    mDataTypeIconId,
                    mContentDescriptionWimax,
                    mContentDescriptionDataType);
        } else {
            // normal mobile data
            cluster.setMobileDataIndicators(
                    mHasMobileDataFeature,
                    mShowPhoneRSSIForData ? mPhoneSignalIconId : mDataSignalIconId,
                    mMobileActivityIconId,
                    mDataTypeIconId,
                    mContentDescriptionPhoneSignal,
                    mContentDescriptionDataType);
        }
        cluster.setIsAirplaneMode(mAirplaneMode, mAirplaneIconId);
    }

    public void setStackedMode(boolean stacked) {
        mDataAndWifiStacked = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Xlog.d(TAG, "onReceive(): action = " + action);

        if (action.equals(WifiManager.RSSI_CHANGED_ACTION)
                || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            updateWifiState(intent);
            refreshViews();
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            updateSimState(intent);
            updateDataIcon();
            refreshViews();
        } else if (action.equals(FeatureOption.SPN_STRINGS_UPDATED_ACTION)) {
            updateNetworkName(intent.getBooleanExtra(FeatureOption.EXTRA_SHOW_SPN, false),
                        intent.getStringExtra(FeatureOption.EXTRA_SPN),
                        intent.getBooleanExtra(FeatureOption.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(FeatureOption.EXTRA_PLMN));
            refreshViews();
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                 action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            updateConnectivity(intent);
            refreshViews();
        } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
        	// Aurora <tongyh> <2014-04-09> add CarrierLabel begin
            if(mNetworkName.equals(mNetworkNameDefault)){
                mNetworkName = mContext.getString(
                com.android.internal.R.string.lockscreen_carrier_default);
            }else if(!(mNetworkName.equals(""))){
                int index = mNetworkName.indexOf(mNetworkNameSeparator);
                if(index < 0){
                    mNetworkName = getLocaleString(mNetworkName);
                }else{
                    mNetworkName = getLocaleString(mNetworkName.substring(0,index))
                        +mNetworkNameSeparator
                        +getLocaleString(mNetworkName.substring(index+1));
                }
            }
         // Aurora <tongyh> <2014-04-09> add CarrierLabel end
            refreshViews();
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            updateAirplaneMode();
            refreshViews();
        } else if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION) ||
                action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION) ||
                action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            updateWimaxState(intent);
            refreshViews();
        }
        /// M: [SystemUI] Support "SIM Color Change". @{
        else if (action.equals(FeatureOption.SIM_SETTINGS_INFO_CHANGED)) {
            SIMHelper.updateSIMInfos(context);
            int type = intent.getIntExtra("type", -1);
            long simId = intent.getLongExtra("simid", -1);
            if (type == 1) {
                // color changed
                updateDataNetType();
                updateTelephonySignalStrength();
            }
            refreshViews();
        }
        /// M: [SystemUI] Support "SIM Color Change". }@
    }

   //gionee fengxb 2012-11-22 add for CR00735378 start
	private void compareLevel(final int nowLevel, final SignalStrength signalStrength) {
		if (DEBUG) {
			Slog.d(TAG, "mLastSignalLevel = " + mLastSignalLevel + "; nowLevel = " + nowLevel);
        }
		if (mLastSignalLevel < nowLevel) {
			if (mTimer != null) {
				mTimer.cancel();
			}
			mLastSignalStrength = signalStrength;
			updateLevel();
		} else if (mLastSignalLevel == nowLevel) {
			if (mTimer != null) {
				mTimer.cancel();
			}
		} else {
			if (mTimer != null) {
				mTimer.cancel();
			}
			mTimer = new Timer();
			mTimertask = new TimerTask() {
				@Override
				public void run() {
					if (DEBUG) {
						Slog.d(TAG, "send message, level = " + nowLevel);
			        }
					mLastSignalStrength = signalStrength;
					Message msg = mGNHandler.obtainMessage(DELAY);
					mGNHandler.sendMessage(msg);
				}
			};
			mTimer.schedule(mTimertask, DELAY_TIME);
		}
	}

	private Handler mGNHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DELAY:
				updateLevel();
				break;
			}
		}
	};

	private void updateLevel() {
        mSignalStrength = mLastSignalStrength;
        updateTelephonySignalStrength();
        refreshViews();
	}
   //gionee fengxb 2012-11-22 add for CR00735378 end

    // ===== Telephony ==============================================================

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (DEBUG) {
                Slog.d(TAG, "onSignalStrengthsChanged signalStrength=" + signalStrength +
                    ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
            }
            //gionee fengxb 2012-11-22 add for CR00735378 start
            /*
            mSignalStrength = signalStrength;
            updateTelephonySignalStrength();
            refreshViews();*/
            if(signalStrength == null){
                return;
            }
            int signalStrengthLevel = signalStrength.getLevel();
            if(mGNDelayUpdate){
                compareLevel(signalStrengthLevel, signalStrength);
                //Gionee huangshuiqiang 2013-12-16 for CR00968639 add start
                refreshViews();
                //Gionee huangshuiqiang 2013-12-16 for CR00968639 add end
            } else {
                mSignalStrength = signalStrength;
                updateTelephonySignalStrength();
                refreshViews();
            }
            //gionee fengxb 2012-11-22 add for CR00735378 end
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (DEBUG) {
                Slog.d(TAG, "onServiceStateChanged state=" + state.getState());
            }
            mServiceState = state;
            updateTelephonySignalStrength();
            updateDataNetType();
            updateDataIcon();
            refreshViews();
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DEBUG) {
                Slog.d(TAG, "onCallStateChanged state=" + state);
            }
            // In cdma, if a voice call is made, RSSI should switch to 1x.
            if (isCdma()) {
                updateTelephonySignalStrength();
                refreshViews();
            }
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            if (DEBUG) {
                Slog.d(TAG, "onDataConnectionStateChanged: state=" + state
                        + " type=" + networkType);
            }
            mDataState = state;
            mDataNetType = networkType;
            updateDataNetType();
            updateDataIcon();
            refreshViews();
        }

        @Override
        public void onDataActivity(int direction) {
            if (DEBUG) {
                Slog.d(TAG, "onDataActivity: direction=" + direction);
            }
            mDataActivity = direction;
            updateDataIcon();
            refreshViews();
        }
    };

    protected void updateSimState(Intent intent) {
        String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
            mSimState = IccCardConstants.State.ABSENT;
        }
        else if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
            mSimState = IccCardConstants.State.READY;
        }
        else if (IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
            final String lockedReason =
                    intent.getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);
            if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
                mSimState = IccCardConstants.State.PIN_REQUIRED;
            }
            else if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
                mSimState = IccCardConstants.State.PUK_REQUIRED;
            }
            else {
                mSimState = IccCardConstants.State.NETWORK_LOCKED;
            }
        } else {
            mSimState = IccCardConstants.State.UNKNOWN;
        }
    }

    private boolean isCdma() {
        return (mSignalStrength != null) && !mSignalStrength.isGsm();
    }

    private boolean hasService() {
        if (mServiceState != null) {
            switch (mServiceState.getState()) {
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_POWER_OFF:
                    return false;
				// Aurora <Steve.Tang> 2014-09-17 STATE_EMERGENCY_ONLY return false, start
                case ServiceState.STATE_EMERGENCY_ONLY:
                    return false;
				// Aurora <Steve.Tang> 2014-09-17 end
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    protected void updateAirplaneMode() {
        mAirplaneMode = (Settings.Global.getInt(mContext.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
    }

    private final void updateTelephonySignalStrength() {
        if (!hasService()) {
            if (CHATTY) Slog.d(TAG, "updateTelephonySignalStrength: !hasService()");
			// Aurora <zhanggp> <2013-10-21> modified for systemui begin
         // Aurora <tongyh> <2014-05-12> update sim state begin
//			if(mSimState.iccCardExist()){
            if(mSimState != IccCardConstants.State.ABSENT){
         // Aurora <tongyh> <2014-05-12>update sim state end
	            mPhoneSignalIconId.setResources(null);
	            mPhoneSignalIconId.setIconId(R.drawable.stat_sys_signal_null);
	            mDataSignalIconId.setResources(null);
	            mDataSignalIconId.setIconId(R.drawable.stat_sys_signal_null);
			}else{
	            mPhoneSignalIconId.setResources(null);
	            mPhoneSignalIconId.setIconId(R.drawable.stat_sys_signal_no_sim);
	            mDataSignalIconId.setResources(null);
	            mDataSignalIconId.setIconId(R.drawable.stat_sys_signal_no_sim);
			}
			/*
            mPhoneSignalIconId.setResources(null);
            mPhoneSignalIconId.setIconId(R.drawable.stat_sys_signal_null);
            mDataSignalIconId.setResources(null);
            mDataSignalIconId.setIconId(R.drawable.stat_sys_signal_null);
			*/
			// Aurora <zhanggp> <2013-10-21> modified for systemui end
        } else {
            if (mSignalStrength == null) {
                if (CHATTY) Slog.d(TAG, "updateTelephonySignalStrength: mSignalStrength == null");
                mPhoneSignalIconId.setResources(null);
                mPhoneSignalIconId.setIconId(R.drawable.stat_sys_signal_null);
                mDataSignalIconId.setResources(null);
                mDataSignalIconId.setIconId(R.drawable.stat_sys_signal_null);
                mContentDescriptionPhoneSignal = mContext.getString(
                        AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0]);
            } else {
                int iconLevel;
                int[] iconList;
                if (isCdma() && mAlwaysShowCdmaRssi) {
                    mLastSignalLevel = iconLevel = mSignalStrength.getCdmaLevel();
                    if(DEBUG) Slog.d(TAG, "mAlwaysShowCdmaRssi=" + mAlwaysShowCdmaRssi
                            + " set to cdmaLevel=" + mSignalStrength.getCdmaLevel()
                            + " instead of level=" + mSignalStrength.getLevel());
                } else {
                    mLastSignalLevel = iconLevel = mSignalStrength.getLevel();
                }
                /// M: [SystemUI] Support "SIM Color Change". @{
                int simColor = SIMHelper.getSIMColorIdBySlot(mContext, -1);
                Slog.d(TAG, "athens iconLevel=" + iconLevel + " mInetCondition= " + mInetCondition + " simColor = " + simColor);
                if (simColor < 0 || simColor > 3) {
                    return;
                }
                if (isCdma()) {
                    if (isCdmaEri()) {
                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING[simColor];
                    } else {
                        iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[simColor];
                    }
                    mPhoneSignalIconId.setResources(null);
                    if (iconLevel < 5) {
                        mPhoneSignalIconId.setIconId(iconList[iconLevel]);
                    }
                } else {
                    // Though mPhone is a Manager, this call is not an IPC
                    if (mPhone.isNetworkRoaming()) {
                        final int signalIcon = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthIcon(true, simColor, iconLevel, false);
                        if (signalIcon != -1) {
                            mPhoneSignalIconId.setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                            mPhoneSignalIconId.setIconId(signalIcon);
                        } else {
                            iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING[simColor];
                            mPhoneSignalIconId.setResources(null);
                            if (iconLevel < 5) {
                                mPhoneSignalIconId.setIconId(iconList[iconLevel]);
                            }
                        }
                    } else {
                        int signalIcon = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthIcon(false, simColor, iconLevel, false);
                        if (signalIcon != -1) {
                            mPhoneSignalIconId.setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                            mPhoneSignalIconId.setIconId(signalIcon);
                        } else {
                            iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[simColor];
                            mPhoneSignalIconId.setResources(null);
                            if (iconLevel < 5) {
                                mPhoneSignalIconId.setIconId(iconList[iconLevel]);
                            }
                        }
                    }
                }

                String desc = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthDescription(iconLevel);
                if (desc != null) {
                    mContentDescriptionPhoneSignal = desc;
                } else {
                    if (iconLevel < 5) {
                        mContentDescriptionPhoneSignal = mContext.getString(
                                AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[iconLevel]);
                    }
                }
                int dataSignalIcon = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthIcon(false, simColor, iconLevel, false);
                if (dataSignalIcon != -1) {
                    mDataSignalIconId.setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                    mDataSignalIconId.setIconId(dataSignalIcon);
                } else {
                    mDataSignalIconId.setResources(null);
                    if (iconLevel < 5) {
                        mDataSignalIconId.setIconId(TelephonyIcons.DATA_SIGNAL_STRENGTH[simColor][iconLevel]);
                    }
                }
                /// M: [SystemUI] Support "SIM Color Change". }@
            }
        }
    }

    private final void updateDataNetType() {
        if (CHATTY) {
            Xlog.d(TAG, "updateDataNetType(): mShowAtLeastThreeGees = " + mShowAtLeastThreeGees);
        }

        /// M: [SystemUI] Support "SIM Color Change". @{

        int simColor = SIMHelper.getSIMColorIdBySlot(mContext, -1);
        Slog.d(TAG, "updateDataNetType simColor = " + simColor);
        if (simColor < 0 || simColor > 4) {
            return;
        }

        /// M: Support Roam Data Icon both show.
        if (isCdma()) {
            if (isCdmaEri()) {
                mRoamIconId = TelephonyIcons.ROAMING[simColor];
                mIsRoaming = true;
            }
        } else if (mPhone.isNetworkRoaming()) {
            mRoamIconId = TelephonyIcons.ROAMING[simColor];
            mIsRoaming = true;
        } else {
            mRoamIconId = 0;
            mIsRoaming = false;
        }
        Xlog.d(TAG, "updateDataNetType, DataNetType=" + mDataNetType + " mIsRoaming = " + mIsRoaming);
        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is a special 4g network not handled by telephony
            if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                mDataIconList = TelephonyIcons.DATA_4G[simColor];
            } else {
                mDataIconList = TelephonyIcons.DATA_4G_ROAM[simColor];
            }
            mDataTypeIconId = mDataIconList[0];
            mContentDescriptionDataType = mContext.getString(
                    R.string.accessibility_data_connection_4g);
        } else {
            switch (mDataNetType) {
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    if (!mShowAtLeastThreeGees) {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_G[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_G_ROAM[simColor];
                        }
                        mDataTypeIconId = 0;
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_gprs);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    if (!mShowAtLeastThreeGees) {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_E[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_E_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_edge);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                        mDataIconList = TelephonyIcons.DATA_3G[simColor];
                    } else {
                        mDataIconList = TelephonyIcons.DATA_3G_ROAM[simColor];
                    }
                    mDataTypeIconId = mDataIconList[0];
                    mContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_3g);
                    break;
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    if (mHspaDataDistinguishable) {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_H[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_H_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3_5g);
                    } else {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_3G[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_3G_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                    }
                    break;
                /// M: [SystemUI] Support "H Plus". @{
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    if (mHspaDataDistinguishable) {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_H_PLUS[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_H_PLUS_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3_5g);
                    } else {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_3G[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_3G_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                    }
                    break;
                    /// M: [SystemUI] Support "H Plus". }@
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    if (!mShowAtLeastThreeGees) {
                    // display 1xRTT for IS95A/B
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_1X[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_1X_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_cdma);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    if (!mShowAtLeastThreeGees) {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_1X[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_1X_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_cdma);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_EVDO_0: //fall through
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                        mDataIconList = TelephonyIcons.DATA_3G[simColor];
                    } else {
                        mDataIconList = TelephonyIcons.DATA_3G_ROAM[simColor];
                    }
                    mDataTypeIconId = mDataIconList[0];
                    mContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_3g);
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                        mDataIconList = TelephonyIcons.DATA_4G[simColor];
                    } else {
                        mDataIconList = TelephonyIcons.DATA_4G_ROAM[simColor];
                    }
                    mDataTypeIconId = mDataIconList[0];
                    mContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_4g);
                    break;
                default:
                    if (!mShowAtLeastThreeGees) {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_G[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_G_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_gprs);
                    } else {
                        if (!mIsRoaming) {  /// M: Support Roam Data Icon both show.
                            mDataIconList = TelephonyIcons.DATA_3G[simColor];
                        } else {
                            mDataIconList = TelephonyIcons.DATA_3G_ROAM[simColor];
                        }
                        mDataTypeIconId = mDataIconList[0];
                        mContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                    }
                    break;
            }
        }
        /// M: [SystemUI] Support "SIM Color Change". }@
    }

    boolean isCdmaEri() {
        if (mServiceState != null) {
            final int iconIndex = mServiceState.getCdmaEriIconIndex();
            if (iconIndex != EriInfo.ROAMING_INDICATOR_OFF) {
                final int iconMode = mServiceState.getCdmaEriIconMode();
                if (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                        || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH) {
                    return true;
                }
            }
        }
        return false;
    }

    private final void updateDataIcon() {
        int iconId;
        boolean visible = true;

        if (!isCdma()) {
            // GSM case, we have to check also the sim state
            if (mSimState == IccCardConstants.State.READY ||
                    mSimState == IccCardConstants.State.UNKNOWN) {
                if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
                    switch (mDataActivity) {
                        /// M: [SystemUI] Support "SIM Color Change". @{
                        case TelephonyManager.DATA_ACTIVITY_IN:
                            iconId = mDataIconList[0];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_OUT:
                            iconId = mDataIconList[0];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_INOUT:
                            iconId = mDataIconList[0];
                            break;
                        /// M: [SystemUI] Support "SIM Color Change". }@
                        default:
                            iconId = mDataIconList[0];
                            break;
                    }
                    mDataDirectionIconId = iconId;
                } else {
                    // The data network type will be displayed even if date connection is switched to 
                    // off according to OP01 spec
                    if (PluginFactory.getStatusBarPlugin(mContext).supportDataTypeAlwaysDisplayWhileOn() && hasService()) {
                        iconId = mDataIconList[0];
                        visible = false;
                        Slog.d(TAG, "updateDataIcon(): for OP01 project data network type"
                                + " is shown even if switch off data connection.");
                    } else {
                        iconId = 0;
                        visible = false;
                    }
                }
            } else {
                iconId = R.drawable.stat_sys_no_sim;
                visible = false; // no SIM? no data
            }
        } else {
            // CDMA case, mDataActivity can be also DATA_ACTIVITY_DORMANT
            if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
                switch (mDataActivity) {
                    /// M: [SystemUI] Support "SIM Color Change". @{
                    case TelephonyManager.DATA_ACTIVITY_IN:
                        iconId = mDataIconList[0];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_OUT:
                        iconId = mDataIconList[0];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_INOUT:
                        iconId = mDataIconList[0];
                        break;
                    /// M: [SystemUI] Support "SIM Color Change". }@
                    case TelephonyManager.DATA_ACTIVITY_DORMANT:
                    default:
                        iconId = mDataIconList[0];
                        break;
                }
            } else {
                iconId = 0;
                visible = false;
            }
        }
        Xlog.d(TAG, "updateDataIcon, iconId=" + iconId + ", visible=" + visible);
        // yuck - this should NOT be done by the status bar
        long ident = Binder.clearCallingIdentity();
        try {
            mBatteryStats.notePhoneDataConnectionState(mPhone.getNetworkType(), visible);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        mDataDirectionIconId = iconId;
        mDataConnected = visible;
    }

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        if (false) {
            Slog.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn + " spn=" + spn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        StringBuilder str = new StringBuilder();
        boolean something = false;
        if (showPlmn && plmn != null) {
        	// Aurora <tongyh> <2014-04-09> add CarrierLabel begin
//        	str.append(plmn);
            str.append(getLocaleString(plmn));
         // Aurora <tongyh> <2014-04-09> add CarrierLabel end
            something = true;
        }
        if (showSpn && spn != null) {
            if (something) {
                str.append(mNetworkNameSeparator);
            }
         // Aurora <tongyh> <2014-04-09> add CarrierLabel begin
//            str.append(spn);
            str.append(getLocaleString(spn));
         // Aurora <tongyh> <2014-04-09> add CarrierLabel end
            something = true;
        }
        if (something) {
            mNetworkName = str.toString();
        } else {
            mNetworkName = mNetworkNameDefault;
        }
    }

    // ===== Wifi ===================================================================

    class WifiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        mWifiChannel.sendMessage(Message.obtain(this,
                                AsyncChannel.CMD_CHANNEL_FULL_CONNECTION));
                    } else {
                        Slog.e(TAG, "Failed to connect to wifi");
                    }
                    break;
                case WifiManager.DATA_ACTIVITY_NOTIFICATION:
                    if (msg.arg1 != mWifiActivity) {
                        mWifiActivity = msg.arg1;
                        refreshViews();
                    }
                    break;
                default:
                    //Ignore
                    break;
            }
        }
    }

    protected void updateWifiState(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            mWifiEnabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;
        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            final NetworkInfo networkInfo = (NetworkInfo)
                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean wasConnected = mWifiConnected;
            mWifiConnected = networkInfo != null && networkInfo.isConnected();
            // If we just connected, grab the inintial signal strength and ssid
            if (mWifiConnected && !wasConnected) {
                // try getting it out of the intent first
                WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                if (info == null) {
                    info = mWifiManager.getConnectionInfo();
                }
                if (info != null) {
                    mWifiSsid = huntForSsid(info);
                } else {
                    mWifiSsid = null;
                }
            } else if (!mWifiConnected) {
                mWifiSsid = null;
            }
            // Apparently the wifi level is not stable at this point even if we've just connected to
            // the network; we need to wait for an RSSI_CHANGED_ACTION for that. So let's just set
            // it to 0 for now
            /// M: FOR ALPS00249371 wifilevel is wrong when start phone. @{
            if (mWifiConnected) {
                WifiInfo wifiInfo = ((WifiManager) mContext
                        .getSystemService(Context.WIFI_SERVICE))
                        .getConnectionInfo();
                if (wifiInfo != null) {
                    int newRssi = wifiInfo.getRssi();
                    int newSignalLevel = WifiManager.calculateSignalLevel(
                            newRssi, WifiIcons.WIFI_LEVEL_COUNT);
                    Xlog.d(TAG, "updateWifiState: mWifiLevel = " + mWifiLevel
                            + "  newRssi=" + newRssi + " newSignalLevel = "
                            + newSignalLevel);
                        if (newSignalLevel != mWifiLevel) {
                            mWifiLevel = newSignalLevel;
                        }
                    }
            }/// M: FOR ALPS00249371. @}
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            mWifiRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
            mWifiLevel = WifiManager.calculateSignalLevel(
                    mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT);
        }

        updateWifiIcons();
    }

    protected void updateWifiIcons() {
        if (mWifiConnected) {
            mWifiIconId = WifiIcons.WIFI_SIGNAL_STRENGTH[mInetCondition][mWifiLevel];
            mContentDescriptionWifi = mContext.getString(
                    AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH[mWifiLevel]);
        } else {
            if (mDataAndWifiStacked) {
                mWifiIconId = 0;
            } else {
                mWifiIconId = mWifiEnabled ? WifiIcons.WIFI_SIGNAL_STRENGTH[0][0] : 0;
            }
            mContentDescriptionWifi = mContext.getString(R.string.accessibility_no_wifi);
        }
    }

    private String huntForSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        // OK, it's not in the connectionInfo; we have to go hunting for it
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration net : networks) {
            if (net.networkId == info.getNetworkId()) {
                return net.SSID;
            }
        }
        return null;
    }


    // ===== Wimax ===================================================================
    protected final void updateWimaxState(Intent intent) {
        final String action = intent.getAction();
        boolean wasConnected = mWimaxConnected;
        if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION)) {
            int wimaxStatus = intent.getIntExtra(WimaxManagerConstants.EXTRA_4G_STATE,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mIsWimaxEnabled = (wimaxStatus ==
                    WimaxManagerConstants.NET_4G_STATE_ENABLED);
        } else if (action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION)) {
            mWimaxSignal = intent.getIntExtra(WimaxManagerConstants.EXTRA_NEW_SIGNAL_LEVEL, 0);
        } else if (action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            mWimaxState = intent.getIntExtra(WimaxManagerConstants.EXTRA_WIMAX_STATE,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxExtraState = intent.getIntExtra(
                    WimaxManagerConstants.EXTRA_WIMAX_STATE_DETAIL,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxConnected = (mWimaxState ==
                    WimaxManagerConstants.WIMAX_STATE_CONNECTED);
            mWimaxIdle = (mWimaxExtraState == WimaxManagerConstants.WIMAX_IDLE);
        }
        updateDataNetType();
        updateWimaxIcons();
    }

    protected void updateWimaxIcons() {
        if (mIsWimaxEnabled) {
            if (mWimaxConnected) {
                if (mWimaxIdle)
                    mWimaxIconId = WimaxIcons.WIMAX_IDLE;
                else
                    mWimaxIconId = WimaxIcons.WIMAX_SIGNAL_STRENGTH[mInetCondition][mWimaxSignal];
                mContentDescriptionWimax = mContext.getString(
                        AccessibilityContentDescriptions.WIMAX_CONNECTION_STRENGTH[mWimaxSignal]);
            } else {
                mWimaxIconId = WimaxIcons.WIMAX_DISCONNECTED;
                mContentDescriptionWimax = mContext.getString(R.string.accessibility_no_wimax);
            }
        } else {
            mWimaxIconId = 0;
        }
    }

    // ===== Full or limited Internet connectivity ==================================

    protected void updateConnectivity(Intent intent) {
        if (CHATTY) {
            Slog.d(TAG, "updateConnectivity: intent=" + intent);
        }

        final ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = connManager.getActiveNetworkInfo();

        // Are we connected at all, by any interface?
        mConnected = info != null && info.isConnected();
        if (mConnected) {
            mConnectedNetworkType = info.getType();
            mConnectedNetworkTypeName = info.getTypeName();
        } else {
            mConnectedNetworkType = ConnectivityManager.TYPE_NONE;
            mConnectedNetworkTypeName = null;
        }

        int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);

        if (CHATTY) {
            Slog.d(TAG, "updateConnectivity: networkInfo=" + info);
            Slog.d(TAG, "updateConnectivity: connectionStatus=" + connectionStatus);
        }
        Xlog.d(TAG, "updateConnectivity: connectionStatus=" + connectionStatus);
        mInetCondition = (connectionStatus > INET_CONDITION_THRESHOLD ? 1 : 0);

        if (info != null && info.getType() == ConnectivityManager.TYPE_BLUETOOTH) {
            mBluetoothTethered = info.isConnected();
        } else {
            mBluetoothTethered = false;
        }

        // We want to update all the icons, all at once, for any condition change
        updateDataNetType();
        updateWimaxIcons();
        updateDataIcon();
        updateTelephonySignalStrength();
        updateWifiIcons();
    }


    // ===== Update the views =======================================================

    void refreshViews() {
        Context context = mContext;

        IconIdWrapper combinedSignalIconId = new IconIdWrapper(0);
        IconIdWrapper combinedActivityIconId = new IconIdWrapper(0);
        String combinedLabel = "";
        String wifiLabel = "";
        String mobileLabel = "";
        int N;

        /// M: [SystemUI] Support "SIM Color Change". @{

        int simColor = SIMHelper.getSIMColorIdBySlot(mContext, -1);
        Slog.d(TAG, "refreshView simColor = " + simColor);
        
        /// M: [SystemUI] Support "SIM Color Change". }@

        /// M : merge 4.1.1.r4 change.
        final boolean emergencyOnly = isEmergencyOnly();

        if (!mHasMobileDataFeature) {
            mPhoneSignalIconId.setResources(null);
            mPhoneSignalIconId.setIconId(0);
            mDataSignalIconId.setResources(null);
            mDataSignalIconId.setIconId(0);
            mobileLabel = "";
        } else {
            // We want to show the carrier name if in service and either:
            //   - We are connected to mobile data, or
            //   - We are not connected to mobile data, as long as the *reason* packets are not
            //     being routed over that link is that we have better connectivity via wifi.
            // If data is disconnected for some other reason but wifi (or ethernet/bluetooth)
            // is connected, we show nothing.
            // Otherwise (nothing connected) we show "No internet connection".

            if (mDataConnected) {
                mobileLabel = mNetworkName;
            } else if (mConnected || emergencyOnly) { /// M : merge 4.1.1.r4 change.
                if (hasService() || emergencyOnly) { /// M : merge 4.1.1.r4 change.
                    // The isEmergencyOnly test covers the case of a phone with no SIM
                    mobileLabel = mNetworkName;
                } else {
                    // Tablets, basically
                    mobileLabel = "";
                }
            } else {
                mobileLabel
                    = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }

            /// M: [SystemUI] Support "SIM Color Change". @{

            // Now for things that should only be shown when actually using mobile data.
            if (mDataConnected && simColor >= 0 && simColor <= 3) {
                combinedSignalIconId = mDataSignalIconId.clone();
                int[] iconList = PluginFactory.getStatusBarPlugin(mContext).getDataActivityIconList(simColor, false);
                if (iconList != null) {
                    mMobileActivityIconId.setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                    mMobileActivityIconId.setIconId(iconList[mDataActivity]);
                } else {
                    mMobileActivityIconId.setResources(null);
                    switch (mDataActivity) {// here need white pic
                        case TelephonyManager.DATA_ACTIVITY_IN:
                            mMobileActivityIconId.setIconId(R.drawable.stat_sys_signal_in);
                            break;
                        case TelephonyManager.DATA_ACTIVITY_OUT:
                            mMobileActivityIconId.setIconId(R.drawable.stat_sys_signal_out);
                            break;
                        case TelephonyManager.DATA_ACTIVITY_INOUT:
                            mMobileActivityIconId.setIconId(R.drawable.stat_sys_signal_inout);
                            break;
                        default:
                            mMobileActivityIconId.setIconId(0);
                            break;
                    }
                }
                /// M: [SystemUI] Support "SIM Color Change". }@

                combinedLabel = mobileLabel;
                combinedActivityIconId = mMobileActivityIconId.clone();
                combinedSignalIconId = mDataSignalIconId.clone(); // set by updateDataIcon()
                mContentDescriptionCombinedSignal = mContentDescriptionDataType;
            } else {
                // Hide data flow arrow icon
                if (!PluginFactory.getStatusBarPlugin(mContext).supportDataTypeAlwaysDisplayWhileOn()) {
                    mMobileActivityIconId.setResources(null);
                }
                mMobileActivityIconId.setIconId(0);
            }
        }

        Xlog.d(TAG, "refreshViews(): mDataConnected = " + mDataConnected + " mDataActivity = " + mDataActivity
                + " mMobileActivityIconId= " + mMobileActivityIconId + " mMobileActivityIconId.getIconId= "
                + mMobileActivityIconId.getIconId() + " mMobileActivityIconId.getResources= "
                + mMobileActivityIconId.getResources() + ".");
        
        if (mWifiConnected) {
            if (mWifiSsid == null) {
                wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_wifi_nossid);
                mWifiActivityIconId = 0; // no wifis, no bits
            } else {
                wifiLabel = mWifiSsid;
                if (DEBUG) {
                    wifiLabel += "xxxxXXXXxxxxXXXX";
                }
                switch (mWifiActivity) {
                    case WifiManager.DATA_ACTIVITY_IN:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_in;
                        break;
                    case WifiManager.DATA_ACTIVITY_OUT:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_out;
                        break;
                    case WifiManager.DATA_ACTIVITY_INOUT:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_inout;
                        break;
                    case WifiManager.DATA_ACTIVITY_NONE:
                        mWifiActivityIconId = 0;
                        break;
                }
            }

            combinedActivityIconId.setResources(null);
            combinedActivityIconId.setIconId(mWifiActivityIconId);
            combinedLabel = wifiLabel;
            combinedSignalIconId.setResources(null);
            combinedSignalIconId.setIconId(mWifiIconId); // set by updateWifiIcons()
            mContentDescriptionCombinedSignal = mContentDescriptionWifi;
        } else {
            if (mHasMobileDataFeature) {
                wifiLabel = "";
            } else {
                wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }
        }

        if (mBluetoothTethered) {
            combinedLabel = mContext.getString(R.string.bluetooth_tethered);
            combinedSignalIconId.setResources(null);
            combinedSignalIconId.setIconId(mBluetoothTetherIconId);
            mContentDescriptionCombinedSignal = mContext.getString(
                    R.string.accessibility_bluetooth_tether);
        }

        final boolean ethernetConnected = (mConnectedNetworkType == ConnectivityManager.TYPE_ETHERNET);
        if (ethernetConnected) {
            // TODO: icons and strings for Ethernet connectivity
            combinedLabel = mConnectedNetworkTypeName;
        }

        if (mAirplaneMode &&
                (mServiceState == null || (!hasService() && !mServiceState.isEmergencyOnly()))) {
            // Only display the flight-mode icon if not in "emergency calls only" mode.

            // look again; your radios are now airplanes
            mContentDescriptionPhoneSignal = mContext.getString(
                    R.string.accessibility_airplane_mode);
            mAirplaneIconId = R.drawable.stat_sys_signal_flightmode;
            mPhoneSignalIconId.setResources(null);
            mPhoneSignalIconId.setIconId(R.drawable.stat_sys_signal_flightmode);
            mDataSignalIconId.setResources(null);
            mDataSignalIconId.setIconId(R.drawable.stat_sys_signal_flightmode);
            mDataTypeIconId = 0;

            // combined values from connected wifi take precedence over airplane mode
            if (mWifiConnected) {
                // Suppress "No internet connection." from mobile if wifi connected.
                mobileLabel = "";
            } else {
                if (mHasMobileDataFeature) {
                    // let the mobile icon show "No internet connection."
                    wifiLabel = "";
                } else {
                    wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
                    combinedLabel = wifiLabel;
                }
                mContentDescriptionCombinedSignal = mContentDescriptionPhoneSignal;
                combinedSignalIconId = mDataSignalIconId.clone();
            }
        }
        else if (!mDataConnected && !mWifiConnected && !mBluetoothTethered && !mWimaxConnected && !ethernetConnected) {
            // pretty much totally disconnected

            combinedLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            // On devices without mobile radios, we want to show the wifi icon
            if (mHasMobileDataFeature) {
                combinedSignalIconId = mDataSignalIconId.clone();
            } else {
                combinedSignalIconId.setResources(null);
                combinedSignalIconId.setIconId(mWifiIconId);
            }
            mContentDescriptionCombinedSignal = mHasMobileDataFeature
                ? mContentDescriptionDataType : mContentDescriptionWifi;

            int origDataTypeIconId = mDataTypeIconId;
            mDataTypeIconId = 0;
            /// M: [SystemUI] Support "SIM Color Change". @{
            if (isCdma()) {  /// M: Support Roam Data Icon both show.
                if (isCdmaEri() && simColor >= 0 && simColor <= 3) {
                    mRoamIconId = TelephonyIcons.ROAMING[simColor];
                    mIsRoaming = true;
                }
            } else if (mPhone.isNetworkRoaming() && simColor >= 0 && simColor <= 3) {
                mRoamIconId = TelephonyIcons.ROAMING[simColor];
                mIsRoaming = true;
            /// M: [SystemUI] Support "SIM Color Change". }@
            } else {
                mRoamIconId = 0;
                mIsRoaming = false;
            }

            if (PluginFactory.getStatusBarPlugin(mContext).supportDataTypeAlwaysDisplayWhileOn()) {
                    if(hasService() && (mSimState == IccCardConstants.State.READY || mSimState == IccCardConstants.State.UNKNOWN)) {
                        mDataTypeIconId = origDataTypeIconId;
                    }
                    combinedActivityIconId.setResources(null);
                    combinedActivityIconId.setIconId(0);
            }
            Xlog.d(TAG, "refreshViews(): mDataConnected = " + mDataConnected + " mWifiConnected = " + mWifiConnected
                    + " mDataTypeIconId = " + mDataTypeIconId + ".");
        }

        if (DEBUG) {
            Slog.d(TAG, "refreshViews connected={"
                    + (mWifiConnected?" wifi":"")
                    + (mDataConnected?" data":"")
                    + " } level="
                    + ((mSignalStrength == null)?"??":Integer.toString(mSignalStrength.getLevel()))
                    + " combinedSignalIconId=0x"
                    + Integer.toHexString(combinedSignalIconId.getIconId())
                    + "/" + getResourceName(combinedSignalIconId.getIconId())
                    + " combinedActivityIconId=0x" + Integer.toHexString(combinedActivityIconId.getIconId())
                    + " mobileLabel=" + mobileLabel
                    + " wifiLabel=" + wifiLabel
                    + " emergencyOnly=" + emergencyOnly
                    + " combinedLabel=" + combinedLabel
                    + " mAirplaneMode=" + mAirplaneMode
                    + " mDataActivity=" + mDataActivity
                    + " mPhoneSignalIconId=0x" + Integer.toHexString(mPhoneSignalIconId.getIconId())
                    + " mDataDirectionIconId=0x" + Integer.toHexString(mDataDirectionIconId)
                    + " mDataSignalIconId=0x" + Integer.toHexString(mDataSignalIconId.getIconId())
                    + " mDataTypeIconId=0x" + Integer.toHexString(mDataTypeIconId)
                    + " mLastDataTypeIconId=0x" + Integer.toHexString(mLastDataTypeIconId)
                    + " mWifiIconId=0x" + Integer.toHexString(mWifiIconId)
                    + " mBluetoothTetherIconId=0x" + Integer.toHexString(mBluetoothTetherIconId));
        }

        /// M: [SystemUI] Support "SIM Indicator". @{
        for (SignalCluster cluster : mSignalClusters) {
            cluster.setShowSimIndicator(mSimIndicatorFlag, mSimIndicatorResId);
            /// M: Support Roam Data Icon both show.
            cluster.setRoamingFlagandResource(mIsRoaming, mRoamIconId);
        }
        /// M: [SystemUI] Support "SIM SIM Indicator". }@

        if (mLastPhoneSignalIconId          != mPhoneSignalIconId.getIconId()
         || mLastDataDirectionOverlayIconId != combinedActivityIconId.getIconId()
         || mLastWifiIconId                 != mWifiIconId
         || mLastWimaxIconId                != mWimaxIconId
         || mLastDataTypeIconId             != mDataTypeIconId
         || mLastAirplaneMode               != mAirplaneMode)
        {
            if (DEBUG) {
                Xlog.d(TAG, "refreshViews(): mLastDataTypeIconId: " + mLastDataTypeIconId + ", mDataTypeIconId: " 
                    + mDataTypeIconId + ", mLastAirplaneMode: " + mLastAirplaneMode + ", mAirplaneMode: " 
                    + mAirplaneMode + ", mLastWifiIconId: " + mLastWifiIconId + ", mWifiIconId: " + mWifiIconId);
            }
            // NB: the mLast*s will be updated later
            for (SignalCluster cluster : mSignalClusters) {
                refreshSignalCluster(cluster);
            }
        }

        if (mLastAirplaneMode != mAirplaneMode) {
            mLastAirplaneMode = mAirplaneMode;
        }

        // the phone icon on phones
        if (mLastPhoneSignalIconId != mPhoneSignalIconId.getIconId()) {
            mLastPhoneSignalIconId = mPhoneSignalIconId.getIconId();
            N = mPhoneSignalIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mPhoneSignalIconViews.get(i);
                if (mPhoneSignalIconId.getIconId() == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    if (mPhoneSignalIconId.getResources() != null) {
                        v.setImageDrawable(mPhoneSignalIconId.getDrawable());
                    } else {
                        if (mPhoneSignalIconId.getIconId() == 0) {
                            v.setImageDrawable(null);
                        } else {
                            v.setImageResource(mPhoneSignalIconId.getIconId());
                        }
                    }
                    v.setContentDescription(mContentDescriptionPhoneSignal);
                }
            }
        }

        // the data icon on phones
        if (mLastDataDirectionIconId != mDataDirectionIconId) {
            mLastDataDirectionIconId = mDataDirectionIconId;
            N = mDataDirectionIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mDataDirectionIconViews.get(i);
                v.setImageResource(mDataDirectionIconId);
                v.setContentDescription(mContentDescriptionDataType);
            }
        }

        // the wifi icon on phones
        if (mLastWifiIconId != mWifiIconId) {
            mLastWifiIconId = mWifiIconId;
            N = mWifiIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mWifiIconViews.get(i);
                if (mWifiIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mWifiIconId);
                    v.setContentDescription(mContentDescriptionWifi);
                }
            }
        }

        // the wimax icon on phones
        if (mLastWimaxIconId != mWimaxIconId) {
            mLastWimaxIconId = mWimaxIconId;
            N = mWimaxIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mWimaxIconViews.get(i);
                if (mWimaxIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mWimaxIconId);
                    v.setContentDescription(mContentDescriptionWimax);
                }
           }
        }
        // the combined data signal icon
        if (mLastCombinedSignalIconId != combinedSignalIconId.getIconId()) {
            mLastCombinedSignalIconId = combinedSignalIconId.getIconId();
            N = mCombinedSignalIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mCombinedSignalIconViews.get(i);
                if (combinedSignalIconId.getResources() != null) {
                    v.setImageDrawable(combinedSignalIconId.getDrawable());
                } else {
                    if (combinedSignalIconId.getIconId() == 0) {
                        v.setImageDrawable(null);
                    } else {
                        v.setImageResource(combinedSignalIconId.getIconId());
                    }
                }
                v.setContentDescription(mContentDescriptionCombinedSignal);
            }
        }

        // the data network type overlay
        if (mLastDataTypeIconId != mDataTypeIconId) {
            mLastDataTypeIconId = mDataTypeIconId;
            N = mDataTypeIconViews.size();
            Xlog.d(TAG, "refreshViews(): mLastDataTypeIconId = " + mLastDataTypeIconId + " N = " + N + ".");
            for (int i=0; i<N; i++) {
                final ImageView v = mDataTypeIconViews.get(i);
                if (mDataTypeIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mDataTypeIconId);
                    v.setContentDescription(mContentDescriptionDataType);
                }
            }
        }

        // the data direction overlay
        if (mLastDataDirectionOverlayIconId != combinedActivityIconId.getIconId()) {
            if (DEBUG) {
                Slog.d(TAG, "changing data overlay icon id to " + combinedActivityIconId.getIconId());
            }
            mLastDataDirectionOverlayIconId = combinedActivityIconId.getIconId();
            N = mDataDirectionOverlayIconViews.size();
            for (int i=0; i<N; i++) {
                final ImageView v = mDataDirectionOverlayIconViews.get(i);
                if (combinedActivityIconId.getIconId() == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    if (combinedActivityIconId.getResources() != null) {
                        v.setImageDrawable(combinedActivityIconId.getDrawable());
                    } else {
                        if (combinedActivityIconId.getIconId() == 0) {
                            v.setImageDrawable(null);
                        } else {
                            v.setImageResource(combinedActivityIconId.getIconId());
                        }
                    }
                    v.setContentDescription(mContentDescriptionDataType);
                }
            }
        }

        // the combinedLabel in the notification panel
        if (!mLastCombinedLabel.equals(combinedLabel)) {
            mLastCombinedLabel = combinedLabel;
            N = mCombinedLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mCombinedLabelViews.get(i);
                v.setText(combinedLabel);
            }
        }

        // wifi label
        N = mWifiLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mWifiLabelViews.get(i);
            v.setText(wifiLabel);
            if ("".equals(wifiLabel)) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }

        // mobile label
        N = mMobileLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mMobileLabelViews.get(i);
         // Aurora <tongyh> <2014-04-09> add CarrierLabel begin
//            v.setText(mobileLabel);
            v.setText(mNetworkName);
//            if ("".equals(mobileLabel)) {
         // Aurora <tongyh> <2014-04-09> add CarrierLabel end
            if ("".equals(mNetworkName)) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }

        // e-call label
        N = mEmergencyLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mEmergencyLabelViews.get(i);
            if (!emergencyOnly) {
                v.setVisibility(View.GONE);
            } else {
                v.setText(mobileLabel); // comes from the telephony stack
                v.setVisibility(View.VISIBLE);
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkController state:");
        pw.println(String.format("  %s network type %d (%s)",
                mConnected?"CONNECTED":"DISCONNECTED",
                mConnectedNetworkType, mConnectedNetworkTypeName));
        pw.println("  - telephony ------");
        pw.print("  hasService()=");
        pw.println(hasService());
        pw.print("  mHspaDataDistinguishable=");
        pw.println(mHspaDataDistinguishable);
        pw.print("  mDataConnected=");
        pw.println(mDataConnected);
        pw.print("  mSimState=");
        pw.println(mSimState);
        pw.print("  mPhoneState=");
        pw.println(mPhoneState);
        pw.print("  mDataState=");
        pw.println(mDataState);
        pw.print("  mDataActivity=");
        pw.println(mDataActivity);
        pw.print("  mDataNetType=");
        pw.print(mDataNetType);
        pw.print("/");
        pw.println(TelephonyManager.getNetworkTypeName(mDataNetType));
        pw.print("  mServiceState=");
        pw.println(mServiceState);
        pw.print("  mSignalStrength=");
        pw.println(mSignalStrength);
        pw.print("  mLastSignalLevel=");
        pw.println(mLastSignalLevel);
        pw.print("  mNetworkName=");
        pw.println(mNetworkName);
        pw.print("  mNetworkNameDefault=");
        pw.println(mNetworkNameDefault);
        pw.print("  mNetworkNameSeparator=");
        pw.println(mNetworkNameSeparator.replace("\n","\\n"));
        pw.print("  mPhoneSignalIconId=0x");
        pw.print(Integer.toHexString(mPhoneSignalIconId.getIconId()));
        pw.print("/");
        pw.println(getResourceName(mPhoneSignalIconId.getIconId()));
        pw.print("  mDataDirectionIconId=");
        pw.print(Integer.toHexString(mDataDirectionIconId));
        pw.print("/");
        pw.println(getResourceName(mDataDirectionIconId));
        pw.print("  mDataSignalIconId=");
        pw.print(Integer.toHexString(mDataSignalIconId.getIconId()));
        pw.print("/");
        pw.println(getResourceName(mDataSignalIconId.getIconId()));
        pw.print("  mDataTypeIconId=");
        pw.print(Integer.toHexString(mDataTypeIconId));
        pw.print("/");
        pw.println(getResourceName(mDataTypeIconId));
        pw.print("/");

        pw.println("  - wifi ------");
        pw.print("  mWifiEnabled=");
        pw.println(mWifiEnabled);
        pw.print("  mWifiConnected=");
        pw.println(mWifiConnected);
        pw.print("  mWifiRssi=");
        pw.println(mWifiRssi);
        pw.print("  mWifiLevel=");
        pw.println(mWifiLevel);
        pw.print("  mWifiSsid=");
        pw.println(mWifiSsid);
        pw.println(String.format("  mWifiIconId=0x%08x/%s",
                    mWifiIconId, getResourceName(mWifiIconId)));
        pw.print("  mWifiActivity=");
        pw.println(mWifiActivity);

        if (mWimaxSupported) {
            pw.println("  - wimax ------");
            pw.print("  mIsWimaxEnabled="); pw.println(mIsWimaxEnabled);
            pw.print("  mWimaxConnected="); pw.println(mWimaxConnected);
            pw.print("  mWimaxIdle="); pw.println(mWimaxIdle);
            pw.println(String.format("  mWimaxIconId=0x%08x/%s",
                        mWimaxIconId, getResourceName(mWimaxIconId)));
            pw.println(String.format("  mWimaxSignal=%d", mWimaxSignal));
            pw.println(String.format("  mWimaxState=%d", mWimaxState));
            pw.println(String.format("  mWimaxExtraState=%d", mWimaxExtraState));
        }

        pw.println("  - Bluetooth ----");
        pw.print("  mBtReverseTethered=");
        pw.println(mBluetoothTethered);

        pw.println("  - connectivity ------");
        pw.print("  mInetCondition=");
        pw.println(mInetCondition);

        pw.println("  - icons ------");
        pw.print("  mLastPhoneSignalIconId=0x");
        pw.print(Integer.toHexString(mLastPhoneSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mLastPhoneSignalIconId));
        pw.print("  mLastDataDirectionIconId=0x");
        pw.print(Integer.toHexString(mLastDataDirectionIconId));
        pw.print("/");
        pw.println(getResourceName(mLastDataDirectionIconId));
        pw.print("  mLastDataDirectionOverlayIconId=0x");
        pw.print(Integer.toHexString(mLastDataDirectionOverlayIconId));
        pw.print("/");
        pw.println(getResourceName(mLastDataDirectionOverlayIconId));
        pw.print("  mLastWifiIconId=0x");
        pw.print(Integer.toHexString(mLastWifiIconId));
        pw.print("/");
        pw.println(getResourceName(mLastWifiIconId));
        pw.print("  mLastCombinedSignalIconId=0x");
        pw.print(Integer.toHexString(mLastCombinedSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mLastCombinedSignalIconId));
        pw.print("  mLastDataTypeIconId=0x");
        pw.print(Integer.toHexString(mLastDataTypeIconId));
        pw.print("/");
        pw.println(getResourceName(mLastDataTypeIconId));
        pw.print("  mLastCombinedLabel=");
        pw.print(mLastCombinedLabel);
        pw.println("");
    }

    protected String getResourceName(int resId) {
        if (resId != 0) {
            final Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }
    
 // Aurora <tongyh> <2014-04-09> add CarrierLabel begin
    private static final int ORIGIN_CARRIER_NAME_ID
                                 = R.array.origin_carrier_names;
    private static final int LOCALE_CARRIER_NAME_ID
                                 = R.array.locale_carrier_names;
    public String getLocaleString(String c) {
        return getLocalString(
                   c, ORIGIN_CARRIER_NAME_ID, LOCALE_CARRIER_NAME_ID);
    }
    private final String getLocalString(String originalString, int originNamesId, int localNamesId) {
        return getLocalString(originalString, "com.android.systemui", originNamesId, localNamesId);
    }
    private final String getLocalString(String originalString, String defPackage,
            int originNamesId, int localNamesId) {
        String[] origNames = mContext.getResources().getStringArray(originNamesId);
        String[] localNames = mContext.getResources().getStringArray(localNamesId);
        for (int i = 0; i < origNames.length; i++) {
            if (origNames[i].equalsIgnoreCase(originalString)) {
                return mContext.getString(mContext.getResources().getIdentifier(localNames[i], "string", defPackage));
            }
        }
        return originalString;
    }
 // Aurora <tongyh> <2014-04-09> add CarrierLabel end
    /// M: [SystemUI] Support "SIM indicator". @{
    
    public void showSimIndicator() {
        //set SimIndicatorFlag and refreshViews.
        int simColor = SIMHelper.getSIMColorIdBySlot(mContext, -1);
        if (simColor > -1 && simColor < 4) {
            mSimIndicatorResId = TelephonyIcons.SIM_INDICATOR_BACKGROUND[simColor];
        }
        Xlog.d(TAG,"showSimIndicator simColor = " + simColor + " mSimIndicatorResId = " + mSimIndicatorResId);
        mSimIndicatorFlag = true;
        updateTelephonySignalStrength();
        updateDataNetType();
        updateDataIcon();
        refreshViews();
    }
    
    public void hideSimIndicator() {
        //reset SimIndicatorFlag and refreshViews.
        Xlog.d(TAG,"hideSimIndicator.");
        mSimIndicatorFlag = false;
        updateTelephonySignalStrength();
        updateDataNetType();
        updateDataIcon();
        refreshViews();
    }
    
    private boolean mSimIndicatorFlag = false;
    private int mSimIndicatorResId = 0;

    /// M: [SystemUI] Support "SIM indicator". }@

    /// M: Support Roam Data Icon both show.
    private boolean mIsRoaming = false;
    private int mRoamIconId = 0;

}