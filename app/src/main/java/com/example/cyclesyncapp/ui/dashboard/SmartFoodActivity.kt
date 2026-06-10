package com.example.cyclesyncapp.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.databinding.ActivitySmartFoodBinding
import com.example.cyclesyncapp.ui.adapter.SfRecommendationAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class SmartFoodActivity : AppCompatActivity() {

    data class RecommendationItem(
        val name: String,
        val reason: String,
        val nutrients: List<String>,
        val badgeText: String,
        val badgeColorType: String
    )

    data class PhaseDetails(
        val name: String,
        val description: String,
        val foods: List<RecommendationItem>,
        val exercises: List<RecommendationItem>,
        val supplements: List<RecommendationItem>,
        val avoids: List<String>,
        val references: List<String>
    )

    private lateinit var binding: ActivitySmartFoodBinding
    private lateinit var database: CycleDatabase
    private val adapter = SfRecommendationAdapter()

    private var currentTabPosition = 0 // 0 = Nutrisi (FOOD), 1 = Olahraga (EXERCISE), 2 = Suplemen (SUPPLEMENT)
    private lateinit var currentPhaseDetails: PhaseDetails
    private var isTutorialMode: Boolean = false

    private val menstruationDetails = PhaseDetails(
        name = "Fase Menstruasi",
        description = "Fase Menstruasi: Tubuh kehilangan zat besi — fokus pada replenishment energi & kurangi peradangan 🩸",
        foods = listOf(
            RecommendationItem("Daging Merah Lean / Hati Ayam", "Zat besi heme — paling mudah diserap tubuh untuk ganti darah yang hilang", listOf("Zat Besi", "Protein"), "Prioritas Utama", "BLUE"),
            RecommendationItem("Jahe Hangat", "Prostaglandin inhibitor alami — terbukti kurangi nyeri kram secara signifikan", listOf("Anti-inflamasi", "Anti-kram"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Pisang", "Kalium & vitamin B6 — kurangi kembung dan bantu mood tetap stabil", listOf("Kalium", "Vit B6"), "Direkomendasikan", "GREEN"),
            RecommendationItem("Tahu & Tempe", "Protein nabati + zat besi non-heme, bagus untuk yang tidak makan daging", listOf("Zat Besi", "Protein"), "Direkomendasikan", "GREEN")
        ),
        exercises = listOf(
            RecommendationItem("Yoga Ringan", "Meningkatkan sirkulasi darah ke area panggul tanpa memicu ketegangan otot", listOf("Relaksasi", "Fleksibilitas"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Peregangan / Stretching", "Mengurangi kaku otot di punggung bawah dan meredakan nyeri menstruasi", listOf("Anti-Kram"), "Direkomendasikan", "GREEN"),
            RecommendationItem("Jalan Santai", "Aktivitas aerobik ringan untuk menstimulasi pelepasan hormon endorfin", listOf("Stamina"), "Direkomendasikan", "GREEN")
        ),
        supplements = listOf(
            RecommendationItem("Tablet Tambah Darah", "Mencegah anemia akibat volume darah yang keluar selama menstruasi", listOf("Zat Besi"), "Prioritas Utama", "BLUE"),
            RecommendationItem("Vitamin C", "Meningkatkan bioavailabilitas dan penyerapan zat besi dari tahu/tempe/bayam", listOf("Vit C"), "⭐ Terbaik", "GOLD")
        ),
        avoids = listOf(
            "Makanan pedas berlebihan — memperburuk kram dan iritasi lambung",
            "Alkohol — meningkatkan prostaglandin, memperparah nyeri",
            "Makanan dingin/es — menurut tradisi & beberapa studi memperburuk kram"
        ),
        references = listOf(
            "Deutch et al. (2019) - Dietary iron and menstrual blood loss"
        )
    )

    private val follicularDetails = PhaseDetails(
        name = "Fase Folikuler",
        description = "Fase Folikuler: Estrogen mulai naik — waktu terbaik untuk nutrisi yang mendukung energi & kesehatan sel telur 🌱",
        foods = listOf(
            RecommendationItem("Telur", "Kolin & protein lengkap — mendukung perkembangan folikel dan keseimbangan hormon", listOf("Kolin", "Protein", "Vit D"), "Prioritas Utama", "BLUE"),
            RecommendationItem("Biji Flaxseed / Biji Wijen", "Lignan & omega-3 — mendukung keseimbangan estrogen secara alami", listOf("Omega-3", "Lignan"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Brokoli & Sayuran Cruciferous", "DIM (diindolylmethane) — membantu metabolisme estrogen yang sehat di hati", listOf("DIM", "Vit C", "Folat"), "Direkomendasikan", "GREEN"),
            RecommendationItem("Buah Beri (Strawberry, Blueberry)", "Antioksidan tinggi — proteksi sel dari stres oksidatif saat folikel berkembang", listOf("Antioksidan", "Vit C"), "Direkomendasikan", "GREEN")
        ),
        exercises = listOf(
            RecommendationItem("Kardio Sedang", "Meningkatkan denyut jantung secara optimal dengan tingkat energi yang pulih", listOf("Kardio", "Stamina"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Latihan Beban (Strength)", "Estrogen tinggi membantu pemulihan dan pembentukan serat otot baru", listOf("Kekuatan", "Tonus"), "Prioritas Utama", "BLUE")
        ),
        supplements = listOf(
            RecommendationItem("Vitamin B Kompleks", "Membantu tubuh mengonversi asupan zat gizi menjadi energi seluler aktif", listOf("Vit B"), "⭐ Terbaik", "GOLD")
        ),
        avoids = listOf(
            "Alkohol berlebihan — mengganggu metabolisme estrogen di hati",
            "Gula rafinasi tinggi — menyebabkan fluktuasi hormon insulin"
        ),
        references = listOf(
            "Gaskins et al. (2018) - Diet and ovarian reserve"
        )
    )

    private val ovulationDetails = PhaseDetails(
        name = "Fase Ovulasi",
        description = "Fase Ovulasi: Estrogen & LH memuncak — dukung fertilitas & kurangi inflamasi saat pelepasan sel telur 🥚",
        foods = listOf(
            RecommendationItem("Salmon & Ikan Berlemak", "Omega-3 DHA — mendukung kualitas sel telur & kurangi inflamasi ovulasi", listOf("Omega-3", "Vit D", "Protein"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Alpukat", "Lemak sehat & vitamin E — antioksidan penting untuk kesehatan sel telur", listOf("Vit E", "Lemak Sehat", "Folat"), "Prioritas Utama", "BLUE"),
            RecommendationItem("Quinoa / Biji-bijian Utuh", "Zinc & selenium — mineral penting untuk proses ovulasi yang normal", listOf("Zinc", "Selenium", "Serat"), "Direkomendasikan", "GREEN"),
            RecommendationItem("Tomat & Paprika Merah", "Likopen & vitamin C — antioksidan yang melindungi sel telur dari kerusakan", listOf("Likopen", "Vit C"), "Direkomendasikan", "GREEN")
        ),
        exercises = listOf(
            RecommendationItem("HIIT (High-Intensity)", "Tingkat energi berada pada puncaknya, optimalkan daya ledak fisik", listOf("HIIT", "Pembakaran Lemak"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Lari / Running", "Aktivitas ketahanan kardiovaskular tinggi sangat didukung performa puncak LH", listOf("Daya Tahan"), "Direkomendasikan", "GREEN")
        ),
        supplements = listOf(
            RecommendationItem("Zinc & Selenium", "Membantu kelancaran ovulasi dan pembelahan sel sel telur yang sehat", listOf("Zinc", "Selenium"), "⭐ Terbaik", "GOLD")
        ),
        avoids = listOf(
            "Trans fat / gorengan — mengganggu sinyal hormon ovulasi",
            "Kafein berlebihan — dikaitkan dengan penurunan kualitas sel telur"
        ),
        references = listOf(
            "Chavarro et al. (2018) - Omega-3 and ovulatory function"
        )
    )

    private val lutealDetails = PhaseDetails(
        name = "Fase Luteal",
        description = "Fase Luteal: Progesteron tinggi — konsumsi anti-inflamasi & mineral untuk stabilkan mood & kurangi gejala PMS 🌙",
        foods = listOf(
            RecommendationItem("Susu & Produk Susu", "Kalsium tinggi terbukti mengurangi nyeri menstruasi & stabilkan mood PMS", listOf("Kalsium", "Vit D"), "Direkomendasikan", "GREEN"),
            RecommendationItem("Kunyit (Curcumin)", "Anti-inflamasi alami, meredakan dismenore dan peradangan saat haid", listOf("Anti-inflamasi"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Sayuran Hijau Gelap (Bayam, Kangkung)", "Zat besi & magnesium — kurangi kram otot & boost energi", listOf("Zat Besi", "Magnesium"), "Direkomendasikan", "GREEN"),
            RecommendationItem("Dark Chocolate >70%", "Magnesium & serotonin booster alami untuk mood swing fase luteal", listOf("Magnesium", "Mood Booster"), "Direkomendasikan", "GREEN")
        ),
        exercises = listOf(
            RecommendationItem("Pilates", "Melatih core dan kestabilan otot dengan gerakan yang terkontrol", listOf("Core", "Postur"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Angkat Beban Ringan", "Menjaga massa otot tanpa membuat sistem pernapasan terlalu stres", listOf("Kekuatan Ringan"), "Direkomendasikan", "GREEN")
        ),
        supplements = listOf(
            RecommendationItem("Kalsium & Vitamin D", "Membantu mengurangi intensitas kelelahan fisik dan mood swing PMS", listOf("Kalsium", "Vit D"), "⭐ Terbaik", "GOLD")
        ),
        avoids = listOf(
            "Kafein berlebihan — memperburuk kecemasan & kram PMS",
            "Makanan asin/processed — menyebababkan kembung & retensi air"
        ),
        references = listOf(
            "Bertone-Johnson et al. (2005) - Calcium and PMS symptoms, AJCN",
            "Hewlings & Kalman (2017) - Curcumin effects, Foods journal"
        )
    )

    private val pregnancyDetails = PhaseDetails(
        name = "Fase Kehamilan",
        description = "Fase Kehamilan: Nutrisi optimal untuk perkembangan janin & kesehatan ibu hamil 🤰",
        foods = listOf(
            RecommendationItem("Sayuran Hijau Gelap", "Tinggi folat untuk mencegah cacat tabung saraf janin", listOf("Folat", "Zat Besi"), "Prioritas Utama", "BLUE"),
            RecommendationItem("Telur & Ayam Matang Sempurna", "Protein berkualitas tinggi dan kolin untuk perkembangan otak janin", listOf("Protein", "Kolin"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Susu Kehamilan / Yogurt", "Kalsium untuk pembentukan tulang dan gigi janin", listOf("Kalsium", "Vit D"), "Direkomendasikan", "GREEN")
        ),
        exercises = listOf(
            RecommendationItem("Yoga Prenatal", "Melatih pernapasan dan kelenturan panggul untuk persiapan persalinan", listOf("Pernapasan", "Relaksasi"), "⭐ Terbaik", "GOLD"),
            RecommendationItem("Jalan Santai", "Menjaga stamina kardio ibu hamil dengan risiko cedera minimal", listOf("Stamina"), "Direkomendasikan", "GREEN")
        ),
        supplements = listOf(
            RecommendationItem("Asam Folat & Zat Besi Bumil", "Mendukung pembentukan sel darah merah dan mencegah anemia bumil", listOf("Folat", "Zat Besi"), "⭐ Terbaik", "GOLD")
        ),
        avoids = listOf(
            "Makanan mentah atau setengah matang (sushi, steak rare) — risiko infeksi bakteri/parasit",
            "Kafein tinggi (kopi/teh pekat) — dapat memengaruhi berat badan janin"
        ),
        references = listOf(
            "WHO Guidelines (2018) - Nutritional interventions during pregnancy"
        )
    )

    private val genericDetails = PhaseDetails(
        name = "Gizi Seimbang Harian",
        description = "Silakan catat tanggal haid Anda untuk mendapatkan rekomendasi gizi yang disesuaikan dengan fase hormonal. 🌸",
        foods = listOf(
            RecommendationItem("Makanan Gizi Seimbang", "Konsumsi karbohidrat kompleks, protein serat, dan lemak sehat", listOf("Makronutrisi"), "Umum", "BLUE"),
            RecommendationItem("Air Putih", "Hidrasi minimal 2 Liter per hari untuk menjaga metabolisme tubuh", listOf("Hidrasi"), "Penting", "GOLD")
        ),
        exercises = listOf(
            RecommendationItem("Jalan Kaki / Cardio Ringan", "Melatih kekuatan jantung dan melancarkan metabolisme tubuh", listOf("Kardio"), "Umum", "GREEN")
        ),
        supplements = listOf(
            RecommendationItem("Multivitamin", "Melengkapi kebutuhan vitamin dan mineral harian tubuh", listOf("Vitamin"), "Penting", "GOLD")
        ),
        avoids = listOf(
            "Gula rafinasi berlebihan (minuman manis kemasan)",
            "Makanan ultra-proses (junk food)"
        ),
        references = listOf(
            "Panduan Gizi Seimbang Kemenkes RI"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmartFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = CycleDatabase.getDatabase(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvSfContent.adapter = adapter
        binding.rvSfContent.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
        val onboardingStep = prefs.getInt("onboarding_step", 0)
        isTutorialMode = onboardingStep == 1
        if (isTutorialMode) {
            binding.cardFoodTutorialGuide.visibility = View.VISIBLE
        } else {
            binding.cardFoodTutorialGuide.visibility = View.GONE
        }

        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        // Refresh phase detection every time screen is opened
        detectPhaseAndLoadRecommendations()
    }

    private fun detectPhaseAndLoadRecommendations() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            val activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
            val user = database.userDao().getUserByEmail(activeEmail) ?: database.userDao().getUser()

            val isPregnant = user?.isPregnant ?: false

            if (isPregnant) {
                currentPhaseDetails = pregnancyDetails
                updateUIForPhase("PREGNANCY", 0)
                return@launch
            }

            // Load cycle info to detect phase
            val latestCycle = database.cycleDao().getLatestCycle()
            val periodLogs = database.dailyLogDao().getPeriodLogs()
            
            val periodStarts = mutableListOf<Long>()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            if (latestCycle != null && latestCycle.notes != "UNKNOWN_LMP") {
                try {
                    sdf.parse(latestCycle.startDate)?.time?.let { periodStarts.add(it) }
                } catch (e: Exception) {}
            }
            
            if (periodLogs.isNotEmpty()) {
                val sortedLogs = periodLogs.sortedBy { it.date }
                val logTimes = sortedLogs.mapNotNull { log ->
                    try { sdf.parse(log.date)?.time } catch (e: Exception) { null }
                }
                for (i in logTimes.indices) {
                    if (i == 0 || logTimes[i] > logTimes[i - 1] + (1.5 * 24.0 * 60.0 * 60.0 * 1000.0).toLong()) {
                        periodStarts.add(logTimes[i])
                    }
                }
            }

            val uniquePeriodStarts = periodStarts.distinct().sorted()

            if (uniquePeriodStarts.isEmpty()) {
                // No cycle data yet, show generic healthy tips
                currentPhaseDetails = genericDetails
                updateUIForPhase("GENERIC", 0)
                return@launch
            }

            val result = com.example.cyclesyncapp.domain.usecase.GetCyclePredictionUseCase().execute(uniquePeriodStarts, isPregnant)

            currentPhaseDetails = when (result.currentPhase) {
                com.example.cyclesyncapp.domain.model.HormonalPhase.MENSTRUATION -> menstruationDetails
                com.example.cyclesyncapp.domain.model.HormonalPhase.FOLLICULAR -> follicularDetails
                com.example.cyclesyncapp.domain.model.HormonalPhase.OVULATION -> ovulationDetails
                com.example.cyclesyncapp.domain.model.HormonalPhase.LUTEAL -> lutealDetails
                else -> follicularDetails
            }

            updateUIForPhase(result.currentPhase.name, result.dayOfCycle)
        }
    }

    private fun updateUIForPhase(phaseStr: String, dayNumber: Int) {
        // Update header views
        binding.tvPhaseTitle.text = currentPhaseDetails.name
        binding.tvPhaseDescription.text = currentPhaseDetails.description

        if (phaseStr == "PREGNANCY") {
            binding.tvPhaseBadge.text = "🤰 Kehamilan"
            binding.tvPhaseDayIndicator.text = "Mode Pemantauan Kehamilan"
            binding.tvSubHeader.text = "Nutrisi & Olahraga Ibu Hamil"
        } else if (phaseStr == "GENERIC") {
            binding.tvPhaseBadge.text = "🌸 Umum"
            binding.tvPhaseDayIndicator.text = "Belum ada tanggal haid tercatat"
            binding.tvSubHeader.text = "Tips Hidup Sehat Umum"
        } else {
            binding.tvPhaseBadge.text = "🌸 ${currentPhaseDetails.name.replace("Fase ", "")}"
            binding.tvPhaseDayIndicator.text = "Hari ke-$dayNumber dari siklus kamu"
            binding.tvSubHeader.text = "Disesuaikan ${currentPhaseDetails.name}"
        }

        // Render Avoids Card
        binding.layoutAvoidItems.removeAllViews()
        val avoids = currentPhaseDetails.avoids
        if (avoids.isEmpty()) {
            binding.cardAvoid.visibility = View.GONE
        } else {
            binding.cardAvoid.visibility = View.VISIBLE
            for (avoidItem in avoids) {
                val tv = TextView(this).apply {
                    text = "• $avoidItem"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setTextColor(Color.parseColor("#E53E3E"))
                    setPadding(0, 8, 0, 8)
                }
                binding.layoutAvoidItems.addView(tv)
            }
        }

        // Setup References Button Click
        binding.btnViewReferences.setOnClickListener {
            showReferencesBottomSheet(currentPhaseDetails)
        }

        // Load content for active tab
        loadActiveTabRecommendations()
    }

    private fun loadActiveTabRecommendations() {
        if (!::currentPhaseDetails.isInitialized) return

        val items = when (currentTabPosition) {
            0 -> {
                binding.tvRecSectionTitle.text = "Rekomendasi Nutrisi"
                binding.tvAvoidTitle.text = "⚠️ Pantangan Nutrisi Fase Ini"
                currentPhaseDetails.foods
            }
            1 -> {
                binding.tvRecSectionTitle.text = "Rekomendasi Olahraga"
                binding.tvAvoidTitle.text = "⚠️ Catatan Olahraga Fase Ini"
                currentPhaseDetails.exercises
            }
            2 -> {
                binding.tvRecSectionTitle.text = "Rekomendasi Suplemen"
                binding.tvAvoidTitle.text = "⚠️ Catatan Suplemen Fase Ini"
                currentPhaseDetails.supplements
            }
            else -> currentPhaseDetails.foods
        }
        adapter.submitList(items)
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                loadActiveTabRecommendations()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showReferencesBottomSheet(details: PhaseDetails) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.layout_references_bottom_sheet, null)
        
        val tvTitle = sheetView.findViewById<TextView>(R.id.tvSheetTitle)
        val tvSubtitle = sheetView.findViewById<TextView>(R.id.tvSheetSubtitle)
        val layoutRefs = sheetView.findViewById<LinearLayout>(R.id.layoutSheetRefs)
        val btnClose = sheetView.findViewById<android.widget.Button>(R.id.btnSheetClose)
        
        tvTitle.text = "Referensi Ilmiah"
        tvSubtitle.text = "Dasar medis rekomendasi untuk ${details.name}"
        
        layoutRefs.removeAllViews()
        for (ref in details.references) {
            val tv = TextView(this).apply {
                text = "📚 $ref"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(ContextCompat.getColor(context, R.color.pt))
                setPadding(0, 10, 0, 10)
            }
            layoutRefs.addView(tv)
        }
        
        btnClose.setOnClickListener { dialog.dismiss() }
        
        dialog.setContentView(sheetView)
        dialog.show()
    }
}