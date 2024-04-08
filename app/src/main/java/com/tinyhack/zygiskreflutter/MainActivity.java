package com.tinyhack.zygiskreflutter;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.view.Menu;

import com.google.android.material.navigation.NavigationView;

import android.content.pm.PackageManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import android.os.Handler;
public class MainActivity extends AppCompatActivity {

    // Used to load the 'zygiskreflutter' library on application startup.
    static {
        System.loadLibrary("zygiskreflutter");
    }

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler(Looper.getMainLooper());

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Handle navigation view item clicks here.
                int id = item.getItemId();

                if (id == R.id.nav_set_host) {
                    Intent intent = new Intent(MainActivity.this, SetHostActivity.class);
                    startActivity(intent);
                }
                if (id == R.id.nav_about) {
                    Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                    startActivity(intent);
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);

        mAdapter = new AppListAdapter(apps, pm, new AppListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ApplicationInfo item) {
                Intent intent = new Intent(MainActivity.this, AppDetailActivity.class);

                // Put the selected information into the Intent
                intent.putExtra("appName", item.loadLabel(pm).toString());
                intent.putExtra("packageName", item.packageName);

                // Start DetailActivity
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(mAdapter);
        //run in new Thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                EngineHashInfo engineHashInfo = EngineHashInfo.getInstance(MainActivity.this);
                try {
                    engineHashInfo.updateData();
                } catch (IOException e) {
                    //show toast
                    Toast.makeText(MainActivity.this, "Error downloading data", Toast.LENGTH_LONG).show();
                }
            }
        }).start();

        //get proxyIP, if not set, set to 192.168.1.1
        String proxyIP = Util.getPoxyIPFromFile(getFilesDir().getAbsolutePath());
        if (proxyIP==null || proxyIP.isEmpty()) {
            Util.saveProxyIPToFile(getFilesDir().getAbsolutePath(), "192.168.1.1");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Use the query to filter your data
                //((AppListAdapter) mAdapter).filter(query);
                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((AppListAdapter) mAdapter).filter(query);
                    }
                }, 500);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Use the newText to filter your data as the user types
                //((AppListAdapter) mAdapter).filter(newText);
                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((AppListAdapter) mAdapter).filter(newText);
                    }
                }, 500);
                return false;
            }
        });

        return true;
    }
}
