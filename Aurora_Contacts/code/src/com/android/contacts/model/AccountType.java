/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.model;

import com.android.contacts.ContactsApplication;
import com.android.contacts.GNContactsUtils;
import com.android.contacts.R;
import com.android.contacts.ResConstant;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.google.common.annotations.VisibleForTesting;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import gionee.provider.GnContactsContract.CommonDataKinds.Phone;
import gionee.provider.GnContactsContract.CommonDataKinds.StructuredPostal;
import gionee.provider.GnContactsContract.Contacts;
import gionee.provider.GnContactsContract.Data;
import gionee.provider.GnContactsContract.RawContacts;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

// The following lines are provided and maintained by Mediatek inc.
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;    
// The previous lines are provided and maintained by Mediatek inc.
import com.mediatek.contacts.util.OperatorUtils;

// Gionee:wangth 20120601 add for CR00611620 begin
import com.android.contacts.ContactsUtils;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
// Gionee:wangth 20120601 add for CR00611620 end


/**
 * Internal structure that represents constraints and styles for a specific data
 * source, such as the various data types they support, including details on how
 * those types should be rendered and edited.
 * <p>
 * In the future this may be inflated from XML defined by a data source.
 */
public abstract class AccountType {
    private static final String TAG = "AccountType";

    // The following lines are provided and maintained by Mediatek inc.
    // Added Local Account Type
    public static final String ACCOUNT_TYPE_SIM = "SIM Account";
    public static final String ACCOUNT_TYPE_USIM = "USIM Account";
    public static final String ACCOUNT_TYPE_LOCAL_PHONE = "Local Phone Account";

    // Added Local Account Name - For Sim/Usim Only
    public static final String ACCOUNT_NAME_SIM = "SIM" + SimCardUtils.SimSlot.SLOT_ID1;
    public static final String ACCOUNT_NAME_SIM2 = "SIM" + SimCardUtils.SimSlot.SLOT_ID2;
    public static final String ACCOUNT_NAME_USIM = "USIM" + SimCardUtils.SimSlot.SLOT_ID1;
    public static final String ACCOUNT_NAME_USIM2 = "USIM" + SimCardUtils.SimSlot.SLOT_ID2;
    public static final String ACCOUNT_NAME_LOCAL_PHONE = "Phone";
    // The previous lines are provided and maintained by Mediatek inc.

    // Gionee liuyanbo 2012-07-31 add for CR00657509 start
    public static final String ACCOUNT_NAME_UIM = "UIM" + SimCardUtils.SimSlot.SLOT_ID1;
    public static final String ACCOUNT_NAME_UIM2 = "UIM" + SimCardUtils.SimSlot.SLOT_ID2;
    // Gionee liuyanbo 2012-07-31 add for CR00657509 end
    /**
     * The {@link RawContacts#ACCOUNT_TYPE} these constraints apply to.
     */
    public String accountType = null;

    /**
     * The {@link RawContacts#DATA_SET} these constraints apply to.
     */
    public String dataSet = null;

    /**
     * Package that resources should be loaded from, either defined through an
     * {@link Account} or for matching against {@link Data#RES_PACKAGE}.
     */
    public String resPackageName;
    public String summaryResPackageName;

    public int titleRes;
    public int iconRes;

    /**
     * Set of {@link DataKind} supported by this source.
     */
    private ArrayList<DataKind> mKinds = Lists.newArrayList();

    /**
     * Lookup map of {@link #mKinds} on {@link DataKind#mimeType}.
     */
    private HashMap<String, DataKind> mMimeKinds = Maps.newHashMap();

    protected boolean mIsInitialized;

    protected static class DefinitionException extends Exception {
        public DefinitionException(String message) {
            super(message);
        }

        public DefinitionException(String message, Exception inner) {
            super(message, inner);
        }
    }

    /**
     * Whether this account type was able to be fully initialized.  This may be false if
     * (for example) the package name associated with the account type could not be found.
     */
    public final boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * @return Whether this type is an "embedded" type.  i.e. any of {@link FallbackAccountType},
     * {@link GoogleAccountType} or {@link ExternalAccountType}.
     *
     * If an embedded type cannot be initialized (i.e. if {@link #isInitialized()} returns
     * {@code false}) it's considered critical, and the application will crash.  On the other
     * hand if it's not an embedded type, we just skip loading the type.
     */
    public boolean isEmbedded() {
        return true;
    }

    public boolean isExtension() {
        return false;
    }

    /**
     * @return True if contacts can be created and edited using this app. If false,
     * there could still be an external editor as provided by
     * {@link #getEditContactActivityClassName()} or {@link #getCreateContactActivityClassName()}
     */
    public abstract boolean areContactsWritable();

    /**
     * Returns an optional custom edit activity.  The activity class should reside
     * in the sync adapter package as determined by {@link #resPackageName}.
     */
    public String getEditContactActivityClassName() {
        return null;
    }

    /**
     * Returns an optional custom new contact activity. The activity class should reside
     * in the sync adapter package as determined by {@link #resPackageName}.
     */
    public String getCreateContactActivityClassName() {
        return null;
    }

    /**
     * Returns an optional custom invite contact activity. The activity class should reside
     * in the sync adapter package as determined by {@link #resPackageName}.
     */
    public String getInviteContactActivityClassName() {
        return null;
    }

    /**
     * Returns an optional service that can be launched whenever a contact is being looked at.
     * This allows the sync adapter to provide more up-to-date information.
     * The service class should reside in the sync adapter package as determined by
     * {@link #resPackageName}.
     */
    public String getViewContactNotifyServiceClassName() {
        return null;
    }

    /** Returns an optional Activity string that can be used to view the group. */
    public String getViewGroupActivity() {
        return null;
    }

    /** Returns an optional Activity string that can be used to view the stream item. */
    public String getViewStreamItemActivity() {
        return null;
    }

    /** Returns an optional Activity string that can be used to view the stream item photo. */
    public String getViewStreamItemPhotoActivity() {
        return null;
    }

    public CharSequence getDisplayLabel(Context context) {
        return getResourceText(context, summaryResPackageName, titleRes, accountType);
    }

    /**
     * @return resource ID for the "invite contact" action label, or -1 if not defined.
     */
    protected int getInviteContactActionResId() {
        return -1;
    }

    /**
     * @return resource ID for the "view group" label, or -1 if not defined.
     */
    protected int getViewGroupLabelResId() {
        return -1;
    }

    /**
     * Returns {@link AccountTypeWithDataSet} for this type.
     */
    public AccountTypeWithDataSet getAccountTypeAndDataSet() {
        return AccountTypeWithDataSet.get(accountType, dataSet);
    }

    /**
     * Returns a list of additional package names that should be inspected as additional
     * external account types.  This allows for a primary account type to indicate other packages
     * that may not be sync adapters but which still provide contact data, perhaps under a
     * separate data set within the account.
     */
    public List<String> getExtensionPackageNames() {
        return new ArrayList<String>();
    }

    /**
     * Returns an optional custom label for the "invite contact" action, which will be shown on
     * the contact card.  (If not defined, returns null.)
     */
    public CharSequence getInviteContactActionLabel(Context context) {
        return getResourceText(context, summaryResPackageName, getInviteContactActionResId(), "");
    }

    /**
     * Returns a label for the "view group" action. If not defined, this falls back to our
     * own "View Updates" string
     */
    public CharSequence getViewGroupLabel(Context context) {
        final CharSequence customTitle =
                getResourceText(context, summaryResPackageName, getViewGroupLabelResId(), null);

        return customTitle == null
                ? context.getText(R.string.view_updates_from_group)
                : customTitle;
    }

    /**
     * Return a string resource loaded from the given package (or the current package
     * if {@code packageName} is null), unless {@code resId} is -1, in which case it returns
     * {@code defaultValue}.
     *
     * (The behavior is undefined if the resource or package doesn't exist.)
     */
    @VisibleForTesting
    static CharSequence getResourceText(Context context, String packageName, int resId,
            String defaultValue) {
        if (resId != -1 && packageName != null) {
            final PackageManager pm = context.getPackageManager();
            return pm.getText(packageName, resId, null);
        } else if (resId != -1) {
            return context.getText(resId);
        } else {
            return defaultValue;
        }
    }

    public Drawable getDisplayIcon(Context context) {
        if (this.titleRes != -1 && this.summaryResPackageName != null) {
            final PackageManager pm = context.getPackageManager();
            return pm.getDrawable(this.summaryResPackageName, this.iconRes, null);
        } else if (this.titleRes != -1) {
            return context.getResources().getDrawable(this.iconRes);
        } else {
            return null;
        }
    }
    /*
     * Change feature by Mediatek Begin.
     *   Original Android's code:
     *     
     *   CR ID: ALPS00233786
     *   Descriptions: cu feature change photo by slot id 
     */
    public Drawable getDisplayIconBySlotId(Context context, int slotid) {
        if (this.titleRes != -1 && this.summaryResPackageName != null) {
            Log.i("checkphoto", "[Accounttype] summaryrespackagename !=null");
            final PackageManager pm = context.getPackageManager();
            return pm.getDrawable(this.summaryResPackageName, this.iconRes, null);
        } else if (this.titleRes != -1) {
        	int photo = this.iconRes;
            Log.i("checkphoto", "[Accounttype] slotid = " + slotid);
            if (slotid == 0) {
                photo = ResConstant.getIconRes(ResConstant.IconTpye.Sim1);
            } else if (slotid == 1) {
                photo = ResConstant.getIconRes(ResConstant.IconTpye.Sim2);
            } else if (-1 == slotid) {
            	photo = ResConstant.getIconRes(ResConstant.IconTpye.LocalPhone);
            }
            return context.getResources().getDrawable(photo);
        } else {
            return null;
        }
    }
    /*
     * Change Feature by Mediatek End.
     */
    public static Drawable getDisplayIconByAccount(Context context, String name, String type) {
    	AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
        List<AccountWithDataSet> accounts = accountTypes.getAccounts(false);
        AccountWithDataSet account = null;
        for (AccountWithDataSet a : accounts) {
        	if (a.name.equals(name) && a.type.equals(type)) {
        		account = a;
        		break;
        	}
        }
        
        if (null == account) {
        	return null;
        }
        
        AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
            
        Drawable icon;
        int slotId = -1;
        
        //Gionee <huangzy> <2013-06-19> modify for CR00827471 begin
        /*boolean isSimUsimAccountType = (AccountType.ACCOUNT_TYPE_SIM.equals(accountType)
                || AccountType.ACCOUNT_TYPE_USIM.equals(accountType));*/
        boolean isSimUsimAccountType = (AccountType.ACCOUNT_TYPE_SIM.equals(accountType.accountType)
                || AccountType.ACCOUNT_TYPE_USIM.equals(accountType.accountType));
        //Gionee <huangzy> <2013-06-19> modify for CR00827471 end
        
        // Gionee <wangth><2013-06-04> add for CR00611620 & CR00622107 & CR00811915 begin
        if (ContactsUtils.mIsGnContactsSupport && isSimUsimAccountType && 
                (FeatureOption.MTK_GEMINI_SUPPORT
                || (GNContactsUtils.isOnlyQcContactsSupport() && ContactsApplication.isMultiSimEnabled))) {
            Drawable nIcon;
            int photo = 0;
            int nSlotId = -1;
            
            try {
                nSlotId  = ((AccountWithDataSetEx)account).getSlotId();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (nSlotId == SimCardUtils.SimSlot.SLOT_ID1) {
                photo = ResConstant.getIconRes(ResConstant.IconTpye.Sim1);
            } else if (nSlotId == SimCardUtils.SimSlot.SLOT_ID2) {
                photo = ResConstant.getIconRes(ResConstant.IconTpye.Sim2);
            }
            
            if (photo > 0) {
                nIcon = context.getResources().getDrawable(photo);
                return nIcon;
            }
        }
        // Gionee <wangth><2013-06-04> add for CR00611620 & CR00622107 & CR00811915 end
        
        if (isSimUsimAccountType) {
        	slotId = ((AccountWithDataSetEx) account).mSlotId;
        }
        
        // Gionee <xuhz> <2013-08-16> modify for CR00858149 begin
        //old:if (isSimUsimAccountType && accountType != null && OperatorUtils.getOptrProperties().equals("OP02")) {
        if (isSimUsimAccountType && accountType != null && OperatorUtils.getActualOptrProperties().equals("OP02")) {
        // Gionee <xuhz> <2013-08-16> modify for CR00858149 end
            icon = accountType.getDisplayIconBySlotId(context, slotId);
        } else {
            icon = accountType != null ? accountType.getDisplayIcon(context) : null;
        }
        
    	return icon;
    }
    /**
     * Whether or not groups created under this account type have editable membership lists.
     */
    abstract public boolean isGroupMembershipEditable();

    /**
     * {@link Comparator} to sort by {@link DataKind#weight}.
     */
    private static Comparator<DataKind> sWeightComparator = new Comparator<DataKind>() {
        public int compare(DataKind object1, DataKind object2) {
            return object1.weight - object2.weight;
        }
    };

    /**
     * Return list of {@link DataKind} supported, sorted by
     * {@link DataKind#weight}.
     */
    public ArrayList<DataKind> getSortedDataKinds() {
        // TODO: optimize by marking if already sorted
        Collections.sort(mKinds, sWeightComparator);
        return mKinds;
    }

    /**
     * Find the {@link DataKind} for a specific MIME-type, if it's handled by
     * this data source. If you may need a fallback {@link DataKind}, use
     * {@link AccountTypeManager#getKindOrFallback(String, String, String)}.
     */
    public DataKind getKindForMimetype(String mimeType) {
        return this.mMimeKinds.get(mimeType);
    }

    /**
     * Add given {@link DataKind} to list of those provided by this source.
     */
    public DataKind addKind(DataKind kind) throws DefinitionException {
        if (kind.mimeType == null) {
            throw new DefinitionException("null is not a valid mime type");
        }
        if (mMimeKinds.get(kind.mimeType) != null) {
            throw new DefinitionException(
                    "mime type '" + kind.mimeType + "' is already registered");
        }

        kind.resPackageName = this.resPackageName;
        this.mKinds.add(kind);
        this.mMimeKinds.put(kind.mimeType, kind);
        return kind;
    }

    /**
     * Description of a specific "type" or "label" of a {@link DataKind} row,
     * such as {@link Phone#TYPE_WORK}. Includes constraints on total number of
     * rows a {@link Contacts} may have of this type, and details on how
     * user-defined labels are stored.
     */
    public static class EditType {
        public int rawValue;
        public int labelRes;
        public boolean secondary;
        /**
         * The number of entries allowed for the type. -1 if not specified.
         * @see DataKind#typeOverallMax
         */
        public int specificMax;
        public String customColumn;

        public EditType(int rawValue, int labelRes) {
            this.rawValue = rawValue;
            this.labelRes = labelRes;
            this.specificMax = -1;
        }

        public EditType setSecondary(boolean secondary) {
            this.secondary = secondary;
            return this;
        }

        public EditType setSpecificMax(int specificMax) {
            this.specificMax = specificMax;
            return this;
        }

        public EditType setCustomColumn(String customColumn) {
            this.customColumn = customColumn;
            return this;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof EditType) {
                final EditType other = (EditType)object;
                return other.rawValue == rawValue;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return rawValue;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName()
                    + " rawValue=" + rawValue
                    + " labelRes=" + labelRes
                    + " secondary=" + secondary
                    + " specificMax=" + specificMax
                    + " customColumn=" + customColumn;
        }
    }

    public static class EventEditType extends EditType {
        private boolean mYearOptional;

        public EventEditType(int rawValue, int labelRes) {
            super(rawValue, labelRes);
        }

        public boolean isYearOptional() {
            return mYearOptional;
        }

        public EventEditType setYearOptional(boolean yearOptional) {
            mYearOptional = yearOptional;
            return this;
        }

        @Override
        public String toString() {
            return super.toString() + " mYearOptional=" + mYearOptional;
        }
    }

    /**
     * Description of a user-editable field on a {@link DataKind} row, such as
     * {@link Phone#NUMBER}. Includes flags to apply to an {@link EditText}, and
     * the column where this field is stored.
     */
    public static final class EditField {
        public String column;
        public int titleRes;
        public int inputType;
        public int minLines;
        public boolean optional;
        public boolean shortForm;
        public boolean longForm;

        public EditField(String column, int titleRes) {
            this.column = column;
            this.titleRes = titleRes;
        }

        public EditField(String column, int titleRes, int inputType) {
            this(column, titleRes);
            this.inputType = inputType;
        }

        public EditField setOptional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public EditField setShortForm(boolean shortForm) {
            this.shortForm = shortForm;
            return this;
        }

        public EditField setLongForm(boolean longForm) {
            this.longForm = longForm;
            return this;
        }

        public EditField setMinLines(int minLines) {
            this.minLines = minLines;
            return this;
        }

        public boolean isMultiLine() {
            return (inputType & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":"
                    + " column=" + column
                    + " titleRes=" + titleRes
                    + " inputType=" + inputType
                    + " minLines=" + minLines
                    + " optional=" + optional
                    + " shortForm=" + shortForm
                    + " longForm=" + longForm;
        }
    }

    /**
     * Generic method of inflating a given {@link Cursor} into a user-readable
     * {@link CharSequence}. For example, an inflater could combine the multiple
     * columns of {@link StructuredPostal} together using a string resource
     * before presenting to the user.
     */
    public interface StringInflater {
        public CharSequence inflateUsing(Context context, Cursor cursor);
        public CharSequence inflateUsing(Context context, ContentValues values);
    }

    /**
     * Compare two {@link AccountType} by their {@link AccountType#getDisplayLabel} with the
     * current locale.
     */
    public static class DisplayLabelComparator implements Comparator<AccountType> {
        private final Context mContext;
        /** {@link Comparator} for the current locale. */
        private final Collator mCollator = Collator.getInstance();

        public DisplayLabelComparator(Context context) {
            mContext = context;
        }

        private String getDisplayLabel(AccountType type) {
            CharSequence label = type.getDisplayLabel(mContext);
            return (label == null) ? "" : label.toString();
        }

        @Override
        public int compare(AccountType lhs, AccountType rhs) {
            return mCollator.compare(getDisplayLabel(lhs), getDisplayLabel(rhs));
        }
    }

}
