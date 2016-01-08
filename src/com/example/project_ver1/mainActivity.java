package com.example.project_ver1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.view.Menu;
import android.view.MenuItem;

public class mainActivity extends ActionBarActivity {

	Button btnUserInfo, btnChatroomList, btnProductUpload, btnStartService,
			btnStartMapMode, btnProductManage, btnSearchProduct, btnShoppingCart;
	public static String Account; // ¨Ï¥ÎªÌ±b¤á
	private static final int STOP_SERVICE = 0 , START_SERVICE = 1;
	private int service_state = STOP_SERVICE;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		btnUserInfo = (Button) this.findViewById(R.id.btnUserInfo);
		btnChatroomList = (Button) this.findViewById(R.id.btnChatroomList);
		btnProductUpload = (Button) this.findViewById(R.id.btnProductUpload);
		btnStartService = (Button) this.findViewById(R.id.btnStartService);
		btnStartMapMode = (Button) this.findViewById(R.id.btnStartMap);
		btnProductManage = (Button) this.findViewById(R.id.btnProductManage);
		btnSearchProduct = (Button) this.findViewById(R.id.btnSearchProduct);
		btnShoppingCart = (Button) this.findViewById(R.id.btnShoppingCart);
		//Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
	    //setSupportActionBar(toolbar);
	    
		Intent con = getIntent();
		Account = con.getStringExtra("Account");
		
		// Start location upload service
		/*
		 * Intent intent = new Intent(GetPosition.START_UPLOAD); Bundle bundle =
		 * new Bundle(); bundle.putString("user Account", Account);
		 * intent.putExtras(bundle); startService(intent);
		 */
		
		Intent ChatNoti = new Intent(mainActivity.this, ChatRoomNoti.class);
		ChatNoti.putExtra("Account", Account);
		startService(ChatNoti);
		
		btnUserInfo.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(mainActivity.this, UserInfo.class);
				startActivity(it);
			}
		});

		btnChatroomList.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(mainActivity.this, ChatRoomList.class);
				startActivity(it);
			}
		});

		btnProductUpload.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(mainActivity.this, OnShelf.class);
				startActivity(it);
			}
		});

		btnStartService.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(service_state == STOP_SERVICE) {
					Intent intent = new Intent(mainActivity.this, GetPosition.class);
					Bundle bundle = new Bundle();
					bundle.putString("Account", Account);
					intent.putExtras(bundle);
					service_state = START_SERVICE;
					btnStartService.setBackgroundResource(R.drawable.location_on);
					startService(intent);
				}
				else if(service_state == START_SERVICE) {
					Intent intent = new Intent(mainActivity.this, GetPosition.class);
					btnStartService.setBackgroundResource(R.drawable.location_off);
					service_state = STOP_SERVICE;
					stopService(intent);
				}
			}
		});

		btnStartMapMode.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(mainActivity.this, MapLocate.class);
				startActivity(it);
			}
		});

		btnProductManage.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(mainActivity.this, ProductManage.class);
				startActivity(it);
			}
		});
		
		btnSearchProduct.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(mainActivity.this, SearchProduct.class);
				startActivity(it);
			}});
		
		btnShoppingCart.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(mainActivity.this, ShoppingCart.class);
				startActivity(it);
			}});
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.on_shelf, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
