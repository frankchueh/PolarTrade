package com.example.project_ver1;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.location.Location;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

// google service library 引用
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;

public class MapLocate extends ActionBarActivity implements ConnectionCallbacks,
OnConnectionFailedListener,
LocationListener,
OnMapReadyCallback {
	
	// 地圖物件
		private GoogleMap polarMap;  
		// Google API 用戶端物件
		private GoogleApiClient mGoogleApiClient;
		// location request 物件
		private LocationRequest locationRequest;
		// 目前座標位置
		private Location currentLocation;
		// 上傳座標按鈕物件
		private Button refreshButton;
		// 處理上傳 message 結果
		public Handler MessageHandler;
		// 上傳座標字串
		private String p_msg;
		// 搜尋結果
		private String [] search_result;
		// 第一次攝影機矯正
		private int first_track;
		// 建立 Google API 用戶端物件
		private synchronized void configGoogleApiClient() {
			mGoogleApiClient = new GoogleApiClient.Builder(this).
								   addConnectionCallbacks(this).
								   addOnConnectionFailedListener(this).
								   addApi(LocationServices.API).
								   build();
		}
		
		// 建立 location 請求物件
		private void configLocationRequest() {
			locationRequest = LocationRequest.create().
						      setInterval(5000).
						      setFastestInterval(1000).
						      setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map_locate);
		
		// 取得 Button 物件 -> 做連結
		refreshButton = (Button)findViewById(R.id.refreshButton);
		// 取得 Google API 用戶端物件
		configGoogleApiClient();
		// 取得 Location request 物件
		configLocationRequest();
		// 建立並取得 message handler 物件
		first_track = 0;
		MessageHandler = new Handler() {
					
			public void handleMessage(Message msg) {
			// 根據不同 message 發出不同通知
				switch(msg.what) {
						
					case SendToServer.GET_SEARCH_RESULT: 
						search_result = msg.obj.toString().split("\n");
						for(String tem:search_result)
						{
							setUsrsLocate(tem.split(":")[0] , Double.parseDouble(tem.split(":")[1]) , Double.parseDouble(tem.split(":")[2]));
						}
						Toast.makeText(getApplicationContext(),"get search result!!", Toast.LENGTH_SHORT).show();
						break;
					case SendToServer.NO_SEARCH_RESULT:
						Toast.makeText(getApplicationContext(),"no result!!", Toast.LENGTH_SHORT).show();
						break;		
					case SendToServer.FAIL:
						Toast.makeText(getApplicationContext(), "Search product failed", Toast.LENGTH_SHORT).show();
						break;
				}
				super.handleMessage(msg);
			}
		};
	}
	
	// 地圖物件設置
		private void setUpMapIfNeeded() {

			if(polarMap == null) {
				polarMap = ((SupportMapFragment) getSupportFragmentManager().
						findFragmentById(R.id.polarMap)).getMap();
			}
		}
		
		
		// App 執行生命階段 -> 不同階段執行不同動作 -> 省電
		@Override
		protected void onResume() {
			super.onResume();
			setUpMapIfNeeded();
			polarMap.setMyLocationEnabled(true);
			mGoogleApiClient.connect();
		}
		
		@Override
		protected void onPause() {
			super.onPause();
			polarMap.setMyLocationEnabled(false);
			if(!mGoogleApiClient.isConnected()) {
				mGoogleApiClient.connect();
			}
		}
		
		@Override
		protected void onStop() {
			super.onStop();
			polarMap.setMyLocationEnabled(false);
			if(mGoogleApiClient.isConnected()) {
				mGoogleApiClient.disconnect();
			}
		}
		
		// Camera tracking
		
	    private void cameraTracking(double latitude , double longitude) {
	    	CameraPosition camPosition = new CameraPosition.Builder().
	    								 target(new LatLng(latitude,longitude)).
	    								 zoom(18).
	    								 build();
	    	polarMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPosition));
	    }
		
		// implementation of onMapReadyCallback
		
		@Override
		public void onMapReady(GoogleMap polarMap) {
			
			//polarMap.setMyLocationEnabled(true);	
		}
		
		// implementation of LocationListner
		
		@Override
		public void onLocationChanged(Location updateLocation) {
			// 顯示目前座標位置
			currentLocation = updateLocation;  // 取得目前座標位置
			if(first_track == 0) {
				cameraTracking(updateLocation.getLatitude(),updateLocation.getLongitude());
				first_track = 1;
			}
		}
		
		// implementation of ConnectionCallback 
		
		@Override
		public void onConnected(Bundle connectBundle) {
			
			// 已連線到 Google Service
			// 啟動位置更新服務
			// 當位置座標更新時，App 會自動呼叫 LocationListner.onLocationChanged
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, 
																	 locationRequest, 
																	 this);
		}
		
		public void getUserAround(View view) {
			 
			 p_msg = "";
			 p_msg += "getNearUser\n" + 
					   mainActivity.Account + "\n" + 
					   currentLocation.getLatitude() + "\n" + 
					   currentLocation.getLongitude();
		     new SendToServer(SendToServer.MessagePort, p_msg, MessageHandler, 
		     SendToServer.GET_AROUND_USER).start();	
		}
		
		public void setUsrsLocate(String usr_name , double lat , double lng) {
			
			Marker newMarker;
			
			MarkerOptions markerOpt = new MarkerOptions().title(usr_name); // 再加上新的marker
	    	markerOpt.position(new LatLng(lat,lng));
	    	markerOpt.title("present locate");
	    	newMarker = polarMap.addMarker(markerOpt);
		}
		 
		@Override
	    public void onConnectionSuspended(int cause) {
	        // Do nothing
	    }
		
		// implementation of onConnectionFailerListener
		
		 @Override
		 public void onConnectionFailed(ConnectionResult result) {
		     
			 // Google Services 連線失敗
			 int errorCode = result.getErrorCode();   // 取得連線失敗資訊
			 
			 // 裝置沒有 Google Play Service
			 if(errorCode == result.SERVICE_MISSING) {
				 Toast.makeText(this, "Service missing", Toast.LENGTH_LONG).show();
			 }
		 }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map_locate, menu);
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
