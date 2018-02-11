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
package com.android.contacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

/**
 * A cursor adapter for the {@link Email#CONTENT_TYPE} content type.
 */
public class EmailAddressListAdapter extends ContactEntryListAdapter {

    protected static class EmailQuery {
        private static final String[] PROJECTION_PRIMARY = new String[] {
            Email._ID,                       // 0
            Email.TYPE,                      // 1
            Email.LABEL,                     // 2
            Email.DATA,                      // 3
            Email.PHOTO_ID,                  // 4
            Email.DISPLAY_NAME_PRIMARY,      // 5
            //The following lines are provided and maintained by Mediatek inc.
            RawContacts.INDICATE_PHONE_SIM,     // 8
            //The following lines are provided and maintained by Mediatek inc.
        };

        private static final String[] PROJECTION_ALTERNATIVE = new String[] {
            Email._ID,                       // 0
            Email.TYPE,                      // 1
            Email.LABEL,                     // 2
            Email.DATA,                      // 3
            Email.PHOTO_ID,                  // 4
            Email.DISPLAY_NAME_ALTERNATIVE,  // 5
            //The following lines are provided and maintained by Mediatek inc.
            RawContacts.INDICATE_PHONE_SIM,     // 8
            //The following lines are provided and maintained by Mediatek inc.
        };

         
        private static final int EMAIL_ID           = 0;
         
        private static final int EMAIL_TYPE         = 1;
         
        private static final int EMAIL_LABEL        = 2;
         
        private static final int EMAIL_ADDRESS      = 3;
        private static final int EMAIL_PHOTO_ID     = 4;
        private static final int EMAIL_DISPLAY_NAME = 5;
        //The following lines are provided and maintained by Mediatek inc.
        private static final int EMAIL_INDICATE_PHONE_SIM_COLUMN_INDEX = 6;
        //The following lines are provided and maintained by Mediatek inc.
    }

    private final CharSequence mUnknownNameText;

    public EmailAddressListAdapter(Context context) {
        super(context);

        mUnknownNameText = context.getText(android.R.string.unknownName);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        final Builder builder;
        if (isSearchMode()) {
            builder = Email.CONTENT_FILTER_URI.buildUpon();
            String query = getQueryString();
            builder.appendPath(TextUtils.isEmpty(query) ? "" : query);
        } else {
            builder = Email.CONTENT_URI.buildUpon();
        }
        builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                String.valueOf(directoryId));
        builder.appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true");
        loader.setUri(builder.build());

        if (getContactNameDisplayOrder() == ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY) {
            loader.setProjection(EmailQuery.PROJECTION_PRIMARY);
        } else {
            loader.setProjection(EmailQuery.PROJECTION_ALTERNATIVE);
        }

        if (getSortOrder() == ContactsContract.Preferences.SORT_ORDER_PRIMARY) {
            loader.setSortOrder(Email.SORT_KEY_PRIMARY);
        } else {
            loader.setSortOrder(Email.SORT_KEY_ALTERNATIVE);
        }
    }

    protected static Builder buildSectionIndexerUri(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true");
    }

    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(EmailQuery.EMAIL_DISPLAY_NAME);
    }

    /**
     * Builds a {@link Data#CONTENT_URI} for the current cursor
     * position.
     */
    public Uri getDataUri(int position) {
        long id = ((Cursor)getItem(position)).getLong(EmailQuery.EMAIL_ID);
        return ContentUris.withAppendedId(Data.CONTENT_URI, id);
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position,
            ViewGroup parent) {
        final ContactListItemView view = new ContactListItemView(context, null);
        view.setUnknownNameText(mUnknownNameText);
        view.setQuickContactEnabled(isQuickContactEnabled());
        return view;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        ContactListItemView view = (ContactListItemView)itemView;
        bindSectionHeaderAndDivider(view, position);
        bindName(view, cursor);
        bindPhoto(view, cursor);
        bindEmailAddress(view, cursor);
    }

    protected void bindEmailAddress(ContactListItemView view, Cursor cursor) {
        CharSequence label = null;
        if (!cursor.isNull(EmailQuery.EMAIL_TYPE)) {
            final int type = cursor.getInt(EmailQuery.EMAIL_TYPE);
            final String customLabel = cursor.getString(EmailQuery.EMAIL_LABEL);

            // TODO cache
            label = Email.getTypeLabel(getContext().getResources(), type, customLabel);
        }
        view.setLabel(label);
        view.showData(cursor, EmailQuery.EMAIL_ADDRESS);
    }

    protected void bindSectionHeaderAndDivider(final ContactListItemView view, int position) {
        final int section = getSectionForPosition(position);
        if (getPositionForSection(section) == position) {
            String title = (String)getSections()[section];
            view.setSectionHeader(title);
        } else {
            view.setDividerVisible(false);
            view.setSectionHeader(null);
        }

        // move the divider for the last item in a section
        if (getPositionForSection(section + 1) - 1 == position) {
            view.setDividerVisible(false);
        } else {
            view.setDividerVisible(true);
        }
    }

    protected void bindName(final ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, EmailQuery.EMAIL_DISPLAY_NAME, getContactNameDisplayOrder());
    }

    protected void bindPhoto(final ContactListItemView view, Cursor cursor) {
        long photoId = 0;
        if (!cursor.isNull(EmailQuery.EMAIL_PHOTO_ID)) {
            photoId = cursor.getLong(EmailQuery.EMAIL_PHOTO_ID);
        }

        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     
         *   CR ID: ALPS00112776
         *   Descriptions: sim card icon display not right
         */
        int indicatePhoneSim = cursor.getInt(EmailQuery.EMAIL_INDICATE_PHONE_SIM_COLUMN_INDEX);
        if (indicatePhoneSim > 0) {
            photoId = getSimType(indicatePhoneSim);
        }
        /*
         * Bug Fix by Mediatek End.
         */
        
        getPhotoLoader().loadPhoto(view.getPhotoView(), photoId, false, false);
    }
//
//    protected void bindSearchSnippet(final ContactListItemView view, Cursor cursor) {
//        view.showSnippet(cursor, SUMMARY_SNIPPET_MIMETYPE_COLUMN_INDEX,
//                SUMMARY_SNIPPET_DATA1_COLUMN_INDEX, SUMMARY_SNIPPET_DATA4_COLUMN_INDEX);
//    }

}
