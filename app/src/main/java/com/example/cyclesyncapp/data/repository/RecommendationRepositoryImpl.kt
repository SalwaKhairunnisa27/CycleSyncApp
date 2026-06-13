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

            // --- 5. PREGNANCY TRIMESTER 1 ---
            RecommendationEntity(
                phase = "PREGNANCY_T1",
                category = "Nutrisi",
                title = "Tinggi Asam Folat & Hidrasi",
                description = "Bayam, telur, jeruk, jahe hangat untuk mual.",
                benefit = "Mencegah cacat tabung saraf janin & meredakan morning sickness.",
                type = "FOOD"
            ),
            RecommendationEntity(
                phase = "PREGNANCY_T1",
                category = "Olahraga",
                title = "Jalan Santai & Peregangan",
                description = "Berjalan 15-20 menit per hari, peregangan leher & bahu.",
                benefit = "Menjaga stamina awal kehamilan tanpa benturan fisik.",
                type = "EXERCISE"
            ),

            // --- 6. PREGNANCY TRIMESTER 2 ---
            RecommendationEntity(
                phase = "PREGNANCY_T2",
                category = "Nutrisi",
                title = "Kalsium, Zat Besi & Protein",
                description = "Susu, yoghurt, daging merah tanpa lemak, kacang-kacangan.",
                benefit = "Mendukung pembentukan tulang bayi dan menambah volume darah ibu.",
                type = "FOOD"
            ),
            RecommendationEntity(
                phase = "PREGNANCY_T2",
                category = "Olahraga",
                title = "Prenatal Yoga & Berenang",
                description = "Berenang santai atau kelas prenatal yoga terpandu.",
                benefit = "Mengurangi nyeri punggung, melatih panggul, & melancarkan nafas.",
                type = "EXERCISE"
            ),

            // --- 7. PREGNANCY TRIMESTER 3 ---
            RecommendationEntity(
                phase = "PREGNANCY_T3",
                category = "Nutrisi",
                title = "Makanan Padat Gizi & Serat",
                description = "Gandum utuh, alpukat, ikan Salmon (Omega-3), porsi kecil tapi sering.",
                benefit = "Mendukung perkembangan otak janin & mencegah sembelit.",
                type = "FOOD"
            ),
            RecommendationEntity(
                phase = "PREGNANCY_T3",
                category = "Olahraga",
                title = "Senam Hamil & Gym Ball",
                description = "Pelvic tilting, senam kegel, duduk aktif dengan gym ball.",
                benefit = "Mempersiapkan otot jalan lahir & mempermudah posisi persalinan.",
                type = "EXERCISE"
            ),

            // --- SUPPLEMENT RECOMMENDATIONS FOR ALL PHASES ---
            RecommendationEntity(
                phase = HormonalPhase.MENSTRUATION.name,
                category = "Suplemen",
                title = "Tablet Tambah Darah & Vitamin C",
                description = "Konsumsi tablet zat besi bersama segelas jus jeruk.",
                benefit = "Membantu penyerapan zat besi untuk mengganti sel darah merah yang hilang.",
                type = "SUPPLEMENT"
            ),
            RecommendationEntity(
                phase = HormonalPhase.FOLLICULAR.name,
                category = "Suplemen",
                title = "Vitamin B Kompleks",
                description = "Suplemen Vitamin B kompleks harian.",
                benefit = "Membantu mengoptimalkan metabolisme karbohidrat menjadi energi sel.",
                type = "SUPPLEMENT"
            ),
            RecommendationEntity(
                phase = HormonalPhase.OVULATION.name,
                category = "Suplemen",
                title = "Zinc & Magnesium",
                description = "Suplemen Zinc 15mg dan Magnesium 250mg.",
                benefit = "Mendukung kualitas sel telur dan menunjang keseimbangan hormon reproduksi.",
                type = "SUPPLEMENT"
            ),
            RecommendationEntity(
                phase = HormonalPhase.LUTEAL.name,
                category = "Suplemen",
                title = "Kalsium & Vitamin D",
                description = "Kalsium 1000mg dan Vitamin D3 1000 IU.",
                benefit = "Membantu menstabilkan fluktuasi emosional PMS dan meredakan ketegangan otot rahim.",
                type = "SUPPLEMENT"
            ),
            // --- PREGNANCY SUPPLEMENTS (T1, T2, T3) ---
            RecommendationEntity(
                phase = "PREGNANCY_T1",
                category = "Suplemen",
                title = "Asam Folat & Vitamin B6",
                description = "Asam folat 400mcg harian dan Vitamin B6 jika mual.",
                benefit = "Mendukung perkembangan tabung saraf janin & mengurangi rasa mual.",
                type = "SUPPLEMENT"
            ),
            RecommendationEntity(
                phase = "PREGNANCY_T2",
                category = "Suplemen",
                title = "Kalsium & Tablet Besi (Fe)",
                description = "Kalsium 1000mg dan tablet Zat Besi harian.",
                benefit = "Mencegah anemia kehamilan & menunjang pertumbuhan gigi/tulang janin.",
                type = "SUPPLEMENT"
            ),
            RecommendationEntity(
                phase = "PREGNANCY_T3",
                category = "Suplemen",
                title = "DHA & Vitamin D3",
                description = "Minyak ikan kaya DHA dan Vitamin D3 1000 IU.",
                benefit = "Mendukung pematangan otak & mata janin serta menjaga kekebalan tubuh ibu.",
                type = "SUPPLEMENT"
            )
        )

        dao.clearRecommendations()
        dao.insertRecommendations(initialData)

        println("ARINI_DEBUG: Log harian terenkripsi berhasil disimpan!")
    }
}