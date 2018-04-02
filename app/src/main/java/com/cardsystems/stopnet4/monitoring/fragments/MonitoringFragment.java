package com.cardsystems.stopnet4.monitoring.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MonitoringFragment extends Fragment{
    /*private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;*/

    public static final int PROGRESSBAR_CHANGE = 1;

    public static final String MONITORING_FRAGMENT_DATA = "MONITORING_FRAGMENT_DATA";

    private boolean autoScrollChacked = true;

    private int maxEventCount;

    private int eventCount;

    private DataReceiver dataReceiver;

    // for communication with Activity
    private OnSendToActivityListener mListener;

    private Context monitoringContext;

    // for showing info and foto of person
    private LinearLayout monitoringContent;

    private NestedScrollView monitoringScrollWidget;

    private Handler handler;

    private Thread backThread;

    private CheckingTask chckTask;

    static long oldTime;

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
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/

        dataReceiver = new DataReceiver();
        IntentFilter intentFilter = new IntentFilter(MONITORING_FRAGMENT_DATA);
        getActivity().registerReceiver(dataReceiver, intentFilter);

        handler = new SafeHandler(this);
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

                        if(eventCount > maxEventCount) {
                            monitoringContent.removeAllViews();
                            eventCount = 1;
                        }

                        monitoringContent.addView(imVie);
                        monitoringContent.addView(edTxt);
                        eventCount++;

                        long now = System.currentTimeMillis();
                        Toast.makeText(monitoringContext, (now-oldTime)/1000.0+" сек.", Toast.LENGTH_SHORT)
                                .show();
                        oldTime = now;

                        if(autoScrollChacked) {
                            monitoringScrollWidget.fullScroll(View.FOCUS_DOWN);
                            monitoringScrollWidget.fullScroll(View.FOCUS_DOWN);
                        }
                    }
                } catch (Exception e) {
                    Log.e("MonitFrag", "err:", e);
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
            case CheckingTask.NOROUTETOHOST_EXEPTION: {
                Toast.makeText(monitoringContext, "Server is not online!", Toast.LENGTH_SHORT)
                        .show();
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
        monitoringScrollWidget = getView().findViewById(R.id.monitoring_scroll_widget);
    }


    @Override
    public void onStart() {
        super.onStart();
        //start background task
        chckTask = new CheckingTask(handler);
        chckTask.pause();

        backThread = new Thread(chckTask);
        backThread.start();
    }

    @Override
    public void onResume() {
        super.onResume();

        Toast.makeText(monitoringContext, "Resumed", Toast.LENGTH_SHORT)
                .show();

        eventCount = 1;

        //check and update settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        String ipAddress = sharedPref.getString("ip_address", getString(R.string.default_ip));
        int port, timeout;
        try {
            maxEventCount = Integer.parseInt(sharedPref.getString("max_event_count", getString(R.string.default_max_event_count)));
            port = Integer.parseInt(sharedPref.getString("port", getString(R.string.default_port)));
            timeout = Integer.parseInt(sharedPref.getString("query_timeout", getString(R.string.default_query_timeout)));
        }catch (Exception e){
            port = timeout = 0;
           Log.e("parseInt", "error", e);
        }

                    String queryPath = sharedPref.getString("query_path", "/sdcard/query.sql");
                    try {
                        Date c = Calendar.getInstance().getTime();
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        String formattedDate = df.format(c);

                        QueryContainer.query = "query "
                                + QueryContainer.getStringFromFile(queryPath)
                                    .replace("yyyy-mm-dd", formattedDate)
                                + "!@e\n";

                    }catch (Exception e){
                        Toast.makeText(monitoringContext, e.getMessage(), Toast.LENGTH_LONG)
                                .show();
                        QueryContainer.query = QueryContainer.LAST_EVENT;
                    }

        chckTask.updateConnectionSettings(ipAddress, port, timeout);

        //resume background task
        chckTask.resume();

        //send to activity to change status of progress bar
        sendToActivity(PROGRESSBAR_CHANGE);
    }

    @Override
    public void onPause() {
        Toast.makeText(monitoringContext, "Paused", Toast.LENGTH_SHORT)
                .show();

        //pause background task
        chckTask.pause();

        //send to activity to change status of progress bar
        sendToActivity(PROGRESSBAR_CHANGE);

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

    private void sendToActivity(int what) {
        if (mListener != null) {
            mListener.onSendToActivityFromMonitoringFragment(what);
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
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        mListener = null;
        dataReceiver = null;
        super.onDetach();
    }

    public interface OnSendToActivityListener {
        void onSendToActivityFromMonitoringFragment(int what);
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

    private class DataReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            int status = intent.getIntExtra(Monitoring.AUTOSCROLL_CHANGED, -1);

            if(status == 1) {
                autoScrollChacked = true;
                monitoringScrollWidget.fullScroll(View.FOCUS_DOWN);
            }
            else
                autoScrollChacked = false;
        }
    }
}