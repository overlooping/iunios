package com.aurora.mms.countmanage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


public class DataCount {
    
    private DataCount() {
    }
    
    public static ContentResolver resolver = null;
    public final static Uri m_uri = Uri.parse("content://com.iuni.reporter/module/");
    public static ContentResolver getResolver(Context context)
    {
        if(null == resolver)
            resolver = context.getContentResolver();
        return resolver;
        
    }

    public static int updataData(Context context,String module_id,String action_id,int value)
    {
        int count = 0;
        try {
            resolver = getResolver(context);
            ContentValues values = new ContentValues();
            values.put("module_key", module_id);
            values.put("item_tag", action_id);
            values.put("value", value);

            count = resolver.update(m_uri, values, null, null);

            Cursor cursor = context.getContentResolver().query(
                    m_uri,
                    null,
                    "module_key" + " = '" + module_id + "' and " + "item_tag" + " = '"
                            + action_id + "'", null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    Log.v("qiaohu"," count ="+ cursor.getInt(cursor.getColumnIndex("value")));
                }
                cursor.close();
            }
        } catch (java.lang.IllegalArgumentException e) {
            e.printStackTrace();
        }
        return count;
    }
    
}