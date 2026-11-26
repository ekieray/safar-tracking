package com.konvoiku.touring.data

import com.konvoiku.touring.model.ManasikGuideType
import com.konvoiku.touring.model.ManasikLandmark
import com.konvoiku.touring.model.ManasikSimulationLocation
import com.konvoiku.touring.model.ManasikStepCategory
import com.konvoiku.touring.model.ManasikStepItem
import com.konvoiku.touring.model.RoomActivityItem
import com.konvoiku.touring.util.GeoUtils
import com.konvoiku.touring.util.LatLngGeo
import kotlin.math.roundToInt

data class UmrahActivityTemplate(
    val key: String,
    val title: String,
    val timeLabel: String,
    val locationNote: String
)

data class LandmarkProximityMatch(
    val landmark: ManasikLandmark,
    val distanceMeters: Int,
    val isWithinRadius: Boolean
)

object ManasikGuideData {

    val simulationLocations: List<ManasikSimulationLocation> = listOf(
        ManasikSimulationLocation("tanim", "Miqat Tan'im (Masjid Aisyah)", 21.4671, 39.8007),
        ManasikSimulationLocation("mataf", "Pelataran Mataf / Ka'bah", 21.4225, 39.8262),
        ManasikSimulationLocation("sai", "Area Sa'i (Koridor Safa–Marwah)", 21.4234, 39.8273),
        ManasikSimulationLocation("marwah", "Bukit Marwah (Area Tahallul)", 21.4250, 39.8274),
        ManasikSimulationLocation("arafah", "Padang Arafah · Jabal Rahmah", 21.3549, 39.9841),
        ManasikSimulationLocation("muzdalifah", "Area Mabit Muzdalifah", 21.3889, 39.9144),
        ManasikSimulationLocation("jamarat", "Kompleks Jamarat · Mina", 21.4209, 39.8732)
    )

    val umrahSteps: List<ManasikStepItem> = listOf(
        ManasikStepItem(
            id = "umrah_ihram_miqat",
            guideType = ManasikGuideType.UMRAH,
            order = 1,
            title = "Ihram & Niat dari Miqat",
            category = ManasikStepCategory.RUKUN,
            locationHint = "Miqat (Bir Ali / Yalamlam / Tan'im / Ji'ranah)",
            landmark = ManasikLandmark("Masjid Tan'im (Miqat)", 21.4671, 39.8007, 700),
            alternateLandmarks = listOf(
                ManasikLandmark("Masjid Dzulhulaifah (Bir Ali · Madinah)", 24.4136, 39.5431, 900),
                ManasikLandmark("Masjid Ji'ranah (Miqat Makkah)", 21.5689, 39.9517, 700)
            ),
            summary = "Berpakaian ihram, membaca niat Umroh dari Miqat, lalu menjaga seluruh larangan ihram hingga tahallul.",
            details = listOf(
                "Mandi sunnah, memakai wewangian di badan sebelum niat ihram, serta mengenakan 2 lembar kain ihram bagi pria (tanpa pakaian berjahit).",
                "Melaksanakan shalat sunnah ihram 2 rakaat jika tidak di waktu terlarang.",
                "Mengucapkan niat Umroh dari titik Miqat, lalu memperbanyak bacaan Talbiyah hingga memulai Tawaf.",
                "Menjaga larangan ihram: tidak memakai wewangian, tidak memotong kuku/rambut, tidak berburu, serta menjaga lisan dan ketenangan."
            ),
            arabicText = "لَبَّيْكَ اللَّهُمَّ عُمْرَةً",
            transliteration = "Labbaikallāhumma 'Umratan",
            translation = "Aku sambut panggilan-Mu ya Allah untuk menunaikan ibadah Umroh."
        ),
        ManasikStepItem(
            id = "umrah_tawaf",
            guideType = ManasikGuideType.UMRAH,
            order = 2,
            title = "Tawaf Mengelilingi Ka'bah (7 Putaran)",
            category = ManasikStepCategory.RUKUN,
            locationHint = "Area Mataf Masjidil Haram (Mulai sejajar Hajar Aswad)",
            landmark = ManasikLandmark("Pelataran Mataf · Ka'bah", 21.4225, 39.8262, 350),
            summary = "Berjalan mengelilingi Ka'bah 7 kali berlawanan arah jarum jam dengan posisi Ka'bah di sebelah kiri.",
            details = listOf(
                "Pastikan dalam keadaan suci dari hadats kecil & besar (berwudhu).",
                "Bagi pria disunnahkan Idhtiba' (membuka bahu kanan) selama Tawaf dan Raml (berjalan cepat) pada 3 putaran pertama.",
                "Mulai dan akhiri setiap putaran sejajar garis Hajar Aswad sambil melambaikan tangan kanan (Istilam) dan membaca Bismillāhi Allāhu Akbar.",
                "Memperbanyak dzikir dan doa; antara Rukun Yamani dan Hajar Aswad disunnahkan membaca doa Sapu Jagat."
            ),
            arabicText = "بِسْمِ اللَّهِ وَاللَّهُ أَكْبَرُ",
            transliteration = "Bismillāhi wallāhu akbar",
            translation = "Dengan menyebut nama Allah, dan Allah Maha Besar."
        ),
        ManasikStepItem(
            id = "umrah_shalat_zamzam",
            guideType = ManasikGuideType.UMRAH,
            order = 3,
            title = "Shalat Sunnah Maqam Ibrahim & Air Zamzam",
            category = ManasikStepCategory.SUNNAH,
            locationHint = "Belakang Maqam Ibrahim · Area Dispenser Zamzam",
            landmark = ManasikLandmark("Maqam Ibrahim · Masjidil Haram", 21.4226, 39.8264, 380),
            summary = "Menutup bahu kanan kembali (bagi pria), shalat sunnah 2 rakaat di belakang Maqam Ibrahim, lalu minum air Zamzam.",
            details = listOf(
                "Jika area tepat di belakang Maqam Ibrahim padat, shalat sunnah boleh dilakukan di bagian dalam Masjidil Haram mana saja.",
                "Rakaat pertama membaca surat Al-Kafirun, rakaat kedua membaca surat Al-Ikhlas setelah Al-Fatihah.",
                "Minum air Zamzam sambil menghadap kiblat dan berdoa memohon ilmu bermanfaat, rezeki luas, dan kesembuhan."
            ),
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا وَاسِعًا وَشِفَاءً مِنْ كُلِّ دَاءٍ",
            transliteration = "Allāhumma innī as'aluka 'ilman nāfi'an wa rizqan wāsi'an wa syifā'an min kulli dā'",
            translation = "Ya Allah, aku memohon kepada-Mu ilmu yang bermanfaat, rezeki yang luas, dan kesembuhan dari segala penyakit."
        ),
        ManasikStepItem(
            id = "umrah_sai",
            guideType = ManasikGuideType.UMRAH,
            order = 4,
            title = "Sa'i Antara Bukit Safa & Marwah (7 Kali)",
            category = ManasikStepCategory.RUKUN,
            locationHint = "Mas'a (Koridor Safa ke Marwah, Masjidil Haram)",
            landmark = ManasikLandmark("Koridor Mas'a (Area Sa'i)", 21.4234, 39.8273, 420),
            summary = "Berjalan 7 kali perjalanan dimulai dari Bukit Safa dan berakhir di Bukit Marwah.",
            details = listOf(
                "Dari Safa ke Marwah dihitung 1 kali, dan dari Marwah kembali ke Safa dihitung 1 kali (berakhir di Marwah pada hitungan ke-7).",
                "Setiap tiba di Bukit Safa dan Marwah, menghadap kiblat, mengangkat kedua tangan, dan berdoa.",
                "Jamaah pria disunnahkan berlari-lari kecil ketika melewati tanda lampu hijau di antara dua pilar.",
                "Pantau rombongan & posisi pendamping karena area Mas'a memiliki jalur kursi roda dan pejalan kaki terpisah."
            ),
            arabicText = "إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ",
            transliteration = "Innaṣ-ṣafā wal-marwata min sya'ā'irillāh",
            translation = "Sesungguhnya Safa dan Marwah adalah sebagian dari syiar-syiar Allah."
        ),
        ManasikStepItem(
            id = "umrah_tahallul",
            guideType = ManasikGuideType.UMRAH,
            order = 5,
            title = "Tahallul (Memotong / Mencukur Rambut)",
            category = ManasikStepCategory.RUKUN,
            locationHint = "Area Bukit Marwah / Tempat Cukur Sekitar Haram",
            landmark = ManasikLandmark("Bukit Marwah · Area Tahallul", 21.4250, 39.8274, 420),
            summary = "Memotong atau mencukur rambut sebagai penutup rangkaian Umroh sehingga seluruh larangan ihram menjadi halal kembali.",
            details = listOf(
                "Pria lebih utama mencukur habis (halq) atau memendekkan seluruh rambut kepala secara merata.",
                "Wanita cukup memotong ujung rambut sepanjang satu ruas jari secara tertutup/aman.",
                "Setelah Tahallul, ibadah Umroh selesai dan jamaah dapat berganti pakaian biasa."
            ),
            arabicText = "اللَّهُمَّ اغْفِرْ لِلْمُحَلِّقِينَ وَالْمُقَصِّرِينَ",
            transliteration = "Allāhummaghfir lil-muḥalliqīna wal-muqaṣṣirīn",
            translation = "Ya Allah, ampunilah orang-orang yang mencukur habis dan memendekkan rambutnya."
        )
    )

    val hajjSteps: List<ManasikStepItem> = listOf(
        ManasikStepItem(
            id = "haji_ihram_tarwiyah",
            guideType = ManasikGuideType.HAJI,
            order = 1,
            title = "Ihram & Niat Haji (8 Dzulhijjah / Tarwiyah)",
            category = ManasikStepCategory.RUKUN,
            locationHint = "Makkah (Maktab/Hotel) → Mabit ke Mina",
            landmark = ManasikLandmark("Kawasan Maktab Mina", 21.4133, 39.8933, 1600),
            alternateLandmarks = listOf(
                ManasikLandmark("Hotel Area Ajyad · Makkah", 21.4180, 39.8275, 1200)
            ),
            summary = "Memakai pakaian ihram dan berniat Haji pada hari Tarwiyah (8 Dzulhijjah), lalu bertolak menuju Mina.",
            details = listOf(
                "Bagi jamaah Haji Tamattu', ihram Haji dimulai dari pemondokan/hotel di Makkah pada tanggal 8 Dzulhijjah.",
                "Melaksanakan shalat Dzuhur, Ashar, Maghrib, Isya, dan Subuh di Mina (qashar tanpa jamak untuk shalat 4 rakaat).",
                "Memperbanyak Talbiyah bersama rombongan dan menjaga ketertiban kloter di tenda Mina."
            ),
            arabicText = "لَبَّيْكَ اللَّهُمَّ حَجًّا",
            transliteration = "Labbaikallāhumma Ḥajjan",
            translation = "Aku sambut panggilan-Mu ya Allah untuk menunaikan ibadah Haji."
        ),
        ManasikStepItem(
            id = "haji_wukuf_arafah",
            guideType = ManasikGuideType.HAJI,
            order = 2,
            title = "Wukuf di Padang Arafah (9 Dzulhijjah)",
            category = ManasikStepCategory.RUKUN,
            locationHint = "Padang Arafah · Area Jabal Rahmah & Masjid Namirah",
            landmark = ManasikLandmark("Padang Arafah · Jabal Rahmah", 21.3549, 39.9841, 2500),
            summary = "Rukun puncak ibadah Haji mulai tergelincir matahari (Dzuhur) hingga terbenam matahari pada 9 Dzulhijjah.",
            details = listOf(
                "Berangkat dari Mina menuju Arafah setelah matahari terbit pada 9 Dzulhijjah.",
                "Melaksanakan shalat Dzuhur dan Ashar secara jamak taqdim dan qashar setelah khutbah Wukuf.",
                "Memperbanyak doa, dzikir, istighfar, dan munajat menghadap kiblat di dalam tenda atau area Arafah hingga Maghrib."
            ),
            arabicText = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "Lā ilāha illallāhu waḥdahu lā syarīka lah, lahul-mulku wa lahul-ḥamdu wa huwa 'alā kulli syai'in qadīr",
            translation = "Tiada sesembahan yang berhak disembah selain Allah semata, tiada sekutu bagi-Nya; milik-Nya kerajaan dan segala pujian, dan Dia Maha Kuasa atas segala sesuatu."
        ),
        ManasikStepItem(
            id = "haji_mabit_muzdalifah",
            guideType = ManasikGuideType.HAJI,
            order = 3,
            title = "Mabit di Muzdalifah & Ambil Kerikil",
            category = ManasikStepCategory.WAJIB,
            locationHint = "Muzdalifah (Antara Arafah dan Mina)",
            landmark = ManasikLandmark("Area Mabit Muzdalifah", 21.3889, 39.9144, 1800),
            summary = "Menuju Muzdalifah setelah matahari terbenam di Arafah, shalat Maghrib & Isya jamak qashar, lalu bermalam/singgah.",
            details = listOf(
                "Tiba di Muzdalifah untuk shalat Maghrib dan Isya secara jamak takhir dan qashar.",
                "Mengumpulkan batu kerikil seukuran biji kacang (7 butir untuk Jumrah Aqabah, atau 49/70 butir untuk seluruh hari di Mina).",
                "Beristirahat hingga lewat tengah malam atau Subuh (lansia/uzur diperbolehkan melanjutkan ke Mina setelah lewat tengah malam)."
            )
        ),
        ManasikStepItem(
            id = "haji_jumrah_aqabah",
            guideType = ManasikGuideType.HAJI,
            order = 4,
            title = "Melontar Jumrah Aqabah (10 Dzulhijjah)",
            category = ManasikStepCategory.WAJIB,
            locationHint = "Kompleks Jamarat, Mina (Jamrah Kubra)",
            landmark = ManasikLandmark("Kompleks Jamarat · Mina", 21.4209, 39.8732, 850),
            summary = "Melontar 7 butir kerikil satu per satu ke arah Jamrah Al-Aqabah sambil bertakbir pada setiap lemparan.",
            details = listOf(
                "Berangkat dari Muzdalifah menuju Mina sebelum atau sesudah terbit matahari pada 10 Dzulhijjah.",
                "Melempar 7 kerikil satu per satu dengan ucapan \"Bismillāhi Allāhu Akbar\" agar kerikil jatuh ke dalam sumur Jamarat.",
                "Ikuti arahan jalur rombongan dan jadwal lontar dari Mutawwif demi kepadatan yang aman."
            ),
            arabicText = "بِسْمِ اللَّهِ وَاللَّهُ أَكْبَرُ",
            transliteration = "Bismillāhi wallāhu akbar",
            translation = "Dengan nama Allah, Allah Maha Besar."
        ),
        ManasikStepItem(
            id = "haji_hadyu_tahallul_awal",
            guideType = ManasikGuideType.HAJI,
            order = 5,
            title = "Menyembelih Hadyu & Tahallul Awal",
            category = ManasikStepCategory.WAJIB,
            locationHint = "Mina / Layanan Dam Resmi",
            landmark = ManasikLandmark("Area Tenda & Layanan Mina", 21.4133, 39.8933, 1600),
            summary = "Menunaikan hadyu/dam bagi Haji Tamattu' atau Qiran lalu mencukur/memotong rambut untuk Tahallul Awal.",
            details = listOf(
                "Pastikan penyembelihan hewan hadyu telah dikonfirmasi melalui otoritas/panitia resmi.",
                "Mencukur atau memendekkan rambut (pria utama mencukur habis; wanita memotong ujung rambut sebatas ruas jari).",
                "Setelah dua dari tiga amalan (Jumrah Aqabah, Tahallul, Tawaf Ifadah) terlaksana, jamaah berstatus Tahallul Awal."
            )
        ),
        ManasikStepItem(
            id = "haji_tawaf_ifadah",
            guideType = ManasikGuideType.HAJI,
            order = 6,
            title = "Tawaf Ifadah (Rukun Haji)",
            category = ManasikStepCategory.RUKUN,
            locationHint = "Masjidil Haram (Area Mataf Ka'bah)",
            landmark = ManasikLandmark("Pelataran Mataf · Ka'bah", 21.4225, 39.8262, 380),
            summary = "Kembali ke Masjidil Haram untuk melaksanakan Tawaf Ifadah 7 putaran beserta shalat sunnah 2 rakaat.",
            details = listOf(
                "Dapat dilaksanakan mulai 10 Dzulhijjah setelah melontar Jumrah Aqabah & Tahallul Awal, atau pada hari Tasyrik.",
                "Tata cara sama seperti Tawaf 7 putaran dimulai sejajar Hajar Aswad dan ditutup shalat sunnah di belakang Maqam Ibrahim.",
                "Setelah Tawaf Ifadah dan Sa'i Haji selesai, status menjadi Tahallul Tsani (seluruh larangan ihram gugur sempurna)."
            )
        ),
        ManasikStepItem(
            id = "haji_sai",
            guideType = ManasikGuideType.HAJI,
            order = 7,
            title = "Sa'i Haji (Safa ke Marwah 7 Kali)",
            category = ManasikStepCategory.RUKUN,
            locationHint = "Mas'a (Bukit Safa & Marwah, Masjidil Haram)",
            landmark = ManasikLandmark("Koridor Mas'a (Area Sa'i)", 21.4234, 39.8273, 420),
            summary = "Melaksanakan Sa'i 7 perjalanan antara Safa dan Marwah setelah Tawaf Ifadah (wajib bagi Haji Tamattu').",
            details = listOf(
                "Dimulai dari Bukit Safa dan berakhir di Bukit Marwah pada lintasan ke-7.",
                "Bagi jamaah Haji Ifrad atau Qiran yang sudah Sa'i setelah Tawaf Qudum, tidak perlu mengulangi Sa'i lagi."
            )
        ),
        ManasikStepItem(
            id = "haji_mabit_mina_tasyrik",
            guideType = ManasikGuideType.HAJI,
            order = 8,
            title = "Mabit Mina & Lontar 3 Jumrah (11–13 Dzulhijjah)",
            category = ManasikStepCategory.WAJIB,
            locationHint = "Mina & Kompleks Jamarat (Ula, Wustha, Aqabah)",
            landmark = ManasikLandmark("Kompleks Jamarat & Mina", 21.4170, 39.8830, 1600),
            summary = "Bermalam di Mina selama hari Tasyrik dan melontar ketiga jumrah secara berurutan setelah Dzuhur.",
            details = listOf(
                "Melontar Jumrah Ula (7 kerikil), berdoa menghadap kiblat, lalu Jumrah Wustha (7 kerikil), berdoa, lalu Jumrah Aqabah (7 kerikil).",
                "Nafar Awal: meninggalkan Mina pada 12 Dzulhijjah sebelum matahari terbenam setelah melontar hari ke-11 dan ke-12.",
                "Nafar Tsani: menyempurnakan hingga melontar hari ke-13 Dzulhijjah sebelum kembali ke Makkah."
            )
        ),
        ManasikStepItem(
            id = "haji_tawaf_wada",
            guideType = ManasikGuideType.HAJI,
            order = 9,
            title = "Tawaf Wada' (Tawaf Perpisahan)",
            category = ManasikStepCategory.WAJIB,
            locationHint = "Masjidil Haram (Sebelum Meninggalkan Makkah)",
            landmark = ManasikLandmark("Masjidil Haram · Area Ka'bah", 21.4225, 39.8262, 480),
            summary = "Tawaf penghormatan terakhir 7 putaran sebelum bertolak meninggalkan kota Makkah.",
            details = listOf(
                "Dilaksanakan menjelang kepulangan meninggalkan Makkah agar menjadi amalan penutup di Baitullah.",
                "Wanita yang sedang haid atau nifas mendapat keringanan (rukhshah) tidak wajib melaksanakan Tawaf Wada'.",
                "Setelah selesai, panjatkan doa agar seluruh ibadah diterima sebagai Haji Mabrur dan dimudahkan perjalanan pulang."
            )
        )
    )

    fun stepsForGuide(type: ManasikGuideType): List<ManasikStepItem> =
        if (type == ManasikGuideType.UMRAH) umrahSteps else hajjSteps

    fun evaluateLandmarkProximity(
        step: ManasikStepItem,
        userLat: Double,
        userLng: Double
    ): LandmarkProximityMatch {
        val candidates = listOf(step.landmark) + step.alternateLandmarks
        var best: LandmarkProximityMatch? = null
        for (lm in candidates) {
            val distanceMeters = GeoUtils.haversineMeters(
                LatLngGeo(userLat, userLng),
                LatLngGeo(lm.latitude, lm.longitude)
            ).roundToInt()
            val isWithin = distanceMeters <= lm.radiusMeters
            if (best == null ||
                (isWithin && !best.isWithinRadius) ||
                (isWithin == best.isWithinRadius && distanceMeters < best.distanceMeters)
            ) {
                best = LandmarkProximityMatch(lm, distanceMeters, isWithin)
            }
        }
        return best ?: LandmarkProximityMatch(step.landmark, 999_999, false)
    }

    val umrahActivityQuickTemplates: List<UmrahActivityTemplate> = listOf(
        UmrahActivityTemplate(
            key = "tpl-kumpul-lobi",
            title = "Kumpul Lobi Hotel & Absen Rombongan",
            timeLabel = "Persiapan",
            locationNote = "Lobi Hotel / Titik Kumpul Awal"
        ),
        UmrahActivityTemplate(
            key = "tpl-miqat-niat",
            title = "Pakai Kain Ihram & Niat Umroh di Miqat",
            timeLabel = "Berangkat",
            locationNote = "Masjid Miqat (Bir Ali / Tan'im / Yalamlam)"
        ),
        UmrahActivityTemplate(
            key = "tpl-tawaf",
            title = "Tawaf Umroh 7 Putaran Bersama",
            timeLabel = "Masjidil Haram",
            locationNote = "Pelataran Ka'bah · Mulai Hajar Aswad"
        ),
        UmrahActivityTemplate(
            key = "tpl-maqam-zamzam",
            title = "Sholat Sunnah Maqam Ibrahim & Minum Zamzam",
            timeLabel = "Setelah Tawaf",
            locationNote = "Belakang Maqam Ibrahim"
        ),
        UmrahActivityTemplate(
            key = "tpl-sai",
            title = "Sa'i Shafa ke Marwah 7 Kali",
            timeLabel = "Mas'a",
            locationNote = "Lintasan Shafa – Marwah"
        ),
        UmrahActivityTemplate(
            key = "tpl-tahallul",
            title = "Tahallul & Kumpul Pintu Marwah",
            timeLabel = "Penutup",
            locationNote = "Bukit Marwah · Titik Kumpul Rombongan"
        ),
        UmrahActivityTemplate(
            key = "tpl-ziarah-makkah",
            title = "Ziarah Jabal Nur, Jabal Tsur & Arafah",
            timeLabel = "Ziarah",
            locationNote = "Bus Rombongan Makkah"
        ),
        UmrahActivityTemplate(
            key = "tpl-raudhah",
            title = "Kunjungan Raudhah & Salam Makam Rasulullah",
            timeLabel = "Madinah",
            locationNote = "Masjid Nabawi · Pintu Tasreh"
        )
    )

    fun createStarterRoomUmrahActivities(createdByUid: String = "leader"): List<RoomActivityItem> {
        val now = System.currentTimeMillis()
        return umrahActivityQuickTemplates.take(4).mapIndexed { idx, tpl ->
            RoomActivityItem(
                id = "act-init-${idx + 1}",
                title = tpl.title,
                timeLabel = tpl.timeLabel,
                locationNote = tpl.locationNote,
                orderIndex = idx,
                createdByUid = createdByUid,
                createdAtMs = now,
                updatedAtMs = now
            )
        }
    }
}
