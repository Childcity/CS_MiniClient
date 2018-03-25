package com.cardsystems.stopnet4.monitoring.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cardsystems.stopnet4.monitoring.CheckingTask;
import com.cardsystems.stopnet4.monitoring.EventData;
import com.cardsystems.stopnet4.monitoring.Monitoring;
import com.cardsystems.stopnet4.monitoring.QueryContainer;
import com.cardsystems.stopnet4.monitoring.R;

import java.lang.ref.WeakReference;

public class MonitoringFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    // for communication with Activity
    private OnSendToActivityListener mListener;

    private Context monitoringContext;

    // for showing info and foto of person
    private LinearLayout monitoringContent;

    private Handler h;

    private Thread t;

    private CheckingTask chckTask;

    public MonitoringFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentMonitoring.
     */

    public static MonitoringFragment newInstance(String param1, String param2) {
        MonitoringFragment fragment = new MonitoringFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        h = new SafeHandler(this);
    }

    public void onMessageReceive(Message msg){
        switch (msg.what) {
            case CheckingTask.UPDATE_INFO: {
                try {
                    ImageView imVie = new ImageView(getContext());
                    EditText edTxt = new EditText(getContext());

                    if (!(msg.obj == null || imVie == null || edTxt == null)) {
                        imVie.setImageBitmap(((EventData) msg.obj).getBmp());
                        edTxt.setText(((EventData) msg.obj).getInfo());
                        monitoringContent.addView(imVie);
                        monitoringContent.addView(edTxt);
                    }
                } catch (Exception e) {
                    Log.e("HANDLER", "err:", e);
                }
                break;
            }
            case CheckingTask.TIMEOUT_EXEPTION: {
                Toast.makeText(monitoringContext, "Timeout expired. Reconnecting to server...", Toast.LENGTH_SHORT)
                        .show();
                break;
            }
            case CheckingTask.SENDING_INFO: {
                Toast.makeText(monitoringContext, "Sending query...", Toast.LENGTH_SHORT)
                        .show();
                break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitoring, container, false);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        monitoringContent = getView().findViewById(R.id.monitoring_lin_lay);
    }


    @Override
    public void onStart() {
        super.onStart();
        //start background task
        chckTask = new CheckingTask(h);
        chckTask.pause();

        t = new Thread(chckTask);
        t.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        Toast.makeText(monitoringContext, "Resumed", Toast.LENGTH_SHORT)
                .show();

        //resume background task

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        String ipAddress = sharedPref.getString("ip_address", getString(R.string.default_ip));
        int port, timeout;
        try {
            port = Integer.parseInt(sharedPref.getString("port", getString(R.string.default_port)));
            timeout = Integer.parseInt(sharedPref.getString("query_timeout", getString(R.string.default_query_timeout)));
        }catch (Exception e){
            port = timeout = 0;
           Log.e("parseInt", "error", e);
        }

                    String queryPath = sharedPref.getString("query_path", "/sdcard/query.sql");
                    try {
                        QueryContainer.query = "query " + QueryContainer.getStringFromFile(queryPath) + "!@e\n";
                    }catch (Exception e){
                        Toast.makeText(monitoringContext, e.getMessage(), Toast.LENGTH_LONG)
                                .show();
                        QueryContainer.query = QueryContainer.LAST_EVENT;
                    }

        chckTask.updateConnectionSettings(ipAddress, port, timeout);
        chckTask.resume();
    }

    @Override
    public void onPause() {
        Toast.makeText(monitoringContext, "Paused", Toast.LENGTH_SHORT)
                .show();

        //pause background task
        chckTask.pause();

        super.onPause();
    }

    @Override
    public void onStop() {

        //stop background task
        chckTask.stop();

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Toast.makeText(getContext(), monitoringContent.getChildCount(), Toast.LENGTH_SHORT)
        //        .show();
    }

    private void sendToActivity(Bitmap image, String info) {
        if (mListener != null) {
            mListener.onSendToActivity(image, info);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnSendToActivityListener) {
            mListener = (OnSendToActivityListener) context;
            monitoringContext = context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public  void onDestroy() {
        if (h != null) {
            h.removeCallbacksAndMessages(null);
            h = null;

            super.onDestroy();
        }
    }

    @Override
    public void onDetach() {
        mListener = null;

        super.onDetach();
    }

    public interface OnSendToActivityListener {
        void onSendToActivity(Bitmap image, String info);
    }

    static class SafeHandler extends Handler {

        WeakReference<MonitoringFragment> wrFragment;

        SafeHandler(MonitoringFragment fragment) {
            wrFragment = new WeakReference<MonitoringFragment>(fragment);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            MonitoringFragment fragment = wrFragment.get();
            if (fragment != null) {
                fragment.onMessageReceive(msg);
            }
        }
    }
}
