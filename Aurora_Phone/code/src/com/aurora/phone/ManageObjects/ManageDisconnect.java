/*
 * Copyright (c) 2013 The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.phone;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.android.internal.telephony.CallManager;

import android.os.AsyncResult;

public class ManageDisconnect {
	private static final String LOG_TAG = "ManageDisconnect";

	private PhoneGlobals mApp;
	private CallManager mCM;
	private static final int PHONE_DISCONNECT = 102;
	private PowerManager.WakeLock mOnDisconnectScreenOnLock;
	private PowerManager mPowerManager;

	public ManageDisconnect(CallManager cm, PhoneGlobals app) {
		mCM = cm;
		mApp = app;
		mCM.registerForDisconnect(mHandler, PHONE_DISCONNECT, null);
		mPowerManager = (PowerManager) mApp.getSystemService(Context.POWER_SERVICE);
		mOnDisconnectScreenOnLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK
						| PowerManager.ACQUIRE_CAUSES_WAKEUP,
				"DisconnectScreenOnLock");
		mOnDisconnectScreenOnLock.setReferenceCounted(false);

	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case PHONE_DISCONNECT:
				onDisconnect((AsyncResult) msg.obj);
				break;
			}
		}
	};

	private void onDisconnect(AsyncResult r) {
		if (!mPowerManager.isScreenOn() && mOnDisconnectScreenOnLock != null
				&& !mOnDisconnectScreenOnLock.isHeld()) {
			mOnDisconnectScreenOnLock.acquire(3 * 1000);
		}
	}

	private void log(String msg) {
		Log.d(LOG_TAG, msg);
	}

	public void clear() {
		if (mOnDisconnectScreenOnLock != null
				&& mOnDisconnectScreenOnLock.isHeld()) {
			mOnDisconnectScreenOnLock.release();
		}
	}

}
