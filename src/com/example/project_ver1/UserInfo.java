package com.example.project_ver1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class UserInfo extends ActionBarActivity {

	Handler MessageHandler;
	Button btnUserinfoEdit, btnUserinfoBack;
	TextView txtUserAccount , txtUserName , txtUserAge , txtUserBirth , txtUserGender , txtUserMobile , txtUserEmail;
	ImageView imgUserinfoPhoto;
	String photoPath = ""; // 另外接收photopath,傳到server來取得照片
	String [] user_info = null;
	byte[] mPhoto;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.userinfo);

		txtUserAccount = (TextView) this.findViewById(R.id.txtUserAccount);
		txtUserName = (TextView) this.findViewById(R.id.txtUserName);
		txtUserAge = (TextView) this.findViewById(R.id.txtUserAge);
		txtUserBirth = (TextView) this.findViewById(R.id.txtUserBirth);
		txtUserGender = (TextView) this.findViewById(R.id.txtUserGender);
		txtUserMobile = (TextView) this.findViewById(R.id.txtUserMobile);
		txtUserEmail = (TextView) this.findViewById(R.id.txtUserEmail);
		imgUserinfoPhoto = (ImageView) this.findViewById(R.id.imgUIMPhoto);
		btnUserinfoEdit = (Button) this.findViewById(R.id.btnUserinfoEdit);
		btnUserinfoBack = (Button) this.findViewById(R.id.btnUIMComfirm);

		btnUserinfoBack.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});

		btnUserinfoEdit.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(UserInfo.this, UserinfoManager.class);
				startActivity(it);
			}
		});

		MessageHandler = new Handler() {

			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.SUCCESS_GET_USERINFO:
					// Toast.makeText(getApplicationContext(), "",
					// Toast.LENGTH_SHORT).show();
					user_info = msg.obj.toString().split("\n");
					//txtUserinfo.setText(msg.obj.toString());
					//photoPath = msg.obj.toString().split("\n")[2]; // 2為photopath的位置
					txtUserAccount.setText(user_info[0]);
					txtUserName.setText(user_info[1]);
					txtUserAge.setText(user_info[3]);
					txtUserBirth.setText(user_info[4]);
					txtUserGender.setText(user_info[5]);
					txtUserMobile.setText(user_info[6]);
					txtUserEmail.setText(user_info[7]);
					photoPath = user_info[2];
					String msg_getphoto = "GetPhoto\n" + photoPath;
					new SendToServer(SendToServer.PhotoPort, msg_getphoto,
							MessageHandler, SendToServer.GET_PHOTO).start(); // 傳到server並抓取圖片
					break;
				case SendToServer.SUCCESS_GET_PHOTO:
					mPhoto = (byte[]) msg.obj;
					Bitmap bm = BitmapFactory.decodeByteArray(mPhoto, 0,
							mPhoto.length, null);
					imgUserinfoPhoto.setImageBitmap(bm);
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(), "Get Info Fail",
							Toast.LENGTH_SHORT).show();
					break;

				case SendToServer.SERVER_ERROR:
					Toast.makeText(getApplicationContext(),
							"Server not response", Toast.LENGTH_SHORT).show();
					break;
				}
				super.handleMessage(msg);
			}
		};

	}

	public void getUserInfo(String account) {
		String msg = "GetUserInfo" + "\n" + account;
		new SendToServer(SendToServer.MessagePort, msg, MessageHandler,
				SendToServer.GET_USER_INFO).start();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		
		getUserInfo(mainActivity.Account);
		super.onResume();
	}
	
	
}
