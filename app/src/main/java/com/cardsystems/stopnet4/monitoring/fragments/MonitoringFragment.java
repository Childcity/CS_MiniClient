package com.cardsystems.stopnet4.monitoring.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cardsystems.stopnet4.monitoring.CheckingTask;
import com.cardsystems.stopnet4.monitoring.EventData;
import com.cardsystems.stopnet4.monitoring.EventListAdapter;
import com.cardsystems.stopnet4.monitoring.Monitoring;
import com.cardsystems.stopnet4.monitoring.QueryContainer;
import com.cardsystems.stopnet4.monitoring.R;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MonitoringFragment extends Fragment{

    public static final int PROGRESSBAR_CHANGE = 1;

    public static final String MONITORING_FRAGMENT_DATA = "MONITORING_FRAGMENT_DATA";

    private boolean autoScrollChecked = true;

    private int maxEventCount;

    private DataReceiver dataReceiver;

    // for communication with Activity
    private OnSendToActivityListener mListener;

    private Context monitoringContext;

    // list of events, that consists of EventData
    private List<EventData> events;

    // adapter for list
    private EventListAdapter eventListAdapter;

    // layout (list)
    private RecyclerView recyclerViewEventsList;

    private Handler handler;

    private CheckingTask chckTask;

    static long oldTime;

    public MonitoringFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        autoScrollChecked = true;

        // initiate mechanism to get information from activity
        dataReceiver = new DataReceiver();
        IntentFilter intentFilter = new IntentFilter(MONITORING_FRAGMENT_DATA);
        getActivity().registerReceiver(dataReceiver, intentFilter);

        // create list of events
        events = new ArrayList<>();

        // create adapter
        eventListAdapter = new EventListAdapter(monitoringContext, events);

        // create handler for communicating with other threads
        handler = new SafeHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitoring, container, false);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
                recyclerViewEventsList = Objects.requireNonNull(getView()).findViewById(R.id.monitoring_list);
                // set adapter for list
                recyclerViewEventsList.setAdapter(eventListAdapter);
        } catch (NullPointerException e) {
            Log.e("onMonit ActivCreated", "NullPointerException", e);
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        //start background task
        chckTask = new CheckingTask(handler);
        chckTask.pause();

        Thread backThread = new Thread(chckTask);
        backThread.start();
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onResume() {
        super.onResume();

        /*Toast.makeText(monitoringContext, "Resumed", Toast.LENGTH_SHORT)
                .show();*/

        //check and update settings
        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... voids) {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(monitoringContext);
                String ipAddress = sharedPref.getString("ip_address", getString(R.string.default_ip));
                int port, timeout;
                try {
                    maxEventCount = Integer.parseInt(sharedPref.getString("max_event_count", getString(R.string.default_max_event_count)));
                    if (maxEventCount < 1) maxEventCount = 1;
                    port = Integer.parseInt(sharedPref.getString("port", getString(R.string.default_port)));
                    timeout = Integer.parseInt(sharedPref.getString("query_timeout", getString(R.string.default_query_timeout)));
                }catch (Exception e){
                    port = timeout = 0;
                    e.printStackTrace();
                }

                String queryPath = sharedPref.getString("query_path", "/sdca"+"rd/query.sql");
                try {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String formattedDate = df.format(Calendar.getInstance().getTime());

                    QueryContainer.query = "query "
                            + QueryContainer.getStringFromFile(queryPath)
                            .replace("yyyy-mm-dd", formattedDate)
                            + "!@e\n";

                }catch (Exception e){
                    /*Toast.makeText(monitoringContext, e.getMessage(), Toast.LENGTH_LONG)
                            .show();*/
                    QueryContainer.query = QueryContainer.LAST_EVENT;
                }

                chckTask.updateConnectionSettings(ipAddress, port, timeout);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                //resume background task
                chckTask.resume();
                //send to activity to change status of progress bar
                sendToActivity(PROGRESSBAR_CHANGE);
            }
        }.execute();
    }

    @Override
    public void onPause() {
        /*Toast.makeText(monitoringContext, "Paused", Toast.LENGTH_SHORT)
                .show();*/

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

        mListener = (OnSendToActivityListener) context;
        monitoringContext = context;
    }

    @Override
    public  void onDestroy() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        // unregister Receiver, that was registered in onCreate()
        getActivity().unregisterReceiver(dataReceiver);

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        mListener = null;
        dataReceiver = null;
        super.onDetach();
    }

    @SuppressLint("StaticFieldLeak")
    private class UpdateList extends AsyncTask<EventData, Void,Void> {
        @Override
        protected Void doInBackground(EventData... params) {
            // add new event to list
            events.add(params[0]);
            // if events more than user set in settings, remove first
            if (eventListAdapter.getItemCount() > maxEventCount)
                events.remove(0);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //notify adapter, that list was changed
            eventListAdapter.notifyDataSetChanged();
            // if user checked auto scroll, scrolling down list
            if (autoScrollChecked)
                recyclerViewEventsList.smoothScrollToPosition(eventListAdapter.getItemCount() - 1);

            /*long now = System.currentTimeMillis();
            Toast.makeText(monitoringContext, (now-oldTime)/1000.0+" сек.", Toast.LENGTH_SHORT)
                    .show();
            oldTime = now;*/
        }
    }

    public void onMessageReceive(final Message msg){
        switch (msg.what) {
            case CheckingTask.UPDATE_INFO: {
                if (msg.obj != null) {
                    new UpdateList().execute((EventData) msg.obj);
                }

            }break;
            case CheckingTask.TIMEOUT_EXEPTION: {
                Toast.makeText(monitoringContext, "Timeout expired. Reconnecting to server...", Toast.LENGTH_SHORT)
                        .show();

            } break;
            case CheckingTask.SENDING_INFO: {
                Toast.makeText(monitoringContext, "Sending query...", Toast.LENGTH_SHORT)
                        .show();
            } break;
            case CheckingTask.NOROUTETOHOST_EXEPTION: {
                Toast.makeText(monitoringContext, "Server is not online!", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    public interface OnSendToActivityListener {
        void onSendToActivityFromMonitoringFragment(int what);
    }

    static class SafeHandler extends Handler {

        WeakReference<MonitoringFragment> wrFragment;

        SafeHandler(MonitoringFragment fragment) {
            wrFragment = new WeakReference<>(fragment);
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

            try {
                int status = intent.getIntExtra(Monitoring.AUTOSCROLL_CHANGED, -1);

                if(status == 1) {
                    autoScrollChecked = true;
                    int evCount = eventListAdapter.getItemCount();
                    if(evCount > 1)
                        recyclerViewEventsList.smoothScrollToPosition(evCount - 1);
                }
                else
                    autoScrollChecked = false;
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}