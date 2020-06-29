package com.example.sharing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String nickName = "Sharing";
    private String serviceID = "ShareNow";
    private Strategy strategy = Strategy.P2P_CLUSTER;
    private Button send,stop;
    private TextView status;
    List<String>connectedPeers = new ArrayList<>();
    String [] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
    private final int REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(!hasPermissions())
            ActivityCompat.requestPermissions(this,permissions,REQUEST_CODE);
        status=findViewById(R.id.status);
        send=findViewById(R.id.send_btn);
        stop=findViewById(R.id.stop_btn);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connectedPeers.size()==0)
                    toast("No Connected Peers!");
                for(String s:connectedPeers)
                    sendPayload(s);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
                status.setText("Stopped");
            }
        });

    }

    private void stop() {
        Nearby.getConnectionsClient(getApplicationContext()).stopAllEndpoints();
        Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
        Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
    }

    private boolean hasPermissions() {
        //ArrayList<String> reqPermissions= new ArrayList<>();
        for(String permission:permissions) {
            if (ContextCompat.checkSelfPermission(this,
                    permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==REQUEST_CODE){
            if (grantResults.length > 0
                    && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //toast("Permissions Denied");
                finish();
                //onDestroy();
            }
        }
    }

    public void startDiscovering(View v) {
        Nearby.getConnectionsClient(this).stopAdvertising();
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(strategy).build();
        Nearby.getConnectionsClient(this).startDiscovery(serviceID, endpointDiscoveryCallback, discoveryOptions);
        status.setText("Discovering");
    }

    public void startAdvertising(View v) {
        Nearby.getConnectionsClient(this).stopDiscovery();
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(strategy).build();
        Nearby.getConnectionsClient(this).startAdvertising(nickName,serviceID,connectionLifecycleCallback,advertisingOptions);
        status.setText("Advertising");
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endPointID, ConnectionInfo connectionInfo) {
            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endPointID, payloadCallback);
        }

        @Override
        public void onConnectionResult(String endPointID, ConnectionResolution connectionResolution) {

            switch (connectionResolution.getStatus().getStatusCode()){
                case ConnectionsStatusCodes.STATUS_OK:
                    //CONNECTED
                    status.setText("Connected to: "+endPointID);
                    connectedPeers.add(endPointID);
                    break;

                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    //CONNECTION REJECTED
                    toast("Connection Rejected");
                    break;

                case ConnectionsStatusCodes.STATUS_ERROR:
                    //CONNECTION ERROR
                    toast("Connection ERROR");
                    break;

                default:
            }
        }

        @Override
        public void onDisconnected(String endPointID) {
            status.setText(endPointID+"-disconnected");
            connectedPeers.remove(endPointID);
        }
    };

    private void sendPayload(final String endPointID) {
        String message = "Hello There!";
        Payload mPayload = Payload.fromBytes(message.getBytes());
        Nearby.getConnectionsClient(this).sendPayload(endPointID,mPayload).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //toast("Message Sent");
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endPointID, DiscoveredEndpointInfo discoveredEndpointInfo) {
            if(connectedPeers.contains(endPointID)) return;
            toast(endPointID+" found");
            Nearby.getConnectionsClient(getApplicationContext()).requestConnection(nickName, endPointID,connectionLifecycleCallback);
        }

        @Override
        public void onEndpointLost(String endPointID) {
            //Disconnected
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(final String endPointID, Payload payload) {
            final byte[] receivedMsg = payload.asBytes();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast(endPointID + "-" +new String(receivedMsg));
                }
            });
        }

        @Override
        public void onPayloadTransferUpdate(String endPointID, PayloadTransferUpdate payloadTransferUpdate) {
            if(payloadTransferUpdate.getStatus()==PayloadTransferUpdate.Status.SUCCESS){
                //toast("Message Sent to: "+ endPointID);
            }
        }
    };

    @Override
    protected void onDestroy() {
        stop();
        super.onDestroy();
    }
}
