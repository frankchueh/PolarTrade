package com.example.project_ver1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.lang.String;

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
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class OnShelf extends ActionBarActivity {
    
	// 元件變數宣告 
	private Button btnProductUpload , btnCancelUpload , btnChangePhoto , btnDeletePhoto;
	private EditText editProductName , editProductPrice , editProductInfo;
	private ImageView productImage;
	// 商品相關變數字串宣告
	String productName = "" , productPrice = "" , productInfo = "";
	// 商品圖片相關變數宣告
	private Bitmap myBitmap;
	private File mPhoto;
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
		setContentView(R.layout.activity_on_shelf);
		
		this.objectInitialize();
		this.setButtonClick();
		
		MessageHandler = new Handler() {

			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SendToServer.SUCCESS:
					Toast.makeText(getApplicationContext(), "Product upload success", Toast.LENGTH_SHORT).show();
					finish(); // 結束 activity
					break;
				case SendToServer.FAIL:
					Toast.makeText(getApplicationContext(), "Product upload failed", Toast.LENGTH_SHORT).show();
					break;
				}
				super.handleMessage(msg);
			}
		};
	}
	
	private void objectInitialize() {
		
		btnProductUpload = (Button) this.findViewById(R.id.onShelf);
		btnCancelUpload = (Button) this.findViewById(R.id.cancel);
		btnChangePhoto= (Button) this.findViewById(R.id.changePhoto);
		btnDeletePhoto= (Button) this.findViewById(R.id.deletePhoto);
		editProductName = (EditText) this.findViewById(R.id.productNameText);
		editProductPrice = (EditText) this.findViewById(R.id.productPriceText);
		editProductInfo = (EditText) this.findViewById(R.id.productInfoText);
		
		productImage = (ImageView) this.findViewById(R.id.productPhotoShot);
	}
	
	private void setButtonClick() {
		
		productInfo = "";
		btnProductUpload.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if(!editProductName.getText().toString().equals("")) {	// 若 product 名稱欄位不為空
		
				  if(!editProductPrice.getText().toString().equals("")) {
					String pPrice = editProductPrice.getText().toString();
					if(CheckInput(pPrice)) {		// 若 product 金額輸入正確
						if(mContent != null) {		// 若 沒有選擇 product 照片
							String msg = "";
							// 要傳送的商品上傳訊息 ( command + name + price + account )
							msg += "InsertProduct" + "\n" +			
									editProductName.getText().toString() + "\n" +
									editProductPrice.getText().toString() +"\n" +
									mainActivity.Account;
							
							// 商品詳細訊息分成另一個 String
							String info_msg = editProductInfo.getText().toString();
							
							// 要傳送給 Server 的訊息 
							String [] msg_set = { msg , info_msg };  // 包含 ( productName + productPrice ) + (productInfo)
							
							// 分別傳送訊息和圖片上傳 -> 兩個 thread
							new SendToServer(SendToServer.MessagePort ,msg_set,MessageHandler,SendToServer.UPLOAD_PRODUCT).start();
							new SendToServer(SendToServer.PhotoPort ,mContent,MessageHandler,SendToServer.UPLOAD_PRODUCT_PHOTO).start();
							
							Toast.makeText(getApplicationContext(),"done upload",
									Toast.LENGTH_LONG).show();
						}
						else {
							Toast.makeText(getApplicationContext(), "請指定商品照片",
									Toast.LENGTH_LONG).show();
									return;
						}
					  }
					  else {
					    Toast.makeText(getApplicationContext(), "請輸入正確數字金額",
						Toast.LENGTH_LONG).show();
						return;
					  }
			        }
					else {
						Toast.makeText(getApplicationContext(), "請輸入數字金額",
						Toast.LENGTH_LONG).show();
						return;
					}
				}		
				else {
					Toast.makeText(getApplicationContext(), "請輸入商品名稱",
							Toast.LENGTH_LONG).show();
					return;
				}
	
			}
		});
		
		btnCancelUpload.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				finish();
			}
		});
		
		btnChangePhoto.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				final CharSequence[] items = { "選擇相簿", "拍照" };
				
				AlertDialog dlg = new AlertDialog.Builder(OnShelf.this)
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
				
				productImage.setImageResource(R.drawable.placeholder);
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
