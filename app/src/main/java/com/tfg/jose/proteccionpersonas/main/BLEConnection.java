package com.tfg.jose.proteccionpersonas.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tfg.jose.proteccionpersonas.R;

import java.text.DecimalFormat;

/**
 * Created by Jose on 22/10/2016.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEConnection {

    private Context mContext;
    private Activity mActivity;

    private Notification notifi;

    private boolean deviceFound; // True si encuentra el dispositivo
    private double px;  // Valor de intensidad de señal entre dos dispositivos a un metro de distancia.

    private final static int REQUEST_ENABLE_BT = 1;

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;

    private String direccion_dispositivo;
    private int distancia_limite;

    // Constructor
    public BLEConnection(Context context, Activity activity){
        this.mContext = context;
        this.mActivity = activity;

        this.notifi = new Notification(context, activity);

        btManager = (BluetoothManager)activity.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        this.deviceFound = false;

        this.px = -54;

//        this.nombre_dispositivo = "jose-TravelMate-5742-0";
        this.direccion_dispositivo = "F4:BE:76:06:43:75";
        this.distancia_limite = 30;

        btAdapter.startLeScan(leScanCallback);
    }

    // Si está desactivado el Bluetooth, enviamos mensaje para activarlo
    void estaActivado(){
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
    }

    boolean bleState(){
        if(btAdapter.isEnabled()){ return true; }
        else { return false; }
    }

    void startScanBLEDevices(){
        btAdapter.startLeScan(leScanCallback);
    }

    void stopScanBLEDevices(){
        btAdapter.stopLeScan(leScanCallback);
    }

    boolean isDiscovering(){
        return btAdapter.isDiscovering();
    }

    // Devuelve la distancia aproximada en metros entre dos dispositivos
    double getDistance(double rssi, double txPower) {
        // El 2.7 es el valor de n y si no hay obstáculos de por medio se usa el valor 2
        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2.7));
    }

    boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    // Si no encuentra nada tras doce segundos
    void sinPeligro(){
        //delay in ms
        int DELAY = 12000;

        // Si cuando se acaba la búsqueda, no lo encontró, no hay peligro
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView rssi_msg = (TextView) mActivity.findViewById(R.id.res_busqueda);
                TextView rssi_dist = (TextView) mActivity.findViewById(R.id.res_distancia);

                mActivity.invalidateOptionsMenu(); // Refrescamos el menu

                if (deviceFound == false && btAdapter.isEnabled()) {
                    btAdapter.stopLeScan(leScanCallback);

                    notifi.setSms_enviado(0); // Acutlizamos a 0 para si vuelve a encotnrar al agresor

                    rssi_msg.setText(mContext.getString(R.string.sin_peligro));
                    rssi_dist.setText("");

                } else if (deviceFound == false && !btAdapter.isEnabled()) {
                    btAdapter.stopLeScan(leScanCallback);

                    rssi_msg.setText(mContext.getString(R.string.b_desactivado));
                    rssi_dist.setText("");

                    notifi.bluetooth_desactivado();
                } else {
                    deviceFound = false;
                }
            }
        }, DELAY);
    }

    // Se llama cada vez que se detecta un dispositivo BLE
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            Log.i("FIND_D", "Encontrado: " + device.getName());
            Log.i("FIND_D", "Encontrado: " + device.getAddress());

            if(device.getAddress().equals("F4:BE:76:06:43:75")){
                btAdapter.stopLeScan(leScanCallback);

                // Calculamos la distancia aproximada
                double distance = getDistance(rssi, px);

                deviceFound = true; // Encontró el dispositivo

                // Parseamos el resultado para que muestre dos decimales
                DecimalFormat df = new DecimalFormat("#.##");
                String rdistance = df.format(distance);

                TextView rssi_msg = (TextView) mActivity.findViewById(R.id.res_busqueda);
                TextView res_dist = (TextView) mActivity.findViewById(R.id.res_distancia);

                // Si está dentro de la distancia límite se le avisa
                if(distance < distancia_limite){

                    rssi_msg.setText(mContext.getString(R.string.peligro) + "\n" + mContext.getString(R.string.mensaje_peligro));
                    res_dist.setText(rdistance + "m");

                    // Notificación del límite superado
                    notifi.notificar_limite();

                    // Envío de aviso a los contactos
                    notifi.enviar_sms();
                    notifi.setSms_enviado(1);

                    // Abre la activity, si esta cerrada, con los resultados
                    if(mActivity.hasWindowFocus() == false) {
                        Intent intento = new Intent(mContext, Inicio.class);
                        intento.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        mContext.startActivity(intento);
                    }

                    // Iniciamos el servicio con la grabación de vídeo
                    if(isMyServiceRunning(BackgroundVideoRecorder.class) == false){
                        mContext.startService(new Intent(mContext, BackgroundVideoRecorder.class));
                    }

                    TextView grabando = (TextView) mActivity.findViewById(R.id.grabando);
                    grabando.setCompoundDrawablesWithIntrinsicBounds(R.drawable.grabando, 0, 0, 0);
                    grabando.setVisibility(View.VISIBLE);
                }

                // Si lo encuentra pero no la supera, se le dice
                else {

                    rssi_msg.setText(mContext.getString(R.string.mensaje_aviso));
                    res_dist.setText(rdistance + "m");

                    // Notificación de que se encuentra por los alrededores
                    notifi.notificar_radio();
                }

                mActivity.invalidateOptionsMenu(); // Refrescamos el menú
            }
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
        }
    };

}