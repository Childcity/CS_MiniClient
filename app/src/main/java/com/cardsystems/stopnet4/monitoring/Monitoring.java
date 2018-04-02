package com.cardsystems.stopnet4.monitoring;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import com.cardsystems.stopnet4.monitoring.fragments.MonitoringFragment;
import com.cardsystems.stopnet4.monitoring.fragments.SettingsFragment;

public class Monitoring extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MonitoringFragment.OnSendToActivityListener,
        SettingsFragment.OnSendToActivityListener{

    private static int REQUEST_READ_STORAGE;

    private Toolbar toolbar;

    public static final String AUTOSCROLL_CHANGED = "AUTOSCROLL_CHANGED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Load default settings
        PreferenceManager.setDefaultValues(this, R.xml.preference, false);

        // Request permission to read and write to internal sdcard
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_READ_STORAGE);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.monitoring_container, new MonitoringFragment(), "fmonitoring_tag")
                .commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.monitoring, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case R.id.action_settings: {
                getFragmentManager().beginTransaction()
                        .replace(R.id.monitoring_container, new SettingsFragment(), "fsettings_tag")
                        .addToBackStack(null)
                        .commit();
            }break;
            case R.id.action_autoscroll: {
                Intent retIntent = new Intent(MonitoringFragment.MONITORING_FRAGMENT_DATA);

                if (item.isChecked()) {
                    item.setChecked(false);
                    retIntent.putExtra(AUTOSCROLL_CHANGED, 0);
                } else {
                    item.setChecked(true);
                    retIntent.putExtra(AUTOSCROLL_CHANGED, 1);
                }

                sendBroadcast(retIntent);
            }break;
            case android.R.id.home: {
                onBackPressed();
            }break;

            default: {
                return super.onOptionsItemSelected(item);
            }
        }

        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentTransaction ftrans = getFragmentManager().beginTransaction();

        if (id == R.id.nav_monitoring) {
            /*if(fragmentManager.findFragmentByTag("fmonitoring_tag") != null) {return true;}*/

            // Display the fragment as the main content.
            ftrans.replace(R.id.monitoring_container, new MonitoringFragment(), "fmonitoring_tag");

        } /*else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        ftrans.commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSendToActivityFromMonitoringFragment(int what) {
        switch (what){
            case MonitoringFragment.PROGRESSBAR_CHANGE: {

                // Set Title
                setTitle(getString(R.string.monitoring_item));

                ProgressBar prgBar = findViewById(R.id.toolbar_progress_bar);

                if(prgBar != null) {
                    if(prgBar.getVisibility() == ProgressBar.INVISIBLE)
                        prgBar.setVisibility(ProgressBar.VISIBLE);
                    else
                        prgBar.setVisibility(ProgressBar.INVISIBLE);
                }

            }break;
        }
    }

    @Override
    public void onSendToActivityFromSettingsFragment(int what) {
        switch (what){
            case SettingsFragment.SETTINGSSTATUS_CHANGE: {
                // Set settings item in menu invisible if user in settings. Else make it visible
                if(toolbar.getMenu().findItem(R.id.action_settings).isVisible()) {
                    toolbar.getMenu().setGroupVisible(R.id.settings_group, false);
                    setTitle(getString(R.string.action_settings));
                }else{
                    if(this.hasWindowFocus())
                        toolbar.getMenu().setGroupVisible(R.id.settings_group, true);
                }
            }
        }
    }

}
