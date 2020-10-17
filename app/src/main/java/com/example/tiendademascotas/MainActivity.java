package com.example.tiendademascotas;

import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    JSONArray datosJSON;
    JSONObject jsonObject;
    Integer posicion;
    ArrayList<String> arrayList = new ArrayList<String>();
    ArrayList<String> copyStringArrayList = new ArrayList<String>();
    ArrayAdapter<String> stringArrayAdapter;
    utilidadescomunes uc;
    DetectarmiInternet di;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        di = new DetectarmiInternet(getApplicationContext());
        //Si hay conección
        if (di.HayConexcionaInternet()) {
            conexionservidor objTiendaa = new conexionservidor();
            objTiendaa.execute( uc.url_consulta, "GET" );
        } else  {
            Toast.makeText(getApplicationContext(), "No hay conexion a internet.", Toast.LENGTH_LONG).show();
        }

        FloatingActionButton btnAgregarNuevaTie = findViewById( R.id.btnAgregarProductoTie );
        btnAgregarNuevaTie.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                agregarNuevaTienda( "nuevo", jsonObject );

            }
        } );
        buscarTienda();

    }
    void buscarTienda(){
        final TextView tempVal = (TextView)findViewById(R.id.txtBuscarProductoTienda);
        tempVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                arrayList.clear();
                if( tempVal.getText().toString().trim().length()<1 ){//no hay texto para buscar
                    arrayList.addAll(copyStringArrayList);
                } else{//hacemos la busqueda
                    for (String mascota : copyStringArrayList){
                        if(mascota.toLowerCase().contains(tempVal.getText().toString().trim().toLowerCase())){
                            arrayList.add(mascota);
                        }
                    }
                }
                stringArrayAdapter.notifyDataSetChanged();
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private class conexionservidor extends AsyncTask<String, String, String>{
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(String... parametros) {
            StringBuilder result= new StringBuilder( );
            try {
                String uri = parametros[0];
                String metodo= parametros[1];
                URL url= new URL( uri);
                urlConnection =(HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(metodo);

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
                mostrarDatosTienda();
            }catch (Exception ex){
                Toast.makeText(MainActivity.this, "Error la parsear los datos: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
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
                agregarNuevaTienda( "nuevo", jsonObject );
                return true;

            case R.id.mnxModificarMasco:
                try {
                    agregarNuevaTienda( "modificar", datosJSON.getJSONObject( posicion ) );
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
    private void agregarNuevaTienda(String accion, JSONObject jsonObject) {
        try {
            Bundle enviarParametros = new Bundle();
            enviarParametros.putString( "accion", accion );
            enviarParametros.putString( "dataTienda", jsonObject.toString() );

            Intent agregarTienda = new Intent( MainActivity.this, agregar_entienda.class );
            agregarTienda.putExtras( enviarParametros );
            startActivity( agregarTienda );
        } catch (Exception e) {
            Toast.makeText( getApplicationContext(), "Error al llamar agregar Tienda: " + e.toString(), Toast.LENGTH_LONG ).show();
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
                            try {
                                conexionservidor objEliminarAmigo = new conexionservidor();
                                objEliminarAmigo.execute(uc.url_mto +
                                        datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_id") + "?rev=" +
                                        datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_rev"), "DELETE");

                            }catch (Exception ex){
                                Toast.makeText(getApplicationContext(), "Error al intentar eliminar en Tienda: "+ ex.getMessage() , Toast.LENGTH_LONG).show();
                            }
                            dialogInterface.dismiss();
                        }
                    });

            confirmacion.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText( getApplicationContext(), "Eliminacion cancelada por el usuario.", Toast.LENGTH_SHORT ).show();
                    dialogInterface.dismiss();
                }
            });
                }catch (Exception ex){
                    Toast.makeText(getApplicationContext(), "Error al mostrar la confoirmacion: "+ ex.getMessage(), Toast.LENGTH_LONG).show();
                }

                return confirmacion.create();
            }

            //Mostrar datos


    private void mostrarDatosTienda (){
        ListView ltsMascotas = findViewById( R.id.ltsTiendaCouchDB );
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

    }


