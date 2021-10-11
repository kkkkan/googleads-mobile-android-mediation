package com.google.ads.mediation.snap;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.snap.adkit.external.AdKitAudienceAdsNetwork;
import com.snap.adkit.external.AdKitSlotType;
import com.snap.adkit.external.AudienceNetworkAdsApi;
import com.snap.adkit.external.LoadAdConfig;
import com.snap.adkit.external.LoadAdConfigBuilder;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdDismissed;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdImpressionHappened;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdKitSlot;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdVisible;

public class SnapInterstitialAd implements MediationInterstitialAd {
    private static final String SLOT_ID_KEY = "adSlotId";

    private MediationInterstitialAdConfiguration adConfiguration;
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationAdLoadCallback;
    private MediationInterstitialAdCallback mInterstitialAdCallback;
    private String mSlotId;

    @Nullable
    private final AudienceNetworkAdsApi adsNetworkApi = AdKitAudienceAdsNetwork.getAdsNetwork();

    public SnapInterstitialAd(MediationInterstitialAdConfiguration adConfiguration,
                              MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.mMediationAdLoadCallback = callback;
    }

    public void loadAd() {
        if (adsNetworkApi == null) {
            mMediationAdLoadCallback.onFailure(new AdError(0, "snap ad network not properly initialized", SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
        }
        Bundle serverParameters = adConfiguration.getServerParameters();
        mSlotId = serverParameters.getString(SLOT_ID_KEY);
        adsNetworkApi.setupListener(new SnapAdEventListener() {
            @Override
            public void onEvent(SnapAdKitEvent snapAdKitEvent, String slotId) {
                handleEvent(snapAdKitEvent);
            }
        });

        String bid = adConfiguration.getBidResponse();
        LoadAdConfig loadAdConfig = new LoadAdConfigBuilder()
                .withPublisherSlotId(mSlotId).withBid(bid).build();
        adsNetworkApi.loadInterstitial(loadAdConfig);
    }

    @Override
    public void showAd(Context context) {
        adsNetworkApi.playAd(new SnapAdKitSlot(mSlotId, AdKitSlotType.INTERSTITIAL));
    }

    private void handleEvent(SnapAdKitEvent snapAdKitEvent) {
        if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
            if (mMediationAdLoadCallback != null) {
                mInterstitialAdCallback = mMediationAdLoadCallback.onSuccess(this);
            }
        } else if (snapAdKitEvent instanceof SnapAdLoadFailed) {
            if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(
                        new AdError(0, "ad load fail", SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            }
        } else if (snapAdKitEvent instanceof SnapAdVisible) {
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.onAdOpened();
            }
        } else if (snapAdKitEvent instanceof SnapAdClicked) {
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.reportAdClicked();
            }
        } else if (snapAdKitEvent instanceof SnapAdImpressionHappened) {
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.reportAdImpression();
            }
        } else if (snapAdKitEvent instanceof SnapAdDismissed) {
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.onAdClosed();
            }
        }
    }
}
