package com.example.ipollusen;  //Declares the package name (namespace) for this file.//


import android.os.Bundle;     //Imports necessary Android classes for UI, logging, fragments, and ViewModel.//
//(Button, EditText, TextView are imported but not used in this file.)//
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;



import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import info.mqtt.android.service.MqttAndroidClient;
//Imports MQTT client classes, which are used for MQTT protocol communication (not used yet in this code).//

public class AdvancedViewFragment extends Fragment {   // Defines a class AdvancedViewFragment that extends Android's Fragment (a reusable UI component).




    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advanced_view, container, false);



        return view;
    }
// Overrides the onCreateView method, which is called when this fragment's UI is created.//
    //Inflates (creates) the UI from fragment_advanced_view.xml and returns it.//

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

}
//Overrides onDestroy, which is called when the fragment is destroyed. (No additional logic is added.)//