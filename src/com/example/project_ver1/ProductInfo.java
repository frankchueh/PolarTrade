package com.example.project_ver1;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ProductInfo extends Activity {

	private EditText ProductDetailInfo;
	private TextView ProductName , ProductPrice;
	private ImageView ProductImage;
	private Button productEditButton;
	
	int pid = -1;
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
		
		this.objectInitialize();
		this.setButtonClick();
		Intent call_it = getIntent();
		Bundle bu = call_it.getExtras();
		
		pid = bu.getInt("id");
		productName = bu.getString("name");
		productPrice = bu.getInt("price");
		productInfo = new String(bu.getByteArray("info"),Charset.forName("UTF-8"));
		
		ProductName.setText(productName);
		ProductPrice.setText(String.valueOf(productPrice));
		ProductDetailInfo.setText(productInfo);
		
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
		
	}
	
	@Override
	public void onBackPressed() {
	    
		Intent it = new Intent();							// �ϥ� intent �N��s��T�^�� ProductManage
		Bundle bu = new Bundle();
		
		bu.putString("name", productName);					// �s�W�r
		bu.putInt("price", productPrice); // �s����
		bu.putByteArray("info",productInfo.getBytes());		// �s�ӫ~��T
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
	}
	
	private void setButtonClick() {
		
		productEditButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				Intent it = new Intent();
				Bundle bu = new Bundle();
				
				bu.putInt("id", pid);
				bu.putString("name", productName);					// �s�W�r
				bu.putInt("price", productPrice); 					// �s����
				bu.putByteArray("info",productInfo.getBytes());		// �s�ӫ~��T
				bu.putString("photo",orgUri.toString());   // �ӫ~�Ϥ��W�s��
				
				it.putExtras(bu);
				it.setClass(ProductInfo.this,ProductEdit.class);
				startActivityForResult(it , EDIT_PRODUCT);		
			}
		});
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
