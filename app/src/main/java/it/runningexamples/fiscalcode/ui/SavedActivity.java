package it.runningexamples.fiscalcode.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import com.elconfidencial.bubbleshowcase.BubbleShowCaseBuilder;
import com.elconfidencial.bubbleshowcase.BubbleShowCaseSequence;
import it.runningexamples.fiscalcode.R;
import it.runningexamples.fiscalcode.db.AppDatabase;
import it.runningexamples.fiscalcode.db.CodiceFiscaleEntity;
import it.runningexamples.fiscalcode.tools.PreferenceManager;
import it.runningexamples.fiscalcode.tools.ThemeUtilities;
import android.view.Menu;
import android.view.MenuItem;


public class SavedActivity extends AppCompatActivity implements RecyclerAdapter.AdapterCallback {

    private static final String SAVED = "saved"; //NON-NLS
    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    ItemTouchHelper itemTouchHelper;
    PreferenceManager prefs;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtilities.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        if (AppDatabase.getInstance(getApplicationContext()).codiceFiscaleDAO().getDbSize() == 0) {
            setContentView(R.layout.layout_empty_list);
        } else {
            prefs = new PreferenceManager(this);
            setContentView(R.layout.activity_saved);
            mRecyclerView = findViewById(R.id.recyclerView);
            mRecyclerView.setHasFixedSize(true);
            mLayoutManager = new LinearLayoutManager(this);
            mAdapter = new RecyclerAdapter(getApplicationContext(), this);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mAdapter);
            itemTouchHelper = new ItemTouchHelper(new SwipeCallback(mAdapter, mRecyclerView));
            itemTouchHelper.attachToRecyclerView(mRecyclerView);
            if (prefs.isFirstActivity(SAVED)){
                firstTutorial();
            }
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
        menu.findItem(R.id.shareSelected).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.deleteAll:
                mAdapter.deleteSelected();
                return true;
            case R.id.shareSelected:
                mAdapter.shareSelected();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void firstTutorial(){
        BubbleShowCaseBuilder builder1 = new BubbleShowCaseBuilder(SavedActivity.this);
        builder1.title(getString(R.string.bubbleSavedCard));
        builder1.targetView(findViewById(R.id.help1));

        BubbleShowCaseBuilder builder2 = new BubbleShowCaseBuilder(SavedActivity.this);
        builder2.title(getString(R.string.bubbleSavedCode));
        builder2.targetView(findViewById(R.id.help2));

        BubbleShowCaseBuilder builder3 = new BubbleShowCaseBuilder(SavedActivity.this);
        builder3.title(getString(R.string.bubbleSavedSwipe));
        builder3.targetView(findViewById(R.id.help3));
        builder3.image(getDrawable(R.drawable.swipe_left)); //Bubble main image

        BubbleShowCaseSequence sequence = new BubbleShowCaseSequence();
        sequence.addShowCase(builder1);
        sequence.addShowCase(builder2);
        sequence.addShowCase(builder3);
        sequence.show();

        prefs.setFirstActivity(SAVED,false);
    }

    @Override
    public void showHideItem(boolean delete, boolean share) {          // usa l'interfaccia implementata in RecyclerAdapter
            MenuItem item3 = menu.findItem(R.id.shareSelected);
            item3.setVisible(share);
        if (delete) {
            MenuItem item = menu.findItem(R.id.deleteAll);
            MenuItem item2 = menu.findItem(R.id.selectedCounter);
            item2.setVisible(!item2.isVisible());
            item.setVisible(!item.isVisible());
        }
    }

    @Override
    public void counter(boolean add, boolean set) {
        MenuItem item = menu.findItem(R.id.selectedCounter);
        if (set) {
            item.setTitle(String.valueOf(0));
            return;
        }
        int current = Integer.parseInt(String.valueOf(item.getTitle()));
        if (add) {
            current++;
            item.setTitle(String.valueOf(current));
        } else {
            current--;
            item.setTitle(String.valueOf(current));
        }
    }

    @Override
    public void getLastSelected(CodiceFiscaleEntity lastSelected) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, lastSelected.getFinalFiscalCode());
        sharingIntent.setType("text/plain");
        startActivity(Intent.createChooser(sharingIntent, "Condividi Codice Tramite"));
    }
}