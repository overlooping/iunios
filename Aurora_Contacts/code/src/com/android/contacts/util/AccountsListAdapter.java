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

package com.android.contacts.util;

import com.android.contacts.ContactsApplication;
import com.android.contacts.GNContactsUtils;
// Gionee lihuafang 20120419 add for CR00573564 begin
import android.os.ServiceManager;
import android.os.SystemProperties;
import com.android.internal.telephony.ITelephony;
import android.graphics.Color;
import android.text.TextUtils;
import gionee.provider.GnTelephony.SIMInfo;
//import com.mediatek.telephony.TelephonyManagerEx;
import com.gionee.internal.telephony.GnTelephonyManagerEx;
// Gionee lihuafang 20120419 add for CR00573564 end
import com.android.contacts.R;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

// The following lines are provided and maintained by Mediatek Inc.
// Description: for SIM name display
import android.util.Log;

import com.mediatek.contacts.ContactsFeatureConstants;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SIMInfoWrapper;
import com.mediatek.contacts.simcontact.SimCardUtils;

import com.mediatek.contacts.util.OperatorUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroup;
// The previous lines are provided and maintained by Mediatek inc.

//Gionee:wangth 20120413 add for CR00572641 begin
import com.android.contacts.ContactsUtils;
//Gionee:wangth 20120413 add for CR00572641 end

/**
 * List-Adapter for Account selection
 */
public final class AccountsListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final List<AccountWithDataSet> mAccounts;
    private final AccountTypeManager mAccountTypes;
    private final Context mContext;

    // The following lines are provided and maintained by Mediatek Inc.
    // Description: for SIM name display
    private static final String TAG = "AccountListAdapter";

    private int mSlotId = -1;
    private int mSimId = -1;
    private String mDisplayName = null;
    // The previous lines are provided and maintained by Mediatek inc.

    // Gionee lihuafang 20120419 add for CR00573564 begin
    private class ViewHolder {
        View mSimIcon;

        ImageView mSimStatus;

        TextView mSimSignal;

        TextView mShortPhoneNumber;

        TextView mDisplayName;

        TextView mPhoneNumber;
    }
    // Gionee lihuafang 20120419 add for CR00573564 end

    /**
     * Filters that affect the list of accounts that is displayed by this adapter.
     */
    public enum AccountListFilter {
        ALL_ACCOUNTS,                   // All read-only and writable accounts
        ACCOUNTS_CONTACT_WRITABLE,      // Only where the account type is contact writable
        ACCOUNTS_GROUP_WRITABLE,         // Only accounts where the account type is group writable
        GN_ACCOUNTS_GROUP_WRITABLE         // Only accounts where the account type is group writable
    }

    public AccountsListAdapter(Context context, AccountListFilter accountListFilter) {
        this(context, accountListFilter, null);
    }

    /**
     * @param currentAccount the Account currently selected by the user, which should come
     * first in the list. Can be null.
     */
    public AccountsListAdapter(Context context, AccountListFilter accountListFilter,
            AccountWithDataSet currentAccount) {
        mContext = context;
        mAccountTypes = AccountTypeManager.getInstance(context);
        mAccounts = getAccounts(accountListFilter);
        // gionee xuhz 20120516 modify for CR00582595 start
        if (ContactsApplication.sIsGnContactsSupport) {
            if (currentAccount != null
                    && !mAccounts.isEmpty()
                    && !mAccounts.get(0).equals(currentAccount)) {
                String accountType =  currentAccount.type.toString();
                if (isSimUsimAccountType(accountType)) {
                	for (AccountWithDataSet account : mAccounts) {
                        if ((currentAccount.equals(account)) 
                                && (account instanceof AccountWithDataSetEx)) {
                            currentAccount = (AccountWithDataSetEx) account;
                        }
                    }
                }
                if (mAccounts.remove(currentAccount)) {
                    mAccounts.add(0, currentAccount);
                }
            }
        } else {
            if (currentAccount != null
                    && !mAccounts.isEmpty()
                    && !mAccounts.get(0).equals(currentAccount)
                    && mAccounts.remove(currentAccount)) {
                mAccounts.add(0, currentAccount);
            }
        }
        // gionee xuhz 20120516 modify for CR00582595 end
        mInflater = LayoutInflater.from(context);
    }

    private List<AccountWithDataSet> getAccounts(AccountListFilter accountListFilter) {
        if (accountListFilter == AccountListFilter.ACCOUNTS_GROUP_WRITABLE) {
            List<AccountWithDataSet> accountList = mAccountTypes.getGroupWritableAccounts();
            List<AccountWithDataSet> newAccountList = new ArrayList<AccountWithDataSet>();
            Log.d(TAG, "[getAccounts]accountList size:" + accountList.size());
            for (AccountWithDataSet account : accountList) {
                if (account instanceof AccountWithDataSetEx) {
                    int slotId = ((AccountWithDataSetEx) account).getSlotId();
                    Log.d(TAG, "[getAccounts]slotId:" + slotId);
                    if (SimCardUtils.isSimUsimType(slotId)) {
                        Log.d(TAG, "[getAccounts]getUSIMGrpMaxNameLen:"
                                + USIMGroup.getUSIMGrpMaxNameLen(slotId));
                        if (USIMGroup.getUSIMGrpMaxNameLen(slotId) > 0) {
                            newAccountList.add(account);
                        }
                    } else {
                        newAccountList.add(account);
                    }
                } else {
                    newAccountList.add(account);
                }
            }
            
            return new ArrayList<AccountWithDataSet>(newAccountList);
        } else if (accountListFilter == AccountListFilter.GN_ACCOUNTS_GROUP_WRITABLE) {
            List<AccountWithDataSet> accountList = mAccountTypes.getGroupWritableAccounts();
            List<AccountWithDataSet> newAccountList = new ArrayList<AccountWithDataSet>();
            Log.d(TAG, "[getAccounts]accountList size:" + accountList.size());
            for (AccountWithDataSet account : accountList) {
                if (account instanceof AccountWithDataSetEx) {
                    int slotId = ((AccountWithDataSetEx) account).getSlotId();
                    Log.d(TAG, "[getAccounts]slotId:" + slotId);
                    if (!SimCardUtils.isSimUsimType(slotId)) {
                    	newAccountList.add(account);
                    }
                } else {
                    newAccountList.add(account);
                }
            }
            
            return new ArrayList<AccountWithDataSet>(newAccountList);
        }
        return new ArrayList<AccountWithDataSet>(mAccountTypes.getAccounts(
                accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Gionee lihuafang 20120419 modify for CR00573564 begin
        if (ContactsUtils.mIsGnContactsSupport 
                && (ContactsUtils.mIsGnShowSlotSupport || ContactsUtils.mIsGnShowDigitalSlotSupport)
                && !GNContactsUtils.isOnlyQcContactsSupport()) {
            View view = convertView;
            ViewHolder holder = null;
            AccountWithDataSet account = mAccounts.get(position);
            AccountType accountType = mAccountTypes.getAccountType(account.type, account.dataSet);

            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                holder = new ViewHolder();
                view = inflater.inflate(R.layout.gn_account_selector_list_item, null);
                holder.mSimSignal = (TextView) view.findViewById(R.id.simSignal);
                holder.mSimStatus = (ImageView) view.findViewById(R.id.simStatus);
                holder.mShortPhoneNumber = (TextView) view.findViewById(R.id.shortPhoneNumber);
                holder.mDisplayName = (TextView) view.findViewById(R.id.displayName);
                holder.mPhoneNumber = (TextView) view.findViewById(R.id.phoneNumber);
                holder.mSimIcon = view.findViewById(R.id.simIcon);
                view.setTag(holder);
            }
            holder = (ViewHolder) view.getTag();

            if (account instanceof AccountWithDataSetEx) {
                mSlotId = ((AccountWithDataSetEx) account).getSlotId();
                mDisplayName = SIMInfoWrapper.getDefault().getSimDisplayNameBySlotId(mSlotId);
                SIMInfo simInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(mSlotId);
                // Gionee:wangth 20120822 modify for CR00679732 begin
                /*
                int resId = getSimStatusIcon(simInfo.mSlot);
                */
                int resId = 0;
                try {
                    resId = getSimStatusIcon(simInfo.mSlot);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Gionee:wangth 20120822 modify for CR00679732 end

                if (null == mDisplayName) {
                    holder.mDisplayName.setText(account.name);
                }

                holder.mDisplayName.setText(mDisplayName);
                // Gionee:wangth 20120822 modify for CR00679732 begin
                /*
                holder.mSimIcon.setBackgroundResource(Telephony.SIMBackgroundRes[simInfo.mColor]);
                */
                try {
                    holder.mSimIcon.setBackgroundResource(gionee.provider.GnTelephony.SIMBackgroundRes[simInfo.mColor]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Gionee:wangth 20120822 modify for CR00679732 end
                
                try {
                    String shortNumber = "";
                    ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager
                            .getService(Context.TELEPHONY_SERVICE));
                    if (!TextUtils.isEmpty(simInfo.mNumber)) {
                        switch (simInfo.mDispalyNumberFormat) {
                            case gionee.provider.GnTelephony.SimInfo.DISPLAY_NUMBER_FIRST:
                                if (simInfo.mNumber.length() <= 4)
                                    shortNumber = simInfo.mNumber;
                                else
                                    shortNumber = simInfo.mNumber.substring(0, 4);
                                break;
                            case gionee.provider.GnTelephony.SimInfo.DISPLAY_NUMBER_LAST:
                                if (simInfo.mNumber.length() <= 4)
                                    shortNumber = simInfo.mNumber;
                                else
                                    shortNumber = simInfo.mNumber.substring(
                                            simInfo.mNumber.length() - 4, simInfo.mNumber.length());
                                break;
                            case 0:// android.provider.Telephony.SimInfo.DISPLAY_NUMBER_NONE:
                                shortNumber = "";
                                break;
                        }
                        holder.mPhoneNumber.setText(simInfo.mNumber);
                        holder.mPhoneNumber.setVisibility(View.VISIBLE);

                    } else {
                        holder.mPhoneNumber.setVisibility(View.GONE);
                    }
                    holder.mShortPhoneNumber.setText(shortNumber);
                    holder.mShortPhoneNumber.setVisibility(View.VISIBLE);
                    holder.mSimSignal.setVisibility(View.VISIBLE);
                    if (mSlotId == ContactsFeatureConstants.GEMINI_SIM_1) {
                        holder.mSimSignal.setText(R.string.slotA_signal);
                    } else if (mSlotId == ContactsFeatureConstants.GEMINI_SIM_2) {
                        holder.mSimSignal.setText(R.string.slotB_signal);
                    }

                } catch (Exception e) {
                    holder.mShortPhoneNumber.setText("");
                }
                if (resId > 0 ) {
                    holder.mSimStatus.setImageResource(resId);
                    holder.mSimStatus.setVisibility(View.VISIBLE);
                } else {
                    holder.mSimStatus.setVisibility(View.INVISIBLE);
                }

            } else {
                String phoneString = null;

                if (AccountType.ACCOUNT_TYPE_LOCAL_PHONE.equals(account.type)) {
                    phoneString = mContext.getResources().getString(R.string.gn_phone_text);
                    holder.mSimIcon.setBackgroundResource(R.drawable.gn_phone);
                } else {
                    // Gionee:wangth 20120820 add for CR00628405 begin
                    if (ContactsUtils.mIsGnContactsSupport && holder.mDisplayName != null) {
                        holder.mDisplayName.setSingleLine(false);
                    }
                    // Gionee:wangth 20120820 add for CR00628405 end
                    phoneString = account.name;
                    holder.mSimIcon.setBackgroundDrawable(accountType.getDisplayIcon(mContext));
                }
                holder.mDisplayName.setText(phoneString);
                holder.mPhoneNumber.setVisibility(View.GONE);
                holder.mShortPhoneNumber.setVisibility(View.INVISIBLE);
                holder.mSimSignal.setVisibility(View.INVISIBLE);
                holder.mSimStatus.setVisibility(View.INVISIBLE);

            }
            return view;
        } else {
        final View resultView = convertView != null ? convertView
                : mInflater.inflate(R.layout.account_selector_list_item, parent, false);

        final TextView text1 = (TextView) resultView.findViewById(android.R.id.text1);
        final TextView text2 = (TextView) resultView.findViewById(android.R.id.text2);
        final ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);

        final AccountWithDataSet account = mAccounts.get(position);
        final AccountType accountType = mAccountTypes.getAccountType(account.type, account.dataSet);

        text1.setText(accountType.getDisplayLabel(mContext));

        // For email addresses, we don't want to truncate at end, which might cut off the domain
        // name.
        // The following lines are provided and maintained by Mediatek Inc.
        // Description: for SIM name display
        // Keep previous code here.
        // text2.setText(account.name);
        if (account instanceof AccountWithDataSetEx) {
            mSlotId = ((AccountWithDataSetEx) account).getSlotId();
            mDisplayName = SIMInfoWrapper.getDefault().getSimDisplayNameBySlotId(mSlotId);
            if (null == mDisplayName) {
                mDisplayName = account.name;
            }
            text2.setText(mDisplayName);
            Log.i(TAG, "getView slotId:" + mSlotId + " simId:" + mSimId + " displayName:" + mDisplayName);
        } else {
            //Gionee:wangth 20120413 modify for CR00572641 begin
            //text2.setText(account.name);
            String phoneString = null;
            
            if (ContactsUtils.mIsGnContactsSupport && AccountType.ACCOUNT_TYPE_LOCAL_PHONE.equals(account.type)) {
                phoneString = mContext.getResources().getString(R.string.gn_phone_text);
            } else {
                phoneString = account.name;
            }
            
            text2.setText(phoneString);
            //Gionee:wangth 20120413 modify for CR00572641 end
        }
        // The previous lines are provided and maintained by Mediatek inc.
        text2.setEllipsize(TruncateAt.MIDDLE);
        
        // gionee xuhz 20120904 add for CR00682817 start
        if (ContactsApplication.sIsGnDarkTheme) {
            text1.setTextColor(mContext.getResources().getColor(android.R.color.white));
            text2.setTextColor(mContext.getResources().getColor(android.R.color.white));
        }
        // gionee xuhz 20120904 add for CR00682817 end
        
        /*
         * Change feature by Mediatek Begin.
         *   Original Android's code:
         *    icon.setImageDrawable(accountType.getDisplayIcon(mContext));
         *   CR ID: ALPS00233786
         *   Descriptions: cu feature change photo by slot id 
         */
        // Gionee <xuhz> <2013-08-16> modify for CR00858149 begin
        //old:if (OperatorUtils.getOptrProperties().equals("OP02") && isSimUsimAccountType(accountType.accountType)) {
        if (OperatorUtils.getActualOptrProperties().equals("OP02") && isSimUsimAccountType(accountType.accountType)) {
        // Gionee <xuhz> <2013-08-16> modify for CR00858149 end
            Log.i("checkphoto","accountlistadpter mSlotId : "+mSlotId);
            icon.setImageDrawable(accountType.getDisplayIconBySlotId(mContext, mSlotId));
        } else {
            icon.setImageDrawable(accountType.getDisplayIcon(mContext));
        }
        /*
         * Change Feature by Mediatek End.
         */
        
        // qc begin
        if (GNContactsUtils.isOnlyQcContactsSupport() && ContactsApplication.isMultiSimEnabled 
                && mSlotId > -1 && !AccountType.ACCOUNT_TYPE_LOCAL_PHONE.equals(account.type)) {
            icon.setImageDrawable(accountType.getDisplayIconBySlotId(mContext, mSlotId));
        }
        // qc end

        return resultView;
        }
        // Gionee lihuafang 20120419 modify for CR00573564 end
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public AccountWithDataSet getItem(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    /*
     * Change feature by Mediatek Begin.
     *   Original Android's code:
     *   
     *   CR ID: ALPS00233786
     *   Descriptions: cu feature change photo by slot id 
     */
    private boolean isSimUsimAccountType(String accountType) {
        Log.i("checkphoto","accountType : "+accountType);
        boolean bRet = false;
        if (AccountType.ACCOUNT_TYPE_SIM.equals(accountType)  
                || AccountType.ACCOUNT_TYPE_USIM.equals(accountType)) {
            bRet = true;
        }
        return bRet;
    }
    /*
     * Change Feature by Mediatek End.
     */
    
    // Gionee lihuafang 20120419 add for CR00573564 begin
    protected int getSimStatusIcon(int slot) {
        GnTelephonyManagerEx telephonyManager = GnTelephonyManagerEx.getDefault();
        int state = telephonyManager.getSimIndicatorStateGemini(slot);
        int resourceId = 0;
        switch (state) {
            case ContactsFeatureConstants.SIM_INDICATOR_LOCKED:
                resourceId = R.drawable.sim_locked;
                break;
            case ContactsFeatureConstants.SIM_INDICATOR_RADIOOFF:
                resourceId = R.drawable.sim_radio_off;
                break;
            case ContactsFeatureConstants.SIM_INDICATOR_ROAMING:
                resourceId = R.drawable.sim_roaming;
                break;
            case ContactsFeatureConstants.SIM_INDICATOR_SEARCHING:
                resourceId = R.drawable.sim_searching;
                break;
            case ContactsFeatureConstants.SIM_INDICATOR_INVALID:
                resourceId = R.drawable.sim_invalid;
                break;
            case ContactsFeatureConstants.SIM_INDICATOR_CONNECTED:
                resourceId = R.drawable.sim_connected;
                break;
            case ContactsFeatureConstants.SIM_INDICATOR_ROAMINGCONNECTED:
                resourceId = R.drawable.sim_roaming_connected;
                break;
            default:
                resourceId = -1;
                break;
        }
        Log.i(TAG, "getSimStatusIcon, state = " + state);
        return resourceId;
    }
    // Gionee lihuafang 20120419 add for CR00573564 end
}

