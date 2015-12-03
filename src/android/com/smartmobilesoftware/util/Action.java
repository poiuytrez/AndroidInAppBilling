package com.smartmobilesoftware.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.apache.cordova.CallbackContext;

import com.smartmobilesoftware.inappbilling.InAppBillingPlugin;
import com.smartmobilesoftware.util.Purchase;
import com.smartmobilesoftware.util.IabHelper;
import com.smartmobilesoftware.util.IabResult;
import com.smartmobilesoftware.util.Inventory;
import com.smartmobilesoftware.util.SkuDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.util.Log;

/**
 * Represents an action (method call)
 */
public class Action {
	String 				action;
	JSONArray 			data;
	InAppBillingPlugin 	plugin;
	IabHelper 			mHelper;
	CallbackContext 	callbackContext;


	public Action(String action, JSONArray data, InAppBillingPlugin plugin, IabHelper mHelper, CallbackContext callbackContext) {
		this.action = action;
		this.data = data;
		this.plugin = plugin;
		this.mHelper = mHelper;
		this.callbackContext = callbackContext;
	}

	public Action(InAppBillingPlugin plugin, IabHelper mHelper, CallbackContext callbackContext) {
		this.plugin = plugin;
		this.mHelper = mHelper;
		this.callbackContext = callbackContext;
	}

	public boolean execute() throws JSONException, IllegalStateException  {
		if ("refreshPurchases".equals(action)) {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } else if ("getPurchases".equals(action)) {
			// Get the list of purchases
			JSONArray jsonSkuList = new JSONArray();
			jsonSkuList = getPurchases();
            // Call the javascript back
            callbackContext.success(jsonSkuList);
		} else if ("buy".equals(action)) {
			// Buy an item
			// Get Product Id 
			final String sku = data.getString(0);
			buy(sku);
		} else if ("subscribe".equals(action)) {
			// Subscribe to an item
			// Get Product Id 
			final String sku = data.getString(0);
			subscribe(sku);
		} else if ("consumePurchase".equals(action)) {
			consumePurchase(data);
		} else if ("getAvailableProducts".equals(action)) {
			// Get the list of purchases
			JSONArray jsonSkuList = new JSONArray();
			jsonSkuList = getAvailableProducts();
            // Call the javascript back
            callbackContext.success(jsonSkuList);
		} else if ("getProductDetails".equals(action)) {
			JSONArray jsonSkuList = new JSONArray(data.getString(0));
			final List<String> sku = new ArrayList<String>();			
			int len = jsonSkuList.length();
			Log.d(plugin.TAG, "Num SKUs Found: "+len);
			 for (int i=0;i<len;i++){
				sku.add(jsonSkuList.get(i).toString());
				Log.d(plugin.TAG, "Product SKU Added: "+jsonSkuList.get(i).toString());
			 }
			getProductDetails(sku);				
		} else {
			return false;
		}
		return true;
	}

	/*********************************** ACTIONS ********************************/


	public void refreshPurchases(final List<String> skus) {
		mHelper.queryInventoryAsync(true, skus, mGotInventoryListener);
	}

	public void refreshPurchases() {
		mHelper.queryInventoryAsync(mGotInventoryListener);
	}

	// Buy an item
	private void buy(final String sku){
		/* TODO: for security, generate your payload here for verification. See the comments on 
         *        verifyDeveloperPayload() for more info. Since this is a sample, we just use 
         *        an empty string, but on a production app you should generate this. */
		final String payload = "";
		
		plugin.startActivity();
		
		mHelper.launchPurchaseFlow(plugin.cordova.getActivity(), sku, plugin.RC_REQUEST, 
                mPurchaseFinishedListener, payload);

	}
	
	// Buy an item
	private void subscribe(final String sku){
		if (!mHelper.subscriptionsSupported()) {
            callbackContext.error("Subscriptions not supported on your device yet. Sorry!");
            return;
        }
		
		/* TODO: for security, generate your payload here for verification. See the comments on 
         *        verifyDeveloperPayload() for more info. Since this is a sample, we just use 
         *        an empty string, but on a production app you should generate this. */
		final String payload = "";
		
		plugin.startActivity();
        Log.d(plugin.TAG, "Launching purchase flow for subscription.");

		mHelper.launchSubscriptionPurchaseFlow(plugin.cordova.getActivity(), sku, plugin.RC_REQUEST, 
            mPurchaseFinishedListener);
	}
	

	// Get the list of purchases
	private JSONArray getPurchases() throws JSONException {
        List<Purchase>purchaseList = plugin.myInventory.getAllPurchases();

        // Convert the java list to json
        JSONArray jsonPurchaseList = new JSONArray();
        for (Purchase p : purchaseList) {
            JSONObject purchaseJsonObject = new JSONObject(p.getOriginalJson());
            purchaseJsonObject.put("signature", p.getSignature());
            purchaseJsonObject.put("receipt", p.getOriginalJson().toString());
            jsonPurchaseList.put(purchaseJsonObject);
        }

        return jsonPurchaseList;

	}

	// Get the list of available products
	private JSONArray getAvailableProducts(){
		// Get the list of owned items
		if(plugin.myInventory == null){
			callbackContext.error("Billing plugin was not initialized");
			return new JSONArray();
		}
        List<SkuDetails>skuList = plugin.myInventory.getAllProducts();
        
		// Convert the java list to json
	    JSONArray jsonSkuList = new JSONArray();
		try{
	        for (SkuDetails sku : skuList) {
				Log.d(plugin.TAG, "SKUDetails: Title: "+sku.getTitle());
	        	jsonSkuList.put(sku.toJson());
	        }
		}catch (JSONException e){
			callbackContext.error(e.getMessage());
		}
		return jsonSkuList;
	}

	//Get SkuDetails for skus
	private void getProductDetails(final List<String> skus){
		Log.d(plugin.TAG, "Beginning Sku(s) Query!");
		mHelper.queryInventoryAsync(true, skus, mGotDetailsListener);
	}
	
	// Consume a purchase
	private void consumePurchase(JSONArray data) throws JSONException{	
		String sku = data.getString(0);
		
		// Get the purchase from the inventory
		Purchase purchase = plugin.myInventory.getPurchase(sku);
		if (purchase != null)
			// Consume it
			mHelper.consumeAsync(purchase, mConsumeFinishedListener);
		else
			callbackContext.error(sku + " is not owned so it cannot be consumed");
	}


	/*********************************** PRIVATE METHODS ********************************/


	// Check if there is any errors in the iabResult and update the inventory
    private Boolean hasErrorsAndUpdateInventory(IabResult result, Inventory inventory){
    	if (result.isFailure()) {
        	callbackContext.error("Failed to query inventory: " + result);
        	return true;
        }
        
        // Have we been disposed of in the meantime? If so, quit.
        if (mHelper == null) {
        	callbackContext.error("The billing helper has been disposed");
        	return true;
        }
        
        // Update the inventory
        plugin.myInventory = inventory;
        
        return false;
    }

    /** Verifies the developer payload of a purchase. */
    private Boolean verifyDeveloperPayload(Purchase p) {
        @SuppressWarnings("unused")
		String payload = p.getDeveloperPayload();
        
        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         * 
         * WARNING: Locally generating a random string when starting a purchase and 
         * verifying it here might seem like a good approach, but this will fail in the 
         * case where the user purchases an item on one device and then uses your app on 
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         * 
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         * 
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on 
         *    one device work on other devices owned by the user).
         * 
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
        
        return true;
    }


	/*********************************** LISTENERS ********************************/


	// Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
        	Log.d(plugin.TAG, "Inside mGotInventoryListener");
        	if (hasErrorsAndUpdateInventory(result, inventory)) return;

            Log.d(plugin.TAG, "Query inventory was successful.");
            callbackContext.success();
            
        }
    };
    // Listener that's called when we finish querying the details
    IabHelper.QueryInventoryFinishedListener mGotDetailsListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(plugin.TAG, "Inside mGotDetailsListener");
            if (hasErrorsAndUpdateInventory(result, inventory)) return;

            Log.d(plugin.TAG, "Query details was successful.");

            List<SkuDetails>skuList = inventory.getAllProducts();
        
            // Convert the java list to json
            JSONArray jsonSkuList = new JSONArray();
            try {
                for (SkuDetails sku : skuList) {
                    Log.d(plugin.TAG, "SKUDetails: Title: "+sku.getTitle());
                    jsonSkuList.put(sku.toJson());
                }
            } catch (JSONException e) {
                callbackContext.error(e.getMessage());
            }
            callbackContext.success(jsonSkuList);
        }
    };

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(plugin.TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            
            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) {
            	callbackContext.error("The billing helper has been disposed");
            }
            
            if (result.isFailure()) {
            	callbackContext.error("Error purchasing: " + result);
                return;
            }
            
            if (!verifyDeveloperPayload(purchase)) {
            	callbackContext.error("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(plugin.TAG, "Purchase successful.");
            
            // add the purchase to the inventory
            plugin.myInventory.addPurchase(purchase);
            
            // append the purchase signature & receipt to the json
            try {
                JSONObject purchaseJsonObject = new JSONObject(purchase.getOriginalJson());
                purchaseJsonObject.put("signature", purchase.getSignature());
                purchaseJsonObject.put("receipt", purchase.getOriginalJson().toString());
                callbackContext.success(purchaseJsonObject);
            } catch (JSONException e) {
                callbackContext.error("Could not create JSON object from purchase object");
            }
        }
    };
    
    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(plugin.TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic
            	
                // remove the item from the inventory
            	plugin.myInventory.erasePurchase(purchase.getSku());
                Log.d(plugin.TAG, "Consumption successful. .");
                
                callbackContext.success(purchase.getOriginalJson());
                
            }
            else {
                callbackContext.error("Error while consuming: " + result);
            }
            
        }
    };

}