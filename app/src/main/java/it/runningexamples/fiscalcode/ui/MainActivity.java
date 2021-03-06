/**
 * Activity che gestisce il layout principale, dove è possibile calcolare
 * il proprio codice fiscale ed accedere a tutte le altre activity
 */

package it.runningexamples.fiscalcode.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.elconfidencial.bubbleshowcase.BubbleShowCaseBuilder;
import com.elconfidencial.bubbleshowcase.BubbleShowCaseSequence;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import it.runningexamples.fiscalcode.db.AppDatabase;
import it.runningexamples.fiscalcode.db.CodiceFiscaleEntity;
import it.runningexamples.fiscalcode.R;
import it.runningexamples.fiscalcode.entity.Comune;
import it.runningexamples.fiscalcode.entity.Parser;
import it.runningexamples.fiscalcode.entity.Stato;
import it.runningexamples.fiscalcode.tools.PreferenceManager;
import it.runningexamples.fiscalcode.tools.ThemeUtilities;

public class MainActivity extends AppCompatActivity {
    private static final String DATE_TAG = "datePicker"; //NON-NLS
    private static final String MAIN = "main"; //NON-NLS
    private static final String CALC = "calc"; //NON-NLS
    public static CodiceFiscaleEntity codiceFiscaleEntity;
    public PreferenceManager prefs;
    Holder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtilities.applyActivityTheme(this);    //Applica il tema impostato nelle preferenze ad ogni avvio
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        holder = new Holder();
    }

    //Chiede una conferma prima di uscire dall'applicazione quando si preme il pulsante indietro
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirmExit)
                .setCancelable(false)
                .setPositiveButton(R.string.choicePositive, (dialog, id) -> MainActivity.this.finish())
                .setNegativeButton(R.string.choiceNegative, null)
                .show();
    }

    //Imposta il menu della toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Classe Holder utilizzata per inizializzare tutti gli oggetti del layout
    private class Holder implements View.OnClickListener,Switch.OnCheckedChangeListener, Toolbar.OnMenuItemClickListener {
        Parser parser;
        List<Comune> comuniList;
        List<Stato> statiList;
        AutoCompleteTextView atComuni;
        FloatingActionButton btnCalcola;
        Comune comuneSelected;
        Stato statoSelected;
        Toolbar toolbar;
        Switch swEstero;
        TextView tvRisultato;
        Button btnBirthday;
        EditText etName, etSurname;
        RadioGroup rgGender;
        ImageButton btnSaveDB,btnCopy,btnShare,btnDelete;
        com.google.android.material.textfield.TextInputLayout autocompleteLayout;
        AdapterView.OnItemClickListener onItemClickListener;

        Holder() {
            tvRisultato = findViewById(R.id.tvRisultato);
            rgGender = findViewById(R.id.rgGender);
            btnBirthday = findViewById(R.id.btnData);
            etName = findViewById(R.id.etNome);
            etSurname = findViewById(R.id.etCognome);
            btnCalcola = findViewById(R.id.btnCalcola);
            atComuni = findViewById(R.id.atComuni);
            swEstero = findViewById(R.id.swEstero);
            toolbar = findViewById(R.id.toolbar);
            btnSaveDB = findViewById(R.id.btnSaveDB);
            btnCopy = findViewById(R.id.btnCopy);
            btnShare = findViewById(R.id.btnShare);
            btnDelete = findViewById(R.id.btnDelete);
            autocompleteLayout = findViewById(R.id.autocompleteLayout);

            btnCalcola.setOnClickListener(this);
            swEstero.setOnCheckedChangeListener(this);
            btnSaveDB.setOnClickListener(this);
            btnCopy.setOnClickListener(this);
            btnShare.setOnClickListener(this);
            btnDelete.setOnClickListener(this);

            //Imposta la Toolbar come una ActionBar
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            toolbar.setOnMenuItemClickListener(this);

            //Imposta il parser per l'AutoComplete
            parser = new Parser(MainActivity.this);
            comuniList = parser.parserComuni();
            statiList = parser.parserStati();

            setUpDialogDate();
            setUpAutoCompleteTextView();

            //Esegue il tutorial al primo avvio
            prefs = new PreferenceManager(getApplicationContext());
            if (prefs.isFirstActivity(MAIN)){
                firstTutorial();
            }
        }

        //Metodo per impostare l'autocompleteTextView relativa ai comuni/stati
        private void setUpAutoCompleteTextView() {
            if (!swEstero.isChecked()) {
                ArrayAdapter<Comune> comuneArrayAdapter = new ArrayAdapter<>(MainActivity.this,
                        R.layout.autocomplete_layout, R.id.tvAutoCompleteItem,comuniList);
                    atComuni.setAdapter(comuneArrayAdapter);
            }else{
                ArrayAdapter<Stato> statoArrayAdapter = new ArrayAdapter<>(MainActivity.this,
                        R.layout.autocomplete_layout, R.id.tvAutoCompleteItem, statiList);
                    atComuni.setAdapter(statoArrayAdapter);
            }
            onItemClickListener = (parent, view, position, id) -> {
                if (swEstero.isChecked()) {
                    statoSelected = (Stato) parent.getItemAtPosition(position);
                    hideKeyboard();
                } else {
                    comuneSelected = (Comune) parent.getItemAtPosition(position);
                    hideKeyboard();
                }
            };
            atComuni.setOnItemClickListener(onItemClickListener);
        }

        // Imposta il dialog relativo al DatePicker
        private void setUpDialogDate() {
            View.OnClickListener dateClickListener = v -> showDatePickerDialog();
            btnBirthday.setOnClickListener(dateClickListener);
        }

        // Mostra il dialog del DatePicker
        private void showDatePickerDialog() {
            DialogFragment newFragment = new DatePickerFragment(getApplicationContext());
            newFragment.show(getSupportFragmentManager(), DATE_TAG);
        }


        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btnCalcola) {
                hideKeyboard();
                tryCalc();
            }
            if (v.getId() == R.id.btnData){
                showDatePickerDialog();
            }
            if (v.getId() == R.id.btnSaveDB) {
                saveCodeDB(v);
            }
            if (v.getId() == R.id.btnCopy){     //Copia il codice calcolato negli appunti
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(null, tvRisultato.getText());
                assert clipboard != null;
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(),getString(R.string.clipboardCode),Toast.LENGTH_SHORT).show();
            }
            if (v.getId() == R.id.btnShare){    //Intent per condividere il codice calcolato
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.putExtra(Intent.EXTRA_TEXT, tvRisultato.getText());
                sharingIntent.setType("text/plain"); //NON-NLS
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareCode)));
            }
            if (v.getId() == R.id.btnDelete){
                resetForm();
            }
        }

        /*
        Metodo che calcola il codice fiscale tramite i dati inseriti nel form. Se tutti i campi
        sono inseriti correttamente viene calcolato il codice e ritorna true, altrimenti false.
         */
        private boolean computeCF() {
            String surname = etSurname.getText().toString();
            String name = etName.getText().toString();
            int radioID = rgGender.getCheckedRadioButtonId();

            String gender = (String) ((RadioButton) findViewById(radioID)).getText();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy"); //NON-NLS
            Date birthDay = null;
            try {
                birthDay = simpleDateFormat.parse(btnBirthday.getText().toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (!name.equals("") & !surname.equals("") & (comuneSelected != null || statoSelected != null) & birthDay!=null) {
                if (swEstero.isChecked()) {
                    codiceFiscaleEntity = new CodiceFiscaleEntity(name, surname, birthDay, gender, null, statoSelected,0);
                } else if (!swEstero.isChecked()){
                    codiceFiscaleEntity = new CodiceFiscaleEntity(name, surname, birthDay, gender, comuneSelected, null,0);
                }
                String fiscalCode = codiceFiscaleEntity.calculateCF();
                tvRisultato.setText(fiscalCode);
                tvRisultato.setVisibility(View.VISIBLE);
                return true;
            }else{
                Toast.makeText(getApplicationContext(), R.string.fillForm, Toast.LENGTH_LONG).show();
                return false;
            }
        }

        /*
        Metodo che cambia l'AutoCompleteTextView se si effettua lo switch da comune a stato estero
        e viceversa eliminando il testo inserito in precedenza
         */
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            atComuni.getText().clear();
            if (swEstero.isChecked()){
                autocompleteLayout.setHint(getString(R.string.formStatoEstero));
                setUpAutoCompleteTextView();
            }
            else{
                autocompleteLayout.setHint(getString(R.string.formComune));
                setUpAutoCompleteTextView();
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == R.id.menu_settings) {
                Intent intentSettings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intentSettings);
            }
            if (item.getItemId() == R.id.menu_list){
                Intent intentList = new Intent(MainActivity.this, SavedActivity.class);
                startActivity(intentList);
            }
            if (item.getItemId() == R.id.menu_favorites){
                Intent intentList = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intentList);
            }
            return true;
        }
    }

    //Metodo che nasconde la tastiera se nessuna view ha il focus
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    //Metodo che salva il codice calcolato nel database e mostra una snackbar con il risultato
    private void saveCodeDB(View v) {
        Snackbar sn;
        if (AppDatabase.getInstance(getApplicationContext()).codiceFiscaleDAO().getCode(codiceFiscaleEntity.getFinalFiscalCode()) == 0) {
            AppDatabase.getInstance(getApplicationContext()).codiceFiscaleDAO().saveNewCode(codiceFiscaleEntity);
            sn = Snackbar.make(v, getString(R.string.savedElement), Snackbar.LENGTH_LONG);
            sn.getView().setBackgroundColor(getColor(R.color.greenSnackbar));
            sn.show();
        } else {
            sn = Snackbar.make(v, getString(R.string.presentElement), Snackbar.LENGTH_LONG);
            sn.getView().setBackgroundColor(getColor(R.color.colorOutlineRed));
            sn.show();
        }
    }

    //Metodo che calcola il codice fiscale tramite computeCF e mostra i pulsanti per la gestione del codice
    private void tryCalc(){
        if (holder.computeCF()) {
            if (prefs.isFirstActivity(CALC)){
                calcTutorial();
            }
            holder.btnSaveDB.setVisibility(View.VISIBLE);
            holder.btnCopy.setVisibility(View.VISIBLE);
            holder.btnShare.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        }
    }

    //Metodo utilizzato per resettare tutti i valori del form a quelli di default
    public void resetForm(){
        holder.swEstero.setChecked(false);
        holder.rgGender.check(R.id.rbFemale);
        codiceFiscaleEntity = null;
        holder.tvRisultato.setText(null);
        holder.btnBirthday.setText(R.string.formData);
        ThemeUtilities.resetDateTextColor(getApplicationContext(),holder.btnBirthday);
        holder.etName.getText().clear();
        holder.etSurname.getText().clear();
        holder.atComuni.getText().clear();
        holder.etName.requestFocus();
        holder.btnDelete.setVisibility(View.INVISIBLE);
        holder.btnShare.setVisibility(View.INVISIBLE);
        holder.btnCopy.setVisibility(View.INVISIBLE);
        holder.btnSaveDB.setVisibility(View.INVISIBLE);
    }

    //Metodo che mostra il tutorial della schermata al primo avvio
    private void firstTutorial(){
        BubbleShowCaseBuilder builder1 = new BubbleShowCaseBuilder(MainActivity.this);
        builder1.title(getString(R.string.bubbleMainNome));
        builder1.targetView(findViewById(R.id.etNome));

        BubbleShowCaseBuilder builder2 = new BubbleShowCaseBuilder(MainActivity.this);
        builder2.title(getString(R.string.bubbleMainFab));
        builder2.targetView(findViewById(R.id.btnCalcola));

        BubbleShowCaseSequence sequence = new BubbleShowCaseSequence();
        sequence.addShowCase(builder1);
        sequence.addShowCase(builder2);
        sequence.show();
        prefs.setFirstActivity(MAIN,false);
    }

    //Metodo che mostra il tutorial della schermata una volta calcolato il primo codice
    private void calcTutorial(){
        BubbleShowCaseBuilder builder1 = new BubbleShowCaseBuilder(MainActivity.this);
        builder1.title(getString(R.string.bubbleCalcRisultato));
        builder1.targetView(findViewById(R.id.tvRisultato));

        BubbleShowCaseBuilder builder2 = new BubbleShowCaseBuilder(MainActivity.this);
        builder2.title(getString(R.string.bubbleCalcSave));
        builder2.targetView(findViewById(R.id.btnSaveDB));

        BubbleShowCaseBuilder builder3 = new BubbleShowCaseBuilder(MainActivity.this);
        builder3.title(getString(R.string.bubbleCalcCopy));
        builder3.targetView(findViewById(R.id.btnCopy));

        BubbleShowCaseBuilder builder4 = new BubbleShowCaseBuilder(MainActivity.this);
        builder4.title(getString(R.string.bubbleCalcShare));
        builder4.targetView(findViewById(R.id.btnShare));

        BubbleShowCaseBuilder builder5 = new BubbleShowCaseBuilder(MainActivity.this);
        builder5.title(getString(R.string.bubbleCalcProfile));
        builder5.targetView(holder.toolbar.findViewById(R.id.menu_favorites));

        BubbleShowCaseBuilder builder6 = new BubbleShowCaseBuilder(MainActivity.this);
        builder6.title(getString(R.string.bubbleCalcList));
        builder6.targetView(holder.toolbar.findViewById(R.id.menu_list));

        BubbleShowCaseSequence sequence = new BubbleShowCaseSequence();
        sequence.addShowCase(builder1);
        sequence.addShowCase(builder2);
        sequence.addShowCase(builder3);
        sequence.addShowCase(builder4);
        sequence.addShowCase(builder5);
        sequence.addShowCase(builder6);
        sequence.show();
        prefs.setFirstActivity(CALC,false);
    }
}

