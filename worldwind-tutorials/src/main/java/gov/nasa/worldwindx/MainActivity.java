/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

import gov.nasa.worldwind.FrameMetrics;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.util.Logger;

/**
 * This abstract Activity class implements a Navigation Drawer menu shared by all the WorldWind Example activities.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected static final int PRINT_METRICS = 1;

    protected static final int PRINT_METRICS_DELAY = 3000;

    protected static int selectedItemId = R.id.nav_basic_globe_activity;

    protected ActionBarDrawerToggle drawerToggle;

    protected NavigationView navigationView;

    protected boolean twoPaneView;

    protected String tutorialUrl;

    protected final String aboutBoxTitle = "WorldWind Tutorials";        // TODO: use a string resource, e.g., app name

    protected final String aboutBoxText = "A collection of tutorials";    // TODO: make this a string resource

    protected final Handler handler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == PRINT_METRICS) {
            return printMetrics();
        } else {
            return false;
        }
    });

    /**
     * Returns a reference to the WorldWindow.
     * <p>
     * Derived classes must implement this method.
     *
     * @return The WorldWindow GLSurfaceView object
     */
    public WorldWindow getWorldWindow() {
        // TODO: Implement via Fragment Manager and findFragmentById
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onCreateDrawer();

        if (findViewById(R.id.code_container) != null) {
            // The code container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPaneView = true;
        }

        if (!twoPaneView) {
            FloatingActionButton codeViewButton = findViewById(R.id.fab);
            codeViewButton.setVisibility(View.VISIBLE);    // is set to GONE in layout
            codeViewButton.setOnClickListener(view -> {
                Context context = view.getContext();
                Intent intent = new Intent(context, CodeActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("url", tutorialUrl);
                intent.putExtra("arguments", bundle);
                context.startActivity(intent);
            });
        }
        if (savedInstanceState == null) {
            // savedInstanceState is non-null when there is fragment state
            // saved from previous configurations of this activity
            // (e.g. when rotating the screen from portrait to landscape).
            // In this case, the fragment will automatically be re-added
            // to its container so we don't need to manually add it.
            // For more information, see the Fragments API guide at:
            //
            // http://developer.android.com/guide/components/fragments.html
            //
            loadTutorial(BasicGlobeFragment.class, "file:///android_asset/basic_globe_tutorial.html", R.string.title_basic_globe);
        }
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop printing frame metrics when this activity is paused.
        this.handler.removeMessages(PRINT_METRICS);
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

        WorldWindow wwd = this.getWorldWindow();
        if (wwd == null) {
            return false;
        }
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
        if (selectedItemId == R.id.nav_basic_globe_activity) {
            loadTutorial(BasicGlobeFragment.class, "file:///android_asset/basic_globe_tutorial.html", R.string.title_basic_globe);
        } else if (selectedItemId == R.id.nav_camera_view_activity) {
            loadTutorial(CameraViewFragment.class, "file:///android_asset/camera_view_tutorial.html", R.string.title_camera_view);
        } else if (selectedItemId == R.id.nav_camera_control_activity) {
            loadTutorial(CameraControlFragment.class, "file:///android_asset/camera_control_tutorial.html", R.string.title_camera_controls);
        } else if (selectedItemId == R.id.nav_ellipse_activity) {
            loadTutorial(EllipseFragment.class, "file:///android_asset/ellipse_tutorial.html", R.string.title_ellipse);
        } else if (selectedItemId == R.id.nav_geopackage_activity) {
            loadTutorial(GeoPackageFragment.class, "file:///android_asset/geopackage_tutorial.html", R.string.title_geopackage);
        } else if (selectedItemId == R.id.nav_labels_activity) {
            loadTutorial(LabelsFragment.class, "file:///android_asset/labels_tutorial.html", R.string.title_labels);
        } else if (selectedItemId == R.id.nav_look_at_view_activity) {
            loadTutorial(LookAtViewFragment.class, "file:///android_asset/look_at_view_tutorial.html", R.string.title_look_at_view);
        } else if (selectedItemId == R.id.nav_navigator_event_activity) {
            loadTutorial(NavigatorEventFragment.class, "file:///android_asset/navigator_events_tutorial.html", R.string.title_navigator_event);
        } else if (selectedItemId == R.id.nav_omnidirectional_sightline_activity) {
            loadTutorial(OmnidirectionalSightlineFragment.class, "file:///android_asset/omnidirectional_sightline_tutorial.html", R.string.title_omni_sightline);
        } else if (selectedItemId == R.id.nav_paths_activity) {
            loadTutorial(PathsFragment.class, "file:///android_asset/paths_tutorial.html", R.string.title_paths);
        } else if (selectedItemId == R.id.nav_placemarks_activity) {
            loadTutorial(PlacemarksFragment.class, "file:///android_asset/placemarks_tutorial.html", R.string.title_placemarks);
        } else if (selectedItemId == R.id.nav_placemarks_picking_activity) {
            loadTutorial(PlacemarksPickingFragment.class, "file:///android_asset/placemarks_picking_tutorial.html", R.string.title_placemarks_picking);
        } else if (selectedItemId == R.id.nav_polygons_activity) {
            loadTutorial(PolygonsFragment.class, "file:///android_asset/polygons_tutorial.html", R.string.title_polygons);
        } else if (selectedItemId == R.id.nav_shapes_dash_and_fill) {
            loadTutorial(ShapesDashAndFillFragment.class, "file:///android_asset/shapes_dash_and_fill.html", R.string.title_shapes_dash_and_fill);
        } else if (selectedItemId == R.id.nav_show_tessellation_activity) {
            loadTutorial(ShowTessellationFragment.class, "file:///android_asset/show_tessellation_tutorial.html", R.string.title_show_tessellation);
        } else if (selectedItemId == R.id.nav_surface_image_activity) {
            loadTutorial(SurfaceImageFragment.class, "file:///android_asset/surface_image_tutorial.html", R.string.title_surface_image);
        } else if (selectedItemId == R.id.nav_wms_layer_activity) {
            loadTutorial(WmsLayerFragment.class, "file:///android_asset/wms_layer_tutorial.html", R.string.title_wms_layer);
        } else if (selectedItemId == R.id.nav_wmts_layer_activity) {
            loadTutorial(WmtsLayerFragment.class, "file:///android_asset/wmts_layer_tutorial.html", R.string.title_wmts_layer);
        } else if (selectedItemId == R.id.nav_wcs_elevation_activity) {
            loadTutorial(WcsElevationFragment.class, "file:///android_asset/wcs_elevation_tutorial.html", R.string.title_wcs_elevation_coverage);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void loadTutorial(Class<? extends Fragment> globeFragment, String url, int titleId) {
        try {
            this.setTitle(titleId);
            Fragment globe = globeFragment.newInstance();
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.globe_container, globe)    // replace (destroy) existing fragment (if any)
                .commit();

            if (this.twoPaneView) {
                Bundle bundle = new Bundle();
                bundle.putString("url", url);

                Fragment code = new CodeFragment();
                code.setArguments(bundle);
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.code_container, code)
                    .commit();
            } else {
                this.tutorialUrl = url;
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
