package com.example.project_ver1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang.SerializationUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class ProductEdit extends Activity {
	
	private Button btnProductUpload , btnCancelUpload , btnChangePhoto , btnDeletePhoto;
	private EditText editProductName , editProductPrice , editProductInfo;
	private ImageView productImage;
	private Product editedProduct;
	
	String productName , productPrice , productInfo;
	
	private Bitmap myBitmap;
	private File mPhoto;
	private String p_msg;
	private File CropPhotoDir = new File(
			Environment.getExternalStorageDirectory() + "/DCIM/CropPhoto");
	private File TakePhotoDir = new File(
			Environment.getExternalStorageDirectory() + "/DCIM/TakePhoto");
	private final int PICTURE_FROM_CAMERA = 1000;
	private final int PICTURE_FROM_ALBUM = 1001;
	private final int PICTURE_AFTER_CROP = 1002;
	private byte[] mContent = null;  // 用於儲存商品照片資料 (byte) -> 用於傳輸
	// MessageHandler 宣告
	Handler MessageHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_product_edit);
		
		this.objectInitialize();
		Intent call_it = getIntent();
		editedProduct = (Product) call_it.getSerializableExtra("product");
		this.setButtonClick();
		
		editProductName.setText(editedProduct.productName);
		editProductPrice.setText(String.valueOf(editedProduct.productPrice));
		editProductInfo.setText(new String(editedProduct.productInfo,Charset.forName("UTF-8")));
		byte [] pPhoto = editedProduct.productPhoto;
		Bitmap bm = BitmapFactory.decodeByteArray(pPhoto, 0,
				pPhoto.length, null);
		productImage.setImageBitmap(bm);
		
		MessageHandler = new Handler() {

			public void handleMessage(Message msg) {
				switch (msg.what) {
				
				case SendToServer.FAIL:
					
					Toast.makeText(getApplicationContext(),"Product info download failed", Toast.LENGTH_SHORT).show();
				}
				super.handleMessage(msg);
			}
		};
		
	}
	
private void objectInitialize() {
		
		btnProductUpload = (Button) this.findViewById(R.id.update_product);
		btnCancelUpload = (Button) this.findViewById(R.id.cancel_update);
		btnChangePhoto= (Button) this.findViewById(R.id.changeProductPhoto);
		btnDeletePhoto= (Button) this.findViewById(R.id.deleteProductPhoto);
		editProductName = (EditText) this.findViewById(R.id.NewproductNameText);
		editProductPrice = (EditText) this.findViewById(R.id.NewproductPriceText);
		editProductInfo = (EditText) this.findViewById(R.id.NewproductInfoText);
		productImage = (ImageView) this.findViewById(R.id.productNewPhotoShot);
}

private void setButtonClick() {
	
	productInfo = "";
	btnProductUpload.setOnClickListener(new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			if(!editProductName.getText().toString().equals("")) {
				productName = editProductName.getText().toString();
				
				if(!editProductName.getText().toString().equals("")) {
					productPrice = editProductPrice.getText().toString();
					
					if(!CheckInput(productPrice)) {
						Toast.makeText(getApplicationContext(),
								"請輸入正確數字金額 (由 0 ~ 9 組成 )", Toast.LENGTH_LONG)
								.show();
						return;
					}
					
					if(!editProductInfo.getText().toString().equals("")) {
						productInfo = editProductInfo.getText().toString();
					}
					
				}
				else {
					Toast.makeText(getApplicationContext(), "請輸入商品金額",
							Toast.LENGTH_LONG).show();
					return;
				}
			}
			else {
				Toast.makeText(getApplicationContext(), "請輸入商品名稱",
						Toast.LENGTH_LONG).show();
				return;
			}
			
			String msg = "";
			msg += "InsertProduct\n" + productName + "\n"
					+ productPrice +"\n"
					+ mainActivity.Account;
			String info_msg = productInfo;
			
			String [] msg_set = { msg , info_msg };  // 包含 ( productName + productPrice ) + (productInfo)
			
		    //new SendToServer(SendToServer.MessagePort ,msg_set,MessageHandler,SendToServer.UPDATE_PRODUCT).start();
		    //new SendToServer(SendToServer.PhotoPort ,mContent,MessageHandler,SendToServer.UPDATE_PRODUCT_PHOTO).start();
		    
			Toast.makeText(getApplicationContext(),"done upload",
					Toast.LENGTH_LONG).show();
		}
	});
	
	btnCancelUpload.setOnClickListener(new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			
		}
	});
	
	btnChangePhoto.setOnClickListener(new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			final CharSequence[] items = { "選擇相簿", "拍照" };
			
			AlertDialog dlg = new AlertDialog.Builder(ProductEdit.this)
					.setTitle("選擇照片")
					.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							// TODO Auto-generated method stub
					        if(which == 1) {
					        	TakePicture();
					        }
					        else {
					        	SelectPhoto();
					        }
						}
					}).create();
		   dlg.show();
		}
	});
	
	btnDeletePhoto.setOnClickListener(new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			productImage.setImageResource(R.drawable.unknow_product);
			mContent = null;
		}
	});		
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	// TODO Auto-generated method stub
	super.onActivityResult(requestCode, resultCode, data);
	ContentResolver contentResolver = getContentResolver();
	/**
	 * 因為兩種方式都用到了startActivityForResult方法，這個方法執行完後都會執行onActivityResult方法，
	 * 所以為了區別到底選擇了那個方式獲取圖片要進行判斷
	 * ，這裡的requestCode跟startActivityForResult裡面第二個參數對應
	 */

	if (requestCode == PICTURE_FROM_ALBUM) {
		try {
			Uri orginalUri = data.getData();
			CropPhoto(orginalUri);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	} else if (requestCode == PICTURE_FROM_CAMERA) {
		try {
			CropPhoto(Uri.fromFile(mPhoto));
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}

	else if (requestCode == PICTURE_AFTER_CROP) {
		try {
			// 獲得圖片的uri
			Uri orginalUri = Uri.fromFile(mPhoto);
			System.out.print(orginalUri);
			// 將圖片内容解析成字節數組
			mContent = readStream(contentResolver.openInputStream(Uri
					.parse(orginalUri.toString())));
			// 將字節數組轉換為ImageView可調用的Bitmap對象
			myBitmap = BitmapFactory.decodeByteArray(mContent, 0,
					mContent.length, null);
			//壓縮照片
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			myBitmap.compress(CompressFormat.JPEG, 80, bos);
			mContent = bos.toByteArray();
			//把得到的圖片绑定在控件上顯示
			productImage.setImageBitmap(myBitmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

/**
 * 取得拍攝後照片的名稱
 */
private String getPhotoFileName() {
	Date date = new Date(System.currentTimeMillis());   // 利用現在時間命名
	SimpleDateFormat dateFormat = new SimpleDateFormat(
			"'IMG'_yyyy-MM-dd HH:mm:ss");
	return dateFormat.format(date) + ".jpg";
}

/**
 * 利用相簿來取得照片
 */
public void SelectPhoto() {
	Intent getImage = new Intent(Intent.ACTION_PICK);   // 建立新 intent
	getImage.setType("image/*");  // 選擇選取資料型態
	startActivityForResult(getImage, PICTURE_FROM_ALBUM);   
}

/**
 * 利用相機來拍攝照片
 */
public void TakePicture() {
	Intent getImageByCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE,
			null);
	getImageByCamera.putExtra("outputFormat", "JPEG");
	TakePhotoDir.mkdirs();
	mPhoto = new File(TakePhotoDir, getPhotoFileName());
	getImageByCamera
			.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPhoto));
	startActivityForResult(getImageByCamera, PICTURE_FROM_CAMERA);
}

/**
 * 對照片做裁剪處理
 */
public void CropPhoto(Uri photoUri) {
	Intent it = new Intent("com.android.camera.action.CROP");
	it.setDataAndType(photoUri, "image/*");
	it.putExtra("crop", "true");
	it.putExtra("outputFormat", "JPEG");
	it.putExtra("return-data", false);     // 不要 return data

	String localTempImgFileName = System.currentTimeMillis() + ".jpg";
	CropPhotoDir.mkdirs();
	mPhoto = new File(CropPhotoDir, localTempImgFileName);
	Uri uri2 = Uri.fromFile(mPhoto);
	it.putExtra(MediaStore.EXTRA_OUTPUT, uri2);
	startActivityForResult(it, PICTURE_AFTER_CROP);
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

public boolean CheckInput(String input) {
	char[] check = input.toCharArray();

	for (int i = 0; i < check.length; i++) {
		if (!(check[i] >= '0' && check[i] <= '9'))
			return false;
	}
	return true;
}
}
