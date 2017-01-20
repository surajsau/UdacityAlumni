package com.google.developer.udacityalumni.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.facebook.stetho.Stetho;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.developer.udacityalumni.R;
import com.google.developer.udacityalumni.adapter.PageAdapter;
import com.google.developer.udacityalumni.data.AlumContract;
import com.google.developer.udacityalumni.fragment.ArticleFragment;
import com.google.developer.udacityalumni.fragment.PlaceholderFragment;
import com.google.developer.udacityalumni.service.AlumIntentService;
import com.google.developer.udacityalumni.utility.Utility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends BaseActivity implements ArticleFragment.ArticleCallback,
        LoaderManager.LoaderCallbacks<Cursor>, TabLayout.OnTabSelectedListener,
        NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String URL_CLASSROOM = "https://classroom.udacity.com/me";
    private static final String URL_CATALOG = "https://www.udacity.com/courses/all";
    private static final String URL_SUCCESS = "https://www.udacity.com/success";

    private List<Long> mArticleIds;
    private List<Integer> mBookmarks;
    private List<String> mTags;
    private static final int LOADER = 101;
    private String mTitle;

    @BindView(R.id.drawer)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.tabs)
    TabLayout mTabs;
    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    @BindView(R.id.nav_view)
    NavigationView mNavView;
    TabLayout.Tab mArticleTab, mCareersTab, mMentorshipTab, mMeetUpsTab;
    @BindView(R.id.nav_bottom)
    BottomNavigationView mBottomNav;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Stetho.initializeWithDefaults(this);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            setContentView(R.layout.activity_main);
            ButterKnife.bind(this);
            startService(new Intent(this, AlumIntentService.class));
            Utility.scheduleArticleSync(this);
            FirebaseUser user = auth.getCurrentUser();
            TextView tv = (TextView) mNavView.getHeaderView(0).findViewById(R.id.nav_header_name_tv);
            tv.setText(user.getDisplayName());
            CircleImageView cv = (CircleImageView) mNavView.getHeaderView(0).findViewById(R.id.nav_header_prof_pic);
            if (user.getPhotoUrl() != null) {
                Picasso.with(this).load(user.getPhotoUrl()).placeholder(R.drawable.placeholder)
                        .error(R.drawable.ic_person).into(cv);
            } else {
                cv.setImageResource(R.drawable.ic_person);
            }

            if (mToolbar != null && savedInstanceState != null)
                mToolbar.setTitle(mTitle);
            if (mToolbar != null) {
                Drawable overflowIcon = mToolbar.getOverflowIcon();
                if (overflowIcon != null)
                    overflowIcon.setTint(ContextCompat.getColor(this, R.color.colorAccent));
                if (savedInstanceState != null) mToolbar.setTitle(mTitle);
            }
            setSupportActionBar(mToolbar);
            setupViewPager(mViewPager);
            mTabs.setupWithViewPager(mViewPager);
            setUpTabs();
            ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null) {
                VectorDrawableCompat indicator
                        = VectorDrawableCompat.create(getResources(), R.drawable.ic_menu, getTheme());
                assert indicator != null;
                indicator.setTint(ResourcesCompat.getColor(getResources(), R.color.colorAccent, getTheme()));

                supportActionBar.setHomeAsUpIndicator(indicator);
                supportActionBar.setDisplayHomeAsUpEnabled(true);
            }
            mNavView.setNavigationItemSelectedListener(this);
            mBottomNav.setOnNavigationItemSelectedListener(this);
        }

    }

    private void setupViewPager(ViewPager viewPager) {
        PageAdapter mPageAdapter = new PageAdapter(getSupportFragmentManager());
        mPageAdapter.addFragment(new ArticleFragment());
        mPageAdapter.addFragment(new PlaceholderFragment());
        mPageAdapter.addFragment(new PlaceholderFragment());
        mPageAdapter.addFragment(new PlaceholderFragment());
        viewPager.setAdapter(mPageAdapter);
    }

    private void setUpTabs() {
        mArticleTab = mTabs.getTabAt(0);
        mCareersTab = mTabs.getTabAt(1);
        mMentorshipTab = mTabs.getTabAt(2);
        mMeetUpsTab = mTabs.getTabAt(3);
        mArticleTab.setIcon(R.drawable.ic_home);
        mCareersTab.setIcon(R.drawable.ic_apps);
        mMentorshipTab.setIcon(R.drawable.ic_mentorship);
        mMeetUpsTab.setIcon(R.drawable.ic_meetups);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTabs.addOnTabSelectedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTabs.removeOnTabSelectedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.sign_out:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                finish();
                            }
                        });
                break;
        }
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_classroom || id == R.id.nav_catalog || id == R.id.nav_success) {
            String url = null;
            switch (id) {
                case R.id.nav_classroom:
                    url = URL_CLASSROOM;
                    break;
                case R.id.nav_catalog:
                    url = URL_CATALOG;
                    break;
                case R.id.nav_success:
                    url = URL_SUCCESS;
                    break;
            }
            if (!TextUtils.isEmpty(url)) Utility.launchUrl(url, this);
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Log.i(LOG_TAG, "ICON SELECTED");
            Drawable icon = item.getIcon();
            if (icon != null) {
                icon.setTint(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                Log.i(LOG_TAG, "TINT SET");
            }
        }

        return true;
    }



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, AlumContract.ArticleEntry.CONTENT_URI,
                new String[]{AlumContract.ArticleEntry.COL_ARTICLE_ID, AlumContract.ArticleEntry.COL_BOOKMARKED,
                        AlumContract.ArticleEntry.COL_RANDOM_TAG},
                null, null, AlumContract.ArticleEntry.COL_CREATED_AT + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() > 0) {
            final long firstId = mArticleIds.get(0);
            while (data.moveToNext()) {
                if (mArticleIds.size() > 10) break;
                long id = data.getLong(0);
                if (firstId != id) {
                    mArticleIds.add(data.getLong(0));
                    mBookmarks.add(data.getInt(1));
                    mTags.add(data.getString(2));
                }
            }
            int len = mArticleIds.size();
            long[] ids = new long[len];
            int[] isBookmarked = new int[len];
            String[] tags = new String[len];
            for (int i = 0; i < len; i++) {
                ids[i] = mArticleIds.get(i);
                isBookmarked[i] = mBookmarks.get(i);
                tags[i] = mTags.get(i);
            }
            startActivity(new Intent(this, ArticleDetailActivity.class)
                    .putExtra(getString(R.string.article_list_key), ids)
                    .putExtra(getString(R.string.article_bookmarks_key), isBookmarked)
                    .putExtra(getString(R.string.tag_key), tags));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onArticleSelected(long articleId, boolean isBookmarked, String tag) {
        mArticleIds = new ArrayList<>();
        mArticleIds.add(articleId);
        mBookmarks = new ArrayList<>();
        mBookmarks.add(isBookmarked ? 1 : 0);
        mTags = new ArrayList<>();
        mTags.add(tag);
        Loader loader = getSupportLoaderManager().getLoader(LOADER);
        if (loader == null || !loader.isStarted())
            getSupportLoaderManager().initLoader(LOADER, null, this);
        else
            getSupportLoaderManager().restartLoader(LOADER, null, this);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Drawable icon = tab.getIcon();
        assert icon != null;
        icon.setTint(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
        int pos = tab.getPosition();
        switch (pos) {
            case 0:
                mTitle = getString(R.string.home);
                break;
            case 1:
                mTitle = getString(R.string.apps);
                break;
            case 2:
                mTitle = getString(R.string.mentorship);
                break;
            case 3:
                mTitle = getString(R.string.meetups);
                break;
            default:
                Log.e(LOG_TAG, "TAB POSITION UNRECOGINIZED");
        }
        mBottomNav.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
        if (mTitle != null) setTitle(mTitle);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        Drawable icon = tab.getIcon();
        assert icon != null;
        icon.setTint(ContextCompat.getColor(MainActivity.this, R.color.unselected_icon_dark));
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(getString(R.string.title_key), mTitle);
    }

    @Override
    protected void onPause() {
        Loader loader = getSupportLoaderManager().getLoader(LOADER);
        if (loader != null) {
            getSupportLoaderManager().destroyLoader(loader.getId());
        }
        super.onPause();
    }
}
