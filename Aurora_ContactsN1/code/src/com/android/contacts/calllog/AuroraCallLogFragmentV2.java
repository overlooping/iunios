/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.calllog;


import com.android.common.io.MoreCloseables;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactsApplication;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.ResConstant;
import com.android.contacts.activities.AuroraCallLogActivity;
import com.android.contacts.activities.AuroraCallRecordActivity;
import com.android.contacts.util.EmptyLoader;
import com.android.contacts.util.IntentFactory;
import com.android.contacts.util.NumberAreaUtil;
import com.android.contacts.util.YuloreUtils;
import com.android.contacts.test.NeededForTesting;
import com.android.contacts.voicemail.VoicemailStatusHelper;
import com.android.contacts.voicemail.VoicemailStatusHelper.StatusMessage;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.ITelephony;
import com.google.common.annotations.VisibleForTesting;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import aurora.app.AuroraAlertDialog; // import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import aurora.preference.AuroraPreferenceManager; // import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import aurora.widget.AuroraButton;
import android.widget.ImageView;
import aurora.widget.AuroraListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import aurora.widget.AuroraSpinner;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.contacts.util.Constants;
import com.mediatek.contacts.calllog.CallLogSubInfoHelper;
import com.android.contacts.calllog.AuroraCallLogAdapterV2;
import com.android.contacts.calllog.PhoneNumberHelper;
// The following lines are provided and maintained by Mediatek Inc.


import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Inflater;

import android.widget.ProgressBar;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
// The previous lines are provided and maintained by Mediatek Inc.

// aurora <ukiliu> <2013-9-27> add for aurora ui begin
import aurora.app.AuroraActivity;
import aurora.widget.AuroraActionBar;
import aurora.widget.AuroraActionBarItem;
import aurora.widget.AuroraActionBar.OnAuroraActionBarItemClickListener;
import aurora.widget.AuroraMenu;
import aurora.widget.AuroraMenuBase.OnAuroraMenuItemClickListener;
import aurora.widget.AuroraMenuItem;
import aurora.widget.NormalAuroraActionBarItem;
import aurora.widget.AuroraButton;
import aurora.app.AuroraAlertDialog; 






// aurora <ukiliu> <2013-9-27> add for aurora ui end
import com.android.contacts.GNContactsUtils;
import com.mediatek.contacts.ContactsFeatureConstants;

import aurora.app.AuroraProgressDialog;

import com.aurora.android.contacts.AuroraITelephony;
import com.aurora.android.contacts.AuroraSubInfoNotifier;
import com.privacymanage.service.AuroraPrivacyUtils;
import com.yulore.framework.YuloreHelper;

import android.database.MatrixCursor;
import android.provider.CallLog;
import android.telephony.*;
/**
 * Displays a list of call log entries.
 */
public class AuroraCallLogFragmentV2 extends ListFragment implements
		CallLogQueryHandler.Listener,
		AuroraCallLogAdapterV2.CallFetcher,
		OnItemClickListener, OnItemLongClickListener, AuroraSubInfoNotifier.SubInfoListener {

	private static final String TAG = "AuroraCallLogFragmentV2";

	private static final int AURORA_EDIT_CALLLOG = 1;
	/**
	 * ID of the empty loader to defer other fragments.
	 */
	private static final int EMPTY_LOADER_ID = 0;
	private static final int TAB_INDEX_CALL_LOG = 1;

	private AuroraCallLogAdapterV2 mAdapter;
	private CallLogQueryHandler mCallLogQueryHandler;
	private boolean mScrollToTop;

	/** Whether there is at least one voicemail source installed. */
	private boolean mVoicemailSourcesAvailable = false;
	/** Whether we are currently filtering over voicemail. */
	private boolean mShowingVoicemailOnly = false;

	private VoicemailStatusHelper mVoicemailStatusHelper;
	private View mStatusMessageView;
	private TextView mStatusMessageText;
	private TextView mStatusMessageAction;
	private KeyguardManager mKeyguardManager;

	private boolean mEmptyLoaderRunning;
	private boolean mCallLogFetched;
	private boolean mVoicemailStatusFetched;


	private boolean isLongClickEnable = true;
	private static String mSelectAllStr;
	private static String mUnSelectAllStr;

	private String[] CALL_TYPE_TITLE;
	private String[] CALL_TYPE_EMPTY_MSG;
	private View mCallLogLayut;
	private Animation mCallLogViewShowAnim = null;
	private Animation mCallLogViewHideAnim = null;
	private AuroraButton mAllCallLogType;
	private AuroraButton mMissingCallLogType;
	private boolean editMode = false;
	private Button calllog_delete;

	//aurora add zhouxiaobing 20140512 start
    private static final String UNKNOWN_NUMBER = "-1";
    private static final String PRIVATE_NUMBER = "-2";
    private static final String PAYPHONE_NUMBER = "-3";
	//aurora add zhouxiaobing 20140512 end
    private static ExecutorService exec = Executors.newSingleThreadExecutor();
    
	// aurora <ukiliu> <2013-9-27> add for aurora ui begin
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setChoiceMode(AuroraListView.CHOICE_MODE_MULTIPLE);
	}
    // aurora <ukiliu> <2013-9-27> add for aurora ui end

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		mScrollToTop = true;
		mCallLogQueryHandler = new CallLogQueryHandler(getActivity()
				.getContentResolver(), this);
		mKeyguardManager = (KeyguardManager) getActivity().getSystemService(
				Context.KEYGUARD_SERVICE);


		CALL_TYPE_TITLE = getResources().getStringArray(
				R.array.gn_call_log_type);
		CALL_TYPE_EMPTY_MSG = getResources().getStringArray(
				R.array.gn_call_log_type_empty);
		
        mContactChangeObserver = new ContactChangeObserver();
        
        if(getActivity() instanceof AuroraCallLogActivity){ 
        	mIsPrivate = ((AuroraCallLogActivity)getActivity()).isPrivate();
        }
        
        if(getActivity() instanceof AuroraActivity){
        	AuroraActionBar actionBar = ((AuroraActivity)getActivity()).getAuroraActionBar();
    		setDefaultActionBar((AuroraActivity)getActivity(), actionBar);

    		mSelectAllStr = getResources().getString(R.string.select_all);
    		mUnSelectAllStr = getResources().getString(
    				R.string.unselect_all);
        }
	}

	public void setDefaultActionBar(AuroraActivity auroraActivity, final AuroraActionBar actionBar) {
        if(actionBar.getVisibility() != View.VISIBLE){
            actionBar.setVisibility(View.VISIBLE);
        }
		actionBar.setTitle(mIsPrivate ? R.string.private_call_log : R.string.aurora_recentCallsLabel);
//		AuroraActionBarItem item = actionBar.getItem(0);
//		if(item != null){
//			int itemId = item.getItemId();
//			if(itemId == AURORA_EDIT_CALLLOG){
//				actionBar.changeItemType(AuroraActionBarItem.Type.Edit, AURORA_EDIT_CALLLOG);
//			} else {
//				actionBar.removeItem(item);
//				actionBar.addItem(AuroraActionBarItem.Type.Edit, AURORA_EDIT_CALLLOG);
//			}
//		} else {
//			actionBar.addItem(AuroraActionBarItem.Type.Edit, AURORA_EDIT_CALLLOG);
//		}
		actionBar.setOnAuroraActionBarListener(auroraActionBarItemClickListener);
		auroraActivity.setAuroraMenuCallBack(auroraMenuCallBack);
		if(mIsPrivate) {
	        actionBar.initActionBottomBarMenu(R.menu.aurora_delete, 1); 
		} else {
			actionBar.initActionBottomBarMenu(R.menu.aurora_calllog_delete, 3);
		}

		if (actionBar != null && actionBar.getSelectLeftButton() != null) {
		    actionBar.getSelectLeftButton().setOnClickListener(
	                new View.OnClickListener() {

	                    @Override
	                    public void onClick(View v) {
	                        if (getEditMode()) {
	                            switch2NormalMode();
	                              Log.v(TAG, "setShowBottomBarMenu1");
	                            actionBar.setShowBottomBarMenu(false);
	                            actionBar.showActionBarDashBoard();
	                            ((TextView) (actionBar.getSelectRightButton())).setText(mSelectAllStr);
	                        }
	                    }
	                });
		}
		
		if (actionBar != null && actionBar.getSelectRightButton() != null) {
		    actionBar.getSelectRightButton().setOnClickListener(
	                new View.OnClickListener() {

	                    @Override
	                    public void onClick(View v) {
	                        String selectStr = ((TextView) (actionBar
	                                .getSelectRightButton())).getText()
	                                .toString();
	                        if (selectStr.equals(mSelectAllStr)) {
	                            ((TextView) (actionBar.getSelectRightButton()))
	                                    .setText(mUnSelectAllStr);
	                            onSelectAll(true);
	                            setBottomMenuEnable(true);
	                        } else if (selectStr.equals(mUnSelectAllStr)) {
	                            ((TextView) (actionBar.getSelectRightButton()))
	                                    .setText(mSelectAllStr);
	                            onSelectAll(false);
	                            setBottomMenuEnable(false);
	                        }
	                    }
	                });
		}
		
		//aurora add liguangyu 201311204 start
		if(!mIsPrivate) {
			auroraActivity.setAuroraMenuCallBack(auroraMenuCallBackCallSettings);
			auroraActivity.setAuroraMenuItems(R.menu.aurora_dialtacts_options);
			auroraActivity.getAuroraMenu().removeMenuItemById(R.id.menu_add_new_contact);
			auroraActivity.getAuroraMenu().removeMenuItemById(R.id.menu_add_exist_contact);
		}
        //aurora add liguangyu 201311204 end
	}
	
	public void setBottomMenuEnable(boolean flag) {
		AuroraActionBar actionBar = ((AuroraActivity)getActivity()).getAuroraActionBar();
        AuroraMenu auroraMenu = actionBar.getAuroraActionBottomBarMenu();
        if(mIsPrivate) {
        	auroraMenu.setBottomMenuItemEnable(1, flag);
        } else {
        	boolean value = getAdapter().getCheckedCount() == 1;
        	auroraMenu.setBottomMenuItemEnable(1, flag);
	        auroraMenu.setBottomMenuItemEnable(2, value);
	        auroraMenu.setBottomMenuItemEnable(3, value);
        }
		
    }

	public void updateMenuItemState(boolean all_checked) {
		AuroraActionBar actionBar = ((AuroraActivity)getActivity()).getAuroraActionBar();
		if (all_checked) {
			if (actionBar != null && actionBar.getSelectRightButton() != null) {
				((TextView) (actionBar.getSelectRightButton()))
				        .setText(mUnSelectAllStr);
			}
		} else {
		    if (actionBar != null && actionBar.getSelectRightButton() != null) {
		        ((TextView) (actionBar.getSelectRightButton()))
                        .setText(mSelectAllStr);
		    }
		}
		
		if (all_checked || getAdapter().getCheckedCount() > 0) {
			setBottomMenuEnable(true);
		} else {
			setBottomMenuEnable(false);
		}
	}
	
	//aurora add liguangyu 20131113 for start
	public void updateActionBar() {
		AuroraActionBar actionBar = ((AuroraActivity)getActivity()).getAuroraActionBar();
		AuroraActionBarItem item = actionBar.getItem(0);
		if(getAdapter().getCount() == 0) {	
			if(item != null) {
				actionBar.removeItem(item);
			}
		} else {
			if(item == null) {
				actionBar.addItem(AuroraActionBarItem.Type.Edit, AURORA_EDIT_CALLLOG);
			}
		}
	}
	//aurora add liguangyu 20131113 for end
    
	//aurora add qiaohu 20141203 for #10247 start 
	public void updateUi(){
		AuroraActionBar actionBar = ((AuroraActivity)getActivity()).getAuroraActionBar();
		((TextView)actionBar.getSelectRightButton()).setText(mSelectAllStr);
		setBottomMenuEnable(false);
	}
	//aurora add qiaohu 20141203 for #10247 end
	
    //aurora add liguangyu 201311204 start
    private OnAuroraMenuItemClickListener auroraMenuCallBackCallSettings = new OnAuroraMenuItemClickListener() {

        @Override
        public void auroraMenuItemClick(int itemId) {
            switch (itemId) {
            case R.id.menu_call_settings: {
            	getActivity().startActivity(GNContactsUtils.getCallSettingsIntent());
                break; 
            }
            
            case R.id.menu_call_record: {
            	getActivity().startActivity(new Intent(ContactsApplication.getInstance().getApplicationContext(), AuroraCallRecordActivity.class));
                break;
            }
            default:
                break;
            }
        }
    };
    //aurora add liguangyu 201311204 end
	
//	private Handler mHandler = new Handler();
	private OnAuroraMenuItemClickListener auroraMenuCallBack = new OnAuroraMenuItemClickListener() {

		@Override
		public void auroraMenuItemClick(int itemId) {
			AuroraActionBar actionBar = ((AuroraActivity)getActivity()).getAuroraActionBar();
			switch (itemId) {
			case R.id.menu_delete: {
				onMenuCleanCallLog();
				break;
			}
			case R.id.menu_copy: {
				String number = getCallLogCheckedNumber();
				ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setPrimaryClip(ClipData.newPlainText(null, number));
                Toast.makeText(getActivity(), R.string.toast_text_copied, Toast.LENGTH_SHORT).show();
				break;
			}
			case R.id.menu_edit: {
				final String number = getCallLogCheckedNumber();
			    mHandler.postDelayed(new Runnable() {
                    public void run() {   
        				getActivity().startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)));
                    }
                }, 450); 
				switch2NormalMode();
				 Log.v(TAG, "setShowBottomBarMenu2");
				actionBar.setShowBottomBarMenu(false);
				actionBar.showActionBarDashBoard();
				((TextView) (actionBar.getActionBarMenu().getActionMenuRightView())).setText(mSelectAllStr);											
				break;
			}

			default:
				break;
			}
		}
	};
	
	//aurora modify liguangyu 20140410 for #4037 start
	private AuroraAlertDialog mCleanCallLogDialog = null;
	 public void onMenuCleanCallLog() {
		 	mCleanCallLogDialog = new AuroraAlertDialog.Builder(getActivity(), AuroraAlertDialog.THEME_AMIGO_FULLSCREEN)
			.setTitle(R.string.gn_clearSelectedCallLogConfirmation_title)  // gionee xuhz 20120728 modify for CR00658189
			.setMessage(R.string.gn_clearSelectedCallLogConfirmation)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (getAdapter().isAllSelect()) {
						deleteAllCalllogs();
					} else {
						removeCallLogRecord();
					}
					updateMenuItemState(false);
					setBottomMenuEnable(false);
				}
			})
			.setNegativeButton(android.R.string.no, null).create();
		 	mCleanCallLogDialog.show();        
	    }
	//aurora modify liguangyu 20140410 for #4037 end
	
	

	private OnAuroraActionBarItemClickListener auroraActionBarItemClickListener = new OnAuroraActionBarItemClickListener() {
		public void onAuroraActionBarItemClicked(int itemId) {
			switch (itemId) {
			case AURORA_EDIT_CALLLOG:
				if (getAdapter().getCount() != 0) {
					switch2EditMode();
					setBottomMenuEnable(false);
				}
				break;
			default:
				break;
			}
		}
	};

	protected String getCurCallTypeTitle() {
		return CALL_TYPE_TITLE[mCurCallTypePosition];
	}

	protected String getCurCallTypeEmptyMsg() {
		return CALL_TYPE_EMPTY_MSG[mCurCallTypePosition];
	}

	// aurora changes zhouxiaobing 20130925 start
	public int checkHotlinenumber(String number) {
		String[] hotnumbers = mAdapter.getHotlinesNumber();
		String[] hotnames = mAdapter.getHotlinesName();
		if(hotnumbers==null)
			return -1;
		for(int i=0;i<hotnumbers.length;i++) {
			if(number.equalsIgnoreCase(hotnumbers[i])) {
				return i;
			}
		}
		return -1;
	}
	public static GnContactInfo contactInfos=null;
	public void changecursor(Cursor cursor) {
		if (cursor == null) {
		    return;
		}
		if (cursor.moveToFirst()) {
	        int[] hotnumberIndexs=mAdapter.getHotlineIndex(cursor.getCount());
			int i=0;
			do{
				String number = cursor
						.getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_NUMBER);
	            hotnumberIndexs[i]=checkHotlinenumber(number);
	            i++;
			}while(cursor.moveToNext());
		}
		if(cursor.moveToFirst()) {	
			
		    contactInfos=mAdapter.getContactInfo(cursor);
		        
		}
		
		//aurora add liguangyu 20131113 for start
//		if (mUpdateActionBarlistener != null) {
//			mUpdateActionBarlistener.updateActionBar();
//	    }
		updateActionBar();
		//aurora add liguangyu 20131113 for end
	}
	// aurora changes zhouxiaobing 20130925 end
	
	/**
	 * Called by the CallLogQueryHandler when the list of calls has been fetched
	 * or updated.
	 */
	@Override
	public void onCallsFetched(Cursor cursor) {
		log("onCallsFetched(), cursor = " + cursor);
		if (getActivity() == null || getActivity().isFinishing()) {
			return;
		}
		
		mAdapter.setLoading(false);
		mAdapter.changeCursor(cursor);
		mAdapter.createCheckedArray(mAdapter.getRealCount());
		changecursor(cursor);    	
		
		if (mScrollToTop) {
			final AuroraListView listView = (AuroraListView) getListView();
			if (listView.getFirstVisiblePosition() > 5) {
				listView.setSelection(5);
			}
			listView.smoothScrollToPosition(0);
			mScrollToTop = false;
		}
		mCallLogFetched = true;

		Log.i(TAG, "onCallsFetched is call");
		isFinished = true;
		// Gionee:wangth 20130326 modify for CR00786566 begin
		mLoadingContainer.setVisibility(View.GONE);
		mProgress.setVisibility(View.GONE);
		mLoadingTextView.setVisibility(View.GONE);
		if (mAdapter.getCount() <= 0) {
			mEmptyTitle.setText(getCurCallTypeEmptyMsg());
			mEmptyTitle.setVisibility(View.VISIBLE);
		} else {
			mEmptyTitle.setVisibility(View.GONE);
		}
		// Gionee:wangth 20130326 modify for CR00786566 end

		destroyEmptyLoaderIfAllDataFetched();
		//aurora add qiaohu 20141203 for #10247 start 
		if(!isLongClickEnable){
			updateUi();
		}
		//aurora add qiaohu 20141203 for #10247 end
	}

	/**
	 * Called by {@link CallLogQueryHandler} after a successful query to
	 * voicemail status provider.
	 */
	@Override
	public void onVoicemailStatusFetched(Cursor statusCursor) {
		if (getActivity() == null || getActivity().isFinishing()) {
			return;
		}
		updateVoicemailStatusMessage(statusCursor);

		int activeSources = mVoicemailStatusHelper
				.getNumberActivityVoicemailSources(statusCursor);
		setVoicemailSourcesAvailable(activeSources != 0);
		MoreCloseables.closeQuietly(statusCursor);
		mVoicemailStatusFetched = true;
		destroyEmptyLoaderIfAllDataFetched();
	}

	private void destroyEmptyLoaderIfAllDataFetched() {
		if (mCallLogFetched && mVoicemailStatusFetched && mEmptyLoaderRunning) {
			mEmptyLoaderRunning = false;
			getLoaderManager().destroyLoader(EMPTY_LOADER_ID);
		}
	}

	/** Sets whether there are any voicemail sources available in the platform. */
	private void setVoicemailSourcesAvailable(boolean voicemailSourcesAvailable) {
		if (mVoicemailSourcesAvailable == voicemailSourcesAvailable)
			return;
		mVoicemailSourcesAvailable = voicemailSourcesAvailable;

		Activity activity = getActivity();
		if (activity != null) {
			// This is so that the options menu content is updated.
			activity.invalidateOptionsMenu();
		}
	}

    // aurora <ukiliu> <2013-9-27> modify for aurora ui begin
	// aurora change zhouxiaobing 20130912 start
	public boolean getEditMode() {
		return editMode;
	}

	public void setEditMode(boolean is_edit) {
		editMode = is_edit;
		mAdapter.setEditMode(is_edit);
		mAdapter.setIs_listitem_changing(true);
		mAdapter.notifyDataSetChanged();

		if (editMode) {
			if(mIAuroraCallLogFragment != null){
				mIAuroraCallLogFragment.setTabWidget(View.INVISIBLE);
			}
		    getActivity().getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, mContactChangeObserver);
		} else {
			if(mIAuroraCallLogFragment != null){
				mIAuroraCallLogFragment.setTabWidget(View.VISIBLE);
			}
			mAdapter.clearAllcheckes();
		    getActivity().getContentResolver().unregisterContentObserver(mContactChangeObserver);
		}
		if(mFooterView != null && !mIsPrivate) {
			mFooterView.setVisibility(editMode ? View.GONE : View.VISIBLE);
		}
	}
	// aurora change zhouxiaobing 20130912 end
    // aurora <ukiliu> <2013-9-27> modify for aurora ui end
	
	public interface IAuroraCallLogFragment{
		public void setTabWidget(int visible);
	}
	private IAuroraCallLogFragment mIAuroraCallLogFragment;
	public void setIAuroraCallLogFragment(IAuroraCallLogFragment iAuroraCallLogFragment){
		mIAuroraCallLogFragment = iAuroraCallLogFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedState) {

		mHasPermanentMenuKey = ViewConfiguration.get(getActivity())
				.hasPermanentMenuKey();
		View fragmentView = inflater.inflate(
				R.layout.aurora_call_log_fragment_v4, container, false);
        mListView = (AuroraListView)fragmentView.findViewById(android.R.id.list);
        if(GNContactsUtils.isMultiSimEnabled()) {
        	mListView.setDivider(getResources().getDrawable(R.color.aurora_calllog_divider_color));
        	mListView.setDividerHeight(1);
        }            
        
    	Animation animation=AnimationUtils.loadAnimation(getActivity(), R.anim.aurora_alpha_in);
        LayoutAnimationController lac= new LayoutAnimationController(animation);
        lac.setOrder(LayoutAnimationController.ORDER_NORMAL);
        lac.setDelay(0.05f);        
        mListView.setLayoutAnimation(lac);
        
        mFooterView = fragmentView.findViewById(R.id.footer_view);
        if(mIsPrivate) {
			mFooterView.setVisibility(View.GONE);
        }
        
		calllog_delete = (Button) fragmentView
				.findViewById(R.id.aurora_calllog_delete);

		mEmptyTitle = (TextView) fragmentView
				.findViewById(R.id.gn_calllog_empty_tip);
		mLoadingContainer = fragmentView.findViewById(R.id.loading_container);
		mLoadingContainer.setVisibility(View.GONE);
		mLoadingTextView = (TextView) fragmentView
				.findViewById(R.id.loading_contact);
		mLoadingTextView.setVisibility(View.GONE);
		mProgress = (ProgressBar) fragmentView
				.findViewById(R.id.progress_loading_contact);
		mProgress.setVisibility(View.GONE);

		SharedPreferences.Editor editor = AuroraPreferenceManager
				.getDefaultSharedPreferences(this.getActivity()).edit();
		editor.putInt(Constants.TYPE_FILTER_PREF, Constants.FILTER_TYPE_ALL);
		editor.commit();

		return fragmentView;
	}

	//aurora add zhouxiaobing 20130929 start	
	public ArrayList<Integer> getCallLogCheckedId() {
		ArrayList<Integer> als = new ArrayList<Integer>();
		int length = mAdapter.getRealCount();
		Log.d("TAG","all length::"+(length));
		for (int i = 0; i < length; i++) {
			if (mAdapter.getCheckedArrayValue(i)) {
				Cursor cursor = (Cursor) mAdapter.getItem(i);
				GnContactInfo contactInfo = mAdapter.getContactInfo(cursor);
				int count=contactInfo.gnCallsCount;
				Log.d("TAG","count::"+(count));
				for(int j=0;j<contactInfo.gnCallsCount;j++) {
					Log.d("TAG","als == null::"+(als == null));
					Log.d("TAG","contactInfo == null::"+(contactInfo == null));
					if(contactInfo != null && contactInfo.ids != null) {
						Log.d("TAG","contactInfo.ids[j]::"+(contactInfo.ids[j]));
						als.add(Integer.valueOf(contactInfo.ids[j]));
					}
				}
			}
		}
		return als;
	}
	
	private String toDeleteSelectionId(ArrayList<Integer> selected) {
		if (null == selected || selected.size() <= 0) {
			return NO_CALL_LOG_SELECTED;
		}

		int callType = getCurCallType();
		StringBuilder selection = new StringBuilder();
		if (callType > 0) {
			selection.append("type=").append(callType).append(" And ");
		}

		if (ContactsApplication.sIsGnCombineCalllogMatchNumber) {
			selection.append("_id in (");
			for (Integer id : selected) {
				selection.append("'").append(id.intValue()).append("',");
			}

			selection.setLength(selection.length() - 1);
			selection.append(")");
		}

		return selection.toString();
	}
    //aurora add zhouxiaobing 20130929 end
	
    // aurora <ukiliu> <2013-9-27> add for aurora ui begin
	public ArrayList<String> getCallLogChecked() {
		ArrayList<String> als = new ArrayList<String>();
		int length = mAdapter.getRealCount();
		for (int i = 0; i < length; i++) {
			if (mAdapter.getCheckedArrayValue(i)) {
				Cursor cursor = (Cursor) mAdapter.getItem(i);
				GnContactInfo contactInfo = mAdapter.getContactInfo(cursor);
				als.add(contactInfo.number);
			}
		}
		return als;
	}

	public void removeCallLogRecord() {
		//ArrayList<String> als = getCallLogChecked();
		ArrayList<Integer> als = getCallLogCheckedId();
		if (als == null || als.size() == 0) {
			initActionBar(false);
			switch2NormalMode();
			return;
		}
//		initActionBar(false);
		//final String selection = toDeleteSelection(als);
		final String selection = toDeleteSelectionId(als);
		new AsyncTask<Integer, Integer, Integer>() {
			private Context mContext = getActivity();
			private AuroraProgressDialog mProgressDialog;

			protected void onPreExecute() {
				if (getActivity() != null && !getActivity().isFinishing()) {
					mProgressDialog = AuroraProgressDialog.show(mContext, "",
							getString(R.string.deleting_call_log));
				}
			};

			@Override
			protected Integer doInBackground(Integer... params) {
				String privateString = "";
				if (ContactsApplication.sIsAuroraPrivacySupport) {
					privateString = " AND privacy_id > -1 "; 
				} 
				mContext.getContentResolver().delete(Calls.CONTENT_URI,
						selection + privateString , null);
				return null;
			}

			protected void onPostExecute(Integer result) {
				if (getActivity() != null && !getActivity().isFinishing()) {
					mProgressDialog.dismiss();
					initActionBar(false);
					switch2NormalMode();
				}
			};
		}.executeOnExecutor(exec);
		
	}

	private void initActionBar(boolean flag) {
		AuroraActionBar actionBar;
		actionBar = ((AuroraActivity) getActivity()).getAuroraActionBar();
		Log.i("TAG","setShowBottomBarMenu" +flag);
		actionBar.setShowBottomBarMenu(flag);
		//aurora modify liguangyu 20140527 for BUG #5151 start
		if(!flag && !editMode) {
		} else {
			actionBar.showActionBarDashBoard();
		}
		//aurora modify liguangyu 20140527 for BUG #5151 end
	}
    // aurora <ukiliu> <2013-9-27> add for aurora ui end

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mAdapter = new AuroraCallLogAdapterV2(getActivity(), this);
		mAdapter.setOnItemClickListener(this);
		mAdapter.setPrivate(mIsPrivate);	
		refreshData();

		final AuroraListView listView = (AuroraListView) getListView();
		if (null != listView) {
			mAdapter.setListView(listView);
			listView.setHeaderDividersEnabled(false);
			listView.setItemsCanFocus(true);
			setListAdapter(mAdapter);
			listView.auroraSetNeedSlideDelete(true);
			listView.setOnScrollListener(mAdapter);
			// listView.setOnCreateContextMenuListener(this);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			
			// aurora ukiliu 2013-10-15 add for aurora UI begin
			listView.auroraSetAuroraBackOnClickListener(
					new AuroraListView.AuroraBackOnClickListener() {
						@Override
						public void auroraOnClick(final int position) {
							mListView.auroraDeleteSelectedItemAnim();
						}
						 
					 	@Override
	                    public void auroraPrepareDraged(int position) {
					 	   if (getListView() != null && getListView().getChildAt(position) != null) {
                               ImageView iv = (ImageView)getListView().getChildAt(position).findViewById(R.id.aurora_secondary_action_icon);
                               if (iv != null) {
                                   iv.setVisibility(View.GONE);
                               }
                           }
					 	}
						
						@Override
	                    public void auroraDragedSuccess(int position) {
							//aurora modify liguangyu 20140419 for BUG #4415 start
							try {
							    if (getListView() != null && getListView().getChildAt(position) != null) {
	                                ImageView iv = (ImageView)getListView().getChildAt(position).findViewById(R.id.aurora_secondary_action_icon);
	                                if (iv != null) {
	                                    iv.setVisibility(View.GONE);
	                                }
	                            }
							} catch (Exception e) {
								e.printStackTrace();
							}
							//aurora modify liguangyu 20140419 for BUG #4415 end
						}
						
						@Override
	                    public void auroraDragedUnSuccess(int position) {
							// aurora ukiliu 2013-10-17 modify for aurora ui begin
							if (getListView() != null && getListView().getChildAt(position) != null) {
							    ImageView iv = (ImageView)getListView().getChildAt(position).findViewById(R.id.aurora_secondary_action_icon);
							    //aurora change liguangyu 20131108 for BUG #511 start
							    if (iv != null && isLongClickEnable) {
							    //aurora change liguangyu 20131108 for BUG #511 end
							        iv.setVisibility(View.VISIBLE);
							    }
							}
							// aurora ukiliu 2013-10-17 modify for aurora ui end
						}
						
						
					});
			// aurora ukiliu 2013-10-15 add for aurora UI end
			
			
			//aurora add liguangyu 20140409 for AuroraListView SlideDelete start
			listView.auroraSetDeleteItemListener(
					new AuroraListView.AuroraDeleteItemListener() { 						
						public void auroraDeleteItem(View v,int position){
							final int pos = position;
							Log.i("TAG","listview SlideDelete pos::"+pos);
							final StringBuilder callIds = new StringBuilder();
							GnContactInfo mContactInfo = getContactInfo(position);
							if (mContactInfo == null || mContactInfo.ids == null) {
							    return;
							}
							
							for (int callUri : mContactInfo.ids) {
								if (callIds.length() != 0) {
									callIds.append(",");
								}
								callIds.append(callUri);
							}
		
							String privateString = "";
							if (ContactsApplication.sIsAuroraPrivacySupport) {
								privateString = " AND privacy_id > -1 "; 
							} 
							getActivity().getContentResolver().delete(
									Calls.CONTENT_URI,
									Calls._ID + " IN (" + callIds + ")" + privateString, null);
						}
					});
			//aurora add liguangyu 20140409 for AuroraListView SlideDelete end
		}
	}

	@Override
	public void onStart() {
		mScrollToTop = false;//mHadGoneCallLogDetail ? false : true;//aurora change zhouxiaobing 20140612 because we need not scroll to top

		// Start the empty loader now to defer other fragments. We destroy it
		// when both calllog
		// and the voicemail status are fetched.
		getLoaderManager().initLoader(EMPTY_LOADER_ID, null,
				new EmptyLoader.Callback(getActivity()));
		mEmptyLoaderRunning = true;
		super.onStart();
	}

	@Override
	public void onResume() {
		log("onResume");
		super.onResume();
		
		PhoneNumberHelper.getVoiceMailNumber();
		//aurora modify liguangyu 20140808 for BUG #7384 start
		if(!editMode) {
			refreshData();
		}
		//aurora modify liguangyu 20140808 for BUG #7384 end
		updateOnEntry();

		if (null != mAdapter) {
			mAdapter.refreshSimColor();
		}
		final AuroraListView listView = (AuroraListView) getListView();
		listView.auroraOnResume(); 
	}

	private void updateVoicemailStatusMessage(Cursor statusCursor) {
		List<StatusMessage> messages = mVoicemailStatusHelper
				.getStatusMessages(statusCursor);
		if (messages.size() == 0) {
			mStatusMessageView.setVisibility(View.GONE);
		} else {
			mStatusMessageView.setVisibility(View.VISIBLE);
			// TODO: Change the code to show all messages. For now just pick the
			// first message.
			final StatusMessage message = messages.get(0);
			if (message.showInCallLog()) {
				mStatusMessageText.setText(message.callLogMessageId);
			}
			if (message.actionMessageId != -1) {
				mStatusMessageAction.setText(message.actionMessageId);
			}
			if (message.actionUri != null) {
				mStatusMessageAction.setVisibility(View.VISIBLE);
				mStatusMessageAction
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								getActivity().startActivity(
										new Intent(Intent.ACTION_VIEW,
												message.actionUri));
							}
						});
			} else {
				mStatusMessageAction.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		final AuroraListView listView = (AuroraListView) getListView();
		listView.auroraOnPause(); //aurora add zhouxiaobing 20131218
	}

	@Override
	public void onStop() {
		super.onStop();
//		updateOnExit();

		if (null != mClearCallLogDialog && mClearCallLogDialog.isShowing()) {
			mClearCallLogDialog.dismiss();
		}
		if(mCleanCallLogDialog != null){
			mCleanCallLogDialog.dismiss();
		}
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mAdapter.changeCursor(null);
		if (getActivity() != null) {
			AuroraSubInfoNotifier.getInstance().removeListener(this);
	        getActivity().getContentResolver().unregisterContentObserver(mContactChangeObserver);
		}
	}

	@Override
	public void fetchCalls() {
		if (mShowingVoicemailOnly) {
			mCallLogQueryHandler.fetchVoicemailOnly();
		} else {
			/**
			 * Change Feature by Mediatek Begin. Original Android's Code:
			 * mCallLogQueryHandler.fetchAllCalls(); Descriptions:
			 */
			Activity activity = this.getActivity();
			if (activity == null) {
				Log.e(TAG,
						" fetchCalls(), but this.getActivity() is null, use default value");
				fetchCallsJionDataView(Constants.FILTER_SIM_DEFAULT,
						Constants.FILTER_TYPE_DEFAULT,mAdapter);
			} else {
				SharedPreferences prefs = AuroraPreferenceManager
						.getDefaultSharedPreferences(this.getActivity());
				int simFilter = prefs.getInt(Constants.SIM_FILTER_PREF,
						Constants.FILTER_SIM_DEFAULT);
				int typeFilter = prefs.getInt(Constants.TYPE_FILTER_PREF,
						Constants.FILTER_TYPE_DEFAULT);
				fetchCallsJionDataView(simFilter, typeFilter, mAdapter);
			}
			/**
			 * Change Feature by Mediatek End.
			 */
		}
	}

	public void startCallsQuery() {
		log("startCallsQuery()");
		Log.v(TAG, "startCallsQuery tiem="+System.currentTimeMillis());
		mAdapter.setLoading(true);
		/**
		 * Change Feature by Mediatek Begin. Original Android's Code:
		 * mCallLogQueryHandler.fetchAllCalls(); Descriptions:
		 */
		SharedPreferences prefs = AuroraPreferenceManager
				.getDefaultSharedPreferences(this.getActivity());
		// gionee xuhz 20120919 modify for CR00696560 start
		final int simFilter = prefs.getInt(Constants.SIM_FILTER_PREF,
				Constants.FILTER_SIM_DEFAULT);
		final int typeFilter = prefs.getInt(Constants.TYPE_FILTER_PREF,
				Constants.FILTER_TYPE_DEFAULT);
	
		mCallLogQueryHandler.postDelayed(new Runnable() {
			public void run() {
				fetchCallsJionDataView(simFilter, typeFilter, mAdapter);
			}
		}, 100);//old is 500, aurora zhouxiaobing changes 
		// gionee xuhz 20120919 modify for CR00696560 end

		/**
		 * Change Feature by Mediatek End.
		 */
		if (mShowingVoicemailOnly) {
			mShowingVoicemailOnly = false;
			getActivity().invalidateOptionsMenu();
		}
		/*
		 * Bug Fix by Mediatek Begin. Original Android's code: CR ID:
		 * ALPS00115673 Descriptions: add wait cursor
		 */
		int i = this.getListView().getCount();

		Log.i(TAG, "***********************i : " + i);
		isFinished = false;
		if (i == 0) {
			Log.i(TAG, "call sendmessage");
			mHandler.sendMessageDelayed(
					mHandler.obtainMessage(WAIT_CURSOR_START),
					WAIT_CURSOR_DELAY_TIME);

		}
		/*
		 * Bug Fix by Mediatek End.
		 */
	}

	private void startVoicemailStatusQuery() {
		log("startVoicemailStatusQuery()");
		mCallLogQueryHandler.fetchVoicemailStatus();
	}


	private void changeCalllogType(int filterType, boolean voicemailOnly) {
		SharedPreferences.Editor editor = AuroraPreferenceManager
				.getDefaultSharedPreferences(ContactsApplication.getInstance())
				.edit();

		editor.putInt(Constants.TYPE_FILTER_PREF, filterType);
		editor.commit();
		refreshData();
		mShowingVoicemailOnly = voicemailOnly;
	}

	public void callSelectedEntry() {
		log("callSelectedEntry()");
		int position = getListView().getSelectedItemPosition();
		if (position < 0) {
			// In touch mode you may often not have something selected, so
			// just call the first entry to make sure that [send] [send] calls
			// the
			// most recent entry.
			position = 0;
		}
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		if (cursor != null) {
			String number = cursor.getString(CallLogQuery.NUMBER);
			if (TextUtils.isEmpty(number)
					|| number.equals(UNKNOWN_NUMBER)//aurora change zhouxiaobing 20140512 for4.4,old is/CallerInfo.UNKNOWN_NUMBER
					|| number.equals(PRIVATE_NUMBER)
					|| number.equals(PAYPHONE_NUMBER)) {
				// This number can't be called, do nothing
				return;
			}
			Intent intent;
			// If "number" is really a SIP address, construct a sip: URI.
			if (PhoneNumberUtils.isUriNumber(number)) {
				intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
						Uri.fromParts("sip", number, null));
				intent.setClassName(Constants.PHONE_PACKAGE,
						Constants.OUTGOING_CALL_BROADCASTER);
			} else {
				// We're calling a regular PSTN phone number.
				// Construct a tel: URI, but do some other possible cleanup
				// first.
				int callType = cursor.getInt(CallLogQuery.CALL_TYPE);
				if (!number.startsWith("+")
						&& (callType == Calls.INCOMING_TYPE || callType == Calls.MISSED_TYPE)) {
					// If the caller-id matches a contact with a better
					// qualified number, use it
					String countryIso = cursor
							.getString(CallLogQuery.COUNTRY_ISO);
					number = mAdapter.getBetterNumberFromContacts(number,
							countryIso);
				}

				intent = IntentFactory.newDialNumberIntent(number);
				intent.setClassName(Constants.PHONE_PACKAGE,
						Constants.OUTGOING_CALL_BROADCASTER);
			}
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			startActivity(intent);
		}
	}

	public AuroraCallLogAdapterV2 getAdapter() {
		return mAdapter;
	}

	/** Requests updates to the data to be shown. */
	private void refreshData() {
		// Mark all entries in the contact info cache as out of date, so they
		// will be looked up
		// again once being shown.
		log("refreshData()");
		startCallsQuery();
		// Deleted by Mediatek Inc to close Google default Voicemail function.
//		updateOnEntry();
	}

	/** Updates call data and notification state while leaving the call log tab. */
	private void updateOnExit() {
		updateOnTransition(false);
	}

	/**
	 * Updates call data and notification state while entering the call log tab.
	 */
	private void updateOnEntry() {
		updateOnTransition(true);
	}

	private void updateOnTransition(boolean onEntry) {
		log("updateOnTransition onEntry = " + onEntry);
		// We don't want to update any call data when keyguard is on because the
		// user has likely not
		// seen the new calls yet.
//		if (!mKeyguardManager.inKeyguardRestrictedInputMode()) {
			// On either of the transitions we reset the new flag and update the
			// notifications.
			// While exiting we additionally consume all missed calls (by
			// marking them as read).
			// This will ensure that they no more appear in the "new" section
			// when we return back.
			mCallLogQueryHandler.markNewCallsAsOld();
//			if (!onEntry) {
				mCallLogQueryHandler.markMissedCallsAsRead();
//			}

			CallLogNotificationsHelper.removeMissedCallNotifications(getActivity());
			// updateVoicemailNotifications();
//		}
	}

	private void updateVoicemailNotifications() {
		Intent serviceIntent = new Intent(getActivity(),
				CallLogNotificationsService.class);
		serviceIntent
				.setAction(CallLogNotificationsService.ACTION_UPDATE_NOTIFICATIONS);
		getActivity().startService(serviceIntent);
	}

	// The following lines are provided and maintained by Mediatek Inc.

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			log("handleMessage msg==== " + msg.what);

			switch (msg.what) {

			case WAIT_CURSOR_START:
				Log.i(TAG, "start WAIT_CURSOR_START !isFinished : "
						+ !isFinished);
				if (!isFinished) {
					// Gionee:wangth 20130326 modify for CR00786566 begin
					if (mNeedShowProgress) {
						mLoadingContainer.setVisibility(View.VISIBLE);
						mProgress.setVisibility(View.VISIBLE);
						mLoadingTextView.setVisibility(View.VISIBLE);
						mNeedShowProgress = false;
					}
					// Gionee:wangth 20130326 modify for CR00786566 end
					mEmptyTitle.setVisibility(View.GONE);
				}
				break;

			default:
				break;
			}
		}
	};

	/**
	 * Called by the CallLogQueryHandler when the list of calls has been
	 * deleted.
	 */
	@Override
	public void onCallsDeleted() {
		log("onCallsDeleted(), do nothing");
	}


	private void log(final String log) {
		Log.i(TAG, log);
	}

	private ProgressBar mProgress;
	// Gionee:wangth 20130326 add for CR00786566 begin
	private View mLoadingContainer;
	private TextView mLoadingTextView;
	private boolean mNeedShowProgress = true;
	// Gionee:wangth 20130326 add for CR00786566 end

	private TextView mEmptyTitle;
	 
	private static boolean isFinished = false;
	private static final int WAIT_CURSOR_START = 1230;
	private static final long WAIT_CURSOR_DELAY_TIME = 500;

	private boolean mHasPermanentMenuKey;
	 
	private final int MATCH_CONTACTS_NUMBER_LENGTH = ContactsApplication.GN_MATCH_CONTACTS_NUMBER_LENGTH;
	private final String NO_CALL_LOG_SELECTED = "";

	private String toDeleteSelection(String number) {
		if (TextUtils.isEmpty(number)) {
			return NO_CALL_LOG_SELECTED;
		}

		int callType = getCurCallType();
		StringBuilder selection = new StringBuilder();
		if (callType > 0) {
			selection.append("type=").append(callType).append(" And ");
		}

		if (!ContactsApplication.sIsGnCombineCalllogMatchNumber
				|| number.length() < MATCH_CONTACTS_NUMBER_LENGTH) {
			selection.append("number ='").append(number).append('\'');
		} else {
			selection.append("number LIKE '%").append(number).append('\'');
		}

		return selection.toString();
	}

	private String toDeleteSelection(ArrayList<String> selected) {
		if (null == selected || selected.size() <= 0) {
			return NO_CALL_LOG_SELECTED;
		}

		int callType = getCurCallType();
		StringBuilder selection = new StringBuilder();
		if (callType > 0) {
			selection.append("type=").append(callType).append(" And ");
		}

		if (!ContactsApplication.sIsGnCombineCalllogMatchNumber) {
			selection.append("number in (");
			for (String number : selected) {
				selection.append("'").append(number).append("',");
			}

			selection.setLength(selection.length() - 1);
			selection.append(")");
		} else {
			selection.append("(").append(Calls.NUMBER).append(" in (");
			int shorterCount = 0;
			for (String number : selected) {
				int numLen = number.length();
				if (numLen < MATCH_CONTACTS_NUMBER_LENGTH) {
					++shorterCount;
					selection.append("'").append(number).append("',");
				}
			}

			if (shorterCount > 0) {
				selection.setLength(selection.length() - 1);
			} else {
				selection.setLength(0);
				selection.append("(");
			}

			if (selected.size() - shorterCount > 0) {
				selection.append(") OR ");

				for (String info : selected) {
					int numLen = info.length();
					if (numLen >= MATCH_CONTACTS_NUMBER_LENGTH) {
						selection
								.append(Calls.NUMBER)
								.append(" LIKE '%")
								.append(info.substring(numLen
										- MATCH_CONTACTS_NUMBER_LENGTH, numLen))
								.append("' OR ");
					}
				}

				selection.setLength(selection.length() - 4);
			}

			selection.append(")");
		}

		return selection.toString();
	}

	private AuroraAlertDialog mClearCallLogDialog;

	private int getCurFilterType() {
		SharedPreferences prefs = AuroraPreferenceManager
				.getDefaultSharedPreferences(this.getActivity());
		return prefs.getInt(Constants.TYPE_FILTER_PREF,
				Constants.FILTER_TYPE_DEFAULT);
	}

	private int getCurCallType() {
		return getCallsType(getCurFilterType());
	}


	// gionee xuhz 20120517 modify for CR00601200 CR00601202 start
	private int getCallsType(int filterType) {
		switch (filterType) {
		case Constants.FILTER_TYPE_ALL:
			return 0;
		case Constants.FILTER_TYPE_OUTGOING:
			return Calls.OUTGOING_TYPE;
		case Constants.FILTER_TYPE_INCOMING:
			return Calls.INCOMING_TYPE;
		case Constants.FILTER_TYPE_MISSED:
			return Calls.MISSED_TYPE;
		default:
			return 0;
		}
	}

	// gionee xuhz 20120517 modify for CR00601200 CR00601202 end


	private int mCurCallTypePosition;


	private GnContactInfo getContactInfo(int position) {
		if (null != mAdapter) {
			Cursor cursor = mAdapter.getCursor();
			if (null != cursor) {
				int cp = cursor.getPosition();
				//aurora change liguangyu 20131108 for BUG #419 start
				if (cursor.moveToPosition(position)) {
				//aurora change liguangyu 20131108 for BUG #419 end
					GnContactInfo info = mAdapter.getContactInfo(cursor);
					cursor.moveToPosition(cp);
					return info;
				}
			}
		}
		return null;
	}


	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Log.v(TAG, "onItemClick start");
		
		//aurora add liguangyu 20140613 for BUG #5519 start
		if (isFastClick()) {   
			return;
		}
		//aurora add liguangyu 20140613 for BUG #5519 end

		RelativeLayout mainUi;
		AuroraCallLogListItemViewV2 cliv = null;

		mainUi = (RelativeLayout)view.findViewById(com.aurora.R.id.aurora_listview_front);
		if (mainUi != null) {

			View frontView = mainUi.getChildAt(0);
			if (frontView != null && frontView instanceof AuroraCallLogListItemViewV2) {
				cliv = (AuroraCallLogListItemViewV2)frontView;

			}
		}

		
		if (cliv == null) {
			Context context = this.getActivity();
			Object obj = null;
			GnContactInfo contactInfo = null;
			boolean isCallPrimary = ResConstant.isCallLogListItemCallPrimary();
			boolean isPrimaryClick = false;
			
			obj = view.getTag();
			isPrimaryClick = true;


			if (null != obj && obj instanceof GnContactInfo) {
				contactInfo = (GnContactInfo) obj;
			}

			if (null == contactInfo) {
				return;
			}
			
			if (null != contactInfo) {
				if(ContactsApplication.sIsAuroraYuloreSupport){
					if(YuloreUtils.getInstance(getActivity()).getName(contactInfo.number)!=null){
						YuloreUtils.getInstance(context).startActivity(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP, "yulore.view", null, "yulore://viewDetail?tel="+contactInfo.number+"&title=通话详情", "yulore/detail_call", null, null);
					}else{
						// detail
						int filterType = getCurFilterType();
						int callsType = getCallsType(filterType);
						// aurora changes zhouxiaobing 20130925
						/*
						 * Intent intent =
						 * IntentFactory.newShowCallDetailIntent(context,
						 * contactInfo.number, callsType, contactInfo.callId,
						 * contactInfo.gnCallsCount, contactInfo.voicemailUri);
						 */
						Intent intent = IntentFactory.newShowCallDetailIntent(context,
								contactInfo.number, callsType, contactInfo.callId,
								contactInfo.gnCallsCount, contactInfo.voicemailUri,
								contactInfo.ids);
						intent.putExtra("contact_sim_id", contactInfo.contactSimId);
						// aurora changes zhouxiaobing 20130925

						context.startActivity(intent);
					}
				}else{
					// detail
					int filterType = getCurFilterType();
					int callsType = getCallsType(filterType);
					// aurora changes zhouxiaobing 20130925
					/*
					 * Intent intent =
					 * IntentFactory.newShowCallDetailIntent(context,
					 * contactInfo.number, callsType, contactInfo.callId,
					 * contactInfo.gnCallsCount, contactInfo.voicemailUri);
					 */
					Intent intent = IntentFactory.newShowCallDetailIntent(context,
							contactInfo.number, callsType, contactInfo.callId,
							contactInfo.gnCallsCount, contactInfo.voicemailUri,
							contactInfo.ids);
					intent.putExtra("contact_sim_id", contactInfo.contactSimId);
					// aurora changes zhouxiaobing 20130925

					context.startActivity(intent);
				}
				
			}
		} else if (cliv != null) {
			Log.v(TAG, "auroraOnItemClick 1position=" + position);
			// aurora changes zhouxiaobing 20130912
            // aurora <ukiliu> <2013-9-27> modify for aurora ui begin
			if (editMode) {
				boolean checked = cliv.mCheckBox
						.isChecked();
//				cliv.mCheckBox
//						.setChecked(!checked);
				cliv.mCheckBox.auroraSetChecked(!checked, true);
				mAdapter.setCheckedArrayValue(position, !checked);

				if (!checked) {
					if (mAdapter.isAllSelect()) {
						updateMenuItemState(true);
					} else {
						setBottomMenuEnable(true);
					}
				} else {
					updateMenuItemState(false);
				}
				return;
			}
			
			Log.v(TAG, "auroraOnItemClick 2position=" + position);
			Context context = this.getActivity();
			GnContactInfo contactInfo = null;
			boolean isCallPrimary = ResConstant.isCallLogListItemCallPrimary();
			boolean isPrimaryClick = false;
			{
				Object obj = null;

				obj = cliv.mSecondaryButton
						.getTag();
				isPrimaryClick = true;


				if (null != obj && obj instanceof GnContactInfo) {
					contactInfo = (GnContactInfo) obj;
				}

				if (null == contactInfo) {
					return;
				}
			}

			if (null != contactInfo) {
				// if ((isPrimaryClick && isCallPrimary) || ((!isPrimaryClick &&
				// !isCallPrimary))) {
				if (!((isPrimaryClick && isCallPrimary) || ((!isPrimaryClick && !isCallPrimary)))) {// aurora
																									// change
																									// zhouxiaobing
					
					//aurora change liguangyu 20140305 for bug #2805 start
					int number_id = 0;
			        if ("-1".equals(contactInfo.number) || TextUtils.isEmpty(contactInfo.number)) {
			        	number_id = R.string.unknown;
			        } else if ("-2".equals(contactInfo.number)) {
			        	number_id = R.string.private_num;
			        } else if ("-3".equals(contactInfo.number)) {
			        	number_id = R.string.payphone;
			        }
					// Gionee <huangzy> <2013-06-09> add for CR00822712 begin
//					if ("-1".equals(contactInfo.number)) {
			        if(number_id != 0) {
						Toast.makeText(context,
//								context.getString(R.string.gn_hidden_number),
								context.getString(number_id),								
								Toast.LENGTH_SHORT).show();
						return;
					}
					// Gionee <huangzy> <2013-06-09> add for CR00822712 end
					//aurora change liguangyu 20140305 for bug #2805 end

					// call
					//aurora change liguangyu 20131206 start
					Intent intent = IntentFactory
							.newDialNumberIntent(contactInfo.number);
	            	intent.putExtra("contactUri", contactInfo.lookupUri);
	                if(GNContactsUtils.isMultiSimEnabled()) {
	                	int slot = ContactsUtils.getSlotBySubId(contactInfo.simId);
	                	if(!(slot==0||slot==1))
	                	{
	                		if(AuroraITelephony.isSimInsert(null,1))
	                		{
	                			slot=1;
	                		}
	                		else
	                		{
	                			slot=0;
	                		}
	                	}
	            		intent.putExtra(ContactsUtils.getSlotExtraKey(), slot);
	                }
					context.startActivity(intent);
					//aurora change liguangyu 20131206 end
					
				} else {
					// detail
					int filterType = getCurFilterType();
					int callsType = getCallsType(filterType);
					// aurora changes zhouxiaobing 20130925
					/*
					 * Intent intent =
					 * IntentFactory.newShowCallDetailIntent(context,
					 * contactInfo.number, callsType, contactInfo.callId,
					 * contactInfo.gnCallsCount, contactInfo.voicemailUri);
					 */
					Intent intent = IntentFactory.newShowCallDetailIntent(context,
							contactInfo.number, callsType, contactInfo.callId,
							contactInfo.gnCallsCount, contactInfo.voicemailUri,
							contactInfo.ids);
					// aurora changes zhouxiaobing 20130925

					context.startActivity(intent);
				}
			}
				
		}
		
	}
	
	//aurora add liguangyu 20140613 for BUG #5519 start
	private long lastClickTime; 
	 public boolean isFastClick() { 
	        long time = System.currentTimeMillis(); 
	        long timeD = time - lastClickTime; 
	        if ( 0 < timeD && timeD < 500) {    
	            return true;    
	        }    
	        lastClickTime = time;    
	        return false;    
	    } 
	//aurora add liguangyu 20140613 for BUG #5519 end

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
        // aurora <ukiliu> <2013-9-27> modify for aurora ui begin
		if (!isLongClickEnable) {
			return false;
		}
		//aurora add liguangyu 20131107 start
		RelativeLayout mainUi;
		AuroraCallLogListItemViewV2 cliv = null;
		mainUi = (RelativeLayout)view.findViewById(com.aurora.R.id.aurora_listview_front);
		if (mainUi != null) {
			View frontView = mainUi.getChildAt(0);
			if (frontView != null && frontView instanceof AuroraCallLogListItemViewV2) {
				cliv = (AuroraCallLogListItemViewV2)frontView;
			}
		}
		if(cliv != null) {
			boolean checked = false;
//			cliv.mCheckBox.setChecked(!checked);
			cliv.mCheckBox.auroraSetChecked(!checked, true);
			mAdapter.setCheckedArrayValue(position, !checked);			
			if (mAdapter.isAllSelect()) {
				updateMenuItemState(true);
			}			
		}
	    //aurora add liguangyu 20131107 start
		switch2EditMode();
		setBottomMenuEnable(true);
        // aurora <ukiliu> <2013-9-27> modify for aurora ui end

		return false;
	}
    // aurora <ukiliu> <2013-9-27> add for aurora ui begin
	public void switch2NormalMode() {
		((AuroraActivity) getActivity()).setMenuEnable(true);
		((AuroraListView)getListView()).auroraSetNeedSlideDelete(true);
		mListView.auroraEnableSelector(true);
		isLongClickEnable = true;
		Log.i(TAG,"mAdapter.getCount() == "+mAdapter.getCount());
		setEditMode(false);
	}

	public void switch2EditMode() {
		((AuroraActivity) getActivity()).setMenuEnable(false);	
		//aurora change liguangyu 20131108 for BUG #511 start
	    try {
            boolean deleteIsShow = mListView.auroraIsRubbishOut();
            if (deleteIsShow) {
            	getAuroraListView().auroraSetRubbishBack();
                mHandler.postDelayed(new Runnable() {
                    public void run() {   
                		isLongClickEnable = false;
                		initActionBar(true);
                		setEditMode(true);
                		mListView.auroraSetNeedSlideDelete(false);
                    	mListView.auroraEnableSelector(false);
                    }
                }, deleteIsShow ? 450 : 0); 
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
		isLongClickEnable = false;
		initActionBar(true);
		setEditMode(true);
		mListView.auroraSetNeedSlideDelete(false);
		mListView.auroraEnableSelector(false);
	    //aurora change liguangyu 20131108 for BUG #511 end
	}
    // aurora <ukiliu> <2013-9-27> add for aurora ui end
	
    // aurora <ukiliu> <2013-9-27> add for aurora ui begin
	public void onSelectAll(boolean check) {
		AuroraCallLogAdapterV2 mAdapter = getAdapter();
		if (mAdapter != null && mAdapter.getCount() > 0) {
//			mAdapter.setAllSelect(check);
			mAdapter.createCheckedArray(mAdapter.getRealCount());			
			mAdapter.setAllSelect(check);
			//aurora add liguangyu 20140331 for bug 3948 start
			mAdapter.setAllSelectFlag(true);
			//aurora add liguangyu 20140331 for bug 3948 end
			mAdapter.notifyDataSetChanged();
		}
	}
    // aurora <ukiliu> <2013-9-27> add for aurora ui end
	public void deleteAllCalllogs(){
		new AsyncTask<Integer, Integer, Integer>() {
			private Context mContext = getActivity();
			private AuroraProgressDialog mProgressDialog;

			protected void onPreExecute() {
				// Gionee <wangth><2013-04-16> modify for CR00796966 begin

				if (getActivity() != null && !getActivity().isFinishing()) {
					mProgressDialog = AuroraProgressDialog.show(mContext, "",
							getString(R.string.deleting_call_log));
				}
				// Gionee <wangth><2013-04-16> modify for CR00796966 end
			};

			@Override
			protected Integer doInBackground(Integer... params) {
				String privateString = null;
				if (ContactsApplication.sIsAuroraPrivacySupport) {
					if(mIsPrivate) {
						privateString = " privacy_id = " + AuroraPrivacyUtils.getCurrentAccountId();
					} else {
						privateString = " privacy_id = " + AuroraPrivacyUtils.getCurrentAccountId() + " or privacy_id = 0";
					}
				} 
				mContext.getContentResolver().delete(Calls.CONTENT_URI,
						privateString, null);
				return null;
			}

			protected void onPostExecute(Integer result) {
				// Gionee <wangth><2013-04-16> modify for CR00796966 begin
				if (getActivity() != null && !getActivity().isFinishing()) {
					mProgressDialog.dismiss();
					initActionBar(false);
					switch2NormalMode();
				}
				// Gionee <wangth><2013-04-16> modify for CR00796966 end
			};
		}.executeOnExecutor(exec);
	}
	
	//aurora add liguangyu 20131108 for BUG #508 start
	private AuroraListView mListView;
    public AuroraListView getAuroraListView() {
        return mListView;
    }
    //aurora add liguangyu 20131108 for BUG #508 end

    //aurora add liguangyu 20131113 for start
//    public interface updateActionBarListener {
//        void updateActionBar();
//    }
    
//    private updateActionBarListener mUpdateActionBarlistener = null;
        
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//        	mUpdateActionBarlistener = (updateActionBarListener) activity;
//         } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString() + " must implement mUpdateActionBarlistener");
//        }
//    }
    //aurora add liguangyu 20131113 for end
    
    public String getCallLogCheckedNumber() {
		ArrayList<Integer> als = new ArrayList<Integer>();
		int length = mAdapter.getRealCount();
		Log.d("TAG","all length::"+(length));
		for (int i = 0; i < length; i++) {
			if (mAdapter.getCheckedArrayValue(i)) {
				Cursor cursor = (Cursor) mAdapter.getItem(i);
				GnContactInfo contactInfo = mAdapter.getContactInfo(cursor);
				return contactInfo.number;
			}
		}
		return "";
	}
    
    private ContactChangeObserver mContactChangeObserver;

    private class ContactChangeObserver extends ContentObserver {
        public ContactChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.i(TAG, "onChange");
    		if (getActivity() == null || getActivity().isFinishing() || !editMode) {
    			return;
    		}
            refreshData();
        }
    }
    

     
    private View mFooterView;
    
    public void animationAfterSetTab() {
		Animation in =AnimationUtils.loadAnimation(getActivity(), R.anim.aurora_alpha_in);
//		this.getContentView().startAnimation(in);
		((AuroraActivity)getActivity()).getAuroraActionBar().startAnimation(in);
		Animation viewIn =AnimationUtils.loadAnimation(getActivity(), R.anim.aurora_alpha_in);
		viewIn.setFillAfter(true);
		getView().startAnimation(viewIn);
        mListView.startLayoutAnimation();
    }
    
    public void animationBeforeSetTab() {
		Animation out =AnimationUtils.loadAnimation(getActivity(), R.anim.aurora_alpha_out);
		Animation viewOut =AnimationUtils.loadAnimation(getActivity(), R.anim.aurora_alpha_out);
		((AuroraActivity)getActivity()).getAuroraActionBar().startAnimation(out);
		viewOut.setFillAfter(true);
		getView().startAnimation(viewOut);
    }
    
  
    
	private boolean mIsPrivate = false;
	
	//aurora add liguangyu 20150127 for #11359 start
	void fetchCallsJionDataView(int simFilter, int typeFilter,AuroraCallLogAdapterV2 adapter) {
		mCallLogQueryHandler.fetchCallsJionDataView(simFilter, typeFilter, adapter, mIsPrivate);
	}
	//aurora add liguangyu 20150127 for #11359 end

	public void setPrivate(boolean isPrivate) {
		mIsPrivate = isPrivate;
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        AuroraSubInfoNotifier.getInstance().addListener(this);
    }
    
    
    public void onSubscriptionsChanged() {
    	if (null != mAdapter) {
			mAdapter.notifyDataSetChanged();
		}
    }
	

}
