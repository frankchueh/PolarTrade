package com.example.project_ver1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import org.apache.commons.lang.SerializationUtils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
	
	ArrayList <Product> product_set = new ArrayList <Product>();
	int [] pid_set;    // 使用者所有商品ID
	
	String p_msg = "";  // 傳送 message
	
	int p_num = 0;  // 總商品數量
	int p_allinfo_count = 0; 
	
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
					byte[] temp_p = (byte[]) msg.obj;
					product_set = (ArrayList<Product>) SerializationUtils.deserialize(temp_p);
					//p_num = temp_pid_set.length;   // 商品 ID 數量 = 使用者總商品數量
					//getAllProductsInfo(temp_pid_set);
					Toast.makeText(getApplicationContext(), "Product ID download success", Toast.LENGTH_SHORT).show();
					setListView();
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(),"Product download failed", Toast.LENGTH_SHORT).show();
				}
				super.handleMessage(msg);
			}
		};
		// 送出商品下載 thread
		p_msg = "getUserProduct" + "\n" + mainActivity.Account;
		new SendToServer(SendToServer.MessagePort, p_msg, MessageHandler, 
				SendToServer.GET_USER_PRODUCT).start();	
	}
	
	protected void setListView() {
		
		productAdapter = new ProductAdapter(this);
		productView.setAdapter(productAdapter);
		productView.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent it = new Intent();
				it.putExtra("product", product_set.get(position));
				it.setClass(ProductManage.this,ProductEdit.class);
				startActivity(it);	
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
			
			return product_set.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			
			return product_set.get(position);
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
			byte [] pPhoto = product_set.get(position).productPhoto;
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int t_width = dm.widthPixels/3;
			int t_height = dm.heightPixels/4;
			Bitmap bm = BitmapFactory.decodeByteArray(pPhoto, 0,
					pPhoto.length, null);
			
			imgProduct.setMinimumHeight(t_height);
			imgProduct.setMinimumWidth(t_width);
			imgProduct.setImageBitmap(getResizedBitmap(bm,100,100));
			txtProductName.setText(product_set.get(position).productName);
			txtProductPrice.setText(String.valueOf(product_set.get(position).productPrice));
			return convertView;
		}
	}
	
	public Bitmap getResizedBitmap(Bitmap bm , int new_width , int new_height) {
		
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) new_width) / width;
		float scaleHeight = ((float) new_height) / height;
		
		Matrix m = new Matrix();
		m.postScale(scaleWidth, scaleHeight);
		
		Bitmap resizedBitmap = Bitmap.createBitmap(bm,0,0,width,height,m,false);
		bm.recycle();
		
		return resizedBitmap;
	}
}

