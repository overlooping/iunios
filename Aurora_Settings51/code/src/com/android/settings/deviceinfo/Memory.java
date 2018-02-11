/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
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

package com.android.settings.deviceinfo;
import com.mediatek.settings.FeatureOption;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import aurora.preference.AuroraPreference;
import aurora.preference.AuroraPreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.google.android.collect.Lists;
import com.mediatek.settings.deviceinfo.MemoryExts;
import com.android.settings.AuroraSettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.os.Handler;
import android.widget.BaseAdapter;
import com.android.settings.MediaFormat;
/**
 * Panel showing storage usage on disk for known {@link StorageVolume} returned
 * by {@link StorageManager}. Calculates and displays usage of data types.
 */
public class Memory extends AuroraSettingsPreferenceFragment implements Indexable {
    private static final String TAG = "MemorySettings";

    private static final boolean DEGUG = false;
    private static final String TAG_CONFIRM_CLEAR_CACHE = "confirmClearCache";

    private static final int DLG_CONFIRM_UNMOUNT = 1;
    private static final int DLG_ERROR_UNMOUNT = 2;
    
    private static final String KEY_FOR_OTG = "";
    
    private static final int MSG_ADD_OTG = 0;
    
    private static final int MSG_REMOVE_OTG = 1;
    
    private static final int MSG_UPDATE_STORAGE_SIZE = 2;

    // The mountToggle AuroraPreference that has last been clicked.
    // Assumes no two successive unmount event on 2 different volumes are performed before the first
    // one's AuroraPreference is disabled
    private static AuroraPreference sLastClickedMountToggle;
    private static String sClickedMountPoint;

    // Access using getMountService()
    private IMountService mMountService;
    private StorageManager mStorageManager;
    private UsbManager mUsbManager;

    private ArrayList<StorageVolumePreferenceCategory> mCategories = Lists.newArrayList();

    private StorageVolumePreferenceCategory mVolumePrefCategory;
    private MemoryExts mMemoryExts;

    private Handler mHandler = new Handler(){
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what) {
			case MSG_ADD_OTG:
				addPhysicalCategory((String)msg.obj);
				break;
			case MSG_REMOVE_OTG:
				removePhysicalCategory((String)msg.obj);
				break;

			case MSG_UPDATE_STORAGE_SIZE:
				  mMemoryExts.updateMtkCategory();

			        updateData();
			        
			        for (StorageVolumePreferenceCategory category : mCategories) {
			            category.onResume();
			        }
			        
				break;
			default:
				break;
			}
    	};
    	
    };
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mStorageManager = StorageManager.from(context);
        mStorageManager.registerListener(mStorageListener);

        addPreferencesFromResource(R.xml.device_info_memory);

        mMemoryExts = new MemoryExts(getActivity(), getPreferenceScreen(), mStorageManager);

        final StorageVolume[] storageVolumes = mMemoryExts.getVolumeList();

        mMemoryExts.initMtkCategory();

        if (mMemoryExts.isAddInternalCategory()) {
            addCategory(StorageVolumePreferenceCategory.buildForInternal(context));
        }

        /*
         * if otg was mounted but we didn't open storage page,so we must add category for it
         */
       for (StorageVolume volume : storageVolumes) {
    	   String mounted = mStorageManager.getVolumeState(volume.getPath());
    	   if(Environment.MEDIA_MOUNTED.equals(mounted)
    			   ||Environment.MEDIA_UNMOUNTED.equals(mounted)){
    		   int count = getPreferenceScreen().getPreferenceCount();
	            if (isPhysicalCategory(volume)) {
	            	StorageVolumePreferenceCategory category = StorageVolumePreferenceCategory.buildForPhysical(context, volume);
	            	category.setKey(volume.getPath());
	                addCategory(category);
	                
	            }
    	   }
        }

        mMemoryExts.registerSdSwapReceiver(mCategories);

        setHasOptionsMenu(true);
        mHandler.sendEmptyMessage(MSG_UPDATE_STORAGE_SIZE);
    }

    
    
    /**
     * add  otg devices into storage page
     */
    private void addPhysicalCategory(String path){
    	
    	final StorageVolume[] storageVolumes = mMemoryExts.getVolumeList();
    	 for (StorageVolume volume : storageVolumes) {
             if (isPhysicalCategory(volume) && Environment.MEDIA_MOUNTED.equals(volume.getState())) {
            	 AuroraPreference existPref = getPreferenceScreen().findPreference(volume.getPath());
             	if(existPref != null){
             		if(DEGUG)
             		Log.d(TAG, "addPhysical storage-->"+path);
             		continue;
             	}
            	 StorageVolumePreferenceCategory category = StorageVolumePreferenceCategory.buildForPhysical(getActivity(), volume);
	            	category.setKey(volume.getPath());
	                addCategory(category);
	                
             }
         }
    	 mHandler.sendEmptyMessage(MSG_UPDATE_STORAGE_SIZE);
    }
    
    private boolean isPhysicalCategory(StorageVolume volume) {
        return !(FeatureOption.MTK_SHARED_SDCARD
        		&& !FeatureOption.MTK_2SDCARD_SWAP
        		&& volume.isEmulated());
    }
    
    
    /**
     * remove OTG preference
     */
    private void removePhysicalCategory(String path){
    	if(DEGUG)
    	Log.d(TAG, "removePhysical storage-->"+path);
    	AuroraPreference existPref = getPreferenceScreen().findPreference(path);
    	if(existPref != null){
        	getPreferenceScreen().removePreference(existPref);
        	mCategories.remove((StorageVolumePreferenceCategory)existPref);
    	}
    	
    }
    
    
    private synchronized void addCategory(StorageVolumePreferenceCategory category) {
        mCategories.add(category);
        getPreferenceScreen().addPreference(category);
        category.init();
    }

    private boolean isMassStorageEnabled() {
        // Mass storage is enabled if primary volume supports it
        final StorageVolume[] volumes = mStorageManager.getVolumeList();
        final StorageVolume primary = StorageManager.getPrimaryVolume(volumes);
        return primary != null && primary.allowMassStorage();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(mMediaScannerReceiver, intentFilter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_STATE);
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        getActivity().registerReceiver(mMediaScannerReceiver, intentFilter);

        if (mMemoryExts.isInUMSState()) {
            removeDialog(DLG_CONFIRM_UNMOUNT);
        }
        mMemoryExts.updateMtkCategory();

        updateData();
        
        for (StorageVolumePreferenceCategory category : mCategories) {
            category.onResume();
        }
    }
    
    private void updateData(){
    	((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
    }

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            mMemoryExts.updateMtkCategory();
            if(DEGUG)
            Log.i(TAG, "Received storage state changed notification that " + path +
                    " changed state from " + oldState + " to " + newState);
            	handleStorageState(path,newState);
            for (StorageVolumePreferenceCategory category : mCategories) {
                final StorageVolume volume = category.getStorageVolume();
                if (volume != null && path.equals(volume.getPath())) {
                    category.onStorageStateChanged();
                    break;
                }
            }
        }
    };

    private void handleStorageState(String path,String newState){
        	/*
        	 * if otg removed or was bad removed ,delete category of it
        	 * 
        	 */
            if(Environment.MEDIA_REMOVED.equals(newState) || Environment.MEDIA_BAD_REMOVAL.equals(newState)){
            	//removeOtg();
            	Message msg = new Message();
            	msg.obj = path;
            	msg.what = MSG_REMOVE_OTG;
            	mHandler.sendMessage(msg);
            	if(DEGUG)
            	Log.d(TAG, "send msg to remove otg");
            }else if(Environment.MEDIA_MOUNTED.equals(newState)){
            	/*
            	 * if otg was mounted correctly,add category for it
            	 */
//            	addOtg();
            	Message msg = new Message();
            	msg.obj = path;
            	msg.what = MSG_ADD_OTG;
            	mHandler.sendMessage(msg);
            	if(DEGUG)
            	Log.d(TAG, "send msg to add otg");
            }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mMediaScannerReceiver);
        for (StorageVolumePreferenceCategory category : mCategories) {
            category.onPause();
        }
    }

    @Override
    public void onDestroy() {
        if (mStorageManager != null && mStorageListener != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
        mMemoryExts.unRegisterSdSwapReceiver();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.storage, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final MenuItem usb = menu.findItem(R.id.storage_usb);
        UserManager um = (UserManager)getActivity().getSystemService(Context.USER_SERVICE);
        boolean usbItemVisible = !isMassStorageEnabled()
                && !um.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER);
        usb.setVisible(usbItemVisible);
        mMemoryExts.setUsbEntranceState(mUsbManager, usb);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.storage_usb:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            UsbSettings.class.getCanonicalName(),
                            null, R.string.storage_title_usb, null, this, 0);
                } else {
                    startFragment(this, UsbSettings.class.getCanonicalName(),
                            R.string.storage_title_usb, -1, null);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private synchronized IMountService getMountService() {
       if (mMountService == null) {
           IBinder service = ServiceManager.getService("mount");
           if (service != null) {
               mMountService = IMountService.Stub.asInterface(service);
           } else {
               Log.e(TAG, "Can't get mount service");
           }
       }
       return mMountService;
    }

    @Override
    public boolean onPreferenceTreeClick(AuroraPreferenceScreen preferenceScreen, AuroraPreference preference) {
        if (StorageVolumePreferenceCategory.KEY_CACHE.equals(preference.getKey())) {
            ConfirmClearCacheFragment.show(this);
            return true;
        }

        for (StorageVolumePreferenceCategory category : mCategories) {
            Intent intent = category.intentForClick(preference);
            if (intent != null) {
//            	String action = intent.getAction();
//            	if("format.storage".equals(action)){
//            		category.showFormatDialog();
//                	return true;
//            	}
                // Don't go across app boundary if monkey is running
                if (!Utils.isMonkeyRunning()) {
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException anfe) {
                        Log.w(TAG, "No activity found for intent " + intent);
                        Toast.makeText(getActivity(), R.string.launch_error,
                                Toast.LENGTH_SHORT).show();

                    }
                }
                return true;
            }

            final StorageVolume volume = category.getStorageVolume();
            if (volume != null && category.mountToggleClicked(preference)) {
                sLastClickedMountToggle = preference;
                sClickedMountPoint = volume.getPath();
                mMemoryExts.setVolumeParameter(sClickedMountPoint, volume);
                String state = mStorageManager.getVolumeState(volume.getPath());
                if (Environment.MEDIA_MOUNTED.equals(state) ||
                        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    unmount();
                } else {
                    mMemoryExts.mount(getMountService());
                }
                return true;
            }
        }

        return false;
    }

    private final BroadcastReceiver mMediaScannerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(UsbManager.ACTION_USB_STATE)) {
               boolean isUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
               String usbFunction = mUsbManager.getDefaultFunction();
               for (StorageVolumePreferenceCategory category : mCategories) {
                   category.onUsbStateChanged(isUsbConnected, usbFunction);
               }
               getActivity().invalidateOptionsMenu();
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                for (StorageVolumePreferenceCategory category : mCategories) {
                    category.onMediaScannerFinished();
                }
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                getActivity().invalidateOptionsMenu();
            }
        }
    };

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DLG_CONFIRM_UNMOUNT:
                return new AlertDialog.Builder(getActivity())
                    .setTitle(mMemoryExts.getResourceId(R.string.dlg_confirm_unmount_usb_title,
                            R.string.dlg_confirm_unmount_title))
                    .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            doUnmount();
                        }})
                    .setNegativeButton(R.string.cancel, null)
                    .setMessage(mMemoryExts.getResourceId(R.string.dlg_confirm_unmount_usb_text,
                            R.string.dlg_confirm_unmount_text))
                    .create();
        case DLG_ERROR_UNMOUNT:
                return new AlertDialog.Builder(getActivity())
            .setTitle(mMemoryExts.getResourceId(R.string.dlg_error_unmount_usb_title,
                            R.string.dlg_error_unmount_title))
            .setNeutralButton(R.string.dlg_ok, null)
            .setMessage(mMemoryExts.getResourceId(R.string.dlg_error_unmount_usb_text,
                            R.string.dlg_error_unmount_text))
            .create();
        }
        return null;
    }

    private void doUnmount() {
        // Present a toast here
        int informTextId = mMemoryExts.getResourceId(R.string.unmount_usb_inform_text,
                R.string.unmount_inform_text);
        Toast.makeText(getActivity(), informTextId, Toast.LENGTH_SHORT).show();
        IMountService mountService = getMountService();
        try {
            sLastClickedMountToggle.setEnabled(false);
            sLastClickedMountToggle.setTitle(getString(R.string.sd_ejecting_title));
            sLastClickedMountToggle.setSummary(getString(R.string.sd_ejecting_summary));
            Log.d(TAG, "Settings unMountVolume : " + sClickedMountPoint);
            mountService.unmountVolume(sClickedMountPoint, true, false);
        } catch (RemoteException e) {
            // Informative dialog to user that unmount failed.
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void showDialogInner(int id) {
        removeDialog(id);
        showDialog(id);
    }

    private boolean hasAppsAccessingStorage() throws RemoteException {
        IMountService mountService = getMountService();
        int stUsers[] = mountService.getStorageUsers(sClickedMountPoint);
        if (stUsers != null && stUsers.length > 0) {
            return true;
        }
        // TODO FIXME Parameterize with mountPoint and uncomment.
        // On HC-MR2, no apps can be installed on sd and the emulated internal storage is not
        // removable: application cannot interfere with unmount
        /*
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationInfo> list = am.getRunningExternalApplications();
        if (list != null && list.size() > 0) {
            return true;
        }
        */
        // Better safe than sorry. Assume the storage is used to ask for confirmation.
        return true;
    }

    private void unmount() {
        // Check if external media is in use.
        try {
           if (hasAppsAccessingStorage()) {
               // Present dialog to user
               showDialogInner(DLG_CONFIRM_UNMOUNT);
           } else {
               doUnmount();
           }
        } catch (RemoteException e) {
            // Very unlikely. But present an error dialog anyway
            Log.e(TAG, "Is MountService running?");
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void mount() {
        IMountService mountService = getMountService();
        try {
            if (mountService != null) {
                mountService.mountVolume(sClickedMountPoint);
            } else {
                Log.e(TAG, "Mount service is null, can't mount");
            }
        } catch (RemoteException ex) {
            // Not much can be done
        }
    }

    private void onCacheCleared() {
        for (StorageVolumePreferenceCategory category : mCategories) {
            category.onCacheCleared();
        }
    }

    private static class ClearCacheObserver extends IPackageDataObserver.Stub {
        private final Memory mTarget;
        private int mRemaining;

        public ClearCacheObserver(Memory target, int remaining) {
            mTarget = target;
            mRemaining = remaining;
        }

        @Override
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            synchronized (this) {
                if (--mRemaining == 0) {
                    mTarget.onCacheCleared();
                }
            }
        }
    }

    /**
     * Dialog to request user confirmation before clearing all cache data.
     */
    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static void show(Memory parent) {
            if (!parent.isAdded()) return;

            final ConfirmClearCacheFragment dialog = new ConfirmClearCacheFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_CLEAR_CACHE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.memory_clear_cache_title);
            builder.setMessage(getString(R.string.memory_clear_cache_message));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Memory target = (Memory) getTargetFragment();
                    final PackageManager pm = context.getPackageManager();
                    final List<PackageInfo> infos = pm.getInstalledPackages(0);
                    final ClearCacheObserver observer = new ClearCacheObserver(
                            target, infos.size());
                    for (PackageInfo info : infos) {
                        pm.deleteApplicationCacheFiles(info.packageName, observer);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Enable indexing of searchable data
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.storage_settings);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.internal_storage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                final StorageVolume[] storageVolumes = StorageManager.from(context).getVolumeList();
                for (StorageVolume volume : storageVolumes) {
                    if (!volume.isEmulated()) {
                        data.title = volume.getDescription(context);
                        data.screenTitle = context.getString(R.string.storage_settings);
                        result.add(data);
                    }
                }

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_size);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_available);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_apps_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_dcim_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_music_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_downloads_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_media_cache_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_media_misc_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                return result;
            }
        };

}