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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.SerializationUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class ProductManage extends ActionBarActivity {
	
	private ListView productView;
	private ProgressBar spinner;
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
	public HashMap<Integer,Bitmap> productMap = new HashMap<Integer,Bitmap>();
	public int buttonHiding = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_product_manage);
		
		productView = (ListView)findViewById(R.id.productlistview);	
		spinner = (ProgressBar)findViewById(R.id.progressBar);
		
		MessageHandler = new Handler() {

			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.SUCCESS_GET_PID:     // 成功取得使用者所有 pids
					byte[] temp_p = (byte[]) msg.obj;
					product_set = (ArrayList<Product>) SerializationUtils.deserialize(temp_p);
					Toast.makeText(getApplicationContext(), "Products download success", Toast.LENGTH_SHORT).show();
					spinner.setVisibility(View.GONE);
					setListView();	// 設置畫面
					break;
				
				case SendToServer.NO_PRODUCTS:
					Toast.makeText(getApplicationContext(), "No product exist", Toast.LENGTH_SHORT).show();
					break;
					
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(),"Product download failed", Toast.LENGTH_SHORT).show();
					
					break;
				
				case SendToServer.DELETE_SUCCESS:
					int deleted_position = Integer.valueOf(String.valueOf(msg.obj));
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
		
		productAdapter = new ProductAdapter(ProductManage.this);
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
				productMap.put(load_P.productID,Bitmap.createScaledBitmap(bm, 200, 200 , false));
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
	
	public class ProductAdapter extends BaseAdapter {
		 
		LayoutInflater myInflater;
		private Activity context;
		Bitmap bm = null;
		
		public ProductAdapter(Activity con) {
			this.context = con;
			myInflater = context.getLayoutInflater();
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
				
				convertView = myInflater.inflate(R.layout.productitem,parent,false);
				pViewHolder = new ViewHolder(); 
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
						p_msg = "deleteProduct" + "\n" + p + "\n" + product_set.get(p).productID;
						final CharSequence[] items = { "是", "否" };
						
						AlertDialog dlg = new AlertDialog.Builder(ProductManage.this)
								.setTitle("確定要移除商品嗎?")
								.setItems(items, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub
								        if(which == 0) {
											new SendToServer(SendToServer.MessagePort, p_msg, MessageHandler, 
													SendToServer.DELETE_PRODUCT).start();
								        }
								        else {
								  
								        }
									}
								}).create();
					   dlg.show();
					}
				});
				convertView.setTag(pViewHolder);
			}
			else {
				pViewHolder = (ViewHolder) convertView.getTag();
			}
			
			pViewHolder.productName.setText(product_set.get(position).productName);
			pViewHolder.productPrice.setText("NT." + String.valueOf(product_set.get(position).productPrice));
			pViewHolder.deleteProduct.setTag(position);
			if(buttonHiding == 0) {
				pViewHolder.deleteProduct.setVisibility(View.GONE);
			}
			else {
				pViewHolder.deleteProduct.setVisibility(View.VISIBLE);
			}
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.product_manage, menu);
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
		else if(id == R.id.action_delete) {
			if(buttonHiding == 0) {
				buttonHiding = 1;
			}
			else {
				buttonHiding = 0;
			}
			productAdapter.notifyDataSetChanged();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
}

