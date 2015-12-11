package com.microsoft.smartalarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.FeedbackManager;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.objects.FeedbackUserDataElement;

public class AlarmMainActivity extends AppCompatActivity
        implements AlarmListFragment.Callbacks,
        OnboardingTutorialFragment.OnOnboardingTutorialListener,
        OnboardingToSFragment.OnOnboardingToSListener {

    private boolean mOboardingStarted = false;
    public final static String SHOULD_ONBOARD = "onboarding";
    public final static String SHOULD_TOS = "show-tos";
    private SharedPreferences mPreferences = null;

    @Override
    public void onAlarmSelected(Alarm alarm, boolean newAlarm) {
        Intent intent = AlarmSettingsActivity.newIntent(this, alarm.getId(), newAlarm);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        String packageName = getApplicationContext().getPackageName();
        mPreferences = getSharedPreferences(packageName, MODE_PRIVATE);
        PreferenceManager.setDefaultValues(this, R.xml.pref_global, false);
        Logger.init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String hockeyappToken = Util.getToken(this, "hockeyapp");
        CrashManager.register(this, hockeyappToken);
        UpdateManager.register(this, hockeyappToken);
        if (mPreferences.getBoolean(SHOULD_ONBOARD, true)) {
            setStatusBarColor();
            if (!mOboardingStarted) {
                mOboardingStarted = true;

                Loggable.UserAction userAction = new Loggable.UserAction(Loggable.Key.ACTION_ONBOARDING);
                Logger.track(userAction);

                Fragment newFragment = new OnboardingTutorialFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment);
                transaction.commit();
            }
        }
        else if (mPreferences.getBoolean(SHOULD_TOS, true)) {
            setStatusBarColor();
            showToS(null);
        }
        else {
            showAlarmList();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        UpdateManager.unregister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FeedbackManager.unregister();
        Logger.flush();
    }

    public void showFeedback(MenuItem item){
        final String hockeyappToken = Util.getToken(this, "hockeyapp");
        FeedbackManager.register(this, hockeyappToken, null);
        FeedbackManager.setRequireUserEmail(FeedbackUserDataElement.OPTIONAL);
        FeedbackManager.showFeedbackActivity(this);
    }

    @Override
    public void onSkip(View view) {
        Loggable.UserAction userAction = new Loggable.UserAction(Loggable.Key.ACTION_ONBOARDING_SKIP);
        Logger.track(userAction);
        showToS(view);
    }

    @Override
    public void onAccept() {
        resetStatusBarColor();
        showAlarmList();
    }

    public void showToS(View view) {
        mPreferences.edit().putBoolean(SHOULD_ONBOARD, false).apply();
        Fragment newFragment = new OnboardingToSFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        if (view != null){
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void showAlarmList() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment = new AlarmListFragment();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
        setTitle(R.string.alarm_list_title);
    }

    private void setStatusBarColor(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.green1));
        }
    }

    private void resetStatusBarColor(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
}
