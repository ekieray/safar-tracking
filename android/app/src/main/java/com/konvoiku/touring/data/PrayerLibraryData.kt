package com.konvoiku.touring.data

enum class PrayerCategory(val id: String, val label: String, val shortLabel: String) {
    SAFAR("SAFAR", "Safar & Perjalanan Travel", "Safar"),
    UMRAH("UMRAH", "Umroh & Masjidil Haram", "Umroh"),
    MADINAH("MADINAH", "Madinah & Ziarah", "Madinah"),
    HARIAN("HARIAN", "Doa Standar Harian", "Harian")
}

data class PrayerLibraryItem(
    val id: String,
    val category: PrayerCategory,
    val title: String,
    val occasionHint: String,
    val verificationBadge: String,
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val adabNote: String? = null
)

object PrayerLibraryData {
    val prayers: List<PrayerLibraryItem> = listOf(
        // === 1. SAFAR & PERJALANAN TRAVEL UMROH ===
        PrayerLibraryItem(
            id = "doa-keluar-rumah",
            category = PrayerCategory.SAFAR,
            title = "Doa Keluar Rumah Berangkat Umroh",
            occasionHint = "Saat melangkah keluar rumah menuju titik kumpul / bandara",
            verificationBadge = "Standar Kemenag RI · HR. Abu Dawud & Tirmidzi",
            arabicText = "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            transliteration = "Bismillāhi tawakkaltu 'alallāhi, wa lā ḥaula wa lā quwwata illā billāh.",
            translation = "Dengan nama Allah, aku bertawakal kepada Allah. Tiada daya dan kekuatan kecuali dengan pertolongan Allah.",
            adabNote = "Dianjurkan shalat sunnah safar 2 rakaat sebelum meninggalkan rumah."
        ),
        PrayerLibraryItem(
            id = "doa-naik-kendaraan",
            category = PrayerCategory.SAFAR,
            title = "Doa Naik Kendaraan (Bus / Pesawat)",
            occasionHint = "Saat duduk di bus rombongan atau pesawat terbang",
            verificationBadge = "Standar Kemenag RI · QS. Az-Zukhruf: 13–14",
            arabicText = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ",
            transliteration = "Bismillāhir-raḥmānir-raḥīm. Subḥānalladzī sakhkhara lanā hādzā wa mā kunnā lahu muqrinīn, wa innā ilā rabbinā lamunqalibūn.",
            translation = "Dengan nama Allah Yang Maha Pengasih lagi Maha Penyayang. Mahasuci Tuhan yang telah menundukkan kendaraan ini bagi kami, padahal kami sebelumnya tidak mampu menguasainya, dan sesungguhnya kami akan kembali kepada Tuhan kami."
        ),
        PrayerLibraryItem(
            id = "doa-safar-lengkap",
            category = PrayerCategory.SAFAR,
            title = "Doa Safar Lengkap (Perjalanan Jauh)",
            occasionHint = "Dibaca bersama rombongan saat bus/pesawat mulai berangkat",
            verificationBadge = "Buku Tuntunan Manasik Kemenag RI · HR. Muslim",
            arabicText = "اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ، اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ، اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ وَالْخَلِيفَةُ فِي الْأَهْلِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ وَعْثَاءِ السَّفَرِ وَكَآبَةِ الْمَنْظَرِ وَسُوءِ الْمُنْقَلَبِ فِي الْمَالِ وَالْأَهْلِ",
            transliteration = "Allāhu akbar (3x). Subḥānalladzī sakhkhara lanā hādzā wa mā kunnā lahu muqrinīn, wa innā ilā rabbinā lamunqalibūn. Allāhumma innā nas'aluka fī safarinā hādzal-birra wat-taqwā, wa minal-'amali mā tarḍā. Allāhumma hawwin 'alainā safaranā hādzā waṭwi 'annā bu'dah. Allāhumma antaṣ-ṣāḥibu fis-safari wal-khalīfatu fil-ahl. Allāhumma innī a'ūdzu bika min wa'tsā'is-safar, wa ka'ābatil-manẓar, wa sū'il-munqalabi fil-māli wal-ahl.",
            translation = "Allah Maha Besar (3x). Mahasuci Allah yang menundukkan kendaraan ini bagi kami, dan kepada-Nya kami kembali. Ya Allah, kami memohon kebaikan dan takwa dalam safar ini serta amal yang Engkau ridhai. Mudahkanlah perjalanan ini dan dekatkanlah jaraknya. Engkaulah pendamping dalam safar dan penjaga keluarga yang kami tinggalkan. Aku berlindung kepada-Mu dari kesulitan safar, pemandangan menyedihkan, dan kepulangan yang buruk pada harta dan keluarga."
        ),
        PrayerLibraryItem(
            id = "doa-singgah-tempat",
            category = PrayerCategory.SAFAR,
            title = "Doa Singgah di Tempat / Hotel Baru",
            occasionHint = "Saat tiba di hotel, bandara, atau tempat peristirahatan safar",
            verificationBadge = "Standar Kemenag RI · HR. Muslim",
            arabicText = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            transliteration = "A'ūdzu bikalimātillāhit-tāmmāti min syarri mā khalaq.",
            translation = "Aku berlindung dengan kalimat-kalimat Allah yang sempurna dari kejahatan segala makhluk yang Dia ciptakan."
        ),
        PrayerLibraryItem(
            id = "doa-masuk-kota-tujuan",
            category = PrayerCategory.SAFAR,
            title = "Doa Memasuki Daerah / Kota Tujuan",
            occasionHint = "Saat rombongan memasuki wilayah kota yang dituju",
            verificationBadge = "Standar Kemenag RI · HR. Nasa'i & Ibnu Hibban",
            arabicText = "اللَّهُمَّ رَبَّ السَّمَاوَاتِ السَّبْعِ وَمَا أَظْلَلْنَ، وَرَبَّ الْأَرَضِينَ السَّبْعِ وَمَا أَقْلَلْنَ، إِنَّا نَسْأَلُكَ خَيْرَ هَذِهِ الْقَرْيَةِ وَخَيْرَ أَهْلِهَا، وَنَعُوذُ بِكَ مِنْ شَرِّهَا وَشَرِّ أَهْلِهَا وَشَرِّ مَا فِيهَا",
            transliteration = "Allāhumma rabbas-samāwātis-sab'i wa mā aẓlalna, wa rabbal-araḍīnas-sab'i wa mā aqlalna, innā nas'aluka khaira hādzihil-qaryati wa khaira ahlihā, wa na'ūdzu bika min syarrihā wa syarri ahlihā wa syarri mā fīhā.",
            translation = "Ya Allah, Tuhan tujuh langit dan apa yang dinaunginya, serta Tuhan tujuh bumi dan apa yang dipikulnya; kami memohon kebaikan negeri ini dan kebaikan penduduknya, dan kami berlindung kepada-Mu dari keburukan negeri ini, penduduknya, dan segala yang ada di dalamnya."
        ),
        PrayerLibraryItem(
            id = "doa-pulang-safar",
            category = PrayerCategory.SAFAR,
            title = "Doa Pulang Safar (Kembali ke Tanah Air)",
            occasionHint = "Saat perjalanan pulang Umroh menuju kampung halaman",
            verificationBadge = "Tuntunan Manasik Kemenag RI · HR. Bukhari & Muslim",
            arabicText = "آيِبُونَ تَائِبُونَ عَابِدُونَ لِرَبِّنَا حَامِدُونَ",
            transliteration = "Āyibūna tā'ibūna 'ābidūna lirabbinā ḥāmidūn.",
            translation = "Kami kembali dengan bertaubat, tetap beribadah, dan memuji kepada Tuhan kami.",
            adabNote = "Dianjurkan membaca doa safar lalu menutup dengan dzikir kepulangan ini."
        ),

        // === 2. IBADAH UMROH & MASJIDIL HARAM ===
        PrayerLibraryItem(
            id = "doa-niat-umroh-miqat",
            category = PrayerCategory.UMRAH,
            title = "Niat Umroh dari Miqat",
            occasionHint = "Diucapkan saat berada di Miqat (Bir Ali / Yalamlam / Tan'im)",
            verificationBadge = "Buku Tuntunan Manasik Kemenag RI",
            arabicText = "لَبَّيْكَ اللَّهُمَّ عُمْرَةً · نَوَيْتُ الْعُمْرَةَ وَأَحْرَمْتُ بِهَا لِلَّهِ تَعَالَى",
            transliteration = "Labbaikallāhumma 'Umratan — Nawaitul-'umrata wa aḥramtu bihā lillāhi ta'ālā.",
            translation = "Aku sambut panggilan-Mu ya Allah untuk berumroh. Aku niat berumroh dan berihram dengannya karena Allah Ta'ala.",
            adabNote = "Setelah mengucap niat dari Miqat, seluruh larangan ihram mulai berlaku hingga Tahallul."
        ),
        PrayerLibraryItem(
            id = "doa-talbiyah",
            category = PrayerCategory.UMRAH,
            title = "Bacaan Talbiyah Lengkap",
            occasionHint = "Diperbanyak sejak niat ihram di Miqat hingga memulai Tawaf",
            verificationBadge = "Tuntunan Manasik Kemenag RI · Muttafaq 'Alaih",
            arabicText = "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ",
            transliteration = "Labbaikallāhumma labbaik, labbaika lā syarīka laka labbaik, innal-ḥamda wan-ni'mata laka wal-mulk, lā syarīka lak.",
            translation = "Aku memenuhi panggilan-Mu ya Allah, aku memenuhi panggilan-Mu. Tiada sekutu bagi-Mu, aku memenuhi panggilan-Mu. Sesungguhnya segala puji, nikmat, dan kerajaan hanya milik-Mu, tiada sekutu bagi-Mu."
        ),
        PrayerLibraryItem(
            id = "doa-masuk-makkah",
            category = PrayerCategory.UMRAH,
            title = "Doa Memasuki Kota Makkah",
            occasionHint = "Saat rombongan memasuki batas Tanah Haram Makkah",
            verificationBadge = "Buku Tuntunan Manasik Kemenag RI",
            arabicText = "اللَّهُمَّ هَذَا حَرَمُكَ وَأَمْنُكَ فَحَرِّمْنِي عَلَى النَّارِ وَآمِنِّي مِنْ عَذَابِكَ يَوْمَ تَبْعَثُ عِبَادَكَ وَاجْعَلْنِي مِنْ أَوْلِيَائِكَ وَأَهْلِ طَاعَتِكَ",
            transliteration = "Allāhumma hādzā ḥaramuka wa amnuka faḥarrimnī 'alan-nār, wa āminnī min 'adzābika yauma tab'atsu 'ibādak, waj'alnī min auliyā'ika wa ahli ṭā'atik.",
            translation = "Ya Allah, kota ini adalah Tanah Haram-Mu dan tempat aman-Mu, maka haramkanlah diriku dari api neraka, amankanlah aku dari siksa-Mu pada hari Engkau membangkitkan hamba-hamba-Mu, dan jadikanlah aku termasuk kekasih-Mu dan ahli ketaatan kepada-Mu."
        ),
        PrayerLibraryItem(
            id = "doa-masuk-masjidil-haram",
            category = PrayerCategory.UMRAH,
            title = "Doa Masuk Masjidil Haram",
            occasionHint = "Mendahulukan kaki kanan saat melangkah masuk Masjidil Haram",
            verificationBadge = "Tuntunan Manasik Kemenag RI",
            arabicText = "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، فَحَيِّنَا رَبَّنَا بِالسَّلَامِ، وَأَدْخِلْنَا الْجَنَّةَ دَارَ السَّلَامِ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ، اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            transliteration = "Allāhumma antas-salām wa minkas-salām, faḥayyinā rabbanā bis-salām, wa adkhilnal-jannata dāras-salām, tabārakta yā dzal-jalāli wal-ikrām. Allāhummaftaḥ lī abwāba raḥmatik.",
            translation = "Ya Allah, Engkaulah sumber keselamatan dan dari-Mu datang keselamatan. Hidupkanlah kami dengan keselamatan dan masukkanlah kami ke surga negeri keselamatan. Mahasuci Engkau pemilik keagungan dan kemuliaan. Ya Allah, bukakanlah untukku pintu-pintu rahmat-Mu."
        ),
        PrayerLibraryItem(
            id = "doa-keluar-masjid",
            category = PrayerCategory.UMRAH,
            title = "Doa Keluar Masjid",
            occasionHint = "Mendahulukan kaki kiri saat keluar dari Masjidil Haram / Nabawi",
            verificationBadge = "Standar Kemenag RI · HR. Muslim",
            arabicText = "بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ، اللَّهُمَّ اعْصِمْنِي مِنَ الشَّيْطَانِ الرَّجِيمِ",
            transliteration = "Bismillāhi waṣ-ṣalātu was-salāmu 'alā rasūlillāh. Allāhumma innī as'aluka min faḍlik, allāhumma'ṣimnī minasy-syaiṭānir-rajīm.",
            translation = "Dengan nama Allah, shalawat dan salam semoga tercurah kepada Rasulullah. Ya Allah, aku memohon karunia-Mu, dan lindungilah aku dari godaan setan yang terkutuk."
        ),
        PrayerLibraryItem(
            id = "doa-melihat-kabah",
            category = PrayerCategory.UMRAH,
            title = "Doa Ketika Melihat Ka'bah",
            occasionHint = "Saat pertama kali memandang Ka'bah di Masjidil Haram",
            verificationBadge = "Tuntunan Manasik Kemenag RI",
            arabicText = "اللَّهُمَّ زِدْ هَذَا الْبَيْتَ تَشْرِيفًا وَتَعْظِيمًا وَتَكْرِيمًا وَمَهَابَةً، وَزِدْ مَنْ شَرَّفَهُ وَكَرَّمَهُ مِمَّنْ حَجَّهُ أَوِ اعْتَمَرَهُ تَشْرِيفًا وَتَكْرِيمًا وَتَعْظِيمًا وَبِرًّا",
            transliteration = "Allāhumma zid hādzal-baita tasyrīfan wa ta'ẓīman wa takrīman wa mahābatan, wa zid man syarrafahu wa karramahu mimman ḥajjahu awi'tamarahu tasyrīfan wa takrīman wa ta'ẓīman wa birrā.",
            translation = "Ya Allah, tambahkanlah kemuliaan, keagungan, kehormatan, dan kewibawaan pada Baitullah ini. Dan tambahkanlah pula kemuliaan, kehormatan, keagungan, dan kebaikan bagi orang yang memuliakannya dari kalangan jamaah haji maupun umroh."
        ),
        PrayerLibraryItem(
            id = "doa-istilam-tawaf",
            category = PrayerCategory.UMRAH,
            title = "Bacaan Memulai Tawaf & Istilam Hajar Aswad",
            occasionHint = "Di garis sejajar Hajar Aswad pada awal tiap 7 putaran Tawaf",
            verificationBadge = "Tuntunan Manasik Kemenag RI · HR. Bukhari",
            arabicText = "بِسْمِ اللَّهِ وَاللَّهُ أَكْبَرُ",
            transliteration = "Bismillāhi wallāhu akbar.",
            translation = "Dengan menyebut nama Allah, dan Allah Maha Besar.",
            adabNote = "Kemenag menegaskan tidak ada bacaan doa wajib khusus untuk setiap putaran Tawaf; jamaah bebas membaca Al-Qur'an, dzikir, istighfar, dan doa yang dipahami."
        ),
        PrayerLibraryItem(
            id = "doa-rukun-yamani-sapu-jagat",
            category = PrayerCategory.UMRAH,
            title = "Doa Antara Rukun Yamani & Hajar Aswad",
            occasionHint = "Dibaca saat melintasi sisi Rukun Yamani menuju Hajar Aswad",
            verificationBadge = "Tuntunan Kemenag RI · QS. Al-Baqarah: 201",
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Rabbanā ātinā fid-dunyā ḥasanatan wa fil-ākhirati ḥasanatan wa qinā 'adzāban-nār.",
            translation = "Ya Tuhan kami, berilah kami kebaikan di dunia dan kebaikan di akhirat, serta lindungilah kami dari siksa api neraka."
        ),
        PrayerLibraryItem(
            id = "doa-minum-zamzam",
            category = PrayerCategory.UMRAH,
            title = "Doa Minum Air Zamzam",
            occasionHint = "Setelah shalat sunnah 2 rakaat di belakang Maqam Ibrahim",
            verificationBadge = "Tuntunan Manasik Kemenag RI · HR. Ibnu Majah",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا وَاسِعًا وَشِفَاءً مِنْ كُلِّ دَاءٍ",
            transliteration = "Allāhumma innī as'aluka 'ilman nāfi'an wa rizqan wāsi'an wa syifā'an min kulli dā'.",
            translation = "Ya Allah, sungguh aku memohon kepada-Mu ilmu yang bermanfaat, rezeki yang luas, dan kesembuhan dari segala penyakit."
        ),
        PrayerLibraryItem(
            id = "doa-sai-safa-marwah",
            category = PrayerCategory.UMRAH,
            title = "Bacaan Di Atas Bukit Safa & Marwah (Sa'i)",
            occasionHint = "Mengawali Sa'i di Safa dan setiap tiba di atas Bukit Safa/Marwah",
            verificationBadge = "Tuntunan Manasik Kemenag RI · HR. Muslim",
            arabicText = "إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ، أَبْدَأُ بِمَا بَدَأَ اللَّهُ بِهِ · اللَّهُ أَكْبَرُ (٣×) لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ، أَنْجَزَ وَعْدَهُ، وَنَصَرَ عَبْدَهُ، وَهَزَمَ الْأَحْزَابَ وَحْدَهُ",
            transliteration = "Innaṣ-ṣafā wal-marwata min sya'ā'irillāh. Abda'u bimā bada'allāhu bih. Allāhu akbar (3x). Lā ilāha illallāhu waḥdahu lā syarīka lah, lahul-mulku wa lahul-ḥamdu wa huwa 'alā kulli syai'in qadīr. Lā ilāha illallāhu waḥdah, anjaza wa'dah, wa naṣara 'abdah, wa hazamal-aḥzāba waḥdah.",
            translation = "Sesungguhnya Safa dan Marwah adalah sebagian dari syiar Allah. Aku memulai dengan apa yang dimulai Allah. Allah Maha Besar (3x). Tiada tuhan selain Allah Yang Maha Esa, milik-Nya kerajaan dan segala pujian. Dia menepati janji-Nya, menolong hamba-Nya, dan mengalahkan pasukan sekutu sendirian.",
            adabNote = "Ayat Innaṣ-ṣafā dibaca saat pertama kali menuju Bukit Safa saja; dzikir tahlil & doa menghadap kiblat dibaca 3 kali di Safa maupun Marwah."
        ),
        PrayerLibraryItem(
            id = "doa-tahallul",
            category = PrayerCategory.UMRAH,
            title = "Doa Tahallul (Mencukur / Memendekkan Rambut)",
            occasionHint = "Di Bukit Marwah setelah selesai Sa'i putaran ke-7",
            verificationBadge = "Tuntunan Manasik Kemenag RI · HR. Bukhari & Muslim",
            arabicText = "اللَّهُمَّ اغْفِرْ لِلْمُحَلِّقِينَ وَالْمُقَصِّرِينَ",
            transliteration = "Allāhummaghfir lil-muḥalliqīna wal-muqaṣṣirīn.",
            translation = "Ya Allah, ampunilah orang-orang yang mencukur habis dan yang memendekkan rambutnya."
        ),

        // === 3. MADINAH & ZIARAH ===
        PrayerLibraryItem(
            id = "doa-masuk-madinah",
            category = PrayerCategory.MADINAH,
            title = "Doa Memasuki Kota Madinah",
            occasionHint = "Saat rombongan tiba di Madinah Al-Munawwarah",
            verificationBadge = "Tuntunan Manasik Kemenag RI",
            arabicText = "اللَّهُمَّ هَذَا حَرَمُ رَسُولِكَ فَاجْعَلْهُ لِي وِقَايَةً مِنَ النَّارِ وَأَمَانًا مِنَ الْعَذَابِ وَسُوءِ الْحِسَابِ",
            transliteration = "Allāhumma hādzā ḥaramu rasūlika faj'alhu lī wiqāyatan minan-nār, wa amānan minal-'adzābi wa sū'il-ḥisāb.",
            translation = "Ya Allah, negeri ini adalah Tanah Haram Rasul-Mu, maka jadikanlah ia penjaga bagiku dari api neraka dan keamanan dari siksa serta buruknya hisab."
        ),
        PrayerLibraryItem(
            id = "doa-masuk-masjid-nabawi",
            category = PrayerCategory.MADINAH,
            title = "Doa Masuk Masjid Nabawi / Raudhah",
            occasionHint = "Mendahulukan kaki kanan saat memasuki Masjid Nabawi",
            verificationBadge = "Standar Kemenag RI · HR. Muslim & Abu Dawud",
            arabicText = "بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ اغْفِرْ لِي ذُنُوبِي وَافْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            transliteration = "Bismillāhi waṣ-ṣalātu was-salāmu 'alā rasūlillāh. Allāhumma-ghfir lī dzunūbī waftaḥ lī abwāba raḥmatik.",
            translation = "Dengan nama Allah, shalawat dan salam atas Rasulullah. Ya Allah, ampunilah dosa-dosaku dan bukakanlah untukku pintu-pintu rahmat-Mu."
        ),
        PrayerLibraryItem(
            id = "doa-salam-rasulullah",
            category = PrayerCategory.MADINAH,
            title = "Salam di Makam Rasulullah SAW",
            occasionHint = "Saat melintasi depan makam mulia Rasulullah SAW dengan tenang",
            verificationBadge = "Buku Tuntunan Manasik Kemenag RI",
            arabicText = "السَّلَامُ عَلَيْكَ يَا رَسُولَ اللَّهِ، السَّلَامُ عَلَيْكَ يَا نَبِيَّ اللَّهِ، السَّلَامُ عَلَيْكَ يَا خِيَرَةَ اللَّهِ مِنْ خَلْقِهِ، أَشْهَدُ أَنَّكَ قَدْ بَلَّغْتَ الرِّسَالَةَ وَأَدَّيْتَ الْأَمَانَةَ وَنَصَحْتَ الْأُمَّةَ",
            transliteration = "Assalāmu 'alaika yā Rasūlallāh, assalāmu 'alaika yā Nabiyyallāh, assalāmu 'alaika yā khiyaratallāhi min khalqih. Asyhadu annaka qad ballaghtar-risālata wa addaital-amānata wa naṣaḥtal-ummah.",
            translation = "Salam sejahtera untukmu wahai Rasulullah, salam sejahtera untukmu wahai Nabi Allah, salam sejahtera untukmu wahai makhluk pilihan Allah. Aku bersaksi engkau telah menyampaikan risalah, menunaikan amanah, dan menasihati umat.",
            adabNote = "Dilakukan dengan suara lirih, sopan, dan tidak berhenti terlalu lama agar tidak menghalangi jamaah lain."
        ),
        PrayerLibraryItem(
            id = "doa-salam-sahabat",
            category = PrayerCategory.MADINAH,
            title = "Salam Kepada Abu Bakar & Umar r.a.",
            occasionHint = "Setelah memberi salam kepada Rasulullah SAW, bergeser sedikit ke kanan",
            verificationBadge = "Tuntunan Manasik Kemenag RI",
            arabicText = "السَّلَامُ عَلَيْكَ يَا أَبَا بَكْرٍ الصِّدِّيقَ خَلِيفَةَ رَسُولِ اللَّهِ، السَّلَامُ عَلَيْكَ يَا عُمَرَ الْفَارُوقَ أَمِيرَ الْمُؤْمِنِينَ، جَزَاكُمَا اللَّهُ عَنْ أُمَّةِ مُحَمَّدٍ خَيْرًا",
            transliteration = "Assalāmu 'alaika yā Abā Bakriṣ-Ṣiddīq khalīfata Rasūlillāh, assalāmu 'alaika yā 'Umaral-Fārūq Amīral-mu'minīn. Jazākumallāhu 'an ummati Muḥammadin khairā.",
            translation = "Salam sejahtera untukmu wahai Abu Bakar Ash-Shiddiq khalifah Rasulullah, dan salam sejahtera untukmu wahai Umar Al-Faruq Amirul Mukminin. Semoga Allah membalas jasa kalian berdua terhadap umat Nabi Muhammad dengan sebaik-baik balasan."
        ),
        PrayerLibraryItem(
            id = "doa-ziarah-makam",
            category = PrayerCategory.MADINAH,
            title = "Doa Ziarah Pemakaman Baqi' / Syuhada Uhud",
            occasionHint = "Saat berziarah ke Jannatul Baqi atau Syuhada Jabal Uhud",
            verificationBadge = "Standar Kemenag RI · HR. Muslim",
            arabicText = "السَّلَامُ عَلَيْكُمْ أَهْلَ الدِّيَارِ مِنَ الْمُؤْمِنِينَ وَالْمُسْلِمِينَ، وَإِنَّا إِنْ شَاءَ اللَّهُ بِكُمْ لَلَاحِقُونَ، نَسْأَلُ اللَّهَ لَنَا وَلَكُمُ الْعَافِيَةَ",
            transliteration = "Assalāmu 'alaikum ahlad-diyāri minal-mu'minīna wal-muslimīn, wa innā in syā'allāhu bikum lalāḥiqūn, nas'alullāha lanā wa lakumul-'āfiyah.",
            translation = "Salam keselamatan atas kalian wahai penghuni kubur dari kalangan mukminin dan muslimin. Sungguh kami insya Allah akan menyusul kalian; kami memohon kepada Allah keselamatan bagi kami dan bagi kalian."
        ),

        // === 4. DOA STANDAR HARIAN (KEMENAG & MUI) ===
        PrayerLibraryItem(
            id = "doa-sebelum-makan",
            category = PrayerCategory.HARIAN,
            title = "Doa Sebelum Makan & Minum",
            occasionHint = "Saat sarapan, makan siang, atau makan malam di hotel",
            verificationBadge = "Standar Kemenag RI & MUI · HR. Bukhari & Tirmidzi",
            arabicText = "بِسْمِ اللَّهِ، اللَّهُمَّ بَارِكْ لَنَا فِيمَا رَزَقْتَنَا وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Bismillāh. Allāhumma bārik lanā fīmā razaqtanā wa qinā 'adzāban-nār.",
            translation = "Dengan menyebut nama Allah. Ya Allah, berkahilah rezeki yang telah Engkau berikan kepada kami dan peliharalah kami dari siksa api neraka."
        ),
        PrayerLibraryItem(
            id = "doa-sesudah-makan",
            category = PrayerCategory.HARIAN,
            title = "Doa Sesudah Makan & Minum",
            occasionHint = "Setelah selesai menyantap hidangan",
            verificationBadge = "Standar Kemenag RI & MUI · HR. Abu Dawud",
            arabicText = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ",
            transliteration = "Alḥamdulillāhilladzī aṭ'amanā wa saqānā wa ja'alanā muslimīn.",
            translation = "Segala puji bagi Allah yang telah memberi kami makan dan minum, serta menjadikan kami sebagai orang-orang Islam."
        ),
        PrayerLibraryItem(
            id = "doa-sebelum-tidur",
            category = PrayerCategory.HARIAN,
            title = "Doa Sebelum Tidur",
            occasionHint = "Sebelum beristirahat malam di kamar hotel",
            verificationBadge = "Standar Kemenag RI & MUI · HR. Bukhari",
            arabicText = "بِسْمِكَ اللَّهُمَّ أَحْيَا وَبِسْمِكَ أَمُوتُ",
            transliteration = "Bismikallāhumma aḥyā wa bismika amūt.",
            translation = "Dengan nama-Mu ya Allah aku hidup, dan dengan nama-Mu aku mati."
        ),
        PrayerLibraryItem(
            id = "doa-bangun-tidur",
            category = PrayerCategory.HARIAN,
            title = "Doa Bangun Tidur",
            occasionHint = "Saat bangun sebelum shalat Tahajud atau Subuh berjamaah",
            verificationBadge = "Standar Kemenag RI & MUI · HR. Bukhari",
            arabicText = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            transliteration = "Alḥamdulillāhilladzī aḥyānā ba'da mā amātanā wa ilaihin-nusyūr.",
            translation = "Segala puji bagi Allah yang telah menghidupkan kami kembali setelah mematikan kami, dan hanya kepada-Nya kami akan dibangkitkan."
        ),
        PrayerLibraryItem(
            id = "doa-kamar-mandi",
            category = PrayerCategory.HARIAN,
            title = "Doa Masuk & Keluar Kamar Mandi",
            occasionHint = "Sebelum masuk (kaki kiri) dan sesudah keluar (kaki kanan)",
            verificationBadge = "Standar Kemenag RI & MUI · HR. Bukhari & Muslim",
            arabicText = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ · غُفْرَانَكَ",
            transliteration = "Masuk: Allāhumma innī a'ūdzu bika minal-khubutsi wal-khabā'its. Keluar: Ghufrānak.",
            translation = "Masuk: Ya Allah, aku berlindung kepada-Mu dari godaan setan laki-laki dan setan perempuan. Keluar: Aku memohon ampunan-Mu (ya Allah)."
        ),
        PrayerLibraryItem(
            id = "doa-perlindungan-pagi-sore",
            category = PrayerCategory.HARIAN,
            title = "Doa Keselamatan & Perlindungan Pagi–Sore",
            occasionHint = "Dibaca 3 kali pada pagi dan petang agar terhindar dari marabahaya",
            verificationBadge = "Standar Kemenag RI & MUI · HR. Abu Dawud & Tirmidzi",
            arabicText = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            transliteration = "Bismillāhilladzī lā yaḍurru ma'asmihi syai'un fil-arḍi wa lā fis-samā'i wa huwas-samī'ul-'alīm.",
            translation = "Dengan nama Allah yang bersama nama-Nya tidak ada sesuatu pun di bumi maupun di langit yang dapat membahayakan, dan Dialah Yang Maha Mendengar lagi Maha Mengetahui."
        ),
        PrayerLibraryItem(
            id = "doa-kesembuhan",
            category = PrayerCategory.HARIAN,
            title = "Doa Mohon Kesehatan & Kesembuhan",
            occasionHint = "Saat merasa lelah, kurang sehat, atau menjenguk rekan jamaah",
            verificationBadge = "Standar Kemenag RI & MUI · HR. Bukhari & Muslim",
            arabicText = "اللَّهُمَّ رَبَّ النَّاسِ أَذْهِبِ الْبَأْسَ، اشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ شِفَاءً لَا يُغَادِرُ سَقَمًا",
            transliteration = "Allāhumma rabban-nāsi adzhibil-ba's, isyfi antasy-syāfī, lā syifā'a illā syifā'uka syifā'an lā yughādiru saqamā.",
            translation = "Ya Allah, Tuhan seluruh manusia, hilangkanlah penyakit ini, sembuhkanlah karena Engkaulah Yang Maha Menyembuhkan; tiada kesembuhan selain kesembuhan dari-Mu, kesembuhan yang tidak meninggalkan penyakit lagi."
        ),
        PrayerLibraryItem(
            id = "doa-orang-tua",
            category = PrayerCategory.HARIAN,
            title = "Doa Untuk Kedua Orang Tua",
            occasionHint = "Diamalkan setiap selesai shalat fardhu dan di tempat mustajab",
            verificationBadge = "Standar Kemenag RI & MUI",
            arabicText = "رَبِّ اغْفِرْ لِي وَلِوَالِدَيَّ وَارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            transliteration = "Rabbighfir lī wa liwālidayya warḥamhumā kamā rabbayānī ṣaghīrā.",
            translation = "Ya Tuhanku, ampunilah dosaku dan dosa kedua orang tuaku, serta sayangilah keduanya sebagaimana mereka menyayangiku di waktu kecil."
        )
    )
}
