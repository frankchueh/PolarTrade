package com.example.project_ver1;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;


public class ProductManage extends Activity {
	
	private ListView productView;
	ProductAdapter productAdapter;
	
	Product [] product_set;
	int [] pid_set;    // 使用者所有商品ID
	String [] pName_set;	// 使用者所有商品名稱
	int [] pPrice_set;   // 使用者所有商品價格
	String[] pPhotoPath_set;   // 使用者所有商品照片路徑
	String[] pInfoPath_set;   // 使用者所有商品資訊路徑
	byte[][] pPhoto_set;   // 使用者所有商品照片
	
	String p_msg = "";  // 傳送 message
	
	int p_num = 0;  // 總商品數量
	int p_allinfo_count = 0;   // 用來儲存商品所有資訊的 index
	int p_photo_count = 0;   // 用來儲存商品照片的 index
	
	public static  String address = "140.118.125.229";
	public static int port = 3838;
	Handler MessageHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_product_manage);
		
		productView = (ListView)findViewById(R.id.productlistview);
						
		MessageHandler = new Handler() {

			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.SUCCESS_GET_PID:     // 成功取得使用者所有 pids
					String [] temp_pid_set = (String[]) msg.obj;
					p_num = temp_pid_set.length;   // 商品 ID 數量 = 使用者總商品數量
					getAllProductsInfo(temp_pid_set);
					Toast.makeText(getApplicationContext(), "Product ID download success", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.SUCCESS_GET_PRODUCTINFO:    // 成功取得 pid 對應的所有資訊 
					String [] temp_pinfo_set = (String[]) msg.obj;
					getProductPhoto(temp_pinfo_set);
					break;
				case SendToServer.SUCCESS_GET_PHOTO:   // 成功取得 pid 的商品照片
					pPhoto_set[p_photo_count] = (byte[]) msg.obj;  // 取得所有照片
					p_photo_count++;
					if(p_photo_count == p_num) {   // 資料全部傳輸完成後 -> 總儲存
						saveAllProductMessage();
						Toast.makeText(getApplicationContext(), "All product download success", Toast.LENGTH_SHORT).show();
						setListView();
						break;
					}	
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(),"Product download failed", Toast.LENGTH_SHORT).show();
				}
				super.handleMessage(msg);
			}
		};
		// 送出商品下載 thread
		p_msg = "getUserProduct" + "\n" + mainActivity.Account;
		new SendToServer(address,port,p_msg,MessageHandler,SendToServer.GET_USER_PRODUCT).start();	
	}
	
	protected void getAllProductsInfo(String [] str) {
		
		pid_set = new int [p_num];
		pName_set = new String [p_num];
		pPrice_set = new int [p_num];
		pPhotoPath_set = new String [p_num];
		pInfoPath_set = new String [p_num];
		pPhoto_set = new byte[p_num][];
		product_set = new Product [p_num];
		
		for(int i = 0; i < p_num; i++) {
			pid_set[i] = Integer.parseInt(str[i]);   // 將所有 pid 存起來
			p_msg = "getProductInfo" + "\n" + str[i];
			new SendToServer(address,port,p_msg,MessageHandler,SendToServer.GET_PRODUCT_INFO).start();
		}
	}
	
	protected void getProductPhoto(String [] str) {
		
		pName_set[p_allinfo_count] = str[0];     // 將特定 pid 的所有資訊存起來
		pPrice_set[p_allinfo_count] = Integer.parseInt(str[1]);
		pPhotoPath_set[p_allinfo_count] = str[2];
		pInfoPath_set[p_allinfo_count] = str[3];
		p_msg = "GetPhoto" + "\n" + pPhotoPath_set[p_allinfo_count++];  // 去取得其圖片
		new SendToServer(address,port,p_msg,MessageHandler,SendToServer.GET_PHOTO).start();
	}
	
	protected void saveAllProductMessage() {   // 儲存所有商品相關訊息
		
		product_set = new Product [p_num];
		for(int i = 0; i < p_num; i++) {
			
			product_set[i] = new Product(pid_set[i],pName_set[i],pPrice_set[i],pInfoPath_set[i],pPhoto_set[i]);
			//Toast.makeText(getApplicationContext(),pInfoPath_set[i], Toast.LENGTH_SHORT).show();
		}
	}
	
	protected void setListView() {
		
		productAdapter = new ProductAdapter(this);
		productView.setAdapter(productAdapter);
		productView.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				
			}
		});
			
	}
	
	class ProductAdapter extends BaseAdapter {

		LayoutInflater myInflater;

		public ProductAdapter(ProductManage listViewActivity) {
			myInflater = LayoutInflater.from(listViewActivity);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			
			return product_set.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			
			return product_set[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			convertView = myInflater.inflate(R.layout.productitem, null);
			ImageView imgProduct = (ImageView) convertView
					.findViewById(R.id.productPhoto);
			TextView txtProductName = (TextView) convertView
					.findViewById(R.id.productName);
			TextView txtProductPrice = (TextView) convertView
					.findViewById(R.id.productPrice);
			byte [] pPhoto = product_set[position].productPhoto;
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int t_width = dm.widthPixels/3;
			int t_height = dm.heightPixels/4;
			Bitmap bm = BitmapFactory.decodeByteArray(pPhoto, 0,
					pPhoto.length, null);
			
			imgProduct.setMinimumHeight(t_height);;
			imgProduct.setMinimumWidth(t_width);;
			imgProduct.setImageBitmap(bm);
			txtProductName.setText(product_set[position].productName);
			txtProductPrice.setText(String.valueOf(product_set[position].productPrice));
			return convertView;
		}
	}

	
}

