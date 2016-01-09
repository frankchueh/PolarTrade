package com.example.project_ver1;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ProductInfo extends ActionBarActivity {

	private EditText ProductDetailInfo;
	private TextView ProductName , ProductPrice;
	private ImageView ProductImage;
	private Button productEditButton, btnChat, btnSaveProduct;
	LinearLayout buyerLayout,sellerLayout;
	Handler MessageHandler;
	String savePath = Environment.getExternalStorageDirectory().getPath()+"/PolarTrade";
	ArrayList<String> productIds;
	FileManager save_product;
	
	int pid = -1;
	int sid = -1;
	
	String productName = "";
	String productInfo = "";
	int productPrice = 0;
	
	Uri orgUri = null;
	byte [] mContent = null;
	
	public static final int EDIT_PRODUCT = 1111;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_product_info);
		
		save_product = new FileManager(savePath + "/SaveProduct.txt");
		productIds = StringArrayToList(save_product.readAllLine());
		
		this.objectInitialize();
		this.setButtonClick();
		
		Intent call_it = getIntent();
		Bundle bu = call_it.getExtras();
		String user = bu.getString("user");
		if(user.equals("buyer"))
			sellerLayout.removeAllViews();
		else if(user.equals("seller"))
			buyerLayout.removeAllViews();
		
		pid = bu.getInt("id");
		productName = bu.getString("name");
		productPrice = bu.getInt("price");
		productInfo = new String(bu.getByteArray("info"),Charset.forName("UTF-8"));
		sid = bu.getInt("sellerID");
		ProductName.setText(productName);
		ProductPrice.setText(String.valueOf(productPrice));
		ProductDetailInfo.setText(productInfo);
		
		//設置購物車狀態
		if(productIds.contains(""+pid))
		{
			btnSaveProduct.setText("移除購物車");
		}
		else
			btnSaveProduct.setText("放入購物車");
		
		/*try {
			orgUri = Uri.parse(bu.getString("photo"));
			ContentResolver contentResolver = getContentResolver();
			mContent = readStream(contentResolver.openInputStream(orgUri));
			Bitmap bm = BitmapFactory.decodeByteArray(mContent, 0,
					mContent.length, null);
			ProductImage.setImageBitmap(bm);
			} catch(Exception e) {
					e.printStackTrace();
			}*/
		MessageHandler = new Handler() {

			public void handleMessage(Message msg) {
				switch (msg.what) {

				case SendToServer.SUCCESS_GET_PHOTO:
					mContent = (byte[]) msg.obj;
					Bitmap bm = BitmapFactory.decodeByteArray(mContent, 0,
							mContent.length, null);
					ProductImage.setImageBitmap(bm);
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(), "Get Info Fail",
							Toast.LENGTH_SHORT).show();
					break;

				case SendToServer.SERVER_ERROR:
					Toast.makeText(getApplicationContext(),
							"Server not response", Toast.LENGTH_SHORT).show();
					break;
				}
				super.handleMessage(msg);
			}
		};
		
		String msg_getphoto = "GetPhoto" + "\n" +
							  "C:/DataBase/product/"+ pid +"/" + "photo.jpg";
		new SendToServer(SendToServer.PhotoPort, msg_getphoto,
				MessageHandler, SendToServer.GET_PHOTO).start(); // 傳到server並抓取圖片
		
	}
	
	@Override
	public void onBackPressed() {
	    
		Intent it = new Intent();							// 使用 intent 將更新資訊回傳 ProductManage
		Bundle bu = new Bundle();
		
		bu.putString("name", productName);					// 新名字
		bu.putInt("price", productPrice); // 新價格
		bu.putByteArray("info",productInfo.getBytes());		// 新商品資訊
		bu.putString("photo", orgUri.toString());
		
		it.putExtras(bu);
		setResult(ProductManage.UPDATE_SUCCESS , it);
		finish();
	}
	
	private void objectInitialize() {
		
		ProductName = (TextView) this.findViewById(R.id.productInfo_Name);
		ProductPrice = (TextView) this.findViewById(R.id.productInfo_Price);
		ProductDetailInfo = (EditText) this.findViewById(R.id.productInfo_Info);
		ProductImage = (ImageView) this.findViewById(R.id.productInfo_Photo);
		productEditButton = (Button) this.findViewById(R.id.productInfo_editButton);
		buyerLayout = (LinearLayout)findViewById(R.id.buyerLayout);
		sellerLayout = (LinearLayout)findViewById(R.id.sellerLayout);
		btnChat = (Button) this.findViewById(R.id.btnChat);
		btnSaveProduct = (Button) this.findViewById(R.id.btnSaveProduct);
	}
	
	private void setButtonClick() {
		
		productEditButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				Intent it = new Intent();
				Bundle bu = new Bundle();
				
				bu.putInt("id", pid);
				bu.putString("name", productName);					// 新名字
				bu.putInt("price", productPrice); 					// 新價格
				bu.putByteArray("info",productInfo.getBytes());		// 新商品資訊
				bu.putString("photo",orgUri.toString());   // 商品圖片超連結
				
				it.putExtras(bu);
				it.setClass(ProductInfo.this, ProductEdit.class);
				startActivityForResult(it , EDIT_PRODUCT);
			}
		});
		
		btnChat.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent();
				it.setClass(ProductInfo.this, Chatroom.class);
				it.putExtra("produceID", pid);
				it.putExtra("sellerID", sid);
				startActivity(it);
			}});
		
		btnSaveProduct.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				productIds = StringArrayToList(save_product.readAllLine());
				
				if(!productIds.contains(""+pid))
				{
					save_product.writeLine(""+pid);
					Toast.makeText(getApplicationContext(), "加入購物車", Toast.LENGTH_SHORT).show();
					btnSaveProduct.setText("移除購物車");
				}
				else
				{
					productIds.remove(""+pid);
					String[] pids = {}; 
					pids = productIds.toArray(pids);
					save_product.writeAllLine(pids);
					Toast.makeText(getApplicationContext(), "移除購物車", Toast.LENGTH_SHORT).show();
					btnSaveProduct.setText("放入購物車");
				}
			}});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub

		if(resultCode == ProductManage.UPDATE_SUCCESS) {
			Bundle bu = data.getExtras();
			productName = bu.getString("name");
			productPrice = bu.getInt("price");
			productInfo = new String(bu.getByteArray("info"),Charset.forName("UTF-8"));
			
			try {
				orgUri = Uri.parse(bu.getString("photo"));
				ContentResolver contentResolver = getContentResolver();
				mContent = readStream(contentResolver.openInputStream(orgUri));
				Bitmap bm = BitmapFactory.decodeByteArray(mContent, 0,
						mContent.length, null);
				ProductImage.setImageBitmap(bm);
				} catch(Exception e) {
						e.printStackTrace();
				}
			
			ProductName.setText(productName);
			ProductPrice.setText(String.valueOf(productPrice));
			ProductDetailInfo.setText(productInfo);
		}
		
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

