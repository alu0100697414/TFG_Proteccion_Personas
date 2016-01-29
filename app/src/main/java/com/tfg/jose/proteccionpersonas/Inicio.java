package com.tfg.jose.proteccionpersonas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class Inicio extends AppCompatActivity {

    BluetoothConnection bluetooth;

    // Constructor
    public Inicio(){
        bluetooth = new BluetoothConnection(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Filtro para cuando encuentre dispositivos bluetooth
        IntentFilter bluetoothFilter = new IntentFilter();
        bluetoothFilter.addAction(BluetoothDevice.ACTION_FOUND);

        registerReceiver(bluetooth.getReceiver(), bluetoothFilter);

        bluetooth.estaActivado(); // Comprobamos si esta activado el bluetooth y sino, envia mensaje de activacion

        bluetooth.emparejados(); // Devuelve dispositivos emparejados

        bluetooth.emparejadoInfo(); // Coge info del dispositivo del agresor (emparejado)

        bluetooth.estaBuscando(); // Si esta buscando, para la busqueda

        bluetooth.buscar(); // Inicia la busqueda

        bluetooth.sinPeligro(); // Si tras 12s no lo encuentra, muestra mensaje de que no hay peligro
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_inicio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Destructor para cuando se cierre el programa
    @Override
    public void onDestroy() {
        bluetooth.estaBuscando(); // Vuelvo a comprobar que esté parada la busqueda de dispositivos

        unregisterReceiver(bluetooth.getReceiver());
        super.onDestroy();
    }

    BluetoothConnection getBluetoothConnection(){
        return bluetooth;
    }
}
