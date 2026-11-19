package com.example.routemate.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.routemate.R

object DestinationImageHelper {

    /**
     * Resolves the realistic scenic destination photograph based on destination name.
     * Safely handles different spellings, word combinations, and letter cases.
     */
    @DrawableRes
    fun getDestinationImageRes(destination: String?): Int {
        val dest = destination?.trim()?.lowercase() ?: ""

        return when {
            dest.contains("pondicherry") || dest.contains("puducherry") || dest.contains("mahabalipuram") -> R.drawable.dest_pondicherry
            dest.contains("ooty") || dest.contains("udhagamandalam") || dest.contains("nilgiri") || dest.contains("coonoor") || dest.contains("kodaikanal") || dest.contains("kodai") || dest.contains("yercaud") || dest.contains("kolli") -> R.drawable.dest_ooty
            dest.contains("coorg") || dest.contains("kodagu") || dest.contains("madikeri") || dest.contains("chikmagalur") || dest.contains("kudremukh") || dest.contains("sakleshpur") || dest.contains("agumbe") -> R.drawable.dest_coorg
            dest.contains("munnar") || dest.contains("idukki") || dest.contains("wayanad") || dest.contains("vagamon") || dest.contains("valparai") || dest.contains("ponmudi") -> R.drawable.dest_munnar
            dest.contains("ladakh") || dest.contains("leh") || dest.contains("pangong") || dest.contains("khardung") || dest.contains("umling") || dest.contains("chang la") || dest.contains("hanle") || dest.contains("tso moriri") || dest.contains("zanskar") || dest.contains("kargil") || dest.contains("drass") || dest.contains("sikkim") || dest.contains("gurudongmar") || dest.contains("nathula") || dest.contains("tawang") || dest.contains("sela pass") -> R.drawable.dest_leh_ladakh
            dest.contains("spiti") || dest.contains("kaza") || dest.contains("tabo") || dest.contains("manali") || dest.contains("atal tunnel") || dest.contains("sissu") || dest.contains("jispa") || dest.contains("keylong") || dest.contains("kunzum") || dest.contains("chandratal") || dest.contains("sach pass") || dest.contains("kinnaur") || dest.contains("chitkul") || dest.contains("chopta") || dest.contains("kedarnath") || dest.contains("badrinath") || dest.contains("auli") || dest.contains("harsil") || dest.contains("tungnath") || dest.contains("srinagar") || dest.contains("gulmarg") || dest.contains("sonamarg") || dest.contains("pahalgam") || dest.contains("gurez") -> R.drawable.dest_spiti_valley
            dest.contains("goa") || dest.contains("panaji") || dest.contains("calangute") || dest.contains("konkan") || dest.contains("tarkarli") || dest.contains("gokarna") -> R.drawable.dest_goa
            dest.contains("araku") || dest.contains("lambasingi") -> R.drawable.dest_araku
            dest.contains("yelagiri") || dest.contains("horsley") || dest.contains("nandi") -> R.drawable.dest_yelagiri
            else -> R.drawable.dest_leh_ladakh
        }
    }

    /**
     * Returns an attractive destination-specific gradient fallback when photograph is loading or unavailable.
     */
    fun getDestinationFallbackGradient(destination: String?, isDark: Boolean): Brush {
        val dest = destination?.trim()?.lowercase() ?: ""

        val colors = when {
            dest.contains("ladakh") || dest.contains("leh") -> listOf(
                Color(0xFF0F3057), Color(0xFF00587A), Color(0xFF008891)
            )
            dest.contains("spiti") -> listOf(
                Color(0xFF2C3E50), Color(0xFF4CA1AF), Color(0xFF2C3E50)
            )
            dest.contains("ooty") || dest.contains("munnar") -> listOf(
                Color(0xFF0A3A2A), Color(0xFF135E44), Color(0xFF1B8A6B)
            )
            dest.contains("coorg") || dest.contains("araku") -> listOf(
                Color(0xFF1B3B2B), Color(0xFF2E6347), Color(0xFF45926B)
            )
            dest.contains("goa") || dest.contains("pondicherry") -> listOf(
                Color(0xFF071A3C), Color(0xFF1B4F72), Color(0xFF117A65)
            )
            else -> listOf(
                Color(0xFF071A3C), Color(0xFF124D91), Color(0xFF20A6C9)
            )
        }

        return Brush.verticalGradient(colors)
    }

    /**
     * Short scenic tagline for the destination.
     */
    fun getDestinationTagline(destination: String?): String {
        val dest = destination?.trim()?.lowercase() ?: ""

        return when {
            dest.contains("pondicherry") || dest.contains("puducherry") -> "Coastal Beach & Seaside Promenade"
            dest.contains("ooty") || dest.contains("nilgiri") -> "Nilgiri Hills, Tea Estates & Mountain Mist"
            dest.contains("coorg") || dest.contains("kodagu") -> "Coffee Plantations, Waterfalls & Curves"
            dest.contains("munnar") -> "Tea Valleys, Cool Mountain Air & Vistas"
            dest.contains("ladakh") || dest.contains("leh") -> "Himalayan High Passes & Blue Skies"
            dest.contains("spiti") -> "Rugged Canyons & Snow-capped Peaks"
            dest.contains("goa") -> "Beaches, Coastal Highways & Sunsets"
            dest.contains("araku") -> "Eastern Ghats & Green Valley Passes"
            dest.contains("yelagiri") -> "Winding Hairpins & Green Landscapes"
            else -> "Scenic Motorcycle Touring Route"
        }
    }
}

