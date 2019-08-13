package com.k_duskplayer.btooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,View.OnClickListener, View.OnLongClickListener {

    private Button send;
    private int[] ids={R.id.aa, R.id.b,R.id.c,R.id.d,R.id.e,R.id.f,R.id.g,R.id.h,R.id.i,R.id.j,R.id.k,R.id.l,R.id.m,R.id.n,R.id.o,R.id.p};
    private String[] out;
    private Button stopConnect;
    BluetoothAdapter badapter;
    public TextView textView;
    public TextView textView2;
    StringBuilder messages;
    int a =0,t=0;
    private ArrayList<BluetoothDevice> btarray = new ArrayList<>();
    private DeviceListAdapter mDevicesListAdapter;
    private ListView lvNewDevices;
    BluetoothConnectionService mBluetoothConnection;
    private EditText textSend;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothDevice mBTDevice;
    LinearLayout lin;
    LinearLayout linearwrap;
    String check;


    private final BroadcastReceiver mReceiver3 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                checkPairedDevices(device);
                btarray.add(device);
                //textView.append("\n onreciever3 Device:" +device.getName()+":"+device.getAddress());
                mDevicesListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, btarray);
                lvNewDevices.setAdapter(mDevicesListAdapter);
            }
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
            {
                Toast.makeText(MainActivity.this, "Discovery Finished", Toast.LENGTH_SHORT).show();
                if (mDevicesListAdapter.getCount() == 0) {
                    String str = "NO DEVICES FOUND, TRY AGAIN";
                    textView2.setText(str);
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver4 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    t=0;
                    print("\nDevice connected with " + device.getName());
                    String connect = "CONNECTED WITH "+mBTDevice.getName();
                    textView2.setText(connect);
                    //print("");
                    //print("\nYou can now send or recieve data...");
                    mBTDevice=device;
                    if (mBluetoothConnection == null)
                    {
                        mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                    }
                    startConnection();

                }
                else if (device.getBondState() == BluetoothDevice.BOND_BONDING)
                {
                    String connect = "CONNECTING WITH: "+mBTDevice.getName()+"...";
                    textView.setText("");
                    textView2.setText(connect);
                    t=1;
                }
                else if (device.getBondState() == BluetoothDevice.BOND_NONE && t==1)
                {
                    String str = "CHOOSE A DEVICE TO CONNECT WITH:";
                    textView2.setText(str);
                    str = "Couldn't connect with "+mBTDevice.getName()+", try again";
                    textView.setText(str);
                    t=0;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stopConnect = (Button) findViewById(R.id.button5);
        textView = (TextView) findViewById(R.id.textView);
        textView2 = (TextView) findViewById(R.id.texthead);
        send = (Button) findViewById(R.id.button4);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        btarray = new ArrayList<>();
        textSend = (EditText) findViewById(R.id.editText);
        lin = (LinearLayout) findViewById(R.id.linear);
        linearwrap = (LinearLayout) findViewById(R.id.linearwrap);
        messages = new StringBuilder();
        badapter = getDefaultAdapter();
        out = new String[16];
        send.setEnabled(false);
        textSend.setEnabled(false);
        stopConnect.setEnabled(false);
        textView2.setText("BLUETOOTH TERMINAL");

        textView.post(new Runnable() {
            @Override
            public void run() {
                if (lin.getHeight()!=linearwrap.getHeight())
                {
                    ViewGroup.LayoutParams textparams = textView.getLayoutParams();
                    int height = textView.getHeight() + lin.getHeight() - linearwrap.getHeight();
                    textparams.height = height-10;
                    textView.setLayoutParams(textparams);
                }
            }
        });


        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver5, new IntentFilter("incomingMessage"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver6, new IntentFilter("logtext"));

        lvNewDevices.setOnItemClickListener(MainActivity.this);

        for(int i=0; i<ids.length; i++){
            Button button = (Button) findViewById(ids[i]);
            button.setOnLongClickListener(MainActivity.this);
            button.setOnClickListener(MainActivity.this);
        }


        IntentFilter bondstatechange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver4, bondstatechange);

        textView.setMovementMethod(new ScrollingMovementMethod());


        if (badapter==null)
        {
            print("\nNo Bluetooth Adapter, activity shutting down...");
            SystemClock.sleep(3000);
            finish();
        }
        else
        {
            bt();
        }




        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = textSend.getText().toString();
                if (txt.equals("") && mBluetoothConnection != null){
                    byte[] bytes = txt.getBytes(Charset.defaultCharset());
                    mBluetoothConnection.write(bytes);
                    textSend.setText("");

                }
                else{
                    print("\nnot possible, error");
                    textSend.setText("");
                }

            }
        });

        stopConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                textView2.setText("BLUETOOTH TERMINAL");
                stop();
            }
        });
    }

    BroadcastReceiver mReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String text = intent.getStringExtra("thedata");
            //StringBuffer st = new StringBuffer();
            //st.append(text);
            //byte[] byteArr = text.getBytes();
                    if (mBTDevice!=null)
                    {
                        //textView.append("\n"+mBTDevice.getName()+": "+Arrays.toString(byteArr));
                        print(text);
                    }
                    else{
                        //textView.append("\nnull?: "+ Arrays.toString(byteArr));
                        print(text);
                    }

        }
    };

    BroadcastReceiver mReceiver6 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String text = intent.getStringExtra("data");
            //messages.append(text + "\n");

            //if (text!="Connected Device Error" && text!="Write Error"  &&  text!="ConnectedThread: Starting.") print("\n"+text);

            if (text.equals("ConnectedThread: Starting."))
            {
                    send.setEnabled(true);
                    textSend.setEnabled(true);
                    stopConnect.setEnabled(true);
                    lvNewDevices.setAdapter(null);
                    a=1;

            }
            //textView.append("\nyo yo0");
            if (stopConnect.isEnabled())
            {
                //textView.append("\nyo yo1");
                if (a==1)
                {
                    //textView.append("\nbla: "+text);
                    if (text.equals("Connected Device Error"))
                    {
                        //textView.append("\nyo yo2");
                        dialog(text);
                    }
                    if (text.equals("Write Error"))
                    {
                        //textView.append("\nyo yo2");
                        dialog(text);
                    }
                }

            }
        }
    };

    public void print(String text){
        if (text.equals(""))
        {
            textView.setText("");
        }
        else{
            if (check != null && !text.startsWith("\n"+badapter.getName())) {
                if ( check.startsWith("\n"+badapter.getName())) {
                    text = "\n"+text;
                    check=null;
                }
            }
            textView.append(text);
            if (text.startsWith("\n"+badapter.getName()))
            {
                check = text;
            }
        }
    }

    private void stop() {

        send.setEnabled(false);
        textSend.setEnabled(false);
        stopConnect.setEnabled(false);
        a=0;
        btarray.clear();
        lvNewDevices.setAdapter(null);
        print("");
        textSend.setText("");
        mBluetoothConnection.cancel();
        mBluetoothConnection=null;
        mBTDevice=null;

    }

    private void dialog(String message){
        textView2.append("--ERROR");
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(message)
                .setPositiveButton("Stop Connection", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stop();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void bt(){
        if(!badapter.isEnabled()){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Bluetooth is switched off")
                    .setPositiveButton("Switch On", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            badapter.enable();
                            Toast.makeText( MainActivity.this,"Bluetooth Enabled",Toast.LENGTH_SHORT).show();
                            btarray.clear();
                            lvNewDevices.setAdapter(null);
                            print("");
                            //btDiscover();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            print("\nBluetooth Off, activity shutting down...");
                            dialog.cancel();
                            SystemClock.sleep(3000);
                            finish();
                        }
                    });
            AlertDialog bt = builder.create();
            bt.show();
        }
        else{
            Toast.makeText( MainActivity.this,"Bluetooth Enabled",Toast.LENGTH_SHORT).show();
        }
    }

    public void startConnection(){
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    public void  startBTConnection(BluetoothDevice device, UUID uuid){
        mBluetoothConnection.startClient(device, uuid);
    }



    public void btDiscover(View view) {
        if (badapter!=null && badapter.isEnabled())
        {
            String str = "CHOOSE A DEVICE TO CONNECT WITH:";
            textView2.setText(str);
            if (a==0)
            {
                if(badapter.isDiscovering()) badapter.cancelDiscovery();
                checkBTPermissions();
                btarray.clear();
                lvNewDevices.setAdapter(null);
                print("");
                badapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver3, discoverDevicesIntent);
            }
            else {
                Toast.makeText( MainActivity.this,"Already connected with "+mBTDevice.getName()+".",Toast.LENGTH_SHORT).show();
                Toast.makeText( MainActivity.this,"Stop Connection First",Toast.LENGTH_SHORT).show();
            }

        }
        else Toast.makeText(MainActivity.this, "Enable Bluetooth First", Toast.LENGTH_SHORT).show();

    }


    @SuppressLint("NewApi")
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            @SuppressLint({"NewApi", "LocalSuppress"}) int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
    }


    public void checkPairedDevices(BluetoothDevice mBTDevice)
    {
        Set<BluetoothDevice> pairedDevices = badapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                try {
                    if (mBTDevice.equals(device)) {

                        try {
                            Method method = mBTDevice.getClass().getMethod("removeBond", (Class[]) null);
                            method.invoke(mBTDevice, (Object[]) null);
                            //textView.append("\n"+mBTDevice+": Bond removed.");

                        } catch (Exception e) {
                            print("\nerror: " + e.getMessage());
                        }
                    }
                }catch(NullPointerException e){
                    print("\nerror: " + e.getMessage());
                }

            }
        }
    }




    @Override
    public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
        if(badapter.isEnabled())
        {
            badapter.cancelDiscovery();
            mBTDevice = btarray.get(i);
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                btarray.get(i).createBond();
                mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
            }
        }
        else {
            Toast.makeText(MainActivity.this, "Enable Bluetooth First", Toast.LENGTH_SHORT).show();
            bt();
        }


    }


    @Override
    public void onClick(View v) {

        if(v.getId()== R.id.aa ||v.getId()==R.id.b ||v.getId()==R.id.c ||v.getId()==R.id.d ||v.getId()==R.id.e ||v.getId()==R.id.f ||v.getId()==R.id.g ||v.getId()==R.id.h ||v.getId()==R.id.i ||v.getId()==R.id.j ||v.getId()==R.id.k ||v.getId()==R.id.l ||v.getId()==R.id.m ||v.getId()==R.id.n ||v.getId()==R.id.o ||v.getId()==R.id.p ){

            String txt="";
            for (int i=0;i<ids.length;i++){
                if(v.getId()==ids[i]){
                    if (out[i] != null) txt = out[i];
                    break;
                }
            }
            if (badapter.isEnabled())
            {
                if (!txt.equals("")){
                    if(a==1){
                        byte[] bytes = txt.getBytes(Charset.defaultCharset());
                        mBluetoothConnection.write(bytes);
                    }
                    else Toast.makeText(MainActivity.this, "Connection Not Established", Toast.LENGTH_SHORT).show();

                }
                else{
                    Toast.makeText(MainActivity.this, "Enter Button Output", Toast.LENGTH_SHORT).show();
                }
            }
            else Toast.makeText(MainActivity.this, "Enable Bluetooth First", Toast.LENGTH_SHORT).show();


        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        try {

            this.unregisterReceiver(mReceiver3);

        } catch(IllegalArgumentException e) {

            e.printStackTrace();
        }
        try {

            this.unregisterReceiver(mReceiver4);

        } catch(IllegalArgumentException e) {

            e.printStackTrace();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver5);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver6);
        mBluetoothConnection.cancel();
        mBluetoothConnection=null;

    }


    @Override
    public boolean onLongClick(View v) {
        final View view = v;

        if(v.getId()== R.id.aa ||v.getId()==R.id.b ||v.getId()==R.id.c ||v.getId()==R.id.d ||v.getId()==R.id.e ||v.getId()==R.id.f ||v.getId()==R.id.g ||v.getId()==R.id.h ||v.getId()==R.id.i ||v.getId()==R.id.j ||v.getId()==R.id.k ||v.getId()==R.id.l ||v.getId()==R.id.m ||v.getId()==R.id.n ||v.getId()==R.id.o ||v.getId()==R.id.p ){

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            LinearLayout layout = new LinearLayout(MainActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            Button button = findViewById(view.getId());
            final EditText butname = new EditText(MainActivity.this);
            butname.setText(button.getText().toString());
            final EditText outstream = new EditText(MainActivity.this);
            for (int i=0;i<ids.length;i++)
            {
                if (ids[i]== view.getId())
                {
                    if (out[i]!=null)
                    {
                        outstream.setText(out[i]);
                    }
                    break;
                }
            }
            final TextView a = new TextView(MainActivity.this);
            a.setText("Button text: ");
            a.setAllCaps(true);
            a.setPadding(5,10,0,0);
            final TextView b = new TextView(MainActivity.this);
            b.setText("Button output: ");
            b.setAllCaps(true);
            b.setPadding(5,5,0,0);
            final TextView c = new TextView(MainActivity.this);
            c.setText("Button Settings:");
            c.setTextSize(20);
            c.setTextColor(getResources().getColor(R.color.colorAccent));
            c.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            c.setPadding(5,5,0,10);
            layout.addView(c);
            layout.addView(a);
            layout.addView(butname);
            layout.addView(b);
            layout.addView(outstream);
            builder.setView(layout)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Button button = findViewById(view.getId());
                            for (int i=0;i<ids.length;i++)
                            {
                                if (ids[i]== view.getId())
                                {
                                    out[i]=outstream.getText().toString();
                                    break;
                                }
                            }
                            if(butname.getText().toString().length()<6)
                            {
                                if(!butname.getText().toString().equals("")){
                                    button.setText(butname.getText().toString());
                                }

                            }
                            else{
                                button.setText(butname.getText().toString().substring(0,5));
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog bt = builder.create();
            bt.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    bt.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                    bt.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
                }
            });
            bt.show();
        }

        return true;
    }

}
