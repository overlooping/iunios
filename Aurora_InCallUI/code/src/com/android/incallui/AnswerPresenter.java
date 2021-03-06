/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui;

import android.content.Context;
import android.telecom.TelecomManager;

import java.util.List;

/**
 * Presenter for the Incoming call widget.
 */
public class AnswerPresenter extends Presenter<AnswerPresenter.AnswerUi>
        implements CallList.CallUpdateListener, CallList.Listener {

    private static final String TAG = AnswerPresenter.class.getSimpleName();

    private String mCallId;
    private Call mCall = null;
    private boolean mHasTextMessages = false;

    @Override
    public void onUiReady(AnswerUi ui) {
        super.onUiReady(ui);

        final CallList calls = CallList.getInstance();
        Call call;
        call = calls.getIncomingCall();
        if (call != null) {
            processIncomingCall(call);
        }
        call = calls.getVideoUpgradeRequestCall();
        if (call != null) {
            processVideoUpgradeRequestCall(call);
        }

        // Listen for incoming calls.
        calls.addListener(this);
    }

    @Override
    public void onUiUnready(AnswerUi ui) {
        super.onUiUnready(ui);

        CallList.getInstance().removeListener(this);

        // This is necessary because the activity can be destroyed while an incoming call exists.
        // This happens when back button is pressed while incoming call is still being shown.
        if (mCallId != null) {
            CallList.getInstance().removeCallUpdateListener(mCallId, this);
        }
    }

    @Override
    public void onCallListChange(CallList callList) {
        /// M: Added for DSDA case. @{
        if (getUi() == null) {
            Log.d(this, "onCallListChange, ui is null, do nothing! ");
            return;
        }

        // Show prompt message to user if needed.
        Call call = callList.getIncomingCall();
        Log.d(this, "onCallListChange " + call);
        if (call != null) {
            getUi().updatePromptMessage(!call.can(android.telecom.Call.Details.CAPABILITY_ANSWER));
            // When there has two incoming calls, after user switch these two
            // calls, we need update incoming call at here.
            if (!call.getId().equals(mCallId)) {
                processIncomingCall(call);
            }
        } else {
            getUi().updatePromptMessage(false);
        }
        /// @}
    }

    @Override
    public void onDisconnect(Call call) {
        // no-op
    }

    @Override
    public void onIncomingCall(Call call) {
        // TODO: Ui is being destroyed when the fragment detaches.  Need clean up step to stop
        // getting updates here.
        Log.d(this, "onIncomingCall: " + this);
        if (getUi() != null) {
            getUi().updatePromptMessage(!call.can(android.telecom.Call.Details.CAPABILITY_ANSWER));
            if (!call.getId().equals(mCallId)) {
                // A new call is coming in.
                // M: when another incoming call coming, need dismiss dialog.
                getUi().dismissPendingDialogues();
                processIncomingCall(call);
            }
        }
    }

    private void processIncomingCall(Call call) {
        mCallId = call.getId();
        mCall = call;

        // Listen for call updates for the current call.
        CallList.getInstance().addCallUpdateListener(mCallId, this);

        Log.d(TAG, "Showing incoming for call id: " + mCallId + " " + this);
        final List<String> textMsgs = CallList.getInstance().getTextResponses(call.getId());
        getUi().showAnswerUi(true);
        configureAnswerTargetsForSms(call, textMsgs);

        ///M: ALPS01856138
        // dismiss message dialog if showing call has not RESPOND_VIA_TEXT capability @{
        if (!call.can(android.telecom.Call.Details.CAPABILITY_RESPOND_VIA_TEXT)) {
            getUi().dismissPendingDialogues();
        }
        /// @}
    }

    private void processVideoUpgradeRequestCall(Call call) {
        mCallId = call.getId();
        mCall = call;

        // Listen for call updates for the current call.
        CallList.getInstance().addCallUpdateListener(mCallId, this);
        getUi().showAnswerUi(true);

        getUi().showTargets(AnswerFragment.TARGET_SET_FOR_VIDEO_UPGRADE_REQUEST);
    }

    @Override
    public void onCallChanged(Call call) {
        Log.d(this, "onCallStateChange() " + call + " " + this);
        /// M: For ALPS02014302 @{
        if (getUi() == null) {
            Log.d(this, "onCallChanged, ui is null, do nothing! ");
            return;
        }
        /// @}
        if (call.getState() != Call.State.INCOMING) {
            // Stop listening for updates.
            CallList.getInstance().removeCallUpdateListener(mCallId, this);

            final Call incomingCall = CallList.getInstance().getIncomingCall();
            if (incomingCall != null) {
                /// M: if foreground incoming call state changed, need dismiss dialog. @{
                if (call.getId().equals(mCallId)) {
                    getUi().dismissPendingDialogues();
                }
                /// @}
                processIncomingCall(incomingCall);
                return;
            }

            getUi().showAnswerUi(false);

            // mCallId will hold the state of the call. We don't clear the mCall variable here as
            // it may be useful for sending text messages after phone disconnects.
            mCallId = null;
            mHasTextMessages = false;
        } else if (!mHasTextMessages) {
            final List<String> textMsgs = CallList.getInstance().getTextResponses(call.getId());
            if (textMsgs != null) {
                configureAnswerTargetsForSms(call, textMsgs);
            }
        }
    }

    public void onAnswer(int videoState, Context context) {
        if (mCallId == null) {
            return;
        }

        Log.d(this, "onAnswer " + mCallId);
        if (mCall.getSessionModificationState()
                == Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
            InCallPresenter.getInstance().acceptUpgradeRequest(context);
        } else {
            TelecomAdapter.getInstance().answerCall(mCall.getId(), videoState);
        }
    }

    /**
     * TODO: We are using reject and decline interchangeably. We should settle on
     * reject since it seems to be more prevalent.
     */
    public void onDecline() {
        Log.d(this, "onDecline " + mCallId);
        TelecomAdapter.getInstance().rejectCall(mCall.getId(), false, null);
    }

    public void onText() {
        if (getUi() != null) {
            InCallPresenter.getInstance().getTelecomManager().silenceRinger();
            getUi().showMessageDialog();
        }
    }

    public void rejectCallWithMessage(String message) {
        Log.d(this, "sendTextToDefaultActivity()...");
        TelecomAdapter.getInstance().rejectCall(mCall.getId(), true, message);
        /// M: ALPS01766524. If Call ended, then send sms. @{
        if (mCall.getHandle() != null && mCall.can(android.telecom.Call.Details.CAPABILITY_RESPOND_VIA_TEXT)) {
            TelecomAdapter.getInstance().sendMessageIfCallEnded(getUi().getContext(),
                    mCall.getId(), mCall.getHandle().getSchemeSpecificPart(), message);
        }
        /// @}

        onDismissDialog();
    }

    public void onDismissDialog() {
        InCallPresenter.getInstance().onDismissDialog();
    }

    private void configureAnswerTargetsForSms(Call call, List<String> textMsgs) {
        final Context context = getUi().getContext();

        mHasTextMessages = textMsgs != null;
        boolean withSms =
                call.can(android.telecom.Call.Details.CAPABILITY_RESPOND_VIA_TEXT)
                && mHasTextMessages;
        if (call.isVideoCall(context)) {
            if (withSms) {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_VIDEO_WITH_SMS);
                getUi().configureMessageDialog(textMsgs);
            } else {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_VIDEO_WITHOUT_SMS);
            }
        } else {
            if (withSms) {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_AUDIO_WITH_SMS);
                getUi().configureMessageDialog(textMsgs);
            } else {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_AUDIO_WITHOUT_SMS);
            }
        }
    }

    interface AnswerUi extends Ui {
        public void showAnswerUi(boolean show);
        public void showTargets(int targetSet);
        public void showMessageDialog();
        public void configureMessageDialog(List<String> textResponses);
        public Context getContext();
        public void updatePromptMessage(boolean show);
        /// M: when another incoming call coming, need dismiss dialog @{
        public void dismissPendingDialogues();
        /// @}
    }

    @Override
    public void onStorageFull() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUpdateRecordState(int state, int customValue) {
        // TODO Auto-generated method stub
    }
}
