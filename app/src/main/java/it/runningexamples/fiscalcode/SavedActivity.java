package it.runningexamples.fiscalcode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import java.util.List;

public class SavedActivity extends AppCompatActivity implements RecyclerAdapter.AdapterCallback {

    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<CodiceFiscaleEntity> cfList;
    ItemTouchHelper itemTouchHelper;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtilities.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        if (AppDatabase.getInstance(getApplicationContext()).codiceFiscaleDAO().getDbSize() == 0) {
            setContentView(R.layout.layout_empty_list);
        } else {
            setContentView(R.layout.activity_saved);
            mRecyclerView = findViewById(R.id.recyclerView);
            mRecyclerView.setHasFixedSize(true);
            mLayoutManager = new LinearLayoutManager(this);
            mAdapter = new RecyclerAdapter(getApplicationContext(), this);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mAdapter);
            itemTouchHelper = new ItemTouchHelper(new SwipeCallback(mAdapter, mRecyclerView));
            itemTouchHelper.attachToRecyclerView(mRecyclerView);

        }
        Toolbar toolbarList = findViewById(R.id.toolbarList);
        setSupportActionBar(toolbarList);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_bar_menu_multipleselection, menu);
        this.menu = menu;
        menu.findItem(R.id.deleteAll).setVisible(false);
        menu.findItem(R.id.selectedCounter).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.deleteAll:
                mAdapter.deleteSelected();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showHideItem() {          // usa l'interfaccia implementata in RecyclerAdapter
        MenuItem item = menu.findItem(R.id.deleteAll);
        MenuItem item2 = menu.findItem(R.id.selectedCounter);
        item2.setVisible(!item2.isVisible());
        item.setVisible(!item.isVisible());
    }

    @Override
    public void counter(boolean add, boolean set) {
        MenuItem item = menu.findItem(R.id.selectedCounter);
        if (set){
            item.setTitle(String.valueOf(0));
            return;
        }
        int current = Integer.parseInt(String.valueOf(item.getTitle()));
        if (add){
            current++;
            item.setTitle(String.valueOf(current));
        }else{
            current--;
            item.setTitle(String.valueOf(current));
        }
    }
}