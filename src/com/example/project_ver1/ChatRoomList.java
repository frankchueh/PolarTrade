package com.example.project_ver1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;

import com.example.project_ver1.SearchProduct.LoadImageThread;
import com.example.project_ver1.SearchProduct.ProductAdapter;
import com.example.project_ver1.SearchProduct.ViewHolder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChatRoomList extends ActionBarActivity {

	ListView listChatroom;
	Button btnBuyer, btnSeller;
	String[] chatID_S = {};
	ArrayList<Product> product_S = null;
	String[] chatID_B = {};
	ArrayList<Product> product_B = null;
	String[] chatID_current = {};
	ArrayList<Product> product_current = new ArrayList<Product>();

	Handler MessageHandler;
	HandlerThread GetHandler;
	Handler workHandler;
	int DownloadTime = 60000;

	String BchatID_with_comma = "";
	String SchatID_with_comma = "";
	String chatID_with_comma = "";
	String[] all_chatID;
	
	int last_request;
	String last_command;
	
	ChatListAdapter appAdapter;
	public HashMap<Integer, Bitmap> productMap = new HashMap<Integer, Bitmap>();
	public HashMap<Integer, String> message_state = new HashMap<Integer, String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chatroomlist);

		GetHandler = new HandlerThread("CheckMessageState");
		GetHandler.start();
		workHandler = new Handler(GetHandler.getLooper());

		listChatroom = (ListView) this.findViewById(R.id.listChatroom);
		btnBuyer = (Button) this.findViewById(R.id.btnBuyer);
		btnSeller = (Button) this.findViewById(R.id.btnSeller);

		btnBuyer.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				chatID_current = chatID_B;
				product_current = product_B;
				if (appAdapter != null)
					appAdapter.notifyDataSetChanged();
			}
		});

		btnSeller.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				chatID_current = chatID_S;
				product_current = product_S;
				if (appAdapter != null)
					appAdapter.notifyDataSetChanged();
			}
		});

		MessageHandler = new Handler() {

			public void handleMessage(Message msg) {
				switch (msg.what) {
				
				case SendToServer.SUCCESS_GET_CHAT_LIST:
//					Toast.makeText(getApplicationContext(), msg.obj.toString(),
//							Toast.LENGTH_SHORT).show();
					BchatID_with_comma = msg.obj.toString().split("\n")[0];
					SchatID_with_comma = msg.obj.toString().split("\n")[1];
					chatID_with_comma = BchatID_with_comma + SchatID_with_comma;
					all_chatID = chatID_with_comma.split(",");
					
					if (!BchatID_with_comma.equals(" ")) {
						chatID_B = BchatID_with_comma.split(",");
						String command = "GetChatRoomProduct" + "\n"
								+ BchatID_with_comma + "\n"
								+ mainActivity.Account;
						new SendToServer(SendToServer.MessagePort, command,
								MessageHandler, SendToServer.GET_PRODUCT)
								.start();
						
						last_request = SendToServer.GET_PRODUCT;
						last_command = command;
					}
					if (!SchatID_with_comma.equals(" ")) {
						chatID_S = SchatID_with_comma.split(",");
						if (chatID_B == null) {
							String command = "GetChatRoomProduct" + "\n"
									+ SchatID_with_comma + "\n"
									+ mainActivity.Account;
							new SendToServer(SendToServer.MessagePort, command,
									MessageHandler, SendToServer.GET_PRODUCT)
									.start();
							
							last_request = SendToServer.GET_PRODUCT;
							last_command = command;
						}
					}
					workHandler.post(checkMessageState);
					
					break;
					
				case SendToServer.SUCCESS:
					ArrayList<Product> product_set = (ArrayList<Product>) SerializationUtils
							.deserialize((byte[]) msg.obj);

					if (chatID_B != null && product_B == null) {
						product_B = product_set;
						if (!SchatID_with_comma.equals(" ")) {
							String command = "GetChatRoomProduct" + "\n"
									+ SchatID_with_comma + "\n"
									+ mainActivity.Account;
							new SendToServer(SendToServer.MessagePort, command,
									MessageHandler, SendToServer.GET_PRODUCT)
									.start();
						}
					} else {
						product_S = product_set;
						setListView();
					}
					break;
				case SendToServer.GET_MESSAGE_STATE:
					if (msg.obj != null) {
						String[] tem_message_state = msg.obj.toString().split(",");
						String[] chatID = chatID_with_comma.split(",");
						for(int index = 0; index < chatID.length; index++)
						{
							if (StringUtils.isNumeric(chatID[index]))
							{
								message_state.put(Integer.parseInt(chatID[index])
										, tem_message_state[index]);
							}
						}
						if (appAdapter != null)
							appAdapter.notifyDataSetChanged();
					}
					break;
				case SendToServer.FAIL:
//					Toast.makeText(getApplicationContext(), msg.obj.toString(),
//							Toast.LENGTH_SHORT).show();
					break;
				case SendToServer.SERVER_ERROR:
					Toast.makeText(getApplicationContext(), "Server Error",
							Toast.LENGTH_SHORT).show();
					workHandler.postDelayed(retry, 1000);
					break;
				}
				super.handleMessage(msg);
			}
		};

	}
	
	Runnable retry = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			new SendToServer(SendToServer.MessagePort, last_command,
					MessageHandler, last_request)
					.start();
		}
		
	};
	
	Runnable checkMessageState = new Runnable() {
		@Override
		public void run() {

			String command = "CheckMessageState" + "\n" + mainActivity.Account
					+ "\n" + chatID_with_comma;
			new SendToServer(SendToServer.MessagePort, command, MessageHandler,
					SendToServer.CHECK_MESSAGE_STATE).start();
			workHandler.postDelayed(checkMessageState, DownloadTime); // ©µ¿ð«áÄ~Äò¶]
		}
	};

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		String msg = "ListChatRoom\n" + mainActivity.Account;
		new SendToServer(SendToServer.MessagePort, msg, MessageHandler,
				SendToServer.LIST_CHAT_ROOM).start();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		workHandler.removeCallbacks(checkMessageState);
		workHandler = null;
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		workHandler.removeCallbacks(checkMessageState);
		super.onPause();
	}

	private void setListView() {
		appAdapter = new ChatListAdapter(this);
		listChatroom.setAdapter(appAdapter);
		listChatroom.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				if (!chatID_current[position].equals("")) {
					Intent it = new Intent();
					it.setClass(ChatRoomList.this, Chatroom.class);
					it.putExtra("chatID",
							Integer.parseInt(chatID_current[position]));
					startActivity(it);
				}
			}
		});
	}

	static class ViewHolder {
		public TextView productName;
		public ImageView productPhoto;
		public ImageView newMessage;
	}

	class ChatListAdapter extends BaseAdapter {

		LayoutInflater myInflater;

		public ChatListAdapter(ChatRoomList listViewActivity) {
			myInflater = LayoutInflater.from(listViewActivity);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return product_current.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return product_current.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder pViewHolder = null;

			if (convertView == null) {
				pViewHolder = new ViewHolder();
				convertView = myInflater.inflate(R.layout.chatlistview, parent,
						false);
				pViewHolder.newMessage = (ImageView) convertView
						.findViewById(R.id.imgNewMessage);
				pViewHolder.productPhoto = (ImageView) convertView
						.findViewById(R.id.imgChatList);
				pViewHolder.productName = (TextView) convertView
						.findViewById(R.id.txtChatList1);
				DisplayMetrics dm = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(dm);
				convertView.setTag(pViewHolder);
			} else {
				pViewHolder = (ViewHolder) convertView.getTag();
			}
			int chatID = Integer.parseInt(chatID_current[position]);
			
			if(message_state.get(chatID).equals("1"))
				pViewHolder.newMessage.setVisibility(View.VISIBLE);
			else
				pViewHolder.newMessage.setVisibility(View.INVISIBLE);
			pViewHolder.productName
					.setText(product_current.get(position).productName);
			new LoadImageThread(pViewHolder.productPhoto)
					.execute(product_current.get(position));
			return convertView;
		}

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
