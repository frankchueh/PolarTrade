package com.example.project_ver1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import org.apache.commons.lang.SerializationUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
	private File mpPhoto;
	private File ProductPhotoDir = new File(
			Environment.getExternalStorageDirectory() + "/DCIM/ProductPhoto");
	ArrayList <Product> product_set = new ArrayList <Product>();
	ArrayList <File> photo_set = new ArrayList <File>();
	FileOutputStream fos = null;
	Uri u = null;
	int [] pid_set;    // 使用者所有商品ID
	String p_msg = "";  // 傳送 message
	int p_num = 0;  // 總商品數量
	public static final int UPDATE_SUCCESS = 7001 , UPDATE_CANCEL = 7002;
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
//					product_set = (ArrayList<Product>) msg.obj;
					Toast.makeText(getApplicationContext(), "Products download success", Toast.LENGTH_SHORT).show();
					setListView();	// 設置畫面
					break;
				
				case SendToServer.NO_PRODUCTS:
					Toast.makeText(getApplicationContext(), "No product exist", Toast.LENGTH_SHORT).show();
					break;
					
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(),"Product download failed", Toast.LENGTH_SHORT).show();
					
					break;
				
				case SendToServer.DELETE_SUCCESS:
					int deleted_position = (Integer) msg.obj;
					product_set.remove(deleted_position);
					productAdapter.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(),"Product delete success", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.DELETE_FAIL:
					Toast.makeText(getApplicationContext(),"Product delete failed", Toast.LENGTH_SHORT).show();
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
				File pfile = null;
				Intent it = new Intent();
				Bundle bu = new Bundle();
				Bitmap bm = BitmapFactory.decodeByteArray(product_set.get(position).productPhoto, 0,
						product_set.get(position).productPhoto.length,null);
				try {
					pfile = savePhoto(bm ,product_set.get(position).productID);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bm.recycle();
				u = Uri.fromFile(pfile);
				bu.putString("user", "seller");
				bu.putInt("id", product_set.get(position).productID);
				bu.putString("name",product_set.get(position).productName);  // 傳遞商品名稱
				bu.putInt("price",product_set.get(position).productPrice);   // 商品價格
				bu.putByteArray("info", product_set.get(position).productInfo);  // 商品資訊
				bu.putString("photo",u.toString());   // 商品圖片超連結
				
				it.putExtras(bu);
				it.setClass(ProductManage.this,ProductInfo.class);
				startActivityForResult(it , position);	
			}
		});		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub

		if(resultCode == UPDATE_SUCCESS) {
			Bundle bu = data.getExtras();
			product_set.get(requestCode).productName = bu.getString("name");
			product_set.get(requestCode).productPrice = bu.getInt("price");
			product_set.get(requestCode).productInfo = bu.getByteArray("info");
			try {
				Uri orgUri = Uri.parse(bu.getString("photo"));
				ContentResolver contentResolver = getContentResolver();
				product_set.get(requestCode).productPhoto = readStream(contentResolver.openInputStream(orgUri));
				productAdapter.notifyDataSetChanged();
				} catch(Exception e) {
						e.printStackTrace();
				}
		}
		else if(resultCode == UPDATE_CANCEL) {
			
		}
	}
	
	static class ViewHolder {
		
		public TextView productName;
		public TextView productPrice;
		public ImageView productPhoto;
		public Button deleteProduct;
	}
	
	public class ProductAdapter extends BaseAdapter {

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
			ViewHolder pViewHolder = null;
			
			if(convertView == null) {
				
				pViewHolder = new ViewHolder(); 
				convertView = myInflater.inflate(R.layout.productitem,parent,false);
				pViewHolder.productPhoto = (ImageView) convertView.findViewById(R.id.productPhoto);
				pViewHolder.productName = (TextView) convertView.findViewById(R.id.productName);
				pViewHolder.productPrice = (TextView) convertView.findViewById(R.id.productPrice);
				pViewHolder.deleteProduct = (Button) convertView.findViewById(R.id.deletedProduct);
				
				DisplayMetrics dm = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(dm);
				int t_width = dm.widthPixels/3;
				int t_height = dm.heightPixels/4;
				pViewHolder.productPhoto.setMinimumHeight(t_height);
				pViewHolder.productPhoto.setMinimumWidth(t_width);
				
				pViewHolder.deleteProduct.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						int p = (Integer) v.getTag();
						p_msg = "deleteProduct" + "\n" + product_set.get(p).productID;
						new SendToServer(SendToServer.MessagePort, p_msg, MessageHandler, 
								SendToServer.DELETE_PRODUCT).start();	
					}
				});
				
				convertView.setTag(pViewHolder);
			}
			else {
				pViewHolder = (ViewHolder) convertView.getTag();
			}
			
			// fill data
			byte [] pPhoto = product_set.get(position).productPhoto;
			Bitmap bm = BitmapFactory.decodeByteArray(pPhoto, 0,
					pPhoto.length, null);
			pViewHolder.productPhoto.setImageBitmap(getResizedBitmap(bm,100,100));
			pViewHolder.productName.setText(product_set.get(position).productName);
			pViewHolder.productPrice.setText(String.valueOf(product_set.get(position).productPrice));
			pViewHolder.deleteProduct.setTag(position);
			
			return convertView;
		}
	}
	
	public Bitmap getResizedBitmap(Bitmap bm , int new_width , int new_height) {
		// 重新設定商品圖片大小
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) new_width) / width;    // 計算縮放大小
		float scaleHeight = ((float) new_height) / height;
		
		Matrix m = new Matrix();
		m.postScale(scaleWidth, scaleHeight);
		Bitmap resizedBitmap = Bitmap.createBitmap(bm,0,0,width,height,m,false);
		
		bm.recycle();
		
		return resizedBitmap;
	}
	
	public File savePhoto (Bitmap bm , int pid) throws IOException {	// 將照片存到內存路徑
		
		String localproductFileName = String.valueOf(pid) + ".jpg";
		ProductPhotoDir.mkdir();
		mpPhoto = new File(ProductPhotoDir, localproductFileName);
		
		try {
			fos = new FileOutputStream(mpPhoto);
			bm.compress(Bitmap.CompressFormat.JPEG,80,fos);
		}catch(Exception e) {
			e.printStackTrace();
		}
		fos.close();
		
		return mpPhoto;
	}
	
	public static byte[] readStream(InputStream in) throws Exception {
		byte[] buffer = new byte[1024];
		int len = -1;
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		while ((len = in.read(buffer)) != -1) {
			outStream.write(buffer, 0, len);
		}
		byte[] data = outStream.toByteArray();
		outStream.close();
		in.close();
		return data;
	}
	
}

