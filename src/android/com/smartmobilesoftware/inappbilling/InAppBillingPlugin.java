/**
 * In App Billing Plugin
 * @author Guillaume Charhon - Smart Mobile Software
 * @modifications Brian Thurlow 10/16/13
 *
 */
package com.smartmobilesoftware.inappbilling;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import com.smartmobilesoftware.util.Purchase;
import com.smartmobilesoftware.util.IabHelper;
import com.smartmobilesoftware.util.IabResult;
import com.smartmobilesoftware.util.Inventory;
import com.smartmobilesoftware.util.SkuDetails;
import com.smartmobilesoftware.util.Action;

import android.content.Intent;
import android.util.Log;

public class InAppBillingPlugin extends CordovaPlugin {
	private final Boolean      ENABLE_DEBUG_LOGGING = true;
    public static final int    RC_REQUEST = 10001; // (arbitrary) request code for the purchase flow

    public final String        TAG = "CORDOVA_BILLING";

    // The helper object
    public  IabHelper           mHelper;

    // A quite up to date inventory of available items and purchase items
    public  Inventory           myInventory;

    // Plugin initialized ?
    boolean                     initialized = false;

    // Activity open
    boolean                     activityOpen = false;

	@Override
	/**
	 * Called by each javascript plugin function
	 */
	public boolean execute(String action, JSONArray data, final CallbackContext callbackContext) {

        try {
            // Action selector
            if ("init".equals(action)) {
                final List<String> sku = new ArrayList<String>();
                if(data.length() > 0){
                    JSONArray jsonSkuList = new JSONArray(data.getString(0));
                    int len = jsonSkuList.length();
                    Log.d(TAG, "Num SKUs Found: "+len);
                 for (int i=0;i<len;i++){
                        sku.add(jsonSkuList.get(i).toString());
                        Log.d(TAG, "Product SKU Added: "+jsonSkuList.get(i).toString());
                 }
                }
                // Initialize
                init(sku, callbackContext);
                return true;
            } else if ("isPurchaseOpen".equals(action)) {
                if (activityOpen == true) {
                    callbackContext.success("true");
                } else {
                    callbackContext.success("false");
                }
                return true;
            } else {
                if (initialized == false) {
                    throw new IllegalStateException("Billing plugin was not initialized");
                }
                Action actionInstance = new Action(action, data, this, mHelper, callbackContext);
                return actionInstance.execute();
            }
        } catch (IllegalStateException e){
            callbackContext.error(e.getMessage());
        } catch (JSONException e){
            callbackContext.error(e.getMessage());
        }

		// Method not found
		return false;
	}

    public void startActivity() {
        this.cordova.setActivityResultCallback(this);
        activityOpen = true;
    }

    public void endActivity() {
        activityOpen = false;
    }

    private String getPublicKey() {
        int billingKeyFromParam = cordova.getActivity().getResources().getIdentifier("billing_key_param", "string", cordova.getActivity().getPackageName());

        if(billingKeyFromParam > 0) {
            return cordova.getActivity().getString(billingKeyFromParam);
        }

        int billingKey = cordova.getActivity().getResources().getIdentifier("billing_key", "string", cordova.getActivity().getPackageName());
        return cordova.getActivity().getString(billingKey);
    }

	// Initialize the plugin
	private void init(final List<String> skus, final CallbackContext callbackContext){
		Log.d(TAG, "init start");

        String base64EncodedPublicKey = getPublicKey();

	 	if (base64EncodedPublicKey.isEmpty())
	 		throw new RuntimeException("Please install the plugin supplying your Android license key. See README.");

	 	// Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(cordova.getActivity().getApplicationContext(), base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(ENABLE_DEBUG_LOGGING);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");

        final InAppBillingPlugin plugin = this;

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh no, there was a problem.
                    callbackContext.error("Problem setting up in-app billing: " + result);
                    return;
                }
                
                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) {
                	callbackContext.error("The billing helper has been disposed");
                }

                // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                initialized = true;

                 Action actionInstance = new Action(plugin, mHelper, callbackContext);
                if(skus.size() <= 0){
					Log.d(TAG, "Setup successful. Querying inventory.");
                	actionInstance.refreshPurchases();
				} else{
					Log.d(TAG, "Setup successful. Querying inventory w/ SKUs.");
                    actionInstance.refreshPurchases(skus);
				}


            }
        });
    }
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        this.endActivity();
        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }
    
    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	// very important:
    	Log.d(TAG, "Destroying helper.");
    	if (mHelper != null) {
    		mHelper.dispose();
    		mHelper = null;
    	}
    }
    
}
