/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.email.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.android.email.LegacyConversions;
import com.android.email.service.ImapAuthenticatorService;
import com.android.email.service.ImapService;
import com.android.emailcommon.Logging;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.Message;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.mail.Part;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.utility.ConversionUtilities;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;

public class Utilities {
    /**
     * Copy one downloaded message (which may have partially-loaded sections)
     * into a newly created EmailProvider Message, given the account and mailbox
     *
     * @param message the remote message we've just downloaded
     * @param account the account it will be stored into
     * @param folder the mailbox it will be stored into
     * @param loadStatus when complete, the message will be marked with this status (e.g.
     *        EmailContent.Message.LOADED)
     */
	 //paul modify , for pop3
    public static void copyOneMessageToProvider(Context context, Message message, Account account,
            Mailbox folder, int loadStatus, boolean isPop3) {
        EmailContent.Message localMessage = null;
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    EmailContent.Message.CONTENT_URI,
                    EmailContent.Message.CONTENT_PROJECTION,
                    EmailContent.MessageColumns.ACCOUNT_KEY + "=?" +
                            " AND " + MessageColumns.MAILBOX_KEY + "=?" +
                            " AND " + SyncColumns.SERVER_ID + "=?",
                            new String[] {
                            String.valueOf(account.mId),
                            String.valueOf(folder.mId),
                            String.valueOf(message.getUid())
                    },
                    null);
            if (c == null) {
                return;
            } else if (c.moveToNext()) {
                localMessage = EmailContent.getContent(c, EmailContent.Message.class);
            } else {
                localMessage = new EmailContent.Message();
            }
            localMessage.mMailboxKey = folder.mId;
            localMessage.mAccountKey = account.mId;
            copyOneMessageToProvider(context, message, localMessage, loadStatus, isPop3);
        } finally {
            if (c != null) {
                c.close();
            }
            //--------add by tangjie 2014/11/19 start-------//
            AuroraAutoCompleteDBHelper dbHelper = AuroraAutoCompleteDBHelper.getInstance(context);
            String tableName = account.getEmailAddress().replace("@", "").replace(".", "");
            dbHelper.saveAddress(message, tableName);
          //--------add by tangjie 2014/11/19 end-------//
        }
    }

    /**
     * Copy one downloaded message (which may have partially-loaded sections)
     * into an already-created EmailProvider Message
     *
     * @param message the remote message we've just downloaded
     * @param localMessage the EmailProvider Message, already created
     * @param loadStatus when complete, the message will be marked with this status (e.g.
     *        EmailContent.Message.LOADED)
     * @param context the context to be used for EmailProvider
     */
	 //paul modify , for pop3
    //cjs add synchronized 2015-11-9
    public synchronized static void copyOneMessageToProvider(Context context, Message message,
            EmailContent.Message localMessage, int loadStatus, boolean isPop3) {
        try {
            EmailContent.Body body = null;
            if (localMessage.mId != EmailContent.Message.NO_MESSAGE) {
                body = EmailContent.Body.restoreBodyWithMessageId(context, localMessage.mId);
            }
            if (body == null) {
                body = new EmailContent.Body();
            }
            try {
                // Copy the fields that are available into the message object
                LegacyConversions.updateMessageFields(localMessage, message,
                        localMessage.mAccountKey, localMessage.mMailboxKey);
                // Now process body parts & attachments
                ArrayList<Part> viewables = new ArrayList<Part>();
                ArrayList<Part> attachments = new ArrayList<Part>();
                MimeUtility.collectParts(message, viewables, attachments);

                final ConversionUtilities.BodyFieldData data =
                        ConversionUtilities.parseBodyFields(viewables);

                // set body and local message values
                localMessage.setFlags(data.isQuotedReply, data.isQuotedForward);
                localMessage.mSnippet = data.snippet;
                body.mTextContent = data.textContent;
                body.mHtmlContent = data.htmlContent;
                body.mHtmlReply = data.htmlReply;
                body.mTextReply = data.textReply;
                body.mIntroText = data.introText;

                // Commit the message & body to the local store immediately
                saveOrUpdate(localMessage, context);
                body.mMessageKey = localMessage.mId;
               
                // process (and save) attachments
                if (loadStatus != EmailContent.Message.FLAG_LOADED_PARTIAL
                        && loadStatus != EmailContent.Message.FLAG_LOADED_UNKNOWN) {
                    // TODO(pwestbro): What should happen with unknown status?
                    if(isPop3){
						body.mHtmlContent = LegacyConversions.updateAttachments(context, localMessage, attachments, body.mHtmlContent);
                    }else{
                    	LegacyConversions.updateAttachments(context, localMessage, attachments, null);
                    }
                } else {
                    EmailContent.Attachment att = new EmailContent.Attachment();
                    // Since we haven't actually loaded the attachment, we're just putting
                    // a dummy placeholder here. When the user taps on it, we'll load the attachment
                    // for real.
                    // TODO: This is not a great way to model this. What we're saying is, we don't
                    // have the complete message, without paying any attention to what we do have.
                    // Did the main body exceed the maximum initial size? If so, we really might
                    // not have any attachments at all, and we just need a button somewhere that
                    // says "load the rest of the message".
                    // Or, what if we were able to load some, but not all of the attachments?
                    // Then we should ideally not be dropping the data we have on the floor.
                    // Also, what behavior we have here really should be based on what protocol
                    // we're dealing with. If it's POP, then we don't actually know how many
                    // attachments we have until we've loaded the complete message.
                    // If it's IMAP, we should know that, and we should load all attachment
                    // metadata we can get, regardless of whether or not we have the complete
                    // message body.
                    att.mFileName = "";
                    att.mSize = message.getSize();
                    att.mMimeType = "text/plain";
                    att.mMessageKey = localMessage.mId;
                    att.mAccountKey = localMessage.mAccountKey;
                    att.mFlags = Attachment.FLAG_DUMMY_ATTACHMENT;
                    att.save(context);
                    localMessage.mFlagAttachment = 1;//paul modify
                }
				
				saveOrUpdate(body, context);
                // One last update of message with two updated flags
                localMessage.mFlagLoaded = loadStatus;

                ContentValues cv = new ContentValues();
                cv.put(EmailContent.MessageColumns.FLAG_ATTACHMENT, localMessage.mFlagAttachment);
                cv.put(EmailContent.MessageColumns.FLAG_LOADED, localMessage.mFlagLoaded);
                Uri uri = ContentUris.withAppendedId(EmailContent.Message.CONTENT_URI,
                        localMessage.mId);
                context.getContentResolver().update(uri, cv, null, null);

            } catch (MessagingException me) {
                LogUtils.e(Logging.LOG_TAG, "Error while copying downloaded message." + me);
            }

        } catch (RuntimeException rte) {
            LogUtils.e(Logging.LOG_TAG, "Error while storing downloaded message." + rte.toString());
        } catch (IOException ioe) {
            LogUtils.e(Logging.LOG_TAG, "Error while storing attachment." + ioe.toString());
        }
        
    }

    public static void saveOrUpdate(EmailContent content, Context context) {
        if (content.isSaved()) {
        	//Log.i("SQF_LOG", "Utilities::saveOrUpdate ----> toContentValues");
            content.update(context, content.toContentValues());
        } else {
            content.save(context);
        }
    }

}
