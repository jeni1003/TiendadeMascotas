package com.example.tiendademascotas;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.net.wifi.aware.PublishConfig;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.Intent;
import android.os.AsyncTask;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    JSONArray datosJSON;
    JSONObject jsonObject;
    Integer posicion;
    ArrayList<String> arrayList = new ArrayList<String>();
    ArrayList<String> copyStringArrayList = new ArrayList<String>();
    ArrayAdapter<String> stringArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        obtenerDatosMascota objMascotas = new obtenerDatosMascota();
        objMascotas.execute();
        FloatingActionButton btnAgregarNuevaMasc = findViewById( R.id.btnAgregarProductoMas );
        btnAgregarNuevaMasc.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                agregarNuevaMascota( "nuevo", jsonObject );
            }
        } );

    }
//Llamada de menú
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu( menu, v, menuInfo );
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate( R.menu.menuprincipal, menu );
        try {
            AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            posicion = adapterContextMenuInfo.position;
            menu.setHeaderTitle( datosJSON.getJSONObject( posicion ).getString( "nombre" ) );
        } catch (Exception ex) {

        }
    }
//Opciones de menú
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnxAgregarMasco:
                agregarNuevaMascota( "nuevo", jsonObject );
                return true;

            case R.id.mnxModificarMasco:
                try {
                    agregarNuevaMascota( "modificar", datosJSON.getJSONObject( posicion ) );
                } catch (Exception ex) {
                }
                return true;

            case R.id.mnxEliminarMasco:

                AlertDialog eliminarFriend = eliminarMascota();
                eliminarFriend.show();
                return true;

            default:
                return super.onContextItemSelected( item );
        }
    }
//agregar
    private void agregarNuevaMascota(String accion, JSONObject jsonObject) {
        try {
            Bundle enviarParametros = new Bundle();
            enviarParametros.putString( "accion", accion );
            enviarParametros.putString( "dataAmigo", jsonObject.toString() );

            Intent agregarMasco = new Intent( MainActivity.this, agregar_enmascotas.class );
            agregarMasco.putExtras( enviarParametros );
            startActivity( agregarMasco );
        } catch (Exception e) {
            Toast.makeText( getApplicationContext(), "Error al llamar agregar Mascota: " + e.toString(), Toast.LENGTH_LONG ).show();
        }
    }
    //eliminar
        AlertDialog eliminarMascota () {
            AlertDialog.Builder confirmacion = new AlertDialog.Builder( MainActivity.this );
            try {
                confirmacion.setTitle( datosJSON.getJSONObject( posicion ).getJSONObject( "value" ).getString( "nombre" ) );
                confirmacion.setMessage( "Esta seguro de eliminar el registro?" );
                confirmacion.setPositiveButton( "Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                         eliminarDatosMascota  objEliminarMascot= new eliminarDatosMascota();
                        objEliminarMascot.execute();

                        Toast.makeText( getApplicationContext(), "Mascota eliminado con exito.", Toast.LENGTH_SHORT ).show();
                        dialogInterface.dismiss();
                    }
                } );
                confirmacion.setNegativeButton( "No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText( getApplicationContext(), "Eliminacion cancelada por el usuario.", Toast.LENGTH_SHORT ).show();
                        dialogInterface.dismiss();
                    }
                } );
            } catch (Exception ex) {
                Toast.makeText( getApplicationContext(), "Error al mostrar la confirmacion: " + ex.getMessage(), Toast.LENGTH_LONG ).show();
            }
            return confirmacion.create();
        }
    //obtener datos
    private class obtenerDatosMascota extends AsyncTask<Void, Void, String>{
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder result= new StringBuilder( );
            try {
                URL url= new URL( "http://192.168.0.15:5984/tiendamascotas/_design/Mascotas/_view/mi-mascota" );
                urlConnection =(HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(  "GET" );
                InputStream in= new BufferedInputStream( urlConnection.getInputStream() );
                BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
                String linea;
                while ((linea= reader.readLine())!=null){
                    result.append( linea );

                }
            } catch (Exception ex){
                //
            }
            return result.toString();
        }
        //onpost
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute( s );
            try{
                jsonObject = new JSONObject(s);
                datosJSON = jsonObject.getJSONArray("rows");
                mostrarDatosMascota();
            }catch (Exception ex){
                Toast.makeText(MainActivity.this, "Error la parsear los datos: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    //Mostrar datos
    private void mostrarDatosMascota (){
        ListView ltsMascotas = findViewById( R.id.ltsTiendaMascotaCouchDB );
        try {

            final ArrayList<String> arrayList = new ArrayList<>();
            final ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>( MainActivity.this, android.R.layout.simple_list_item_1, arrayList );
            ltsMascotas.setAdapter( stringArrayAdapter );
            for (int i = 0; i < datosJSON.length(); i++) {
                stringArrayAdapter.add( datosJSON.getJSONObject( i ).getJSONObject( "value" ).getString( "nombre" ) );
            }
            stringArrayAdapter.notifyDataSetChanged();
            registerForContextMenu( ltsMascotas );
        } catch (Exception ex){
            Toast.makeText(MainActivity.this, "Error al mostrar los datos: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private class eliminarDatosMascota extends AsyncTask<String,String, String> {
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(String... parametros) {
            StringBuilder stringBuilder = new StringBuilder();
            String jsonResponse = null;
            try {
                URL url = new URL( "http://192.168.0.15:5984/tiendamascotas/" +
                        datosJSON.getJSONObject( posicion ).getJSONObject( "value" ).getString( "_id" ) + "?rev=" +
                        datosJSON.getJSONObject( posicion ).getJSONObject( "value" ).getString( "_rev" ) );

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod( "DELETE" );

                InputStream in = new BufferedInputStream( urlConnection.getInputStream() );
                BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );

                String inputLine;
                StringBuffer stringBuffer = new StringBuffer();
                while ((inputLine = reader.readLine()) != null) {
                    stringBuffer.append( inputLine + "\n" );
                }
                if (stringBuffer.length() == 0) {
                    return null;
                }
                jsonResponse = stringBuffer.toString();
                return jsonResponse;
            } catch (Exception ex) {
                //
            }
            return null;
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                if (jsonObject.getBoolean("ok")) {
                    Toast.makeText(getApplicationContext(), "Datos de amigo guardado con exito", Toast.LENGTH_SHORT).show();
                    obtenerDatosMascota objObtenerTMascota = new obtenerDatosMascota();
                    objObtenerTMascota.execute();
                } else {
                    Toast.makeText(getApplicationContext(), "Error al intentar guardar datos de Mascota", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error al guardar Mascota: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}

