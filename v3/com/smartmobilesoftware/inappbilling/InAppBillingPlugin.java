package com.smartmobilesoftware.inappbilling;

import java.util.ArrayList;
import java.util.List;

import net.robotmedia.billing.utils.IabHelper;
import net.robotmedia.billing.utils.IabResult;
import net.robotmedia.billing.utils.Inventory;
import net.robotmedia.billing.utils.Purchase;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * InappBilling Plugin for version 3
 * 
 * @author ASMAN
 * 
 */
public class InAppBillingPlugin extends CordovaPlugin {

	private static final String PURCHASE_STRING = "purchase";
	private static final String SUBSCRIBE_STRING = "subscribe";
	boolean mIsPremium = false;
	private final String TAG = "CORDOVA_BILLING";
	private Context context;
	private boolean mSubscribed = false;
	private static String SKU_PREMIUM;
	private static String SKU;
	private String SKU_Type;
	private static String sku_sub;
	private static final int RC_REQUEST = 10001;
	private String action_type;
	private IabHelper mHelper;

	// Plugin action handler
	@Override
	public boolean execute(String action, JSONArray data,
			CallbackContext callbackContext) {
		context = this.cordova.getActivity().getApplicationContext();
		action_type = action;
		loadData();

		if (PURCHASE_STRING.equals(action)) {
			try {
				SKU = data.getString(0);
				SKU_Type = "inapp";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (SUBSCRIBE_STRING.equals(action)) {
			try {
				SKU = data.getString(0);
				SKU_Type = "subs";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl6lf2mk2XIeBvElhlopTzOvMb+HIYj/lFp1KYxVIw6iTHKfmTz9ZtM0S3Czw7UEfiUT3VFcjhe04AbFZplwfz3deXGD4ZIErywZ+B8jkO3Pg37RAcu33pdCnwHBeBK+kOWrKCMtnEAq21guBq4mdVNluw8wiu0qfBt2vY/XaD+Sv3AYV546tWEsyTZkcjaw2Wrf84WQcuH1A6OYSpbMyrrOILvtpxXxq1Rr2qJIO8bt5NPuBfk1z2JH0CW8hUvA01eDTUZ73kLDemChN4wSx/2GlWIj40fS8JsXnug5ovAt1l6t75jG1Fs0sRn1IB/4rrcK0TTYltkBeUz5aaOtH7wIDAQAB";

		Log.d(TAG, "Creating IAB helper.");
		mHelper = new IabHelper(this.cordova.getActivity()
				.getApplicationContext(), base64EncodedPublicKey);

		mHelper.enableDebugLogging(true);

		Log.d(TAG, "Starting setup.");
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess()) {
					return;
				}

				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});

		return true;
	}

	/*
	 * Listener that's called when we finish querying the items and
	 * subscriptions we own
	 */

	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {
			Log.d(TAG, "Query inventory finished.");
			if (result.isFailure()) {
				// complain("Failed to query inventory: " + result);
				return;
			}

			Log.d(TAG, "Query inventory was successful.");

			Purchase purchase = inventory.getPurchase(SKU);
			if (purchase != null && verifyDeveloperPayload(purchase)) {
				mHelper.consumeAsync(inventory.getPurchase(SKU),
						mConsumeFinishedListener);
				return;
			}

			Log.d(TAG, "Initial inventory query finished; enabling main UI.");
			if (action_type.equals("purchase")) {
				onBuyButtonClicked(new ImageView(context));
			} else if (action_type.equals("ownItems")) {
				Inventory inventory2 = new Inventory();
				try {
					List<String> skuitems = new ArrayList<String>();
					skuitems.add(SKU);

					ArrayList<String> r = mHelper.querySkuDetails1(SKU_Type,
							inventory2, skuitems);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (action_type.equals("restoreTransactions")) {
				Inventory inventory2 = new Inventory();
				try {
					int r = mHelper.RestoreTransaction(inventory2, SKU_Type);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + ","
				+ data);

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		} else {
			Log.d(TAG, "onActivityResult handled by IABUtil.");
		}
	}

	public void onBuyButtonClicked(View arg0) {
		Log.d(TAG, "Buy button clicked.");

		// We will be notified of completion via mPurchaseFinishedListener
		Log.d(TAG, "Launching purchase flow ");

		/*
		 * security related verification should be done here.
		 */
		String payload = "";

		mHelper.launchPurchaseFlow(this.cordova.getActivity(), SKU, RC_REQUEST,
				mPurchaseFinishedListener, payload);
	}

	/* Verifies the developer payload of a purchase. */
	boolean verifyDeveloperPayload(Purchase p) {
		String payload = p.getDeveloperPayload();
		return true;
	}

	/* Callback for when a purchase is finished */
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: "
					+ purchase);
			if (result.isFailure()) {
				return;
			}
			if (!verifyDeveloperPayload(purchase)) {
				return;
			}
			Log.d(TAG, "Purchase successful.");
			if (purchase.getSku().equals(SKU)) {
				mHelper.consumeAsync(purchase, mConsumeFinishedListener);
			} else if (purchase.getSku().equals(SKU_PREMIUM)) {
				mIsPremium = true;
			} else if (purchase.getSku().equals(SKU)) {
				mSubscribed = true;
			}
		}
	};

	/* Called when consumption is complete */
	IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
		public void onConsumeFinished(Purchase purchase, IabResult result) {
			Log.d(TAG, "Consumption finished. Purchase: " + purchase
					+ ", result: " + result);
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Destroying helper.");
		if (mHelper != null)
			mHelper.dispose();
		mHelper = null;
	}

	void loadData() {
		SharedPreferences sp = this.cordova.getActivity().getPreferences(
				this.cordova.getActivity().MODE_PRIVATE);
	}

}
