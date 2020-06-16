package it.runningexamples.fiscalcode;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.Holder> implements View.OnClickListener{
    private List<CodiceFiscaleEntity> savedCF;
    private CodiceFiscaleEntity lastDeleted;
    private Context mContext;
    private int lastDeletedPosition, counterSelected;
    private String stringCode;
    private SparseArray<CodiceFiscaleEntity> selectedItem = new SparseArray<>();
    private boolean selectionON = false;
    private PreferenceManager preferenceManager;
    private AdapterCallback adapterCallback;

    RecyclerAdapter(Context ctx, AdapterCallback callback){
        this.mContext = ctx;
        this.adapterCallback = callback;
        this.savedCF = AppDatabase.getInstance(mContext).codiceFiscaleDAO().getAll();
        preferenceManager = new PreferenceManager(mContext);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false));
    }


    @Override
    public void onBindViewHolder (@NonNull final Holder holder, final int position)  {
        final CodiceFiscaleEntity currentItem = savedCF.get(position);
        holder.tvNome.setText(currentItem.getNome());
        holder.tvCognome.setText(currentItem.getCognome());
        holder.tvData.setText(currentItem.getDataNascita());
        holder.tvLuogo.setText(currentItem.getLuogoNascita());
        if (currentItem.getGenere().equals("M")){
            holder.tvSesso.setText(R.string.genereMaschio);
        }
        else{
            holder.tvSesso.setText(R.string.genereFemmina);
        }
        holder.btnCode.setText(currentItem.getFinalFiscalCode());
        holder.btnCode.setOnClickListener(this);
        stringCode = holder.btnCode.getText().toString();

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!currentItem.isSelected() & !selectionON) {
                    Intent intent = new Intent(mContext, CFDetail.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);     // altrimenti non funziona :(
                    intent.putExtra("CF", currentItem);      // PARCELABLE
                    mContext.startActivity(intent);
                }else{
                    multipleSelection(currentItem, holder, position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                multipleSelection(currentItem, holder, position);
                return true;
            }
        });
    }

    void deleteSelected() {
        int itemCount = savedCF.size();
        for (int i = 0; i < savedCF.size(); i++) {
            if (savedCF.get(i).isSelected()){
                AppDatabase.getInstance(mContext).codiceFiscaleDAO().deleteCode(savedCF.get(i));
                savedCF.remove(i);
                notifyItemRemoved(i);
                notifyItemRangeRemoved(i,itemCount);
                i--;
            }
        }
        counterSelected = 0;
        adapterCallback.counter(false, true);
        adapterCallback.showHideItem();
    }

    private void multipleSelection(CodiceFiscaleEntity currentItem, Holder holder, int pos){
        if (!currentItem.isSelected()) {
            if (counterSelected == 0) {
                adapterCallback.showHideItem();
            }
            currentItem.setSelected(true);
            adapterCallback.counter(true, false);
            counterSelected++;
        }else   {
            currentItem.setSelected(false);
            adapterCallback.counter(false, false);
            --counterSelected;
            if (counterSelected == 0){
                adapterCallback.showHideItem();
            }
        }
        /*  cambiando semplicemente il colore dell'itemview, viene cambiato il colore di tutta la "riga" della recyclerView e quindi vengono
            tolti i bordi arrotondati */
        Drawable roundRectShape = holder.itemView.getBackground();
        roundRectShape.setTint(currentItem.isSelected() ? Color.CYAN : getCardColor());
        holder.itemView.setBackground(roundRectShape);
          /* parametro utilizzato per gestire
         la selezione multipla anche con l'OnClick.
         se la selezione multipla è in atto, allora anche la onClick aggiungerà elementi*/
        selectionON = savedCF.size() > 0;
    }

    private int getCardColor(){
        int colorId;
        if (preferenceManager.getTheme() == 1){
            colorId = ContextCompat.getColor(mContext, R.color.colorCardDark);
        }else{
            colorId = ContextCompat.getColor(mContext, R.color.colorCardLight);
        }
        return colorId;
    }

    void deleteItem(int position, RecyclerView rcv) {
        lastDeleted = savedCF.get(position);
        lastDeletedPosition = position;
        AppDatabase.getInstance(mContext).codiceFiscaleDAO().deleteCode(lastDeleted);
        savedCF.remove(position);
        notifyItemRemoved(position);
        showUndoSnackBar(rcv);
    }

    private void undoDelete() {
        savedCF.add(lastDeleted);
        AppDatabase.getInstance(mContext).codiceFiscaleDAO().saveNewCode(lastDeleted);
        notifyDataSetChanged();
        lastDeleted = null;
    }

    @Override
    public int getItemCount() {
        return savedCF.size();
    }

    private void showUndoSnackBar(RecyclerView recyclerView){

        Snackbar snackbar = Snackbar.make(recyclerView, R.string.itemRemoved, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undoDelete, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoDelete();
            }
        });
        snackbar.show();
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public void onClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Codice Fiscale", stringCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(mContext, R.string.cfCopied,Toast.LENGTH_SHORT).show();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvNome, tvCognome, tvData, tvLuogo, tvSesso;
        Button btnCode;
        Holder(@NonNull View itemView) {
            super(itemView);
            tvNome = itemView.findViewById(R.id.tvDetailNome);
            tvCognome = itemView.findViewById(R.id.tvDetailCognome);
            tvData = itemView.findViewById(R.id.tvDetailData);
            tvLuogo = itemView.findViewById(R.id.tvDetailLuogo);
            tvSesso = itemView.findViewById(R.id.tvDetailSesso);
            btnCode = itemView.findViewById(R.id.btnDetailCode);
        }
    }

    public interface AdapterCallback{
        void showHideItem();
        void counter(boolean add, boolean setZero);
    }
}
