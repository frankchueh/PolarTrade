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
import java.util.HashMap;

import org.apache.commons.lang.SerializationUtils;

import com.example.project_ver1.ProductManage.LoadImageThread;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
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
	private ProgressBar spinner;
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
	public HashMap<Integer,Bitmap> productMap = new HashMap<Integer,Bitmap>();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_shopping_cart);
		resultView = (ListView)findViewById(R.id.resultListView);
		spinner = (ProgressBar)findViewById(R.id.progressBar);
		save_product = new FileManager(savePath + "/SaveProduct.txt");

		MessageHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.SUCCESS:
					product_set = (ArrayList<Product>) SerializationUtils.deserialize((byte[])msg.obj);
					spinner.setVisibility(View.GONE);
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
		command = "getCompressProduct\n";
		for(String pid:pids)
		{
			command += pid + ",";
		}
		
		new SendToServer(SendToServer.MessagePort, command,
				MessageHandler, SendToServer.GET_PRODUCT).start();
	}
	
    public class LoadImageThread extends AsyncTask <Product, Void , Bitmap> {
		
		private Product load_P;
		private ImageView img;
		
		public LoadImageThread(ImageView img) {
			this.img = img;
		}
		@Override 
		protected Bitmap doInBackground(Product... params) {
			load_P = params[0];
			Bitmap bm = null;
			if(productMap.get(load_P.productID) == null) {
				byte [] pPhoto = load_P.productPhoto;
				bm = BitmapFactory.decodeByteArray(pPhoto, 0, pPhoto.length , null);
				productMap.put(load_P.productID, bm);
				//Log.d("firstLoadView", load_P.productName);
			}
			else {
				//Log.d("secondLoadView", load_P.productName);
				bm = productMap.get(load_P.productID);
			}
			return bm;
		}
		@Override
		protected void onPostExecute(Bitmap result) {
			
			super.onPostExecute(result);
			img.setImageBitmap(result);
		}
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
				convertView = myInflater.inflate(R.layout.productitem,null);
				pViewHolder.productPhoto = (ImageView) convertView.findViewById(R.id.productPhoto);
				pViewHolder.productName = (TextView) convertView.findViewById(R.id.productName);
				pViewHolder.productPrice = (TextView) convertView.findViewById(R.id.productPrice);
				convertView.findViewById(R.id.deletedProduct).setVisibility(View.GONE);			
				DisplayMetrics dm = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(dm);
				convertView.setTag(pViewHolder);
			}
			
			// fill data
			ViewHolder vi = (ViewHolder) convertView.getTag();
			byte [] pPhoto = product_set.get(position).productPhoto;
			Bitmap bm = BitmapFactory.decodeByteArray(pPhoto, 0,
					pPhoto.length, null);
			vi.productName.setText(product_set.get(position).productName);
			vi.productPrice.setText(String.valueOf(product_set.get(position).productPrice));
			new LoadImageThread(pViewHolder.productPhoto).execute(product_set.get(position));
			return convertView;
		}
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

