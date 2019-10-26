package com.sandra.falldetector2.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sandra.falldetector2.util.MqttManagerAndroid;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import com.sandra.falldetector2.App;
import com.sandra.falldetector2.R;
import com.sandra.falldetector2.adapter.ContactAdpter;
import com.sandra.falldetector2.model.Contact;
import com.sandra.falldetector2.repository.ContactRepository;
import com.sandra.falldetector2.util.SmsDeliveredReceiver;
import com.sandra.falldetector2.util.SmsSentReceiver;

public class RegisterActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {


    private static final String
            FALL_CAPABILITY_NAME = "fall_notification";

    @BindView(R.id.toolbar_right_button)
    Button rigthButton;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.toolbar_left_button)
    ImageButton leftButton;
    private Toolbar toolbar;
    private ContactRepository contactRepository;
    @BindView(R.id.contactList)
    RecyclerView contactList;
    MqttManagerAndroid mqttManagerAndroid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        contactRepository = App.getInstance().getContactRepository();
        FloatingActionButton fab = findViewById(R.id.fab);
        mqttManagerAndroid = new MqttManagerAndroid(this);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewEmailDialog();
            }
        });


        Wearable.getMessageClient(this).addListener(this);

        configRecyclerView();
        setContactListAdpter();

        new RxPermissions(this)
                .request(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isGranted -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {

                    }


                }, error -> {
                });


    }

    private void sendSmsToAll(){

        String username = PreferenceManager.getDefaultSharedPreferences(this).getString("username", null);
        Location location = App.getInstance().getLocation();
        String message = "";
        if (location != null)
            message = "Este é um chamado de alerta para os amigos de o(a)" + username + ", que provavelmente sofreu uma queda em: \n\n https://www.google.com/maps/search/?api=1&query="+ location.getLatitude()+","+location.getLongitude();
        else
            message = "Este é um chamado de alerta para os amigos de o(a)" + username + ", que provavelmente sofreu uma queda.";
        Contact[] contacts = contactRepository.getAllContacts();
        if (contacts.length > 0){
            for (Contact c:contacts){
                String number = c.getNumber().replace("(","").replace(")","").replace("-","").replace(" ","");
                String phone = "+55" +number;
                sendSMS(phone,message);
                Log.d("teste", "sendSmsToAll: " + phone);
            }
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(this, SmsSentReceiver.class), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(this, SmsDeliveredReceiver.class), 0);
        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> mSMSMessage = sms.divideMessage(message);
            for (int i = 0; i < mSMSMessage.size(); i++) {
                sentPendingIntents.add(i, sentPI);
                deliveredPendingIntents.add(i, deliveredPI);
            }
            sms.sendMultipartTextMessage(phoneNumber, null, mSMSMessage,
                    sentPendingIntents, deliveredPendingIntents);

        } catch (Exception e) {

            e.printStackTrace();
            Toast.makeText(getBaseContext(), "SMS sending failed...",Toast.LENGTH_SHORT).show();
        }

    }

    public void callNumber(){
        Contact[] contacts = contactRepository.getAllContacts();
        for (Contact c: contacts){
            if (c.isImportant()){
                String number = c.getNumber().replace("(","").replace(")","").replace("-","").replace(" ","");
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:+55" + number));
                startActivity(intent);
            }
        }

    }

    void showToast(String text) {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Atenção");
        builder.setMessage(text);
        builder.setPositiveButton("OK", null);
        android.app.AlertDialog toastDialog = builder.create();
        toastDialog.show();
    }


    public void configRecyclerView() {

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        contactList.setLayoutManager(layoutManager);
        contactList.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));
    }

    public void setContactListAdpter(){

        Contact[] contacts = App.getInstance().getContactRepository().getAllContacts();
        if(contacts!=  null && contacts.length > 0){
            ContactAdpter adpter = new ContactAdpter(contacts);
            contactList.setAdapter(adpter);
            contactList.requestLayout();
        }

    }

    public void showNewEmailDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = RegisterActivity.this.getLayoutInflater().inflate(R.layout.dialog_new_contact, null);
        final EditText name = view.findViewById(R.id.name_text);
        final EditText mail = view.findViewById(R.id.mail_text);
        builder.setView(view);
        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.setOnShowListener(dialogInterface -> {

            Button positiveButton = view.findViewById(R.id.ok_button);
            positiveButton.setOnClickListener(v -> {
                //TODO Cadastrar email
                saveContact(name.getText().toString(),mail.getText().toString());
                dialogInterface.dismiss();
            });

            Button negativeButton = view.findViewById(R.id.cancel_button);
            negativeButton.setOnClickListener(v -> dialogInterface.dismiss());

        });
        alertDialog.show();
    }

    public void saveContact(String name, String mail){
        Contact c = new Contact(name,mail,false);
        App.getInstance().getContactRepository().saveContact(c);
        setContactListAdpter();
    }

    public void configActionBar(){

        toolbar = findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbarTitle.setText("Fall Detector");
        getSupportActionBar().setElevation(0);
        leftButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(FALL_CAPABILITY_NAME)) {
            showToast("Mensagem Recebida: " + new String(messageEvent.getData()));
            sendSmsToAll();
            callNumber();
            mqttManagerAndroid.publishMessage("teste","/teste");
        }
    }


//    @Override
//    public void onMessageReceived(MessageEvent messageEvent) {
//
//        if (messageEvent.getPath().equals(FALL_CAPABILITY_NAME)) {
//            showToast("Mensagem Recebida");
//        }
//    }


}
