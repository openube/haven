
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package info.guardianproject.phoneypot.service;


import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;

import java.util.Date;

import info.guardianproject.phoneypot.MonitorActivity;
import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.PreferenceManager;

@SuppressLint("HandlerLeak")
public class MonitorService extends Service {

	/**
	 * To show a notification on service start
	 */
	private NotificationManager manager;
		
	/**
	 * Acceleration detected message
	 */
	public static final int ACCELEROMETER_MESSAGE = 0;
	
	/**
	 * Camera motion detected message
	 */
	public static final int CAMERA_MESSAGE = 1;
	
	/**
	 * Mic noise detected message
	 */
	public static final int MICROPHONE_MESSAGE = 2;

	/**
	* True only if service has been alerted by the accelerometer
	*/
	private boolean already_alerted;
	
	/**
	 * Object used to retrieve shared preferences
	 */
	private PreferenceManager prefs = null;


	/**
	 * Incrementing alert id
	 */
	int mNotificationAlertId = 7007;

    /**
     * Sensor Monitors
     */
    AccelerometerMonitor mAccelManager = null;
    MicrophoneMonitor mMicMonitor = null;

	/**
	 * Handler for incoming messages
	 */
	class MessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			alert(msg.what);
		}
	}
		
	/**
	 * Messenger interface used by clients to interact
	 */
	private final Messenger messenger = new Messenger(new MessageHandler());

    /*
    ** Helps keep the service awake when screen is off
     */
    PowerManager.WakeLock wakeLock;

	/**
	 * Called on service creation, sends a notification
	 */
    @Override
    public void onCreate() {
        manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        prefs = new PreferenceManager(this);

        startSensors();

        showNotification();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
    }
    
    /**
     * Called on service destroy, cancels persistent notification
     * and shows a toast
     */
    @Override
    public void onDestroy() {

        wakeLock.release();
        stopSensors();
		stopForeground(true);


    }
	
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
    
    /**
     * Show a notification while this service is running.
     */
    @SuppressWarnings("deprecation")
	private void showNotification() {

    	Intent toLaunch = new Intent(getApplicationContext(),
    	                                          MonitorActivity.class);

        toLaunch.setAction(Intent.ACTION_MAIN);
        toLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
        toLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        toLaunch,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.secure_service_started);

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_phone_alert)
						.setContentTitle(getString(R.string.app_name))
						.setContentText(text);

		mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setWhen(System.currentTimeMillis());

		startForeground(1, mBuilder.build());

    }

    private void startSensors ()
    {
        mAccelManager = new AccelerometerMonitor(this);
        mMicMonitor = new MicrophoneMonitor(this);

    }

    private void stopSensors ()
    {
        mAccelManager.stop(this);
        mMicMonitor.stop(this);
    }

    private int mLastAlert = -1;

    /**
    * Sends an alert according to type of connectivity
    */
    private synchronized void alert(int alertType) {

        if (alertType == mLastAlert)
            return;

        StringBuffer alertMessage = new StringBuffer();

        alertMessage.append(getString(R.string.intrusion_detected));

        switch (alertType)
        {
            case MonitorService.ACCELEROMETER_MESSAGE:
                alertMessage.append(": Device was moved!");
                break;
            case MonitorService.MICROPHONE_MESSAGE:
                alertMessage.append(": Noise detected!");
                break;
            case MonitorService.CAMERA_MESSAGE:
                alertMessage.append(": Camera motion detected!");
                break;
        }

        alertMessage.append(" @ " + new Date().toLocaleString());

		/*
		 * If SMS mode is on we send an SMS alert to the specified 
		 * number
		 */
		if (prefs.getSmsActivation()) {
			//get the manager
			SmsManager manager = SmsManager.getDefault();
			manager.sendTextMessage(prefs.getSmsNumber(), null, alertMessage.toString(), null, null);
			
		}



        mLastAlert = alertType;
    }

    /**
	private void showNotificationAlert (String message)
	{

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_phone_alert)
						.setContentTitle(getString(R.string.app_name))
						.setContentText(message);

		Intent resultIntent = new Intent(this, MonitorActivity.class);

// Because clicking the notification opens a new ("special") activity, there's
// no need to create an artificial back stack.
		PendingIntent resultPendingIntent =
				PendingIntent.getActivity(
						this,
						0,
						resultIntent,
						PendingIntent.FLAG_UPDATE_CURRENT
				);

		mBuilder.setContentIntent(resultPendingIntent);

		manager.notify(mNotificationAlertId++, mBuilder.build());


	}**/

}
