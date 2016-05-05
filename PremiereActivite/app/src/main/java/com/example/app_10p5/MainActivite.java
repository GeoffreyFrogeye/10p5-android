package com.example.app_10p5;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;


/**
 * Created by beaus on 24/04/2016.
 */
public class MainActivite extends Activity implements ASyncResponse, main_tab_frag.OnFragmentInteractionListener {

    public static final int STATE_RIEN = 0;
    public static final int STATE_COMMANDE = 3;
    public static final int STATE_VIDANGE = 4;
    public static final int STATE_RECHARGEMENT = 2;
    public static final int STATE_CREATION_COMPTE = 1;
    public static final int STATE_CONNEXION = 5;

    public static final long EXPIRATION = 1000*60*10;

    private int mState;
    private String mToken;
    private int mDroit;
    private long mTimeToken;
    private String mUser;

    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setTitle(getResources().getString(R.string.app_name));

        mState = STATE_RIEN;
        mTimeToken = -1;
        mToken = null;

        getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());

        if(savedInstanceState == null){
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            ConnectionFragment fragment = new ConnectionFragment();
            fragmentTransaction.add(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
        else{
            mTimeToken = savedInstanceState.getLong("timeToken");
            mToken = savedInstanceState.getString("token");
            mState = savedInstanceState.getInt("state");
            mUser = savedInstanceState.getString("user");
            mDroit = savedInstanceState.getInt("droit");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
       if(item.getItemId() == R.id.action_settings){
           getFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).addToBackStack("settings").commit();
       }
        else if(item.getItemId() == R.id.action_disconnect){
           disconnect();
       }

       return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String s){

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putString("token", mToken);
        savedInstanceState.putInt("state", mState);
        savedInstanceState.putString("user", mUser);
        savedInstanceState.putInt("droit", mDroit);
        savedInstanceState.putLong("timeToken", mTimeToken);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupForegroundDispatch(this, mNfcAdapter);
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter){
        if(adapter != null){
            final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
            adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
        }
        else{
            Toast.makeText(activity, "Impossible d'initialiser le NFC", Toast.LENGTH_SHORT).show();
        }
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        if(adapter != null){
            adapter.disableForegroundDispatch(activity);
        }
        else{
            Toast.makeText(activity, "Impossible d'initialiser le NFC", Toast.LENGTH_SHORT).show();
        }
    }

    public void valideCreationCompte(View v){
        if((TextUtils.getTrimmedLength(mToken) == 30) && ((System.currentTimeMillis() - mTimeToken) < EXPIRATION)) {
            EditText champMontant = (EditText) findViewById(R.id.creation_montant);
            float montant = 0.0f;

            if(!TextUtils.isEmpty(champMontant.getText().toString())){
                try{
                    montant = Float.parseFloat(champMontant.getText().toString());
                }
                catch (Throwable t){
                    Toast.makeText(this, "Remplir le champ montant avec un nombre: " + t.toString(), Toast.LENGTH_LONG).show();
                }

                if(mDroit >= 1){
                    if((montant > 0.0f) && (montant < 200.0f)){
                        mState = STATE_CREATION_COMPTE;
                        champMontant.setText(null);

                        Bundle b = new Bundle();
                        b.putString("token", mToken);
                        b.putInt("state", mState);
                        b.putFloat("montant", montant);


                        NFCFragment nfc = new NFCFragment();
                        nfc.setArguments(b);

                        getFragmentManager().beginTransaction().replace(R.id.fragment_container, nfc).addToBackStack(null).commit();
                    }
                    else{
                        Toast.makeText(this, "Valeur incorrecte.", Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(this, "Droit insuffisant.", Toast.LENGTH_LONG).show();
                }
            }
            else{
                champMontant.setError("Montant requis.");
            }
        }
        else{
            disconnect();
        }
    }

    public void valideCommande(View v)
    {
        if((TextUtils.getTrimmedLength(mToken) == 30) && ((System.currentTimeMillis() - mTimeToken) < EXPIRATION)) {
            EditText champMontant = (EditText) findViewById(R.id.commande_prix);
            EditText champQuantite = (EditText) findViewById(R.id.commande_quantite);
            float montant = 0.0f;
            int quantite = 0;

            //TODO: gérer le XOR de pute

            try{
                montant = Float.parseFloat(champMontant.getText().toString());
                quantite = Integer.parseInt(champQuantite.getText().toString());
            }
            catch (Throwable t)
            {
                Toast.makeText(this, "Remplir les champs avec des nombres: " + t.toString(), Toast.LENGTH_LONG).show();
            }

            if ((montant > 0.0f) && (montant < 200.0f) && (quantite > 0) && (mDroit >= 1)) {
                mState = STATE_COMMANDE;
                champMontant.setText(null);
                champQuantite.setText(null);

                Bundle b = new Bundle();
                b.putString("token", mToken);
                b.putInt("state", mState);
                b.putFloat("montant", montant);
                b.putInt("quantite", quantite);

                NFCFragment nfc = new NFCFragment();
                nfc.setArguments(b);

                getFragmentManager().beginTransaction().replace(R.id.fragment_container, nfc).addToBackStack(null).commit();
            }
            else{
                Toast.makeText(this, "Valeur incorrecte ou droit insuffisant.", Toast.LENGTH_LONG).show();
            }
        }
        else{
            disconnect();
        }
    }

    public void valideRechargement(View v)
    {
        System.out.println(TextUtils.getTrimmedLength(mToken));
        if((TextUtils.getTrimmedLength(mToken) == 30) && ((System.currentTimeMillis() - mTimeToken) < EXPIRATION)) {
            EditText champMontant = (EditText) findViewById(R.id.rechargement_champ_montant);
            float montant = 0.0f;

            if(!TextUtils.isEmpty(champMontant.getText().toString())){
                try{
                    montant = Float.parseFloat(champMontant.getText().toString());
                }
                catch (Throwable t){
                    Toast.makeText(this, "Remplir le champ montant avec un nombre: " + t.toString(), Toast.LENGTH_LONG).show();
                }

                if(mDroit >= 2){
                    if((montant > 0.0f) && (montant < 200.0f)){
                        mState = STATE_RECHARGEMENT;
                        champMontant.setText(null);

                        Bundle b = new Bundle();
                        b.putString("token", mToken);
                        b.putInt("state", mState);
                        b.putFloat("montant", montant);

                        NFCFragment nfc = new NFCFragment();
                        nfc.setArguments(b);

                        getFragmentManager().beginTransaction().replace(R.id.fragment_container, nfc).addToBackStack(null).commit();
                    }
                    else{
                        Toast.makeText(this, "Valeur incorrecte.", Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(this, "Droit insuffisant.", Toast.LENGTH_LONG).show();
                }
            }
            else{
                champMontant.setError("Montant requis.");
            }
        }
        else{
            disconnect();
        }
    }

    public void valideConnection(View v)
    {
        EditText viewUser = (EditText) findViewById(R.id.connection_username);
        EditText viewPsw = (EditText) findViewById(R.id.connection_password);

        String user = viewUser.getText().toString();
        String password = viewPsw.getText().toString();

        if (!TextUtils.isEmpty(user)) {
            if(!TextUtils.isEmpty(password)){
                mState = STATE_CONNEXION;

                viewUser.setText(null);
                viewPsw.setText(null);

                try{
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    URL url = new URL(settings.getString("server_address", null) + "api/utilisateur/connexion");
                    HashMap<String, String> param = new HashMap<String, String>();
                    param.put("login", URLEncoder.encode(user, "UTF-8"));
                    param.put("mdp", URLEncoder.encode(password, "UTF-8"));
                    NetworkThread nt = new NetworkThread(url, param);
                    nt.delegate = this;
                    nt.execute();
                }
                catch (Throwable t) {
                    Toast.makeText(this, "Erreur: " + t.toString(), Toast.LENGTH_LONG).show();
                    System.out.println("Exception: " + t.toString());
                }
            }
            else {
                viewPsw.setError("Mot de passe requis.");
            }
        }
        else{
            viewUser.setError("Utilisateur requis.");
        }
    }

    public void valideVidange(View v){
        if((TextUtils.getTrimmedLength(mToken) == 30) && ((System.currentTimeMillis() - mTimeToken) < EXPIRATION)) {

            if((mDroit >= 2)){
                mState = STATE_VIDANGE;
                Bundle b = new Bundle();
                b.putString("token", mToken);
                b.putInt("state", mState);

                NFCFragment nfc = new NFCFragment();
                nfc.setArguments(b);

                getFragmentManager().beginTransaction().replace(R.id.fragment_container, nfc).addToBackStack(null).commit();
            }
            else{
                Toast.makeText(this, "Droit insuffisant.", Toast.LENGTH_LONG).show();
            }
        }
        else{
            disconnect();
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        Fragment frag = getFragmentManager().findFragmentById(R.id.fragment_container);

        if(mState != STATE_RIEN && frag instanceof NFCFragment){
            NFCFragment nfc = (NFCFragment) frag;
            nfc.handleIntent(intent);
        }
    }

    /* Retour du network thread */
    @Override
    public void processFinish(JSONObject output) {

        if(output.length() != 0){
            try{
                if(output.get("status").toString().equals("ok")){
                    switch (mState){
                        case STATE_COMMANDE:
                            Snackbar.make(findViewById(R.id.coordinator), "Client débité de " + output.get("montant") + "€. " + output.get("soldeAncien") + "€ -> " + output.getString("soldeNouveau") + "€", Snackbar.LENGTH_INDEFINITE).show();
                            break;
                        case STATE_CONNEXION:
                            mToken = output.get("jeton").toString();
                            mTimeToken = System.currentTimeMillis();
                            mDroit = output.getInt("droit");
                            mUser = output.get("login").toString();
                            Snackbar.make(findViewById(R.id.coordinator), "Bonjour " + mUser + " vous êtes connecté pour " + EXPIRATION / (1000 * 60) + " minutes.", Snackbar.LENGTH_SHORT).show();
                            getFragmentManager().beginTransaction().replace(R.id.fragment_container, new main_tab_frag()).commit();
                            break;
                        case STATE_CREATION_COMPTE:
                            Snackbar.make(findViewById(R.id.coordinator), "Client créé avec un solde de " + output.get("soldeNouveau") + "€", Snackbar.LENGTH_INDEFINITE).show();
                            break;
                        case STATE_RECHARGEMENT:
                            Snackbar.make(findViewById(R.id.coordinator), "Client rechargé: " + output.get("soldeAncien") + "€ ->" + output.get("soldeNouveau") + "€", Snackbar.LENGTH_INDEFINITE).show();
                            break;
                        case STATE_VIDANGE:
                            Snackbar.make(findViewById(R.id.coordinator), "Client vidé: " + output.get("soldeAncien") + "€ -> 0€", Snackbar.LENGTH_INDEFINITE).show();
                            break;
                        case STATE_RIEN:
                        default:
                            Toast.makeText(this, "WTF, le cancer est dans l'application!!", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
                else{
                    Toast.makeText(this, "Erreur: " + output.get("status"), Toast.LENGTH_LONG).show();
                }
            }
            catch(Throwable t){
                Toast.makeText(this, "WTF, le cancer est dans l'application!!" + t.toString(), Toast.LENGTH_LONG).show();
            }
        }
        else{
            Toast.makeText(this, "Impossible de se connecter au serveur", Toast.LENGTH_LONG).show();
        }

        mState = STATE_RIEN;
        getFragmentManager().popBackStack();
    }

    public String getToken(){
        return mToken;
    }

    public long getTimeToken(){
        return mTimeToken;
    }

    public void disconnect(){
        mToken = null;
        mDroit = 0;
        mUser = null;
        mTimeToken = -1;
        mState = STATE_RIEN;

        Toast.makeText(this, "Veuillez vous reconnecter.", Toast.LENGTH_SHORT).show();
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setCurrentItem(0);
    }
}
