package com.example.project_ver1;


import java.io.Serializable;

public class Product implements Serializable{
	
	int productID;
	String productName;
	int productPrice;
	byte[] productInfo;
	byte[] productPhoto;
	int userID;
	
	Product(int pID , String pName, int pPrice , byte[] pInfo , byte[] pPhoto, int pUserID) {
		this.productID = pID;
		this.productName = pName;
		this.productPrice = pPrice;
		this.productInfo = pInfo;
		this.productPhoto = pPhoto;
		this.userID = pUserID;
	}
}
