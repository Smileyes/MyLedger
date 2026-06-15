package com.utopia.finance.data.repository

import com.utopia.finance.data.local.dao.ExchangeRateDao
import com.utopia.finance.data.local.entity.ExchangeRateEntity
import com.utopia.finance.domain.model.CurrencyCode
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class ExchangeRateSnapshot(
    val pair: String,
    val rate: BigDecimal,
    val asOfEpochDay: Long,
    val isCached: Boolean,
)

interface ExchangeRateRepository {
    suspend fun usdCny(): ExchangeRateSnapshot?
    suspend fun refreshUsdCny(): ExchangeRateSnapshot
    suspend fun convertToCny(amountMinor: Long, currency: CurrencyCode): Long
}

class EcbExchangeRateRepository(
    private val exchangeRateDao: ExchangeRateDao,
    private val client: OkHttpClient = OkHttpClient(),
) : ExchangeRateRepository {
    override suspend fun usdCny(): ExchangeRateSnapshot? {
        val cached = exchangeRateDao.get(PAIR_USD_CNY)
        return cached?.toSnapshot(isCached = true)
    }

    override suspend fun refreshUsdCny(): ExchangeRateSnapshot = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(ECB_DAILY_URL).build()
        val body = client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "ECB exchange-rate request failed: ${response.code}" }
            response.body.string()
        }
        val parsed = parseEcbRates(body.toByteArray())
        val usdPerEur = parsed.rates.getValue("USD")
        val cnyPerEur = parsed.rates.getValue("CNY")
        val usdCny = cnyPerEur.divide(usdPerEur, 8, RoundingMode.HALF_UP)
        val entity = ExchangeRateEntity(
            pair = PAIR_USD_CNY,
            rateScaled = usdCny.movePointRight(RATE_SCALE).setScale(0, RoundingMode.HALF_UP).longValueExact(),
            scale = RATE_SCALE,
            asOfEpochDay = parsed.date.toEpochDay(),
            fetchedAtMillis = System.currentTimeMillis(),
        )
        exchangeRateDao.upsert(entity)
        entity.toSnapshot(isCached = false)
    }

    override suspend fun convertToCny(amountMinor: Long, currency: CurrencyCode): Long {
        if (currency == CurrencyCode.CNY) return amountMinor
        if (currency == CurrencyCode.BTC) return amountMinor
        val rate = runCatching { refreshUsdCny() }.getOrElse { usdCny() }
            ?: return amountMinor
        return BigDecimal(amountMinor)
            .multiply(rate.rate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    private fun ExchangeRateEntity.toSnapshot(isCached: Boolean): ExchangeRateSnapshot =
        ExchangeRateSnapshot(
            pair = pair,
            rate = BigDecimal(rateScaled).movePointLeft(scale),
            asOfEpochDay = asOfEpochDay,
            isCached = isCached,
        )

    companion object {
        private const val ECB_DAILY_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
        private const val PAIR_USD_CNY = "USD_CNY"
        private const val RATE_SCALE = 8
    }
}

private data class EcbRates(val date: LocalDate, val rates: Map<String, BigDecimal>)

private fun parseEcbRates(bytes: ByteArray): EcbRates {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    val cubeNodes = document.getElementsByTagName("Cube")
    var date = LocalDate.now()
    val rates = mutableMapOf<String, BigDecimal>()
    for (index in 0 until cubeNodes.length) {
        val node = cubeNodes.item(index)
        val attrs = node.attributes ?: continue
        val time = attrs.getNamedItem("time")?.nodeValue
        if (time != null) date = LocalDate.parse(time)
        val currency = attrs.getNamedItem("currency")?.nodeValue
        val rate = attrs.getNamedItem("rate")?.nodeValue
        if (currency != null && rate != null) {
            rates[currency] = BigDecimal(rate)
        }
    }
    require("USD" in rates && "CNY" in rates) { "ECB data did not contain USD and CNY rates." }
    return EcbRates(date, rates)
}
