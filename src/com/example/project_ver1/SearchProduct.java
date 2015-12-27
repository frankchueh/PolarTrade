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
import com.example.project_ver1.ProductManage.ViewHolder;

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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class SearchProduct extends Activity {
	
	private ListView resultView;
	ProductAdapter productAdapter;
	Button btnSearch;
	
	private File mpPhoto;
	private File ProductPhotoDir = new File(
			Environment.getExternalStorageDirectory() + "/DCIM/ProductPhoto");
	
	ArrayList <Product> product_set = new ArrayList <Product>();
	ArrayList <File> photo_set = new ArrayList <File>();
	FileOutputStream fos = null;
	Uri u = null;

	int p_num = 0;  // 總商品數量
	double[] position;
	Handler MessageHandler;
	String command;
	public HashMap<Integer,Bitmap> productMap = new HashMap<Integer,Bitmap>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_product_search);
		resultView = (ListView)findViewById(R.id.resultListView);
		btnSearch = (Button)findViewById(R.id.btnSearch);
		
		MessageHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.GET_LOCATE_SUCCESS:
					position = (double[]) msg.obj;
//					Toast.makeText(getApplicationContext(), ""+position[0]+","+position[1], Toast.LENGTH_SHORT).show();
					command = "searchProduct\n" + position[0]+ "\n" + position[1] + "\n" + mainActivity.Account;
					new SendToServer(SendToServer.MessagePort, command,
							MessageHandler, SendToServer.SEARCH_PRODUCT).start();
					break;
				case SendToServer.GET_LOCATE_FAIL:
					Toast.makeText(getApplicationContext(), "Get Location fail", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.GET_SEARCH_RESULT:
					String[] result = msg.obj.toString().split("\n");
					String pid = "";
					for(String tem:result)
					{
						pid += tem.split(":")[1];
					}
//					Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
					command = "getProduct\n" + pid + "\n";
					new SendToServer(SendToServer.MessagePort, command,
							MessageHandler, SendToServer.GET_PRODUCT).start();
					break;
				case SendToServer.NO_SEARCH_RESULT:
					Toast.makeText(getApplicationContext(), "No Result", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.SUCCESS:
					product_set = (ArrayList<Product>) SerializationUtils.deserialize((byte[])msg.obj);
					setListView();
					Toast.makeText(getApplicationContext(), "Get Product Success", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(), "Search product failed", Toast.LENGTH_SHORT).show();
					break;
				}
				super.handleMessage(msg);
			}
		};
		
		btnSearch.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String command = "getLocate\n" + mainActivity.Account;
				new SendToServer(SendToServer.MessagePort, command,
						MessageHandler, SendToServer.GET_LOCATE).start();
			}});
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
				it.setClass(SearchProduct.this, ProductInfo.class);
				startActivity(it);
			}
		});		
	}
	
	static class ViewHolder {
		public TextView productName;
		public TextView productPrice;
		public ImageView productPhoto;
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
			if(productMap.get(load_P.productID) == null) {
				byte [] pPhoto = load_P.productPhoto;
				Bitmap bm = getResizedBitmap(BitmapFactory.decodeByteArray(pPhoto, 0, pPhoto.length , null),100,100);
				productMap.put(load_P.productID,bm);
				Log.d("firstLoadView", load_P.productName);
				return bm;
			}
			else {
				Log.d("secondLoadView", load_P.productName);
				return productMap.get(load_P.productID);
			}
		}
		@Override
		protected void onPostExecute(Bitmap result) {
			
			super.onPostExecute(result);
			img.setImageBitmap(result);
		}
	}
	
	class ProductAdapter extends BaseAdapter {

		LayoutInflater myInflater;

		public ProductAdapter(SearchProduct listViewActivity) {
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
				convertView = myInflater.inflate(R.layout.productitem, parent, false);
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
			else {
				pViewHolder = (ViewHolder) convertView.getTag();
			}
			// fill data
			
			pViewHolder.productName.setText(product_set.get(position).productName);
			pViewHolder.productPrice.setText(String.valueOf(product_set.get(position).productPrice));
			new LoadImageThread(pViewHolder.productPhoto).execute(product_set.get(position));
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

