/*
 * Copyright (C) 2012-2013 by Guillaume Charhon
 * Modifications 10/16/2013 by Brian Thurlow
 */
var log = function (msg) {
    console.log("InAppBilling[js]: " + msg);
};

var InAppBilling = function () {
    this.options = {};
};

InAppBilling.prototype.init = function (success, fail, options, skus) {
	options || (options = {});

	this.options = {
		showLog: options.showLog !== false
	};
	
	if (this.options.showLog) {
		log('setup ok');
	}
	
	var hasSKUs = false;
	//Optional Load SKUs to Inventory.
	if(typeof skus !== "undefined"){
		if (typeof skus === "string") {
        	skus = [skus];
    	}
    	if (skus.length > 0) {
        	if (typeof skus[0] !== 'string') {
            	var msg = 'invalid productIds: ' + JSON.stringify(skus);
            	if (this.options.showLog) {
            		log(msg);
            	}
				fail(msg);
            	return;
        	}
        	if (this.options.showLog) {
        		log('load ' + JSON.stringify(skus));
        	}
			hasSKUs = true;
    	}
	}
	
	if(hasSKUs){
		return cordova.exec(success, fail, "InAppBillingPlugin", "init", [skus]);
    }else {
        //No SKUs
		return cordova.exec(success, fail, "InAppBillingPlugin", "init", []);
    }
};
InAppBilling.prototype.getPurchases = function (success, fail) {
	if (this.options.showLog) {
		log('getPurchases called!');
	}
	return cordova.exec(success, fail, "InAppBillingPlugin", "getPurchases", ["null"]);
};
InAppBilling.prototype.refreshPurchases = function (success, fail) {
	if (this.options.showLog) {
		log('refreshPurchases called!');
	}

	var self = this;
	var onSuccess = function() {
		self.getPurchases(function(purchases) {
			success(purchases);
		}, fail);
	};

	return cordova.exec(onSuccess, fail, "InAppBillingPlugin", "refreshPurchases", ["null"]);
};
InAppBilling.prototype.buy = function (success, fail, productId) {
	if (this.options.showLog) {
		log('buy called!');
	}
	return cordova.exec(success, fail, "InAppBillingPlugin", "buy", [productId]);
};
InAppBilling.prototype.subscribe = function (success, fail, productId) {
	if (this.options.showLog) {
		log('subscribe called!');
	}
	return cordova.exec(success, fail, "InAppBillingPlugin", "subscribe", [productId]);
};
InAppBilling.prototype.consumePurchase = function (success, fail, productId) {
	if (this.options.showLog) {
		log('consumePurchase called!');
	}
	return cordova.exec(success, fail, "InAppBillingPlugin", "consumePurchase", [productId]);
};
InAppBilling.prototype.getAvailableProducts = function (success, fail) {
	if (this.options.showLog) {
		log('getAvailableProducts called!');
	}
	return cordova.exec(success, fail, "InAppBillingPlugin", "getAvailableProducts", ["null"]);
};
InAppBilling.prototype.getProductDetails = function (success, fail, skus) {
	if (this.options.showLog) {
		log('getProductDetails called!');
	}
	
	if (typeof skus === "string") {
        skus = [skus];
    }
    if (!skus.length) {
        // Empty array, nothing to do.
        return;
    }else {
        if (typeof skus[0] !== 'string') {
            var msg = 'invalid productIds: ' + JSON.stringify(skus);
            log(msg);
			fail(msg);
            return;
        }
        if (this.options.showLog) {
        	log('load ' + JSON.stringify(skus));
        }
		return cordova.exec(success, fail, "InAppBillingPlugin", "getProductDetails", [skus]);
    }
};
InAppBilling.prototype.isPurchaseOpen = function (success, fail) {

	var onSuccess = function(state) {
		var bool = (state == "true") ? true : false;
		success(bool);
	};

	return cordova.exec(onSuccess, fail, "InAppBillingPlugin", "isPurchaseOpen", []);
}

module.exports = new InAppBilling();
