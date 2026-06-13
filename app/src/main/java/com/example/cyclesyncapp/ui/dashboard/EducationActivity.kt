package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.EducationEntity
import com.example.cyclesyncapp.domain.usecase.GetCyclePredictionUseCase
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.core.content.ContextCompat

class EducationActivity : AppCompatActivity() {

    private lateinit var database: CycleDatabase
    private lateinit var adapter: ArticleAdapter
    private val articlesList = mutableListOf<EducationEntity>()
    private val allArticlesList = mutableListOf<EducationEntity>()
    private var currentSelectedCategory: String = "Semua"
 
    private lateinit var tvFeaturedArticleBadge: TextView
    private lateinit var tvFeaturedArticleTitle: TextView
    private lateinit var cardEduTutorialGuide: androidx.cardview.widget.CardView
    private var isTutorialMode: Boolean = false
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_education)
 
        database = CycleDatabase.getDatabase(this)
 
        tvFeaturedArticleBadge = findViewById(R.id.tvFeaturedArticleBadge)
        tvFeaturedArticleTitle = findViewById(R.id.tvFeaturedArticleTitle)
        cardEduTutorialGuide = findViewById(R.id.cardEduTutorialGuide)
        cardEduTutorialGuide.visibility = View.GONE
 
        val rvArticles = findViewById<RecyclerView>(R.id.rvArticles)
        adapter = ArticleAdapter(articlesList) { article ->
            val intent = Intent(this, ArticleActivity::class.java).apply {
                putExtra("title", article.title)
                putExtra("category", article.category)
                putExtra("content", article.content)
                putExtra("phase", article.phaseRecom)
            }
            startActivity(intent)
        }
        rvArticles.adapter = adapter
 
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
 
        // Bind category filter tabs
        findViewById<TextView>(R.id.btnCatSemua)?.setOnClickListener { filterArticles("Semua") }
        findViewById<TextView>(R.id.btnCatHormon)?.setOnClickListener { filterArticles("Hormon") }
        findViewById<TextView>(R.id.btnCatNutrisi)?.setOnClickListener { filterArticles("Nutrisi") }
        findViewById<TextView>(R.id.btnCatOlahraga)?.setOnClickListener { filterArticles("Olahraga") }
 
        // Seed education articles and load
        seedArticlesAndLoad()
    }

    private fun seedArticlesAndLoad() {
        lifecycleScope.launch {
            // Seed pregnancy and cycle articles dynamically
            val articles = listOf(
                // Trimester 1
                EducationEntity(id = 101, title = "Nutrisi Utama Trimester Pertama", content = "Fokus pada makanan kaya zat besi, kalsium, dan terutama asam folat untuk mendukung pembentukan organ vital janin. Hindari daging mentah dan batasi kafein.", category = "Nutrisi Hamil", phaseRecom = "PREGNANCY_T1"),
                EducationEntity(id = 102, title = "Aman Berolahraga Saat Hamil (T1)", content = "Latihan fisik ringan seperti jalan kaki dan prenatal yoga sangat aman dilakukan. Ini membantu melancarkan persalinan dan mengurangi nyeri punggung bawah.", category = "Kebugaran Hamil", phaseRecom = "PREGNANCY_T1"),
                EducationEntity(id = 103, title = "Tips Mengatasi Morning Sickness", content = "Konsumsi jahe hangat, makan dalam porsi kecil tapi sering, serta minum air cukup. Istirahat yang teratur juga sangat membantu meredakan mual.", category = "Tips Kesehatan", phaseRecom = "PREGNANCY_T1"),
                
                // Trimester 2
                EducationEntity(id = 201, title = "Zat Besi & Kalsium di Trimester Kedua", content = "Di trimester kedua, volume darah ibu meningkat pesat. Konsumsi makanan kaya zat besi (seperti daging merah) and kalsium (susu/yoghurt) untuk pertumbuhan tulang janin.", category = "Nutrisi Hamil", phaseRecom = "PREGNANCY_T2"),
                EducationEntity(id = 202, title = "Berenang & Yoga di Trimester Kedua", content = "Berenang adalah olahraga terbaik di trimester kedua karena menopang berat badan Anda sepenuhnya. Prenatal yoga juga disarankan untuk peregangan otot panggul.", category = "Kebugaran Hamil", phaseRecom = "PREGNANCY_T2"),
                EducationEntity(id = 203, title = "Mengatasi Kram Kaki Trimester Kedua", content = "Seringlah melakukan peregangan betis sebelum tidur, cukupi asupan cairan, dan tingkatkan asupan kalsium/magnesium untuk meredakan kram kaki malam hari.", category = "Tips Kesehatan", phaseRecom = "PREGNANCY_T2"),
                
                // Trimester 3
                EducationEntity(id = 301, title = "Nutrisi Menjelang Lahiran (Trimester Ketiga)", content = "Trimester ketiga membutuhkan kalori padat gizi. Konsumsi makanan berserat tinggi untuk mencegah sembelit, serta Omega-3 (Salmon) untuk pematangan otak bayi.", category = "Nutrisi Hamil", phaseRecom = "PREGNANCY_T3"),
                EducationEntity(id = 302, title = "Senam Hamil & Gym Ball (T3)", content = "Lakukan senam hamil terpandu atau aktiflah duduk di atas gym ball (birthing ball) untuk membantu melenturkan otot jalan lahir dan mempermudah posisi turunnya kepala bayi.", category = "Kebugaran Hamil", phaseRecom = "PREGNANCY_T3"),
                EducationEntity(id = 303, title = "Persiapan Mental & Fisik Persalinan", content = "Latihlah teknik pernapasan persalinan secara teratur, siapkan tas bersalin Anda lebih awal, dan diskusikan rencana persalinan dengan bidan atau dokter kandungan.", category = "Tips Kesehatan", phaseRecom = "PREGNANCY_T3"),

                // Menstruasi
                EducationEntity(id = 401, title = "Manajemen Kram & Nyeri Menstruasi", content = "Lakukan yoga ringan, kompres air hangat pada perut, dan konsumsi jahe atau makanan kaya zat besi untuk mengganti kehilangan darah. Hindari makanan pedas dan kafein.", category = "Kesehatan Hormon", phaseRecom = "MENSTRUATION"),
                EducationEntity(id = 402, title = "Nutrisi Terbaik Saat Menstruasi", content = "Fokus pada asupan zat besi heme dari daging sapi/ayam atau non-heme dari bayam/tempe. Vitamin C membantu penyerapannya. Cukupi hidrasi air hangat.", category = "Nutrisi", phaseRecom = "MENSTRUATION"),

                // Folikuler
                EducationEntity(id = 501, title = "Memaksimalkan Energi di Fase Folikuler", content = "Estrogen mulai meningkat, memberi energi tambahan. Gunakan momen ini untuk latihan beban (strength training) dan konsumsi telur (kolin) serta brokoli.", category = "Kebugaran", phaseRecom = "FOLLICULAR"),
                EducationEntity(id = 502, title = "Mendukung Kesehatan Sel Telur", content = "Konsumsi biji flaxseed dan wijen (lignan) untuk menyeimbangkan estrogen. Antioksidan dari beri-berian melindungi sel dari stres oksidatif.", category = "Nutrisi", phaseRecom = "FOLLICULAR"),

                // Ovulasi
                EducationEntity(id = 601, title = "Meningkatkan Kesuburan di Masa Subur", content = "Sel telur dilepaskan. Dukung dengan lemak sehat dari alpukat dan omega-3 dari ikan salmon. Zinc dan selenium membantu kualitas sel telur.", category = "Nutrisi", phaseRecom = "OVULATION"),
                EducationEntity(id = 602, title = "Latihan Intensitas Tinggi (HIIT) Saat Ovulasi", content = "Estrogen dan LH berada di puncak. Fisik Anda berada dalam kondisi prima untuk latihan HIIT atau lari jarak jauh. Jaga cairan tubuh.", category = "Kebugaran", phaseRecom = "OVULATION"),

                // Luteal
                EducationEntity(id = 701, title = "Mengatasi Gejala PMS di Fase Luteal", content = "Progesteron mendominasi. Konsumsi magnesium dari dark chocolate dan kalsium/vitamin D dari produk susu untuk menstabilkan mood swing dan kram.", category = "Kesehatan Hormon", phaseRecom = "LUTEAL"),
                EducationEntity(id = 702, title = "Latihan Terkontrol: Pilates & Yoga Luteal", content = "Kurangi intensitas latihan berat. Beralihlah ke pilates atau yoga untuk menjaga core tubuh tetap stabil tanpa memicu stres hormonal berlebih.", category = "Kebugaran", phaseRecom = "LUTEAL")
            )
            for (art in articles) {
                database.educationDao().insertArticle(art)
            }
            
            loadArticles()
        }
    }

    private fun loadArticles() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            val activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
            val user = database.userDao().getUserByEmail(activeEmail) ?: database.userDao().getUser()
            
            val isPregnant = user?.isPregnant ?: false

            val activePhase = if (isPregnant) {
                val latestCycle = database.cycleDao().getLatestCycle()
                var trimester = 1
                if (latestCycle != null) {
                    val startParts = latestCycle.startDate.split("-")
                    if (startParts.size == 3) {
                        val today = Calendar.getInstance()
                        val lmp = Calendar.getInstance().apply {
                            set(startParts[0].toInt(), startParts[1].toInt() - 1, startParts[2].toInt())
                        }
                        val diffInMillis = today.timeInMillis - lmp.timeInMillis
                        val totalDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                        val weeks = totalDays / 7
                        trimester = when {
                            weeks <= 12 -> 1
                            weeks <= 27 -> 2
                            else -> 3
                        }
                    }
                }
                "PREGNANCY_T$trimester"
            } else {
                val latestCycle = database.cycleDao().getLatestCycle()
                val periodLogs = database.dailyLogDao().getPeriodLogs()
                
                val periodStarts = mutableListOf<Long>()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                
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
                val prediction = GetCyclePredictionUseCase().execute(uniquePeriodStarts, false)
                prediction.currentPhase.name
            }

            // Customize Featured Banner programmatically
            if (activePhase.startsWith("PREGNANCY_T")) {
                val tNum = activePhase.substringAfter("PREGNANCY_T")
                tvFeaturedArticleBadge.text = "Untuk Trimester $tNum Anda"
                tvFeaturedArticleTitle.text = when(tNum) {
                    "1" -> "Cara Mengatasi Morning Sickness & Nutrisi Trimester Pertama"
                    "2" -> "Aktivitas Fisik & Asupan Nutrisi Optimal Trimester Kedua"
                    else -> "Mempersiapkan Persalinan & Kebugaran Trimester Ketiga"
                }
            } else {
                val phaseFormatted = when (activePhase) {
                    "MENSTRUATION" -> "Menstruasi"
                    "FOLLICULAR" -> "Folikuler"
                    "OVULATION" -> "Ovulasi"
                    "LUTEAL" -> "Luteal"
                    else -> activePhase.lowercase().replaceFirstChar { it.uppercase() }
                }
                tvFeaturedArticleBadge.text = "Untuk Fase $phaseFormatted Anda"
                tvFeaturedArticleTitle.text = when (activePhase) {
                    "MENSTRUATION" -> "Manajemen Kram Menstruasi & Nutrisi Zat Besi"
                    "FOLLICULAR" -> "Memaksimalkan Energi & Kesehatan Sel Telur"
                    "OVULATION" -> "Tips Meningkatkan Kesuburan di Masa Subur"
                    "LUTEAL" -> "Mengurangi Gejala PMS & Latihan Terkontrol"
                    else -> "Bagaimana Perubahan Hormon Memengaruhi Aktivitas Anda"
                }
            }

            // Muat artikel sesuai dengan fase aktif
            database.educationDao().getArticlesByPhase(activePhase).collect { articles ->
                allArticlesList.clear()
                if (articles.isNotEmpty()) {
                    allArticlesList.addAll(articles)
                    filterArticles(currentSelectedCategory)
                } else {
                    // Fallback jika kosong, ambil semua artikel
                    database.educationDao().getAllArticles().collect { allArticles ->
                        allArticlesList.clear()
                        allArticlesList.addAll(allArticles)
                        filterArticles(currentSelectedCategory)
                    }
                }
            }
        }
    }

    inner class ArticleAdapter(
        private val list: List<EducationEntity>,
        private val onClick: (EducationEntity) -> Unit
    ) : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
            val tvBenefit: TextView = view.findViewById(R.id.tvBenefit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recommendation, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val article = list[position]
            holder.tvCategory.text = article.category ?: "Edukasi"
            holder.tvTitle.text = article.title ?: ""
            holder.tvDescription.text = if ((article.content?.length ?: 0) > 80) {
                "${article.content?.substring(0, 80)}..."
            } else {
                article.content ?: ""
            }
            holder.tvBenefit.text = "Fase Rekomendasi: ${article.phaseRecom ?: "Umum"}"
            holder.itemView.setOnClickListener { onClick(article) }
        }

        override fun getItemCount(): Int = list.size
    }
 
    private fun filterArticles(category: String) {
        currentSelectedCategory = category
        val categoryLower = category.lowercase()
        val filtered = if (categoryLower == "semua") {
            allArticlesList
        } else {
            allArticlesList.filter { article ->
                val cat = (article.category ?: "").lowercase()
                val title = (article.title ?: "").lowercase()
                
                if (categoryLower == "hormon") {
                    cat.contains("hormon") || cat.contains("siklus") || title.contains("hormon") || title.contains("siklus")
                } else if (categoryLower == "nutrisi") {
                    cat.contains("nutrisi") || cat.contains("gizi") || cat.contains("makan") || cat.contains("food") ||
                            title.contains("nutrisi") || title.contains("gizi") || title.contains("makan") || title.contains("food")
                } else if (categoryLower == "olahraga") {
                    cat.contains("olahraga") || cat.contains("kebugaran") || cat.contains("latihan") || cat.contains("senam") || cat.contains("yoga") || cat.contains("exercise") ||
                            title.contains("olahraga") || title.contains("kebugaran") || title.contains("latihan") || title.contains("senam") || title.contains("yoga") || title.contains("exercise")
                } else {
                    cat.contains(categoryLower) || title.contains(categoryLower)
                }
            }
        }
        articlesList.clear()
        articlesList.addAll(filtered)
        adapter.notifyDataSetChanged()
 
        updateCategoryTabs(category)
    }
 
    private fun updateCategoryTabs(selectedCat: String) {
        val btnCatSemua = findViewById<TextView>(R.id.btnCatSemua)
        val btnCatHormon = findViewById<TextView>(R.id.btnCatHormon)
        val btnCatNutrisi = findViewById<TextView>(R.id.btnCatNutrisi)
        val btnCatOlahraga = findViewById<TextView>(R.id.btnCatOlahraga)
 
        val tabs = mapOf(
            "Semua" to btnCatSemua,
            "Hormon" to btnCatHormon,
            "Nutrisi" to btnCatNutrisi,
            "Olahraga" to btnCatOlahraga
        )
 
        tabs.forEach { (name, button) ->
            if (button != null) {
                if (name == selectedCat) {
                    button.setBackgroundResource(R.drawable.bg_pill_pink)
                    button.setTextColor(ContextCompat.getColor(this, R.color.white))
                    button.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    button.setBackgroundResource(R.drawable.bg_card)
                    button.setTextColor(ContextCompat.getColor(this, R.color.primary_muted))
                    button.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }
        }
    }
}