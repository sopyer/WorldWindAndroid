/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.Date;
import java.util.Locale;

import gov.nasa.worldwind.FrameMetrics;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Camera;
import gov.nasa.worldwind.util.Logger;

/**
 * This abstract Activity class implements a Navigation Drawer menu shared by all the WorldWind Example activities.
 */
public abstract class AbstractMainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {

    protected final static String SESSION_TIMESTAMP = "session_timestamp";

    protected final static String CAMERA_LATITUDE = "latitude";

    protected final static String CAMERA_LONGITUDE = "longitude";

    protected final static String CAMERA_ALTITUDE = "altitude";

    protected final static String CAMERA_ALTITUDE_MODE = "altitude_mode";

    protected final static String CAMERA_HEADING = "heading";

    protected final static String CAMERA_TILT = "tilt";

    protected final static String CAMERA_ROLL = "roll";

    protected static final int PRINT_METRICS = 1;

    protected static final int PRINT_METRICS_DELAY = 3000;

    protected static final Date sessionTimestamp = new Date();

    protected static int selectedItemId = R.id.nav_general_globe_activity;

    protected ActionBarDrawerToggle drawerToggle;

    protected NavigationView navigationView;

    protected String aboutBoxTitle = "Title goes here";

    protected String aboutBoxText = "Description goes here;";

    protected final Handler handler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == PRINT_METRICS) {
            return printMetrics();
        } else {
            return false;
        }
    });

    /**
     * Returns a reference to the WorldWindow.
     * <p/>
     * Derived classes must implement this method.
     *
     * @return The WorldWindow GLSurfaceView object
     */
    abstract public WorldWindow getWorldWindow();

    /**
     * This method should be called by derived classes in their onCreate method.
     *
     * @param layoutResID Resource ID to be inflated.
     */
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        this.onCreateDrawer();
    }

    protected void onCreateDrawer() {
        // Add support for a Toolbar and set to act as the ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add support for the navigation drawer full of examples
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        this.drawerToggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(this.drawerToggle);
        this.drawerToggle.syncState();

        this.navigationView = findViewById(R.id.nav_view);
        this.navigationView.setNavigationItemSelectedListener(this);
        this.navigationView.setCheckedItem(selectedItemId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the menu by highlighting the last selected menu item
        this.navigationView.setCheckedItem(selectedItemId);
        // Use this Activity's Handler to periodically print the FrameMetrics.
        this.handler.sendEmptyMessageDelayed(PRINT_METRICS, PRINT_METRICS_DELAY);
        // Restore camera state from previously saved session data
        this.restoreCameraState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop printing frame metrics when this activity is paused.
        this.handler.removeMessages(PRINT_METRICS);
        // Save camera state.
        this.saveCameraState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            showAboutBox();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Saves camera state to a SharedPreferences object.
     */
    protected void saveCameraState() {
        WorldWindow wwd = this.getWorldWindow();
        if (wwd != null) {
            SharedPreferences preferences = this.getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            // Write an identifier to the preferences for this session;
            editor.putLong(SESSION_TIMESTAMP, getSessionTimestamp());

            // Write the camera data
            Camera camera = wwd.getCamera();
            editor.putFloat(CAMERA_LATITUDE, (float) camera.position.latitude);
            editor.putFloat(CAMERA_LONGITUDE, (float) camera.position.longitude);
            editor.putFloat(CAMERA_ALTITUDE, (float) camera.position.altitude);
            editor.putFloat(CAMERA_HEADING, (float) camera.heading);
            editor.putFloat(CAMERA_TILT, (float) camera.tilt);
            editor.putFloat(CAMERA_ROLL, (float) camera.roll);
            editor.putInt(CAMERA_ALTITUDE_MODE, camera.altitudeMode);

            editor.apply();
        }
    }

    /**
     * Restores camera state from a SharedPreferences object.
     */
    protected void restoreCameraState() {
        WorldWindow wwd = this.getWorldWindow();
        if (wwd != null) {
            SharedPreferences preferences = this.getPreferences(MODE_PRIVATE);

            // We only want to restore preferences from the same session.
            if (preferences.getLong(SESSION_TIMESTAMP, -1) != getSessionTimestamp()) {
                return;
            }
            // Read the camera data
            float lat = preferences.getFloat(CAMERA_LATITUDE, Float.MAX_VALUE);
            float lon = preferences.getFloat(CAMERA_LONGITUDE, Float.MAX_VALUE);
            float alt = preferences.getFloat(CAMERA_ALTITUDE, Float.MAX_VALUE);
            float heading = preferences.getFloat(CAMERA_HEADING, Float.MAX_VALUE);
            float tilt = preferences.getFloat(CAMERA_TILT, Float.MAX_VALUE);
            float roll = preferences.getFloat(CAMERA_ROLL, Float.MAX_VALUE);
            @WorldWind.AltitudeMode int altMode = preferences.getInt(CAMERA_ALTITUDE_MODE, WorldWind.ABSOLUTE);

            if (lat == Float.MAX_VALUE || lon == Float.MAX_VALUE || alt == Float.MAX_VALUE ||
                heading == Float.MAX_VALUE || tilt == Float.MAX_VALUE || roll == Float.MAX_VALUE) {
                return;
            }

            // Restore the camera state.
            wwd.getCamera().set(lat, lon, alt, altMode, heading, tilt, roll);
        }
    }

    protected void setAboutBoxTitle(String title) {
        this.aboutBoxTitle = title;
    }

    protected void setAboutBoxText(String text) {
        this.aboutBoxText = text;
    }

    /**
     * This method is invoked when the About button is selected in the Options menu.
     */
    protected void showAboutBox() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(this.aboutBoxTitle);
        alertDialogBuilder
            .setMessage(this.aboutBoxText)
            .setCancelable(true)
            .setNegativeButton("Close", (dialog, id) -> {
                // if this button is clicked, just close
                // the dialog box and do nothing
                dialog.cancel();
            });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    protected boolean printMetrics() {
        // Assemble the current system memory info.
        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        // Assemble the current WorldWind frame metrics.
        FrameMetrics fm = this.getWorldWindow().getFrameMetrics();

        // Print a log message with the system memory, WorldWind cache usage, and WorldWind average frame time.
        Logger.log(Logger.INFO, String.format(Locale.US, "System memory %,.0f KB    Heap memory %,.0f KB    Render cache %,.0f KB    Frame time %.1f ms + %.1f ms",
            (mi.totalMem - mi.availMem) / 1024.0,
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0,
            fm.getRenderResourceCacheUsedCapacity() / 1024.0,
            fm.getRenderTimeAverage(),
            fm.getDrawTimeAverage()));

        // Reset the accumulated WorldWind frame metrics.
        fm.reset();

        // Print the frame metrics again after the configured delay.
        return this.handler.sendEmptyMessageDelayed(PRINT_METRICS, PRINT_METRICS_DELAY);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Persist the selected item between Activities
        selectedItemId = item.getItemId();

        // Handle navigation view item clicks here.
        if (selectedItemId == R.id.nav_basic_performance_benchmark_activity) {
            startActivity(new Intent(getApplicationContext(), BasicPerformanceBenchmarkActivity.class));
        } else if (selectedItemId == R.id.nav_basic_stress_test_activity) {
            startActivity(new Intent(getApplicationContext(), BasicStressTestActivity.class));
        } else if (selectedItemId == R.id.nav_day_night_cycle_activity) {
            startActivity(new Intent(getApplicationContext(), DayNightCycleActivity.class));
        } else if (selectedItemId == R.id.nav_general_globe_activity) {
            startActivity(new Intent(getApplicationContext(), GeneralGlobeActivity.class));
        } else if (selectedItemId == R.id.nav_mgrs_graticule_activity) {
            startActivity(new Intent(getApplicationContext(), MGRSGraticuleActivity.class));
        } else if (selectedItemId == R.id.nav_multi_globe_activity) {
            startActivity(new Intent(getApplicationContext(), MultiGlobeActivity.class));
        } else if (selectedItemId == R.id.nav_omnidirectional_sightline_activity) {
            startActivity(new Intent(getApplicationContext(), OmnidirectionalSightlineActivity.class));
        } else if (selectedItemId == R.id.nav_paths_example) {
            startActivity(new Intent(getApplicationContext(), PathsExampleActivity.class));
        } else if (selectedItemId == R.id.nav_paths_and_polygons_activity) {
            startActivity(new Intent(getApplicationContext(), PathsPolygonsLabelsActivity.class));
        } else if (selectedItemId == R.id.nav_placemarks_demo_activity) {
            startActivity(new Intent(getApplicationContext(), PlacemarksDemoActivity.class));
        } else if (selectedItemId == R.id.nav_placemarks_milstd2525_activity) {
            startActivity(new Intent(getApplicationContext(), PlacemarksMilStd2525Activity.class));
        } else if (selectedItemId == R.id.nav_placemarks_milstd2525_demo_activity) {
            startActivity(new Intent(getApplicationContext(), PlacemarksMilStd2525DemoActivity.class));
        } else if (selectedItemId == R.id.nav_placemarks_milstd2525_stress_activity) {
            startActivity(new Intent(getApplicationContext(), PlacemarksMilStd2525StressActivity.class));
        } else if (selectedItemId == R.id.nav_placemarks_select_drag_activity) {
            startActivity(new Intent(getApplicationContext(), PlacemarksSelectDragActivity.class));
        } else if (selectedItemId == R.id.nav_placemarks_stress_activity) {
            startActivity(new Intent(getApplicationContext(), PlacemarksStressTestActivity.class));
        } else if (selectedItemId == R.id.nav_texture_stress_test_activity) {
            startActivity(new Intent(getApplicationContext(), TextureStressTestActivity.class));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected static long getSessionTimestamp() {
        return sessionTimestamp.getTime();
    }
}
