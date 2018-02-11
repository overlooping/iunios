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
package com.android.contacts.interactions;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsApplication;
import com.android.contacts.R;

import aurora.app.AuroraAlertDialog; // import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

/**
 * A dialog for deleting a group.
 */
public class GroupDeletionDialogFragment extends DialogFragment {

    private static final String ARG_GROUP_ID = "groupId";
    private static final String ARG_LABEL = "label";
    private static final String ARG_SHOULD_END_ACTIVITY = "endActivity";

    public static void show(FragmentManager fragmentManager, long groupId, String label,
            boolean endActivity) {
        GroupDeletionDialogFragment dialog = new GroupDeletionDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_GROUP_ID, groupId);
        args.putString(ARG_LABEL, label);
        args.putBoolean(ARG_SHOULD_END_ACTIVITY, endActivity);
        dialog.setArguments(args);
        dialog.show(fragmentManager, "deleteGroup");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String label = getArguments().getString(ARG_LABEL);
        String message = getActivity().getString(R.string.aurora_delete_group_dialog_message);

        return new AuroraAlertDialog.Builder(getActivity(), AuroraAlertDialog.THEME_AMIGO_FULLSCREEN)
//                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(message)
//                .setMessage(message)
                .setPositiveButton(R.string.delete,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            deleteGroup();
                        }
                    }
                )
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    protected void deleteGroup() {
        Bundle arguments = getArguments();
        long groupId = arguments.getLong(ARG_GROUP_ID);

        /*
		 * New feature by Mediatek Begin
		 * Original Android code:
		 *  getActivity().startService(ContactSaveService.createGroupDeletionIntent(
         *       getActivity(), groupId));
		 */
        String groupName = arguments.getString(ARG_LABEL);
        int simId = arguments.getInt(ARG_SIM_ID);
        int slotId = arguments.getInt(ARG_SLOT_ID);
        getActivity().startService(ContactSaveService.createGroupDeletionIntent(
                getActivity(), groupId, simId, slotId, groupName));
    	/*
		 * New feature by Mediatek End
		 */

        if (shouldEndActivity()) {
            getActivity().finish();
        }
        
        // aurora <wangth> <2013-11-11> add for aurora begin
        Toast.makeText(ContactsApplication.getInstance().getApplicationContext(), 
                ContactsApplication.getInstance().
                getResources().getString(R.string.aurora_delete_group_toast, 
                        groupName), Toast.LENGTH_SHORT).show();
        // aurora <wangth> <2013-11-11> add for aurora end
    }

    private boolean shouldEndActivity() {
        return getArguments().getBoolean(ARG_SHOULD_END_ACTIVITY);
    }

    // The following lines are provided and maintained by Mediatek Inc.
    private static final String ARG_SLOT_ID = "slotId";
    private static final String ARG_SIM_ID = "simId";
    public static void show(FragmentManager fragmentManager, long groupId, String label,
            boolean endActivity, int simId, int slotId) {
        GroupDeletionDialogFragment dialog = new GroupDeletionDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_GROUP_ID, groupId);
        args.putString(ARG_LABEL, label);
        args.putBoolean(ARG_SHOULD_END_ACTIVITY, endActivity);
        args.putInt(ARG_SIM_ID, simId);
        args.putInt(ARG_SLOT_ID, slotId);
        dialog.setArguments(args);
        dialog.show(fragmentManager, "deleteGroup");
    }

    // The previous  lines are provided and maintained by Mediatek Inc.
}
