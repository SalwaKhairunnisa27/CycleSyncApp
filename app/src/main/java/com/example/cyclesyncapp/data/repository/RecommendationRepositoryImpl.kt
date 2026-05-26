package com.example.cyclesyncapp.data.repository

import com.example.cyclesyncapp.data.local.dao.RecommendationDao
import com.example.cyclesyncapp.data.local.entity.RecommendationEntity
import com.example.cyclesyncapp.domain.model.HormonalPhase
import com.example.cyclesyncapp.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow

class RecommendationRepositoryImpl(
    private val dao: RecommendationDao
) : RecommendationRepository {

    override fun getRecommendationsByPhase(phase: String): Flow<List<RecommendationEntity>> {
        return dao.getAllRecommendationsByPhase(phase)
    }

    override suspend fun insertRecommendations(recommendations: List<RecommendationEntity>) {
        dao.insertRecommendations(recommendations)
    }

    override suspend fun clearRecommendations() {
        dao.clearRecommendations()
    }

    /**
     * Mengisi database dengan data rekomendasi awal berdasarkan spek CycleSync.
     * Menggunakan Enum HormonalPhase agar sinkron dengan UseCase.
     */
    override suspend fun seedDatabase() {
        val initialData = listOf(
            // --- 1. MENSTRUATION PHASE ---
            RecommendationEntity(
                phase = HormonalPhase.MENSTRUATION.name,
                category = "Nutrisi",
                title = "Makanan Kaya Zat Besi & Hangat",
                description = "Bayam, daging merah tanpa lemak, kunyit, dan asupan Kalsium/Vitamin D.",
                benefit = "Membantu mengganti zat besi yang hilang dan meredakan kram perut.",
                type = "FOOD"
            ),
            RecommendationEntity(
                phase = HormonalPhase.MENSTRUATION.name,
                category = "Olahraga",
                title = "Yoga & Stretching",
                description = "Lakukan pose ringan atau jalan santai.",
                benefit = "Membantu relaksasi otot panggul dan meningkatkan mood saat energi rendah.",
                type = "EXERCISE"
            ),

            // --- 2. FOLLICULAR PHASE ---
            RecommendationEntity(
                phase = HormonalPhase.FOLLICULAR.name,
                category = "Nutrisi",
                title = "Protein & Karbohidrat Kompleks",
                description = "Sayuran segar, kacang-kacangan, ayam, atau ikan.",
                benefit = "Mendukung peningkatan hormon estrogen dan fokus mental.",
                type = "FOOD"
            ),
            RecommendationEntity(
                phase = HormonalPhase.FOLLICULAR.name,
                category = "Olahraga",
                title = "Kardio & Strength Training",
                description = "Latihan kardio intensitas sedang atau angkat beban.",
                benefit = "Memanfaatkan kenaikan energi untuk membangun kekuatan otot.",
                type = "EXERCISE"
            ),

            // --- 3. OVULATION PHASE ---
            RecommendationEntity(
                phase = HormonalPhase.OVULATION.name,
                category = "Nutrisi",
                title = "Tinggi Protein & Hidrasi",
                description = "Banyak minum air, konsumsi makanan berserat tinggi, dan protein.",
                benefit = "Menjaga stamina saat hormon estrogen mencapai puncaknya.",
                type = "FOOD"
            ),
            RecommendationEntity(
                phase = HormonalPhase.OVULATION.name,
                category = "Olahraga",
                title = "HIIT & Running",
                description = "Latihan intensitas tinggi atau lari jarak menengah.",
                benefit = "Waktu terbaik untuk performa fisik maksimal dan pembakaran energi.",
                type = "EXERCISE"
            ),

            // --- 4. LUTEAL PHASE ---
            RecommendationEntity(
                phase = HormonalPhase.LUTEAL.name,
                category = "Nutrisi",
                title = "Magnesium & Kalsium",
                description = "Cokelat hitam, pisang, alpukat, dan makanan hangat.",
                benefit = "Mengurangi gejala PMS, cravings, dan menstabilkan fluktuasi emosi.",
                type = "FOOD"
            ),
            RecommendationEntity(
                phase = HormonalPhase.LUTEAL.name,
                category = "Olahraga",
                title = "Pilates & Light Strength",
                description = "Gerakan pilates terkontrol atau angkat beban ringan.",
                benefit = "Menjaga kebugaran tanpa membebani tubuh yang sedang sensitif.",
                type = "EXERCISE"
            ),

            // --- 5. PREGNANCY PHASE ---
            RecommendationEntity(
                phase = HormonalPhase.PREGNANCY.name,
                category = "Nutrisi",
                title = "Asam Folat & Nutrisi Bumil",
                description = "Konsumsi sayuran hijau, telur, dan suplemen asam folat.",
                benefit = "Mendukung perkembangan saraf janin.",
                type = "FOOD"
            ),
            RecommendationEntity(
                phase = HormonalPhase.PREGNANCY.name,
                category = "Olahraga",
                title = "Prenatal Yoga",
                description = "Gerakan yoga ringan khusus ibu hamil.",
                benefit = "Melenturkan otot panggul dan mengurangi stres.",
                type = "EXERCISE"
            )
        )

        dao.clearRecommendations()
        dao.insertRecommendations(initialData)

        println("ARINI_DEBUG: Log harian terenkripsi berhasil disimpan!")
    }
}