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
 
        // Seed pregnancy articles and load
        seedPregnancyArticlesAndLoad()
    }

    private fun seedPregnancyArticlesAndLoad() {
        lifecycleScope.launch {
            // Seed pregnancy articles dynamically
            val seededCount = database.educationDao().getArticlesByPhase("PREGNANCY")
            // Wait, we can just insert them using REPLACE/IGNORE. Since it's REPLACE, it's safe to write directly
            val articles = listOf(
                EducationEntity(id = 101, title = "Nutrisi Utama Trimester Pertama", content = "Fokus pada makanan kaya zat besi, kalsium, dan terutama asam folat untuk mendukung pembentukan organ vital janin. Hindari daging mentah dan batasi kafein.", category = "Nutrisi Hamil", phaseRecom = "PREGNANCY"),
                EducationEntity(id = 102, title = "Aman Berolahraga Saat Hamil", content = "Latihan fisik ringan seperti jalan kaki dan prenatal yoga sangat aman dilakukan. Ini membantu melancarkan persalinan dan mengurangi nyeri punggung bawah.", category = "Kebugaran Hamil", phaseRecom = "PREGNANCY"),
                EducationEntity(id = 103, title = "Tips Mengatasi Morning Sickness", content = "Konsumsi jahe hangat, makan dalam porsi kecil tapi sering, serta minum air cukup. Istirahat yang teratur juga sangat membantu meredakan mual.", category = "Tips Kesehatan", phaseRecom = "PREGNANCY")
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
                "PREGNANCY"
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
            if (activePhase == "PREGNANCY") {
                tvFeaturedArticleBadge.text = "Untuk Masa Kehamilan Anda"
                tvFeaturedArticleTitle.text = "Cara Menjaga Kesehatan Ibu Hamil dan Janin Tetap Optimal"
            } else {
                val phaseFormatted = activePhase.lowercase().replaceFirstChar { it.uppercase() }
                tvFeaturedArticleBadge.text = "Untuk Fase $phaseFormatted Anda"
                tvFeaturedArticleTitle.text = "Bagaimana Perubahan Hormon Memengaruhi Aktivitas Anda"
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