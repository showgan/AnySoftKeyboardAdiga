package com.anysoftkeyboard.ui.settings.setup;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.DemoAnyKeyboardView;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;
import com.mastegoane.android.anysoftkeyboard.AnyApplication;
import com.mastegoane.android.anysoftkeyboard.R;

public class WizardPageDoneAndMoreSettingsFragment extends WizardPageBaseFragment
        implements View.OnClickListener {

    private DemoAnyKeyboardView mDemoAnyKeyboardView;

    @Override
    protected int getPageLayoutId() {
        return R.layout.keyboard_setup_wizard_page_additional_settings_layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.go_to_languages_action).setOnClickListener(this);
        view.findViewById(R.id.go_to_theme_action).setOnClickListener(this);
        view.findViewById(R.id.go_to_all_settings_action).setOnClickListener(this);

        mDemoAnyKeyboardView = view.findViewById(R.id.demo_keyboard_view);
    }

    @Override
    protected boolean isStepCompleted(@NonNull Context context) {
        return false; // this step is never done! You can always configure more :)
    }

    @Override
    public void onClick(View v) {
        final AppCompatActivity activity = (AppCompatActivity) requireActivity();
        switch (v.getId()) {
            case R.id.go_to_languages_action:
                startActivity(
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                        requireContext()
                                                .getString(R.string.deeplink_url_keyboards)),
                                requireContext(),
                                MainSettingsActivity.class));
                break;
            case R.id.go_to_theme_action:
                startActivity(
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(requireContext().getString(R.string.deeplink_url_themes)),
                                requireContext(),
                                MainSettingsActivity.class));
                break;
            case R.id.go_to_all_settings_action:
                startActivity(new Intent(getContext(), MainSettingsActivity.class));
                // not returning to this Activity any longer.
                activity.finish();
                break;
            default:
                throw new IllegalArgumentException(
                        "Failed to handle "
                                + v.getId()
                                + " in WizardPageDoneAndMoreSettingsFragment");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AnyKeyboard defaultKeyboard =
                AnyApplication.getKeyboardFactory(requireContext())
                        .getEnabledAddOn()
                        .createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
        defaultKeyboard.loadKeyboard(mDemoAnyKeyboardView.getThemedKeyboardDimens());
        mDemoAnyKeyboardView.setKeyboard(defaultKeyboard, null, null);

        SetupSupport.popupViewAnimationWithIds(
                getView(),
                R.id.go_to_languages_action,
                0,
                R.id.go_to_theme_action,
                0,
                R.id.go_to_all_settings_action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDemoAnyKeyboardView.onViewNotRequired();
    }
}
