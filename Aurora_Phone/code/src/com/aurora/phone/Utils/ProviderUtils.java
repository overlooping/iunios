package com.android.phone;  
  
import com.android.internal.telephony.*;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;
import gionee.provider.GnContactsContract.PhoneLookup;
import gionee.provider.GnContactsContract.RawContacts;
import gionee.provider.GnCallLog.Calls;
  
public class ProviderUtils {  
    private static final String TAG = "ProviderUtils";


 	public static int isSimCardPhoneNumber(Context context, String number) {
 		int slot = -1;
 		if(!PhoneUtils.isMultiSimEnabled()) {
 			return slot;
 		}
 		Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI,
 				new String[] { Data.RAW_CONTACT_ID },
 				Data.DATA1 + " = '" + number + "'", null, null);
 		if (cursor != null) {
 			Log.v(TAG, "cursor count=" + cursor.getCount());
 			if (cursor.moveToFirst()) {
 				long contact_id = cursor.getLong(0);
 				cursor.close();
 				Cursor cursor2 = context.getContentResolver().query(
 						RawContacts.CONTENT_URI,
 						new String[] { RawContacts.INDICATE_PHONE_SIM },
 						android.provider.ContactsContract.RawContacts._ID
 								+ " = " + contact_id + " AND " + "deleted"
 								+ " < 1" +  " and is_privacy > -1", null, null);
 				Log.v(TAG, "cursor2 count=" + cursor2.getCount()
 						+ "contact_id=" + contact_id);
 				if (cursor2 != null) {
 					if (cursor2.moveToFirst()) {
 						int simid = cursor2.getInt(0);
 						Log.v("CallCard", "simid=" + simid);
 						cursor2.close();
 						if(simid >= 0){ 							
 							return AuroraSubUtils.getSlotBySubId(context, simid);
 						}
 					}
 					cursor2.close();
 				}
 			} else {
 				cursor.close();
 			}
 		}
 		return slot;

 	}
    
	public static int isSimCardPhoneNumber(Context context, Call call) {
		if(!PhoneUtils.isMultiSimEnabled()) {
			return -1;
		}

		return isSimCardPhoneNumber(context, getNumberFromCall(call));
	}
	
	
	public static String getNumberFromCall(Call call){
		Connection conn = null;
		int phoneType = call.getPhone().getPhoneType();
		if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
			conn = call.getLatestConnection();
		} else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
				|| (phoneType == PhoneConstants.PHONE_TYPE_SIP)
				 || phoneType == PhoneConstants.PHONE_TYPE_IMS) {
			conn = call.getEarliestConnection();
		} else {
			throw new IllegalStateException("Unexpected phone type: "
					+ phoneType);
		}
		return conn.getAddress();
	}

	   public static long queryForRawContactId(ContentResolver cr, long contactId) {
	        Cursor rawContactIdCursor = null;
	        long rawContactId = -1;
	        
	        try {
	            rawContactIdCursor = cr.query(RawContacts.CONTENT_URI,
	                    new String[] {RawContacts._ID},
	                    RawContacts.CONTACT_ID + "=" + contactId + " and is_privacy > -1 ", null, null);
	            if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
	                // Just return the first one.
	                rawContactId = rawContactIdCursor.getLong(0);
	            }
	        } finally {
	            if (rawContactIdCursor != null) {
	                rawContactIdCursor.close();
	            }
	        }
	        return rawContactId;
	    }
    
	   public static long getRawContactId(Uri uri) {
			Log.v("getRawContactId", " uri = " + uri);
			if(uri == null) {
				return 0;
			}
			
	        String url = uri.toString();
			
			if (url.startsWith("content://com.android.contacts/contacts/lookup/") || url.startsWith("content://com.android.contacts/contacts/")) {
				long RawContactId = queryForRawContactId(PhoneGlobals.getInstance().getContentResolver(), Long.parseLong(uri.getLastPathSegment()));
				return RawContactId;
			} else if (url.startsWith("content://com.android.contacts/phone_lookup/")) {
				return getRawContactIdByNumber(uri.getLastPathSegment());
			} else {
				Cursor cursor = PhoneGlobals.getInstance().getContentResolver().query(uri, new String[] { "raw_contact_id"},  null, null, null);
				try {
					if (cursor != null && cursor.getCount() > 0) {
						cursor.moveToFirst();
						return cursor.getLong(0);
					}
					return 0;
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
	    }

	    private static long getRawContactIdByNumber(String number) { 
			Log.v("getRawContactIdByNumber", " number = " + number);
			
			if(TextUtils.isEmpty(number)) {
				return 0;
			}

			Cursor cursor = PhoneGlobals.getInstance().getContentResolver().query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, number), 
					new String[]{"raw_contact_id"},
					null, null, null);
			try {
				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					return cursor.getLong(0);
				}
		    	return 0;
			} finally {
				if(cursor != null) {
					cursor.close();
				}
			}    
	    }
	    
	    public static long getDataIdByRawContactId(long rawContactId) {
			Log.v("getDataIdByRawContactId", " rawContactId = " + rawContactId);
	        long result = -1;
	        if (rawContactId <= 0) {
	            return result;
	        }
	        
	        Cursor cursor = PhoneGlobals.getInstance().getContentResolver().query(Data.CONTENT_URI,
					new String[] {Data._ID},
					Calls.RAW_CONTACT_ID + " = " + rawContactId + " AND is_privacy > -1",
					null, null);    
	        if (null != cursor) {
	            if (cursor.moveToFirst()) {
	                result = cursor.getLong(0);
	            }
	            cursor.close();
	        }
			Log.v("getDataIdByRawContactId", " dataid = " + result);
	        return result;
	    }
	    
	    public static boolean isNotToUseContactsProvier() {
	    	return 	PhoneGlobals.getInstance().mManageVcardReading.isInLoadProcess();
	    }
	    
}  