package com.example.project_ver1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Chatroom extends Activity {

	Button btnChatSend;
	EditText editChatMsg;
	TextView txtChatData;
	int chatID = -1;
	Handler MessageHandler;
	HandlerThread GetHandler;
	Handler workHandler;
	int DownloadTime = 10000;

	boolean newOpen = true; // 讓OnResume知道是否為重新開啟

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.chatroom);

		btnChatSend = (Button) this.findViewById(R.id.btnChatSend);
		editChatMsg = (EditText) this.findViewById(R.id.editChatMsg);
		txtChatData = (TextView) this.findViewById(R.id.txtChatData);

		GetHandler = new HandlerThread("getmsg");
		GetHandler.start();
		workHandler = new Handler(GetHandler.getLooper());

		btnChatSend.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String sendMsg = editChatMsg.getText().toString();
				editChatMsg.setText(""); // 清空輸入框

				String command = chatID + "\n" + mainActivity.Account + "\n"
						+ sendMsg;
				new SendToServer(SendToServer.MessagePort, command,
						MessageHandler, SendToServer.UPDATE_MESSAGE).start();
			}
		});

		MessageHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				super.handleMessage(msg);
				switch (msg.what) {
				case SendToServer.GET_CHAT_ROOM:
					chatID = Integer.parseInt(msg.obj.toString());
					workHandler.post(DownloadMsg);
					break;
				case SendToServer.DOWNLOAD_MESSAGE:
					txtChatData.setText(msg.obj.toString());
					break;
				case SendToServer.UPDATE_MESSAGE_SUCCESS:
					workHandler.post(DownloadMsg);
					break;
				case SendToServer.SERVER_ERROR:
					Toast.makeText(getApplicationContext(), "Server Error",
							Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(), msg.obj.toString(),
							Toast.LENGTH_SHORT).show();
					break;
				}
			}
		};

	}

	// 下載訊息
	Runnable DownloadMsg = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String command = "DownloadMessage\n" + chatID + "\n"
					+ mainActivity.Account;
			new SendToServer(SendToServer.MessagePort, command, MessageHandler,
					SendToServer.DOWNLOAD_MESSAGE).start();
			workHandler.postDelayed(DownloadMsg, DownloadTime); // 持續跑下去
		}
	};

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		Intent call_it = getIntent();
		// 先抓看有沒有chatID
		chatID = call_it.getIntExtra("chatID", -1);
		// 如果沒有chatID
		
		if (chatID != -1) {
			workHandler.post(DownloadMsg);
			Toast.makeText(getApplicationContext(), "OnResume",
					Toast.LENGTH_SHORT).show();
		}
		else
		{	
			int PID = call_it.getIntExtra("produceID", -1);
			int SID = call_it.getIntExtra("sellerID", -1);
			if (PID == -1 || SID == -1) {
				Toast.makeText(getApplicationContext(), "Get Chatroom Error",
						Toast.LENGTH_SHORT).show();
			} else {
				String msg = "GetChatRoom\n" + PID + "\n" + SID + "\n"
						+ mainActivity.Account;
				new SendToServer(SendToServer.MessagePort, msg, MessageHandler,
						SendToServer.GET_CHAT_ROOM).start();
			}
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		workHandler.removeCallbacks(DownloadMsg);
		workHandler = null;
		super.onDestroy();
		Toast.makeText(getApplicationContext(), "OnDestory", Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		workHandler.removeCallbacks(DownloadMsg);
		super.onPause();
		Toast.makeText(getApplicationContext(), "OnPause", Toast.LENGTH_SHORT)
				.show();
	}
}
