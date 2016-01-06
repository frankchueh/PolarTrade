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
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class ShoppingCart extends ActionBarActivity {
	
	private ListView resultView;
	ProductAdapter productAdapter;
	Button btnSearch;
	
	private File mpPhoto;
	private File ProductPhotoDir = new File(
			Environment.getExternalStorageDirectory() + "/DCIM/ProductPhoto");
	
	String savePath = Environment.getExternalStorageDirectory().getPath()+"/PolarTrade";
	FileManager save_product;
	
	ArrayList <Product> product_set = new ArrayList <Product>();
	ArrayList <File> photo_set = new ArrayList <File>();
	FileOutputStream fos = null;
	Uri u = null;

	double[] position;
	Handler MessageHandler;
	String command;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_shopping_cart);
		resultView = (ListView)findViewById(R.id.resultListView);
		save_product = new FileManager(savePath + "/SaveProduct.txt");

		MessageHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.SUCCESS:
					product_set = (ArrayList<Product>) SerializationUtils.deserialize((byte[])msg.obj);
					setListView();
					rewriteShoppingCart();
					Toast.makeText(getApplicationContext(), "Get Product Success", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(), "Search product failed", Toast.LENGTH_SHORT).show();
					break;
				}
				super.handleMessage(msg);
			}
		};
		
	}
	
	private void rewriteShoppingCart()	//刪除搜尋不到的商品
	{	
		String[] pid_new = new String[product_set.size()];
		for (int i = 0; i < product_set.size(); i++)
		{
			pid_new[i] = "" + product_set.get(i).productID;
		}
	}
	
	protected void setListView() {
		
		productAdapter = new ProductAdapter(this);
		resultView.setAdapter(productAdapter);
		resultView.setOnItemClickListener(new ListView.OnItemClickListener() {

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
				
				bu.putString("user", "buyer");
				bu.putInt("id", product_set.get(position).productID);
				bu.putString("name",product_set.get(position).productName);  // 傳遞商品名稱
				bu.putInt("price",product_set.get(position).productPrice);   // 商品價格
				bu.putByteArray("info", product_set.get(position).productInfo);  // 商品資訊
				bu.putString("photo",u.toString());   // 商品圖片超連結
				bu.putInt("sellerID", product_set.get(position).userID);
				
				it.putExtras(bu);
				it.setClass(ShoppingCart.this, ProductInfo.class);
				startActivityForResult(it , position);	
			}
		});		
	}
	
	static class ViewHolder {
		public TextView productName;
		public TextView productPrice;
		public ImageView productPhoto;
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		String[] pids = save_product.readAllLine();
		command = "getProduct\n";
		for(String pid:pids)
		{
			command += pid + ",";
		}
		
		new SendToServer(SendToServer.MessagePort, command,
				MessageHandler, SendToServer.GET_PRODUCT).start();
	}

	class ProductAdapter extends BaseAdapter {

		LayoutInflater myInflater;

		public ProductAdapter(ShoppingCart listViewActivity) {
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
				convertView = myInflater.inflate(R.layout.product_result,null);
				pViewHolder.productPhoto = (ImageView) convertView.findViewById(R.id.productPhoto);
				pViewHolder.productName = (TextView) convertView.findViewById(R.id.productName);
				pViewHolder.productPrice = (TextView) convertView.findViewById(R.id.productPrice);
							
				DisplayMetrics dm = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(dm);
				int t_width = dm.widthPixels/3;
				int t_height = dm.heightPixels/4;
				pViewHolder.productPhoto.setMinimumHeight(t_height);
				pViewHolder.productPhoto.setMinimumWidth(t_width);
				
				convertView.setTag(pViewHolder);
			}
			
			// fill data
			ViewHolder vi = (ViewHolder) convertView.getTag();
			byte [] pPhoto = product_set.get(position).productPhoto;
			Bitmap bm = BitmapFactory.decodeByteArray(pPhoto, 0,
					pPhoto.length, null);
			vi.productPhoto.setImageBitmap(getResizedBitmap(bm,100,100));
			vi.productName.setText(product_set.get(position).productName);
			vi.productPrice.setText(String.valueOf(product_set.get(position).productPrice));
			
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
	
	public ArrayList<String> StringArrayToList(String[] array)
	{
		ArrayList<String> stringList = new ArrayList<String>();
		for(String line:array)
		{
			stringList.add(line);
		}
		return stringList;
	}
	
}

