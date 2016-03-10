package com.example.project_ver1;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public class ChatRoomNoti extends Service{
	
	Handler MessageHandler;
	private String Account;
	HandlerThread GetHandler;
	Handler workHandler;
	int DownloadTime = 60000;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@SuppressLint("NewApi") @Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(getApplicationContext(), "OnCreate", Toast.LENGTH_SHORT).show();
		GetHandler = new HandlerThread("CheckMessage");
		GetHandler.start();
		workHandler = new Handler(GetHandler.getLooper());
		
		final int notifyID = 1; // �q�����ѧO���X
		final int requestCode = notifyID; // PendingIntent��Request Code
		final Intent intent = new Intent(ChatRoomNoti.this, ChatRoomList.class);
		final int flags = PendingIntent.FLAG_CANCEL_CURRENT; // ONE_SHOT�GPendingIntent�u�ϥΤ@���FCANCEL_CURRENT�GPendingIntent����e�|�����������e���FNO_CREATE�G�u�Υ��e��PendingIntent�A���إ߷s��PendingIntent�FUPDATE_CURRENT�G��s���ePendingIntent�ұa���B�~��ơA���~��u��
		final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), requestCode, intent, flags); // ���oPendingIntent
		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); // ���o�t�Ϊ��q���A��
		final Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // �q�����Ī�URI�A�b�o�̨ϥΨt�Τ��ت��q������
		final Notification notification = new Notification.Builder(getApplicationContext())
											.setSmallIcon(R.drawable.ic_launcher)
											.setContentTitle("�z���s�T��")
											.setContentText("���e��r")
											.setContentIntent(pendingIntent)
											.setAutoCancel(true)
											.setSound(soundUri)
											.setVibrate(new long[] { 100, 300, 200, 200})
											.setLights(Color.RED, 3000, 3000)
											.build(); // �إ߳q��
		
		MessageHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.GET_NEW_MESSAGE:
//					Toast.makeText(getApplicationContext(), "New Message", Toast.LENGTH_SHORT).show();
					notificationManager.notify(notifyID, notification); // �o�e�q��
					break;
				
				case SendToServer.NO_MESSAGE:	
//					Toast.makeText(getApplicationContext(), "No Message", Toast.LENGTH_SHORT).show();
					break;
					
				case SendToServer.SERVER_ERROR:
//					Toast.makeText(getApplicationContext(), "Server Error",
//							Toast.LENGTH_SHORT).show();
					break;
				}
				super.handleMessage(msg);
			}
		};
		
	}
	
	@Override
	public void onDestroy() {
		Toast.makeText(getApplicationContext(), "Server Destory", Toast.LENGTH_SHORT).show();
	}
	
	@Override 
	public int onStartCommand(Intent intent , int flags , int startId) {
		Toast.makeText(getApplicationContext(), "Service Start", Toast.LENGTH_SHORT).show();
		Account = intent.getStringExtra("Account");
		workHandler.post(checkNoti);	//����checkNoti
		return Service.START_REDELIVER_INTENT;
	}
	
	Runnable checkNoti = new Runnable()
	{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String command = "CheckMessage" + "\n" + Account ;
			new SendToServer(SendToServer.MessagePort, command,
					MessageHandler, SendToServer.CHECK_MESSAGE).start();
			workHandler.postDelayed(checkNoti, DownloadTime);	//������~��]
		}
	};
	
}
