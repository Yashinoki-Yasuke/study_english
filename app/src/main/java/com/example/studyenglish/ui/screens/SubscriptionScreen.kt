package com.example.studyenglish.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.studyenglish.BuildConfig
import com.example.studyenglish.billing.BillingManager
import com.example.studyenglish.ui.ads.BannerAd
import com.example.studyenglish.ui.ads.findActivity

private val BENEFITS = listOf(
    "オリジナル単語帳（CSVインポート）が使い放題",
    "広告がすべて非表示",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isPremium by BillingManager.isPremium.collectAsState()
    val productDetails by BillingManager.productDetails.collectAsState()

    val offer = productDetails?.subscriptionOfferDetails?.firstOrNull()
    val phases = offer?.pricingPhases?.pricingPhaseList.orEmpty()
    val recurringPrice = phases.lastOrNull()?.formattedPrice
    val hasTrial = phases.size > 1 && phases.first().priceAmountMicros == 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プレミアム会員") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
        bottomBar = { BannerAd() },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))

            if (isPremium) {
                Text("ご登録ありがとうございます！", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "プレミアム会員の特典をすべてご利用いただけます。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text("プレミアム会員になる", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "月額${recurringPrice ?: ""}" + if (hasTrial) "（初回7日間無料）" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    BENEFITS.forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(benefit, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            if (isPremium) {
                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse(
                            "https://play.google.com/store/account/subscriptions" +
                                "?sku=${BillingManager.PREMIUM_SUBSCRIPTION_ID}" +
                                "&package=${BuildConfig.APPLICATION_ID}"
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("サブスクリプションを管理") }
            } else {
                Button(
                    onClick = {
                        context.findActivity()?.let { BillingManager.launchPurchaseFlow(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = productDetails != null,
                ) { Text("登録する") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { BillingManager.refreshPurchases() }) {
                    Text("購入を復元する")
                }
            }
        }
    }
}
