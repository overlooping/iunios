package com.android.contacts.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.app.Activity;
import android.telephony.SubscriptionManager;

public class SystemUtils {
    public static final String TAG = "SystemUtils";
    public static int STATUS_BAR_MODE_BLACK = com.aurora.utils.SystemUtils.STATUS_BAR_MODE_BLACK;
    public static int STATUS_BAR_MODE_WHITE = com.aurora.utils.SystemUtils.STATUS_BAR_MODE_WHITE;
    
    public static void switchStatusBarColorMode(int flag, Activity a) {
    	   com.aurora.utils.SystemUtils.switchStatusBarColorMode(flag, a);
    };
    
    public static void setStatusBarBackgroundTransparent(Activity a) {
    	com.aurora.utils.SystemUtils.setStatusBarBackgroundTransparent(a);
    }
    
    public static int getSlotId(int subId) {
    	return SubscriptionManager.getSlotId(subId);
    }
    
    public static int getSubId(int slot) {
    	return SubscriptionManager.getSubIdUsingPhoneId(slot);
    }
}
