package com.cardsystems.stopnet4.monitoring;

import android.os.Handler;
import android.util.Log;

import android.os.Message;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;

public class CheckingTask implements Runnable {

    public static final int UPDATE_INFO = 1;
    public static final int SENDING_INFO = 2;
    public static final int TIMEOUT_EXEPTION = -1;
    public static final int NOROUTETOHOST_EXEPTION = -2;


    private String server_ip = ""; //your computer IP address
    private int server_port = -1;
    private int timeout = -1;

    private Handler handler;

    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();

    public CheckingTask(Handler handler){
        this.handler = handler;
    }

    @Override
    public void run () {
        Message msg;

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        while (running){
            synchronized (pauseLock) {
                if (!running) { // may have changed while waiting to synchronize on pauseLock
                    break;
                }
                if (paused) {
                    try {
                        pauseLock.wait(); // will cause this Thread to block until
                        // another thread calls pauseLock.notifyAll()
                        // Note that calling wait() will
                        // relinquish the synchronized lock that this
                        // thread holds on pauseLock so another thread
                        // can acquire the lock to call notifyAll()
                        // (link with explanation below this code)
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (!running) { // running might have changed since we paused
                        break;
                    }
                }
            }

            try {
                try {
                    //here you must put your computer's IP address.
                    InetAddress serverAddr = InetAddress.getByName(server_ip);

                    //Log.d("TCP", "C: Connecting... \"" + server_ip + ":" + server_port + "\"");

                    Socket socket = new Socket(serverAddr, server_port);

                    try {
                        BufferedReader mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                        if (mBufferIn != null && mBufferOut != null) {

                            String mServerMessage = mBufferIn.readLine();

                            if (mServerMessage.equals("ping OK")) {

                                //if (handler != null)
                                //    handler.sendEmptyMessage(SENDING_INFO);

                                if (! mBufferOut.checkError()) {
                                    mBufferOut.print(QueryContainer.query);
                                    mBufferOut.flush();
                                }

                                while ((!(mServerMessage = mBufferIn.readLine()).equals("!@e"))) {
                                    if (paused || (!running)) {
                                        break;
                                    }

                                    //Log.d("EVENT", String.valueOf(Thread.currentThread().getId()) + running + mServerMessage);
                                    if (handler != null) {
                                        msg = handler.obtainMessage(UPDATE_INFO, EventParser.getEventData(mServerMessage));
                                        handler.sendMessage(msg);
                                    }
                                }
                            }
                        }

                    } catch (Exception e) {
                        Log.e("TCP", "Error", e);
                    } finally {
                        //the socket must be closed. It is not possible to reconnect to this socket
                        // after it is closed, which means a new socket instance has to be created.
                        socket.close();
                    }
                }catch (ConnectException e){
                    if (handler != null)
                        handler.sendEmptyMessage(TIMEOUT_EXEPTION);
                    Thread.currentThread();
                    Thread.sleep(2000);
                }catch (NoRouteToHostException e){
                    if (handler != null)
                        handler.sendEmptyMessage(NOROUTETOHOST_EXEPTION);
                    Thread.currentThread();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Log.e("TCP", "SOCKET CONNECTION Error", e);
                    Thread.currentThread();
                    Thread.sleep(2000);
                }

                //Log.d("THREAD", String.valueOf(Thread.currentThread().getId()) + " sleeping...:");
                Thread.currentThread();
                Thread.sleep(timeout);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Log.e("THREAD", "error:", e);
            }
        }
    }

    public void stop() {
        running = false;
        // you might also want to interrupt() the Thread that is
        // running this Runnable, too, or perhaps call:
        resume();
        // to unblock
    }

    public void pause() {
        // you may want to throw an IllegalStateException if !running
        paused = true;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
    }

    public void updateConnectionSettings(String serverIp, int serverPort, int timeout){
        server_ip = serverIp;
        server_port = serverPort;
        this.timeout = timeout;
    }
}
