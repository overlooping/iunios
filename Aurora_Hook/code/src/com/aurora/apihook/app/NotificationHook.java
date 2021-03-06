package com.aurora.apihook.app;

import com.android.internal.R;
import com.android.internal.util.NotificationColorUtil;

import android.view.View;
import android.widget.RemoteViews;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;

import java.text.NumberFormat;
import java.util.ArrayList;
import android.app.Notification.Action;
import android.app.Notification;
import android.os.Bundle;
import android.app.Notification.Builder;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.os.SystemClock;
import com.aurora.apihook.ClassHelper;
import com.aurora.apihook.Hook;
import com.aurora.apihook.XC_MethodHook.MethodHookParam;

/**
 * Hook Notification applyStandardTemplate/
 * 
 * @author: Rock.Tong
 * @date: 2015-01-19
 */
public class NotificationHook{
	
	public void after_applyStandardTemplate(MethodHookParam param) {
		int resId = (Integer) param.args[0];
		boolean hasProgress = (Boolean) param.args[1];
		Context mContext = (Context) ClassHelper.getObjectField(param.thisObject, "mContext");
        Bitmap mLargeIcon = (Bitmap) ClassHelper.getObjectField(param.thisObject, "mLargeIcon");
        int mPriority = (Integer) ClassHelper.getObjectField(param.thisObject, "mPriority");
        int mSmallIcon = (Integer) ClassHelper.getObjectField(param.thisObject, "mSmallIcon");
        CharSequence mContentTitle = (CharSequence) ClassHelper.getObjectField(param.thisObject, "mContentTitle");
        CharSequence mContentText = (CharSequence) ClassHelper.getObjectField(param.thisObject, "mContentText");
        CharSequence mContentInfo = (CharSequence) ClassHelper.getObjectField(param.thisObject, "mContentInfo");
        int mNumber = (Integer) ClassHelper.getObjectField(param.thisObject, "mNumber");
        CharSequence mSubText = (CharSequence) ClassHelper.getObjectField(param.thisObject, "mSubText");
        long mWhen = (Long) ClassHelper.getObjectField(param.thisObject, "mWhen");
        int mProgressMax = (Integer) ClassHelper.getObjectField(param.thisObject, "mProgressMax");
        boolean mShowWhen = (Boolean) ClassHelper.getObjectField(param.thisObject, "mShowWhen");
        boolean mUseChronometer = (Boolean) ClassHelper.getObjectField(param.thisObject, "mUseChronometer");
        boolean mProgressIndeterminate = (Boolean) ClassHelper.getObjectField(param.thisObject, "mProgressIndeterminate");
        int mProgress = (Integer) ClassHelper.getObjectField(param.thisObject, "mProgress");
        boolean mHasThreeLines = (Boolean) ClassHelper.getObjectField(param.thisObject, "mHasThreeLines");
        
//        RemoteViews contentView = new BuilderRemoteViews(mContext.getApplicationInfo(), resId);//xian bie guan
//        
//		Class<?> clz = Class.forName("android.app.Notification.BuilderRemoteViews");
//		Constructor<?> constructor = ClassHelper.findConstructorExact(clz,ApplicationInfo.class,int.class);
//		RemoteViews contentView = (RemoteViews)constructor.newInstance(mContext.getApplicationInfo(), resId);
        RemoteViews contentView = new RemoteViews(mContext.getPackageName(),
				resId);
		
//        resetStandardTemplate(contentView);
    	ClassHelper.callMethod(param.thisObject, "resetStandardTemplate", contentView);

        boolean showLine3 = false;
        boolean showLine2 = false;
        boolean contentTextInLine2 = false;

        if (mLargeIcon != null) {
            contentView.setImageViewBitmap(com.aurora.R.id.icon, mLargeIcon);
//            processLargeLegacyIcon(mLargeIcon, contentView);
            ClassHelper.callMethod(param.thisObject, "processLargeLegacyIcon", mLargeIcon, contentView);
            contentView.setImageViewResource(com.aurora.R.id.right_icon, mSmallIcon);
            contentView.setViewVisibility(com.aurora.R.id.right_icon, View.VISIBLE);
//            processSmallRightIcon(mSmallIcon, contentView);
            ClassHelper.callMethod(param.thisObject, "processSmallRightIcon", mSmallIcon, contentView);
        } else { // small icon at left
            contentView.setImageViewResource(com.aurora.R.id.icon, mSmallIcon);
            contentView.setViewVisibility(com.aurora.R.id.icon, View.VISIBLE);
//            processSmallIconAsLarge(mSmallIcon, contentView);
            ClassHelper.callMethod(param.thisObject, "processSmallIconAsLarge", mSmallIcon, contentView);
        }
        if (mContentTitle != null) {
//            contentView.setTextViewText(com.aurora.R.id.title, processLegacyText(mContentTitle));
        	CharSequence processLegacyText= (CharSequence)ClassHelper.callMethod(param.thisObject, "processLegacyText", mContentTitle);
        	contentView.setTextViewText(com.aurora.R.id.title, processLegacyText);
        }
        if (mContentText != null) {
//            contentView.setTextViewText(com.aurora.R.id.text, processLegacyText(mContentText));
        	CharSequence processLegacyText= (CharSequence)ClassHelper.callMethod(param.thisObject, "processLegacyText", mContentText);
            contentView.setTextViewText(com.aurora.R.id.text, processLegacyText);
            
            showLine3 = true;
        }
        if (mContentInfo != null) {
//            contentView.setTextViewText(com.aurora.R.id.info, processLegacyText(mContentInfo));
        	CharSequence processLegacyText= (CharSequence)ClassHelper.callMethod(param.thisObject, "processLegacyText", mContentInfo);
            contentView.setTextViewText(com.aurora.R.id.info, processLegacyText);
            contentView.setViewVisibility(com.aurora.R.id.info, View.VISIBLE);
            showLine3 = true;
        } else if (mNumber > 0) {
            final int tooBig = mContext.getResources().getInteger(
                    R.integer.status_bar_notification_info_maxnum);
            if (mNumber > tooBig) {
//                contentView.setTextViewText(com.aurora.R.id.info, processLegacyText(
//                        mContext.getResources().getString(
//                                R.string.status_bar_notification_info_overflow)));
            	CharSequence processLegacyText= (CharSequence)ClassHelper.callMethod(param.thisObject, "processLegacyText", 
            			mContext.getResources().getString(R.string.status_bar_notification_info_overflow));
            	 contentView.setTextViewText(com.aurora.R.id.info, processLegacyText);
            } else {
                NumberFormat f = NumberFormat.getIntegerInstance();
//                contentView.setTextViewText(com.aurora.R.id.info, processLegacyText(f.format(mNumber)));
                CharSequence processLegacyText= (CharSequence)ClassHelper.callMethod(param.thisObject, "processLegacyText", f.format(mNumber));
                contentView.setTextViewText(com.aurora.R.id.info, processLegacyText);
            }
            contentView.setViewVisibility(com.aurora.R.id.info, View.VISIBLE);
            showLine3 = true;
        } else {
            contentView.setViewVisibility(com.aurora.R.id.info, View.GONE);
        }

        // Need to show three lines?
        if (mSubText != null) {
        	CharSequence processLegacyText_mSubText= (CharSequence)ClassHelper.callMethod(param.thisObject, "processLegacyText", mSubText);
//            contentView.setTextViewText(com.aurora.R.id.text, processLegacyText(mSubText));
        	contentView.setTextViewText(com.aurora.R.id.text, processLegacyText_mSubText);
            if (mContentText != null) {
            	CharSequence processLegacyText_mContentText= (CharSequence)ClassHelper.callMethod(param.thisObject, "processLegacyText", mContentText);
//                contentView.setTextViewText(com.aurora.R.id.text2, processLegacyText(mContentText));
                contentView.setTextViewText(com.aurora.R.id.text2, processLegacyText_mContentText);
                contentView.setViewVisibility(com.aurora.R.id.text2, View.VISIBLE);
                showLine2 = true;
                contentTextInLine2 = true;
            } else {
                contentView.setViewVisibility(com.aurora.R.id.text2, View.GONE);
            }
        } else {
            contentView.setViewVisibility(com.aurora.R.id.text2, View.GONE);
            if (hasProgress && (mProgressMax != 0 || mProgressIndeterminate)) {
                contentView.setViewVisibility(com.aurora.R.id.progress, View.VISIBLE);
                contentView.setProgressBar(
                		com.aurora.R.id.progress, mProgressMax, mProgress, mProgressIndeterminate);
                showLine2 = true;
            } else {
                contentView.setViewVisibility(com.aurora.R.id.progress, View.GONE);
            }
        }
        if (showLine2) {
            // need to shrink all the type to make sure everything fits
//            shrinkLine3Text(contentView);
        	ClassHelper.callMethod(param.thisObject, "shrinkLine3Text", contentView);
        }
        boolean showsTimeOrChronometer = (Boolean)ClassHelper.callMethod(param.thisObject, "showsTimeOrChronometer");
        if (showsTimeOrChronometer) {
            if (mUseChronometer) {
                contentView.setViewVisibility(com.aurora.R.id.chronometer, View.VISIBLE);
                contentView.setLong(com.aurora.R.id.chronometer, "setBase",
                        mWhen + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                contentView.setBoolean(com.aurora.R.id.chronometer, "setStarted", true);
            } else {
                contentView.setViewVisibility(com.aurora.R.id.time, View.VISIBLE);
                contentView.setLong(com.aurora.R.id.time, "setTime", mWhen);
            }
        }

        // Adjust padding depending on line count and font size.
        int calculateTopPadding = (Integer)ClassHelper.callMethod(param.thisObject, "calculateTopPadding", mContext,
                mHasThreeLines, mContext.getResources().getConfiguration().fontScale);
        contentView.setViewPadding(com.aurora.R.id.line1, 0, calculateTopPadding,
                0, 0);

        // We want to add badge to first line of text.
//        boolean addedBadge = addProfileBadge(contentView,
//                contentTextInLine2 ? com.aurora.R.id.profile_badge_line2 : com.aurora.R.id.profile_badge_line3);
/*        boolean addedBadge = (boolean)ClassHelper.callMethod(param.thisObject, "addProfileBadge", contentView,
                contentTextInLine2 ? com.aurora.R.id.profile_badge_line2 : com.aurora.R.id.profile_badge_line3);;
        // If we added the badge to line 3 then we should show line 3.
        if (addedBadge && !contentTextInLine2) {
            showLine3 = true;
        }*/

        // Note getStandardView may hide line 3 again.
        contentView.setViewVisibility(com.aurora.R.id.line3, showLine3 ? View.VISIBLE : View.GONE);
        contentView.setViewVisibility(com.aurora.R.id.overflow_divider, showLine3 ? View.VISIBLE : View.GONE);
		param.setResult(contentView);
	}
	
	
	
	//5.0 begin
	public void after_resetStandardTemplate(MethodHookParam param){
		RemoteViews contentView = (RemoteViews) param.args[0];
//		removeLargeIconBackground(contentView);//
		ClassHelper.callMethod(param.thisObject,"removeLargeIconBackground",contentView);
        contentView.setViewPadding(com.aurora.R.id.icon, 0, 0, 0, 0);
        contentView.setImageViewResource(com.aurora.R.id.icon, 0);
//        contentView.setInt(com.aurora.R.id.icon, "setBackgroundResource", 0);
        contentView.setViewVisibility(com.aurora.R.id.right_icon, View.GONE);
        contentView.setInt(com.aurora.R.id.right_icon, "setBackgroundResource", 0);
        contentView.setImageViewResource(com.aurora.R.id.right_icon, 0);
        contentView.setImageViewResource(com.aurora.R.id.icon, 0);
        contentView.setTextViewText(com.aurora.R.id.title, null);
        contentView.setTextViewText(com.aurora.R.id.text, null);
//        unshrinkLine3Text(contentView);//
        ClassHelper.callMethod(param.thisObject,"unshrinkLine3Text",contentView);
        contentView.setTextViewText(com.aurora.R.id.text2, null);
        contentView.setViewVisibility(com.aurora.R.id.text2, View.GONE);
        contentView.setViewVisibility(com.aurora.R.id.info, View.GONE);
        contentView.setViewVisibility(com.aurora.R.id.time, View.GONE);
        contentView.setViewVisibility(com.aurora.R.id.line3, View.GONE);
        contentView.setViewVisibility(com.aurora.R.id.overflow_divider, View.GONE);
        contentView.setViewVisibility(com.aurora.R.id.progress, View.GONE);
        contentView.setViewVisibility(com.aurora.R.id.chronometer, View.GONE);
        contentView.setViewVisibility(com.aurora.R.id.time, View.GONE);
	}
	//5.0 end
	
    //5.0 begin
	public void after_removeLargeIconBackground(MethodHookParam param){
		RemoteViews contentView = (RemoteViews) param.args[0];
//		contentView.setInt(com.aurora.R.id.icon, "setBackgroundResource", 0);
	}
	//5.0 end
	
    //5.0 begin
	public void before_applyLargeIconBackground(MethodHookParam param){
//		Context mContext = (Context) ClassHelper.getObjectField(param.thisObject, "mContext");
//		RemoteViews contentView = (RemoteViews) param.args[0];
//		int resolveColor = (int)ClassHelper.callMethod(param.thisObject,"resolveColor");
//		contentView.setInt(com.aurora.R.id.icon, "setBackgroundResource",
//                R.drawable.notification_icon_legacy_bg);
//
//        contentView.setDrawableParameters(
//        		com.aurora.R.id.icon,
//                true,
//                -1,
//                resolveColor,
//                PorterDuff.Mode.SRC_ATOP,
//                -1);
//
//        int padding = mContext.getResources().getDimensionPixelSize(
//                R.dimen.notification_large_icon_circle_padding);
//        contentView.setViewPadding(com.aurora.R.id.icon, padding, padding, padding, padding);
        param.setResult(true);
	}
    //5.0 end
	
	//5.0 begin
	public void after_processSmallRightIcon(MethodHookParam param){
		int smallIconDrawableId = (int) param.args[0];
		RemoteViews contentView = (RemoteViews) param.args[1];
		Context mContext = (Context) ClassHelper.getObjectField(param.thisObject, "mContext");
		NotificationColorUtil mColorUtil =  (NotificationColorUtil) ClassHelper.getObjectField(param.thisObject, "mColorUtil");
		boolean isLegacy = (boolean)ClassHelper.callMethod(param.thisObject,"isLegacy");
		int resolveColor = (int)ClassHelper.callMethod(param.thisObject,"resolveColor");
		if (!isLegacy || mColorUtil.isGrayscaleIcon(mContext, smallIconDrawableId)) {
            contentView.setDrawableParameters(com.aurora.R.id.right_icon, false, -1,
                    0xFFFFFFFF,
                    PorterDuff.Mode.SRC_ATOP, -1);

            contentView.setInt(com.aurora.R.id.right_icon,
                    "setBackgroundResource",
                    R.drawable.notification_icon_legacy_bg);

            contentView.setDrawableParameters(
            		com.aurora.R.id.right_icon,
                    true,
                    -1,
                    resolveColor,
                    PorterDuff.Mode.SRC_ATOP,
                    -1);
        }
	}
	//5.0 end
	
	//5.0 begin
	public void after_shrinkLine3Text(MethodHookParam param){
		RemoteViews contentView = (RemoteViews) param.args[0];
		Context mContext = (Context) ClassHelper.getObjectField(param.thisObject, "mContext");
		float subTextSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.notification_subtext_size);
        contentView.setTextViewTextSize(com.aurora.R.id.text, TypedValue.COMPLEX_UNIT_PX, subTextSize);
	}
	
	public void after_unshrinkLine3Text(MethodHookParam param){
		RemoteViews contentView = (RemoteViews) param.args[0];
		Context mContext = (Context) ClassHelper.getObjectField(param.thisObject, "mContext");
		float regularTextSize = mContext.getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.notification_text_size);
        contentView.setTextViewTextSize(com.aurora.R.id.text, TypedValue.COMPLEX_UNIT_PX, regularTextSize);
	}
	//5.0 end
	
	//5.0 begin
	private void aurora_org_resetStandardTemplateWithActions(MethodHookParam param) {
		RemoteViews big = (RemoteViews) param.args[0];
        big.setViewVisibility(com.aurora.R.id.actions, View.GONE);
        big.setViewVisibility(com.aurora.R.id.action_divider, View.GONE);
        big.removeAllViews(com.aurora.R.id.actions);
    }
	//5.0 end
	
	public void after_applyStandardTemplateWithActions(MethodHookParam param) {
		int layoutId = (Integer)param.args[0];
		int MAX_ACTION_BUTTONS = (Integer) ClassHelper.getObjectField(param.thisObject, "MAX_ACTION_BUTTONS");
		ArrayList<Action> mActions = (ArrayList<Action>) ClassHelper.getObjectField(param.thisObject, "mActions");
		
//        RemoteViews big = applyStandardTemplate(layoutId);
        RemoteViews big = (RemoteViews)ClassHelper.callMethod(param.thisObject,"applyStandardTemplate",layoutId, false);

//        resetStandardTemplateWithActions(big);
        ClassHelper.callMethod(param.thisObject, "resetStandardTemplateWithActions", big);

        int N = mActions.size();
        if (N > 0) {
            big.setViewVisibility(com.aurora.R.id.actions, View.VISIBLE);
            big.setViewVisibility(com.aurora.R.id.action_divider, View.VISIBLE);
            if (N>MAX_ACTION_BUTTONS) N=MAX_ACTION_BUTTONS;
            for (int i=0; i<N; i++) {
//                final RemoteViews button = generateActionButton(mActions.get(i));
            	final RemoteViews button = (RemoteViews)ClassHelper.callMethod(param.thisObject, "generateActionButton", mActions.get(i));
                big.addView(com.aurora.R.id.actions, button);
            }
        }
//        return big;
        param.setResult(big);
	}

	public void after_makeContentView(MethodHookParam param) {
		Log.d("0121","after_makeContentView");
		RemoteViews mContentView = (RemoteViews) ClassHelper.getObjectField(param.thisObject, "mContentView");
		Log.d("0121","mContentView != null --- " + (mContentView != null) );
		if (mContentView != null) {
			param.setResult(mContentView);
        } else {
        	int res = com.aurora.R.layout.notification_template_base;
        	param.setResult(ClassHelper.callMethod(param.thisObject,"applyStandardTemplate",res, true)); // no more special large_icon flavor
        }
	}
	
	

	public void after_makeTickerView(MethodHookParam param) {
		RemoteViews mTickerView = (RemoteViews) ClassHelper.getObjectField(param.thisObject, "mTickerView");
		RemoteViews mContentView = (RemoteViews) ClassHelper.getObjectField(param.thisObject, "mContentView");
		Bitmap mLargeIcon = (Bitmap) ClassHelper.getObjectField(param.thisObject, "mLargeIcon");
		if (mTickerView != null) {
			param.setResult(mTickerView);
        } else {
            if (mContentView == null) {
//                return applyStandardTemplate(mLargeIcon == null
//                        ? com.aurora.R.layout.status_bar_latest_event_ticker
//                        : com.aurora.R.layout.status_bar_latest_event_ticker_large_icon, true);
            	param.setResult(ClassHelper.callMethod(param.thisObject,"applyStandardTemplate",mLargeIcon == null
                      ? com.aurora.R.layout.status_bar_latest_event_ticker
                      : com.aurora.R.layout.status_bar_latest_event_ticker_large_icon, true));
            } else {
            	param.setResult(null);
            }
        }
	}

	public void after_makeBigContentView(MethodHookParam param) {
		ArrayList<Action> mActions = (ArrayList<Action>) ClassHelper.getObjectField(param.thisObject, "mActions");
		if (mActions.size() == 0) param.setResult(null);

//        return applyStandardTemplateWithActions(com.aurora.R.layout.notification_template_big_base);
		int res = com.aurora.R.layout.notification_template_big_base;
		RemoteViews result = (RemoteViews)ClassHelper.callMethod(param.thisObject,"applyStandardTemplateWithActions",res);
		param.setResult(result);
		
	}
	

	public void after_generateActionButton(MethodHookParam param) {
		Action action = (Action)param.args[0];
		Context mContext = (Context) ClassHelper.getObjectField(param.thisObject, "mContext");
		final boolean tombstone = (action.actionIntent == null);
        RemoteViews button = new RemoteViews(mContext.getPackageName(),
                tombstone ? com.aurora.R.layout.notification_action_tombstone
                          : com.aurora.R.layout.notification_action);
        button.setTextViewCompoundDrawablesRelative(com.aurora.R.id.action0, action.icon, 0, 0, 0);
        button.setTextViewText(com.aurora.R.id.action0, action.title);
        if (!tombstone) {
            button.setOnClickPendingIntent(com.aurora.R.id.action0, action.actionIntent);
        }
        button.setContentDescription(com.aurora.R.id.action0, action.title);
//        processLegacyAction(action, button);
        ClassHelper.callMethod(param.thisObject, "processLegacyAction", action, button);
        param.setResult(button);
	}
	

	
	public void after_getStandardView(MethodHookParam param) {
		Log.d("Notification", "after_getStandardView " );
		int layoutId = (Integer)param.args[0];
		CharSequence mBigContentTitle = (CharSequence) ClassHelper.getObjectField(param.thisObject, "mBigContentTitle");
        CharSequence mSummaryText = (CharSequence) ClassHelper.getObjectField(param.thisObject, "mSummaryText");
        boolean mSummaryTextSet = (Boolean) ClassHelper.getObjectField(param.thisObject, "mSummaryTextSet");
        Builder mBuilder = (Builder) ClassHelper.getObjectField(param.thisObject, "mBuilder");
        CharSequence mSubText = (CharSequence) ClassHelper.getObjectField(mBuilder, "mSubText");
        ClassHelper.callMethod(param.thisObject,"checkBuilder");
        CharSequence mContentTitle = (CharSequence) ClassHelper.getObjectField(mBuilder, "mContentTitle");
     // Nasty.
        CharSequence oldBuilderContentTitle = mContentTitle;
        
        Log.d("Notification", "mBigContentTitle != null --- " + (mBigContentTitle != null));
        if (mBigContentTitle != null) {
            mBuilder.setContentTitle(mBigContentTitle);
        }

//        RemoteViews contentView = applyStandardTemplateWithActions(layoutId);
        Log.d("Notification", "layoutId = " + layoutId);
        RemoteViews contentView = (RemoteViews)ClassHelper.callMethod(mBuilder, "applyStandardTemplateWithActions", layoutId);
        Log.d("Notification", "mBigContentTitle != null && mBigContentTitle.equals('')---" + (mBigContentTitle != null && mBigContentTitle.equals("")));
//        mBuilder.mContentTitle = oldBuilderContentTitle;
        mBuilder.setContentTitle(oldBuilderContentTitle);
        if (mBigContentTitle != null && mBigContentTitle.equals("")) {
            contentView.setViewVisibility(com.aurora.R.id.line1, View.GONE);
        } else {
            contentView.setViewVisibility(com.aurora.R.id.line1, View.VISIBLE);
        }

        // The last line defaults to the subtext, but can be replaced by mSummaryText
        final CharSequence overflowText =
                mSummaryTextSet ? mSummaryText
                                : mSubText;
        Log.d("Notification", "overflowText = " + overflowText);
        if (overflowText != null) {
            contentView.setTextViewText(com.aurora.R.id.text, overflowText);
            contentView.setViewVisibility(com.aurora.R.id.overflow_divider, View.VISIBLE);
            contentView.setViewVisibility(com.aurora.R.id.line3, View.VISIBLE);
        } else {
        	Log.d("Notification", "com.aurora.R.id.overflow_divider-----com.aurora.R.id.line3-----GONE");
            contentView.setViewVisibility(com.aurora.R.id.overflow_divider, View.GONE);
            contentView.setViewVisibility(com.aurora.R.id.line3, View.GONE);
        }

        param.setResult(contentView);
	}

	public void after_processLegacyAction(MethodHookParam param) {
		Action action = (Action)param.args[0];
		RemoteViews button = (RemoteViews)param.args[1];
		Context mContext = (Context) ClassHelper.getObjectField(param.thisObject, "mContext");
		boolean isLegacy = (boolean)ClassHelper.callMethod(param.thisObject,"isLegacy");
		NotificationColorUtil mColorUtil =  (NotificationColorUtil) ClassHelper.getObjectField(param.thisObject, "mColorUtil");
        if (!isLegacy || mColorUtil.isGrayscaleIcon(mContext, action.icon)) {
            button.setTextViewCompoundDrawablesRelativeColorFilter(R.id.action0, 0,
                    mContext.getResources().getColor(R.color.notification_action_color_filter),
                    PorterDuff.Mode.MULTIPLY);
        }
    }
	
}