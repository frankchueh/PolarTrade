package com.example.project_ver1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang.SerializationUtils;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class SearchProduct extends ActionBarActivity {

	private ListView resultView;
	private ProgressBar spinner;
	ProductAdapter productAdapter;
	Button btnSearch;

	private File mpPhoto;
	private File ProductPhotoDir = new File(
			Environment.getExternalStorageDirectory() + "/DCIM/ProductPhoto");

	ArrayList<Product> product_set = new ArrayList<Product>();
	ArrayList<File> photo_set = new ArrayList<File>();
	FileOutputStream fos = null;
	Uri u = null;

	int p_num = 0; // 總商品數量
	double[] position;
	Handler MessageHandler;
	String command;
	public HashMap<Integer, Bitmap> productMap = new HashMap<Integer, Bitmap>();

	int last_request; // Use to retry when server error.
	String last_command;
	HandlerThread GetHandler;
	Handler RetryHandler;
	int RetryTime = 1500;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_product_search);
		resultView = (ListView) findViewById(R.id.resultListView);
		btnSearch = (Button) findViewById(R.id.btnSearch);
		spinner = (ProgressBar) findViewById(R.id.searchingSpinner);
		
		GetHandler = new HandlerThread("Retry");
		GetHandler.start();
		RetryHandler = new Handler(GetHandler.getLooper());
		
		MessageHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.GET_LOCATE_SUCCESS:
					position = (double[]) msg.obj;
//					Toast.makeText(getApplicationContext(),
//							"Get Location Success!", Toast.LENGTH_SHORT).show();
					command = "searchProduct\n" + position[0] + "\n"
							+ position[1] + "\n" + mainActivity.Account;
					new SendToServer(SendToServer.MessagePort, command,
							MessageHandler, SendToServer.SEARCH_PRODUCT)
							.start();
					
					last_request = SendToServer.SEARCH_PRODUCT;
					last_command = command;
					
					break;
					
				case SendToServer.GET_LOCATE_FAIL:
//					Toast.makeText(getApplicationContext(),
//							"Get Location fail", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.GET_SEARCH_RESULT:
//					Toast.makeText(getApplicationContext(),
//							"Get Search Result!:" + msg.obj.toString(),
//							Toast.LENGTH_SHORT).show();
					String[] result = msg.obj.toString().split("\n");
					String pid = "";
					for (String tem : result) {
						pid += tem.split(":")[1];
					}
					command = "getCompressProduct\n" + pid + "\n";
					new SendToServer(SendToServer.MessagePort, command,
							MessageHandler, SendToServer.GET_PRODUCT).start();
					
					last_request = SendToServer.GET_PRODUCT;
					last_command = command;
					
					break;
				case SendToServer.NO_SEARCH_RESULT:
					spinner.setVisibility(View.GONE);
					btnSearch.setVisibility(View.VISIBLE);
					Toast.makeText(getApplicationContext(), "No Result",
							Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.SUCCESS:
					product_set = (ArrayList<Product>) SerializationUtils
							.deserialize((byte[]) msg.obj);
					spinner.setVisibility(View.GONE);
					btnSearch.setVisibility(View.VISIBLE);
					setListView();
//					Toast.makeText(getApplicationContext(),
//							"Get Product Success", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(),
							"Search product failed", Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.SERVER_ERROR:
//					Toast.makeText(getApplicationContext(), "Retry",
//							Toast.LENGTH_SHORT).show();
					RetryHandler.postDelayed(retryRequest, RetryTime);
					break;
				}
				super.handleMessage(msg);
			}
		};

		btnSearch.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!product_set.isEmpty()) {
					product_set.clear();
					productAdapter.notifyDataSetChanged();
				}
				String command = "getLocate\n" + mainActivity.Account;
				spinner.setVisibility(View.VISIBLE);
				new SendToServer(SendToServer.MessagePort, command,
						MessageHandler, SendToServer.GET_LOCATE).start();
				btnSearch.setVisibility(View.GONE);
			}
		});

		spinner.setVisibility(View.GONE);
	}
	
	Runnable retryRequest = new Runnable()
	{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			new SendToServer(SendToServer.MessagePort, last_command,
					MessageHandler, last_request).start();
		}
		
	};
	
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
				Bitmap bm = BitmapFactory.decodeByteArray(
						product_set.get(position).productPhoto, 0,
						product_set.get(position).productPhoto.length, null);
				try {
					pfile = savePhoto(bm, product_set.get(position).productID);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bm.recycle();
				u = Uri.fromFile(pfile);

				bu.putString("user", "buyer");
				bu.putInt("id", product_set.get(position).productID);
				bu.putString("name", product_set.get(position).productName); // 傳遞商品名稱
				bu.putInt("price", product_set.get(position).productPrice); // 商品價格
				bu.putByteArray("info", product_set.get(position).productInfo); // 商品資訊
				bu.putString("photo", u.toString()); // 商品圖片超連結
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

	public class LoadImageThread extends AsyncTask<Product, Void, Bitmap> {

		private Product load_P;
		private ImageView img;

		public LoadImageThread(ImageView img) {
			this.img = img;
		}

		@Override
		protected Bitmap doInBackground(Product... params) {
			load_P = params[0];
			Bitmap bm = null;
			if (productMap.get(load_P.productID) == null) {
				byte[] pPhoto = load_P.productPhoto;
				bm = BitmapFactory.decodeByteArray(pPhoto, 0, pPhoto.length,
						null);
				productMap.put(load_P.productID, bm);
				// Log.d("firstLoadView", load_P.productName);
			} else {
				// Log.d("secondLoadView", load_P.productName);
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

			if (convertView == null) {
				pViewHolder = new ViewHolder();
				convertView = myInflater.inflate(R.layout.search_result,
						parent, false);
				pViewHolder.productPhoto = (ImageView) convertView
						.findViewById(R.id.productPhoto);
				pViewHolder.productName = (TextView) convertView
						.findViewById(R.id.productName);
				pViewHolder.productPrice = (TextView) convertView
						.findViewById(R.id.productPrice);

				DisplayMetrics dm = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(dm);
				convertView.setTag(pViewHolder);
			} else {
				pViewHolder = (ViewHolder) convertView.getTag();
			}
			// fill data

			pViewHolder.productName
					.setText(product_set.get(position).productName);
			pViewHolder.productPrice.setText(String.valueOf(product_set
					.get(position).productPrice));
			new LoadImageThread(pViewHolder.productPhoto).execute(product_set
					.get(position));
			return convertView;
		}
	}

	public File savePhoto(Bitmap bm, int pid) throws IOException { // 將照片存到內存路徑

		String localproductFileName = String.valueOf(pid) + ".jpg";
		ProductPhotoDir.mkdir();
		mpPhoto = new File(ProductPhotoDir, localproductFileName);

		try {
			fos = new FileOutputStream(mpPhoto);
			bm.compress(Bitmap.CompressFormat.JPEG, 80, fos);
		} catch (Exception e) {
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
