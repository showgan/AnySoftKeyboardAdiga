/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.ui.dev;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.chewbacca.ChewbaccaUtils;
import com.anysoftkeyboard.rx.RxSchedulers;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;
import com.f2prateek.rx.preferences2.Preference;
import com.mastegoane.android.anysoftkeyboard.AnyApplication;
import com.mastegoane.android.anysoftkeyboard.BuildConfig;
import com.mastegoane.android.anysoftkeyboard.R;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import java.io.File;
import net.evendanan.pixel.GeneralDialogController;
import net.evendanan.pixel.RxProgressDialog;

@SuppressLint("SetTextI18n")
public class DeveloperToolsFragment extends Fragment implements View.OnClickListener {

    private static final int TRACING_ENABLED_DIALOG = 123;
    private static final int TRACING_STARTED_DIALOG = 124;

    private GeneralDialogController mGeneralDialogController;
    private Button mFlipper;
    private View mProgressIndicator;
    private View mShareButton;
    @NonNull private Disposable mDisposable = Disposables.empty();
    private Preference<Boolean> mStrictModePrefs;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.developer_tools, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGeneralDialogController =
                new GeneralDialogController(
                        getActivity(), R.style.Theme_AskAlertDialog, this::setupDialog);
        ((TextView) view.findViewById(R.id.dev_title))
                .setText(DeveloperUtils.getAppDetails(requireContext().getApplicationContext()));

        mFlipper = view.findViewById(R.id.dev_flip_trace_file);
        mProgressIndicator = view.findViewById(R.id.dev_tracing_running_progress_bar);
        mShareButton = view.findViewById(R.id.dev_share_trace_file);

        view.findViewById(R.id.memory_dump_button).setOnClickListener(this);
        view.findViewById(R.id.dev_share_mem_file).setOnClickListener(this);
        view.findViewById(R.id.dev_flip_trace_file).setOnClickListener(this);
        view.findViewById(R.id.dev_share_trace_file).setOnClickListener(this);
        view.findViewById(R.id.show_logcat_button).setOnClickListener(this);
        view.findViewById(R.id.share_logcat_button).setOnClickListener(this);

        TextView textWithListener = view.findViewById(R.id.actionDoneWithListener);
        textWithListener.setOnEditorActionListener(
                (textView, i, keyEvent) -> {
                    Toast.makeText(
                                    requireContext().getApplicationContext(),
                                    "OnEditorActionListener i:" + i,
                                    Toast.LENGTH_SHORT)
                            .show();
                    return true;
                });

        mStrictModePrefs =
                AnyApplication.prefs(requireContext())
                        .getBoolean(
                                R.string.settings_key_strict_mode_enabled,
                                R.bool.settings_default_false);
        CheckBox strictMode = view.findViewById(R.id.enable_strict_mode_checkbox);
        strictMode.setChecked(mStrictModePrefs.get());
        strictMode.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    mStrictModePrefs.set(isChecked);
                    Toast.makeText(
                                    requireContext(),
                                    R.string.developer_strict_mode_change_restart,
                                    Toast.LENGTH_LONG)
                            .show();
                });
        if (!BuildConfig.DEBUG) {
            strictMode.setVisibility(View.GONE);
        }
    }

    private void setupDialog(
            Context context, AlertDialog.Builder builder, int optionId, Object data) {
        switch (optionId) {
            case TRACING_ENABLED_DIALOG:
                builder.setIcon(R.drawable.notification_icon_beta_version)
                        .setTitle("How to use Tracing")
                        .setMessage(
                                "Tracing is now enabled, but not started!"
                                        + DeveloperUtils.NEW_LINE
                                        + "To start tracing, you'll need to restart AnySoftKeyboard. How? Either reboot your phone, or switch to another keyboard app (like the stock)."
                                        + DeveloperUtils.NEW_LINE
                                        + "To stop tracing, first disable it, and then restart AnySoftKeyboard (as above)."
                                        + DeveloperUtils.NEW_LINE
                                        + "Thanks!!")
                        .setPositiveButton("Got it!", null);
                break;
            case TRACING_STARTED_DIALOG:
                builder.setIcon(R.drawable.notification_icon_beta_version)
                        .setTitle("How to stop Tracing")
                        .setMessage(
                                "Tracing is now disabled, but not ended!"
                                        + DeveloperUtils.NEW_LINE
                                        + "To end tracing (and to be able to send the file), you'll need to restart AnySoftKeyboard. How? Either reboot your phone (preferable), or switch to another "
                                        + "keyboard app (like the stock)."
                                        + DeveloperUtils.NEW_LINE
                                        + "Thanks!!")
                        .setPositiveButton("Got it!", null);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown option-id " + optionId + " for setupDialog");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateTracingState();
        MainSettingsActivity.setActivityTitle(this, getString(R.string.developer_tools));
    }

    @Override
    public void onStop() {
        super.onStop();
        mGeneralDialogController.dismiss();
    }

    private void updateTracingState() {
        if (DeveloperUtils.hasTracingRequested(requireContext().getApplicationContext())) {
            mFlipper.setText("Disable tracing");
        } else {
            mFlipper.setText("Enable tracing");
        }

        if (DeveloperUtils.hasTracingStarted()) {
            mProgressIndicator.setVisibility(View.VISIBLE);
        } else {
            mProgressIndicator.setVisibility(View.INVISIBLE);
        }

        mShareButton.setEnabled(
                !DeveloperUtils.hasTracingStarted() && DeveloperUtils.getTraceFile().exists());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.memory_dump_button:
                onUserClickedMemoryDump();
                break;
            case R.id.dev_share_mem_file:
                onUserClickedShareMemoryDump(v);
                break;
            case R.id.dev_flip_trace_file:
                onUserClickedFlipTracing();
                break;
            case R.id.dev_share_trace_file:
                onUserClickedShareTracingFile();
                break;
            case R.id.show_logcat_button:
                onUserClickedShowLogCat();
                break;
            case R.id.share_logcat_button:
                onUserClickedShareLogCat();
                break;
            default:
                throw new IllegalArgumentException(
                        "Failed to handle " + v.getId() + " in DeveloperToolsFragment");
        }
    }

    private void onUserClickedMemoryDump() {
        final Context applicationContext = requireContext().getApplicationContext();

        mDisposable.dispose();
        mDisposable =
                RxProgressDialog.create(this, requireActivity(), R.layout.progress_window)
                        .subscribeOn(RxSchedulers.background())
                        .map(fragment -> Pair.create(fragment, DeveloperUtils.createMemoryDump()))
                        .observeOn(RxSchedulers.mainThread())
                        .subscribe(
                                pair -> {
                                    Toast.makeText(
                                                    applicationContext,
                                                    getString(
                                                            R.string.created_mem_dump_file,
                                                            pair.second.getAbsolutePath()),
                                                    Toast.LENGTH_LONG)
                                            .show();
                                    View shareMemFile =
                                            pair.first
                                                    .getView()
                                                    .findViewById(R.id.dev_share_mem_file);
                                    shareMemFile.setTag(pair.second);
                                    shareMemFile.setEnabled(
                                            pair.second.exists() && pair.second.isFile());
                                },
                                throwable ->
                                        Toast.makeText(
                                                        applicationContext,
                                                        getString(
                                                                R.string.failed_to_create_mem_dump,
                                                                throwable.getMessage()),
                                                        Toast.LENGTH_LONG)
                                                .show());
    }

    private void onUserClickedShareMemoryDump(View v) {
        File memDump = (File) v.getTag();

        shareFile(
                memDump,
                "AnySoftKeyboard Memory Dump File",
                "Hi! Here is a memory dump file for "
                        + DeveloperUtils.getAppDetails(requireContext().getApplicationContext())
                        + Logger.NEW_LINE
                        + ChewbaccaUtils.getSysInfo(getActivity()));
    }

    private void onUserClickedFlipTracing() {
        final boolean enable =
                !DeveloperUtils.hasTracingRequested(requireContext().getApplicationContext());
        DeveloperUtils.setTracingRequested(requireContext().getApplicationContext(), enable);

        updateTracingState();

        if (enable) {
            mGeneralDialogController.showDialog(TRACING_ENABLED_DIALOG);
        } else if (DeveloperUtils.hasTracingStarted()) {
            mGeneralDialogController.showDialog(TRACING_STARTED_DIALOG);
        }
    }

    private void onUserClickedShareTracingFile() {
        shareFile(
                DeveloperUtils.getTraceFile(),
                "AnySoftKeyboard Trace File",
                "Hi! Here is a tracing file for "
                        + DeveloperUtils.getAppDetails(requireContext().getApplicationContext())
                        + DeveloperUtils.NEW_LINE
                        + ChewbaccaUtils.getSysInfo(requireContext()));
    }

    private void onUserClickedShowLogCat() {
        Navigation.findNavController(requireView())
                .navigate(
                        DeveloperToolsFragmentDirections
                                .actionDeveloperToolsFragmentToLogCatViewFragment());
    }

    private void onUserClickedShareLogCat() {
        shareFile(
                null,
                "AnySoftKeyboard LogCat",
                "Hi! Here is a LogCat snippet for "
                        + DeveloperUtils.getAppDetails(requireContext().getApplicationContext())
                        + DeveloperUtils.NEW_LINE
                        + ChewbaccaUtils.getSysInfo(requireContext())
                        + DeveloperUtils.NEW_LINE
                        + Logger.getAllLogLines());
    }

    private void shareFile(File fileToShare, String title, String message) {
        Intent sendMail = new Intent();
        sendMail.setAction(Intent.ACTION_SEND);
        sendMail.setType("plain/text");
        sendMail.putExtra(Intent.EXTRA_SUBJECT, title);
        sendMail.putExtra(Intent.EXTRA_TEXT, message);
        if (fileToShare != null) {
            sendMail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(fileToShare));
        }

        try {
            Intent sender = Intent.createChooser(sendMail, "Share");
            sender.putExtra(Intent.EXTRA_SUBJECT, title);
            sender.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(sender);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(
                            requireContext().getApplicationContext(),
                            "Unable to send bug report via e-mail!",
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onDestroy() {
        mDisposable.dispose();
        super.onDestroy();
    }
}
