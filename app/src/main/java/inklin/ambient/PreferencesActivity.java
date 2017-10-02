package inklin.ambient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import java.net.URISyntaxException;

public class PreferencesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
    }

    public void showInfo(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.about_dialog_title));
        builder.setMessage(getString(R.string.about_dialog_message));
        builder.setNeutralButton(R.string.about_dialog_github, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri content_url = Uri.parse("https://github.com/acaoairy/ambient");
                intent.setData(content_url);
                startActivity(Intent.createChooser(intent, null));
            }
        });
        builder.setNegativeButton(R.string.about_dialog_support, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String intentFullUrl = "intent://platformapi/startapp?saId=10000007&" +
                        "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2FFKX04432XWNQIFV2UDCR64%3F_s" +
                        "%3Dweb-other&_t=1472443966571#Intent;" +
                        "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
                try {
                    Intent intent = Intent.parseUri(intentFullUrl, Intent.URI_INTENT_SCHEME );
                    startActivity(intent);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setPositiveButton(R.string.about_dialog_button, null);
        builder.show();
    }


    public static class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public SharedPreferences sp;

        @Override
        public void onCreate(Bundle saveInstanceState) {
            super.onCreate(saveInstanceState);
            // 加载xml资源文件
            addPreferencesFromResource(R.xml.preferences);
            sp = getPreferenceManager().getSharedPreferences();
            refreshSummary();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference){
            if("version_code".equals(preference.getKey()))
                ((PreferencesActivity)getActivity()).showInfo();
            if("notif_permit".equals(preference.getKey()))
                openNotificationListenSettings();
            return false;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals("hide_launcher")){
                PackageManager pkg=getActivity().getPackageManager();
                if(sharedPreferences.getBoolean(key, false)){
                    pkg.setComponentEnabledSetting(new ComponentName(getActivity(), SplashActivity.class),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }else{
                    pkg.setComponentEnabledSetting(new ComponentName(getActivity(), SplashActivity.class),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
            }else{
                getActivity().startService(new Intent(getActivity(), NotificationMonitorService.class)
                        .putExtra("update", true));
            }
            refreshSummary();
        }

        @Override
        public void onResume() {
            super.onResume();

            refreshSummary();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        public void refreshSummary(){
            EditTextPreference numberPref = (EditTextPreference) findPreference("offset_trigger");
            numberPref.setSummary(numberPref.getText());

            numberPref = (EditTextPreference) findPreference("offset_fluctuation");
            numberPref.setSummary(numberPref.getText());

            numberPref = (EditTextPreference) findPreference("offset_y");
            numberPref.setSummary(numberPref.getText());

            numberPref = (EditTextPreference) findPreference("offset_time");
            numberPref.setSummary(numberPref.getText());

            Preference notfPref = (Preference) findPreference("notif_permit");
            notfPref.setSummary(getString(isNotificationListenerEnabled(getActivity())? R.string.pref_enable_permit : R.string.pref_disable_permit));

            Preference aboutPref = (Preference) findPreference("version_code");
            aboutPref.setSummary(getVersion(getActivity()));
        }

        public static String getVersion(Context context){
            String versionName="";
            int versionCode=0;
            boolean isApkInDebug = false;
            try {
                PackageInfo pi=context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                versionName = pi.versionName;
                versionCode = pi.versionCode;
                ApplicationInfo info = context.getApplicationInfo();
                isApkInDebug = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return versionName + "-" + (isApkInDebug? "debug":"release") + "(" + versionCode +")";
        }

        public static boolean isNotificationListenerEnabled(Context context){
            String s = android.provider.Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            if(s!= null && s.contains(context.getPackageName()))
                return true;
            return false;
        }

        public void openNotificationListenSettings() {
            try {
                Intent intent;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                } else {
                    intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                }
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
