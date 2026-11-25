import {
  ManasikGuideType,
  ManasikLandmark,
  ManasikStepItem,
} from '../types/convoy';
import { haversineMeters } from '../utils/geo';

export interface SimulationLocationPreset {
  id: string;
  label: string;
  latitude: number;
  longitude: number;
}

export const MANASIK_SIMULATION_LOCATIONS: SimulationLocationPreset[] = [
  {
    id: 'tanim',
    label: "Miqat Tan'im (Masjid Aisyah)",
    latitude: 21.4671,
    longitude: 39.8007,
  },
  {
    id: 'mataf',
    label: "Pelataran Mataf / Ka'bah",
    latitude: 21.4225,
    longitude: 39.8262,
  },
  {
    id: 'sai',
    label: "Area Sa'i (Koridor Safa–Marwah)",
    latitude: 21.4234,
    longitude: 39.8273,
  },
  {
    id: 'marwah',
    label: 'Bukit Marwah (Area Tahallul)',
    latitude: 21.425,
    longitude: 39.8274,
  },
  {
    id: 'arafah',
    label: 'Padang Arafah · Jabal Rahmah',
    latitude: 21.3549,
    longitude: 39.9841,
  },
  {
    id: 'muzdalifah',
    label: 'Area Mabit Muzdalifah',
    latitude: 21.3889,
    longitude: 39.9144,
  },
  {
    id: 'jamarat',
    label: 'Kompleks Jamarat · Mina',
    latitude: 21.4209,
    longitude: 39.8732,
  },
];

export const UMRAH_MANASIK_STEPS: ManasikStepItem[] = [
  {
    id: 'umrah_ihram_miqat',
    guideType: 'umrah',
    order: 1,
    title: 'Ihram & Niat dari Miqat',
    category: 'RUKUN',
    locationHint: "Miqat (Bir Ali / Yalamlam / Tan'im / Ji'ranah)",
    landmark: {
      name: "Masjid Tan'im (Miqat)",
      latitude: 21.4671,
      longitude: 39.8007,
      radiusMeters: 700,
    },
    alternateLandmarks: [
      {
        name: 'Masjid Dzulhulaifah (Bir Ali · Madinah)',
        latitude: 24.4136,
        longitude: 39.5431,
        radiusMeters: 900,
      },
      {
        name: "Masjid Ji'ranah (Miqat Makkah)",
        latitude: 21.5689,
        longitude: 39.9517,
        radiusMeters: 700,
      },
    ],
    summary:
      'Berpakaian ihram, membaca niat Umroh dari Miqat, lalu menjaga seluruh larangan ihram hingga tahallul.',
    details: [
      'Mandi sunnah, memakai wewangian di badan sebelum niat ihram, serta mengenakan 2 lembar kain ihram bagi pria (tanpa pakaian berjahit).',
      'Melaksanakan shalat sunnah ihram 2 rakaat jika tidak di waktu terlarang.',
      'Mengucapkan niat Umroh dari titik Miqat, lalu memperbanyak bacaan Talbiyah hingga memulai Tawaf.',
      'Menjaga larangan ihram: tidak memakai wewangian, tidak memotong kuku/rambut, tidak berburu, serta menjaga lisan dan ketenangan.',
    ],
    arabicText: 'لَبَّيْكَ اللَّهُمَّ عُمْرَةً',
    transliteration: "Labbaikallāhumma 'Umratan",
    translation: 'Aku sambut panggilan-Mu ya Allah untuk menunaikan ibadah Umroh.',
  },
  {
    id: 'umrah_tawaf',
    guideType: 'umrah',
    order: 2,
    title: "Tawaf Mengelilingi Ka'bah (7 Putaran)",
    category: 'RUKUN',
    locationHint: 'Area Mataf Masjidil Haram (Mulai sejajar Hajar Aswad)',
    landmark: {
      name: "Pelataran Mataf · Ka'bah",
      latitude: 21.4225,
      longitude: 39.8262,
      radiusMeters: 350,
    },
    summary:
      "Berjalan mengelilingi Ka'bah 7 kali berlawanan arah jarum jam dengan posisi Ka'bah di sebelah kiri.",
    details: [
      'Pastikan dalam keadaan suci dari hadats kecil & besar (berwudhu).',
      "Bagi pria disunnahkan Idhtiba' (membuka bahu kanan) selama Tawaf dan Raml (berjalan cepat) pada 3 putaran pertama.",
      'Mulai dan akhiri setiap putaran sejajar garis Hajar Aswad sambil melambaikan tangan kanan (Istilam) dan membaca Bismillāhi Allāhu Akbar.',
      'Memperbanyak dzikir dan doa; antara Rukun Yamani dan Hajar Aswad disunnahkan membaca doa Sapu Jagat.',
    ],
    arabicText: 'بِسْمِ اللَّهِ وَاللَّهُ أَكْبَرُ',
    transliteration: 'Bismillāhi wallāhu akbar',
    translation: 'Dengan menyebut nama Allah, dan Allah Maha Besar.',
  },
  {
    id: 'umrah_shalat_zamzam',
    guideType: 'umrah',
    order: 3,
    title: 'Shalat Sunnah Maqam Ibrahim & Air Zamzam',
    category: 'SUNNAH',
    locationHint: 'Belakang Maqam Ibrahim · Area Dispenser Zamzam',
    landmark: {
      name: 'Maqam Ibrahim · Masjidil Haram',
      latitude: 21.4226,
      longitude: 39.8264,
      radiusMeters: 380,
    },
    summary:
      'Menutup bahu kanan kembali (bagi pria), shalat sunnah 2 rakaat di belakang Maqam Ibrahim, lalu minum air Zamzam.',
    details: [
      'Jika area tepat di belakang Maqam Ibrahim padat, shalat sunnah boleh dilakukan di bagian dalam Masjidil Haram mana saja.',
      'Rakaat pertama membaca surat Al-Kafirun, rakaat kedua membaca surat Al-Ikhlas setelah Al-Fatihah.',
      'Minum air Zamzam sambil menghadap kiblat dan berdoa memohon ilmu bermanfaat, rezeki luas, dan kesembuhan.',
    ],
    arabicText:
      'اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا وَاسِعًا وَشِفَاءً مِنْ كُلِّ دَاءٍ',
    transliteration:
      "Allāhumma innī as'aluka 'ilman nāfi'an wa rizqan wāsi'an wa syifā'an min kulli dā'",
    translation:
      'Ya Allah, aku memohon kepada-Mu ilmu yang bermanfaat, rezeki yang luas, dan kesembuhan dari segala penyakit.',
  },
  {
    id: 'umrah_sai',
    guideType: 'umrah',
    order: 4,
    title: "Sa'i Antara Bukit Safa & Marwah (7 Kali)",
    category: 'RUKUN',
    locationHint: "Mas'a (Koridor Safa ke Marwah, Masjidil Haram)",
    landmark: {
      name: "Koridor Mas'a (Area Sa'i)",
      latitude: 21.4234,
      longitude: 39.8273,
      radiusMeters: 420,
    },
    summary:
      'Berjalan 7 kali perjalanan dimulai dari Bukit Safa dan berakhir di Bukit Marwah.',
    details: [
      'Dari Safa ke Marwah dihitung 1 kali, dan dari Marwah kembali ke Safa dihitung 1 kali (berakhir di Marwah pada hitungan ke-7).',
      'Setiap tiba di Bukit Safa dan Marwah, menghadap kiblat, mengangkat kedua tangan, dan berdoa.',
      'Jamaah pria disunnahkan berlari-lari kecil ketika melewati tanda lampu hijau di antara dua pilar.',
      'Pantau rombongan & posisi pendamping karena area Mas’a memiliki jalur kursi roda dan pejalan kaki terpisah.',
    ],
    arabicText: 'إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ',
    transliteration: "Innaṣ-ṣafā wal-marwata min sya'ā'irillāh",
    translation: 'Sesungguhnya Safa dan Marwah adalah sebagian dari syiar-syiar Allah.',
  },
  {
    id: 'umrah_tahallul',
    guideType: 'umrah',
    order: 5,
    title: 'Tahallul (Memotong / Mencukur Rambut)',
    category: 'RUKUN',
    locationHint: 'Area Bukit Marwah / Tempat Cukur Sekitar Haram',
    landmark: {
      name: 'Bukit Marwah · Area Tahallul',
      latitude: 21.425,
      longitude: 39.8274,
      radiusMeters: 420,
    },
    summary:
      'Memotong atau mencukur rambut sebagai penutup rangkaian Umroh sehingga seluruh larangan ihram menjadi halal kembali.',
    details: [
      'Pria lebih utama mencukur habis (halq) atau memendekkan seluruh rambut kepala secara merata.',
      'Wanita cukup memotong ujung rambut sepanjang satu ruas jari secara tertutup/aman.',
      'Setelah Tahallul, ibadah Umroh selesai dan jamaah dapat berganti pakaian biasa.',
    ],
    arabicText: 'اللَّهُمَّ اغْفِرْ لِلْمُحَلِّقِينَ وَالْمُقَصِّرِينَ',
    transliteration: 'Allāhummaghfir lil-muḥalliqīna wal-muqaṣṣirīn',
    translation: 'Ya Allah, ampunilah orang-orang yang mencukur habis dan memendekkan rambutnya.',
  },
];

export const HAJJ_MANASIK_STEPS: ManasikStepItem[] = [
  {
    id: 'haji_ihram_tarwiyah',
    guideType: 'haji',
    order: 1,
    title: 'Ihram & Niat Haji (8 Dzulhijjah / Tarwiyah)',
    category: 'RUKUN',
    locationHint: 'Makkah (Maktab/Hotel) → Mabit ke Mina',
    landmark: {
      name: 'Kawasan Maktab Mina',
      latitude: 21.4133,
      longitude: 39.8933,
      radiusMeters: 1600,
    },
    alternateLandmarks: [
      {
        name: 'Hotel Area Ajyad · Makkah',
        latitude: 21.418,
        longitude: 39.8275,
        radiusMeters: 1200,
      },
    ],
    summary:
      'Memakai pakaian ihram dan berniat Haji pada hari Tarwiyah (8 Dzulhijjah), lalu bertolak menuju Mina.',
    details: [
      'Bagi jamaah Haji Tamattu’, ihram Haji dimulai dari pemondokan/hotel di Makkah pada tanggal 8 Dzulhijjah.',
      'Melaksanakan shalat Dzuhur, Ashar, Maghrib, Isya, dan Subuh di Mina (qashar tanpa jamak untuk shalat 4 rakaat).',
      'Memperbanyak Talbiyah bersama rombongan dan menjaga ketertiban kloter di tenda Mina.',
    ],
    arabicText: 'لَبَّيْكَ اللَّهُمَّ حَجًّا',
    transliteration: 'Labbaikallāhumma Ḥajjan',
    translation: 'Aku sambut panggilan-Mu ya Allah untuk menunaikan ibadah Haji.',
  },
  {
    id: 'haji_wukuf_arafah',
    guideType: 'haji',
    order: 2,
    title: 'Wukuf di Padang Arafah (9 Dzulhijjah)',
    category: 'RUKUN',
    locationHint: 'Padang Arafah · Area Jabal Rahmah & Masjid Namirah',
    landmark: {
      name: 'Padang Arafah · Jabal Rahmah',
      latitude: 21.3549,
      longitude: 39.9841,
      radiusMeters: 2500,
    },
    summary:
      'Rukun puncak ibadah Haji mulai tergelincir matahari (Dzuhur) hingga terbenam matahari pada 9 Dzulhijjah.',
    details: [
      'Berangkat dari Mina menuju Arafah setelah matahari terbit pada 9 Dzulhijjah.',
      'Melaksanakan shalat Dzuhur dan Ashar secara jamak taqdim dan qashar setelah khutbah Wukuf.',
      'Memperbanyak doa, dzikir, istighfar, dan munajat menghadap kiblat di dalam tenda atau area Arafah hingga Maghrib.',
    ],
    arabicText:
      'لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ',
    transliteration:
      "Lā ilāha illallāhu waḥdahu lā syarīka lah, lahul-mulku wa lahul-ḥamdu wa huwa 'alā kulli syai'in qadīr",
    translation:
      'Tiada sesembahan yang berhak disembah selain Allah semata, tiada sekutu bagi-Nya; milik-Nya kerajaan dan segala pujian, dan Dia Maha Kuasa atas segala sesuatu.',
  },
  {
    id: 'haji_mabit_muzdalifah',
    guideType: 'haji',
    order: 3,
    title: 'Mabit di Muzdalifah & Ambil Kerikil',
    category: 'WAJIB',
    locationHint: 'Muzdalifah (Antara Arafah dan Mina)',
    landmark: {
      name: 'Area Mabit Muzdalifah',
      latitude: 21.3889,
      longitude: 39.9144,
      radiusMeters: 1800,
    },
    summary:
      'Menuju Muzdalifah setelah matahari terbenam di Arafah, shalat Maghrib & Isya jamak qashar, lalu bermalam/singgah.',
    details: [
      'Tiba di Muzdalifah untuk shalat Maghrib dan Isya secara jamak takhir dan qashar.',
      'Mengumpulkan batu kerikil seukuran biji kacang (7 butir untuk Jumrah Aqabah, atau 49/70 butir untuk seluruh hari di Mina).',
      'Beristirahat hingga lewat tengah malam atau Subuh (lansia/uzur diperbolehkan melanjutkan ke Mina setelah lewat tengah malam).',
    ],
  },
  {
    id: 'haji_jumrah_aqabah',
    guideType: 'haji',
    order: 4,
    title: 'Melontar Jumrah Aqabah (10 Dzulhijjah)',
    category: 'WAJIB',
    locationHint: 'Kompleks Jamarat, Mina (Jamrah Kubra)',
    landmark: {
      name: 'Kompleks Jamarat · Mina',
      latitude: 21.4209,
      longitude: 39.8732,
      radiusMeters: 850,
    },
    summary:
      'Melontar 7 butir kerikil satu per satu ke arah Jamrah Al-Aqabah sambil bertakbir pada setiap lemparan.',
    details: [
      'Berangkat dari Muzdalifah menuju Mina sebelum atau sesudah terbit matahari pada 10 Dzulhijjah.',
      'Melempar 7 kerikil satu per satu dengan ucapan "Bismillāhi Allāhu Akbar" agar kerikil jatuh ke dalam sumur (haudh) Jamarat.',
      'Ikuti arahan jalur rombongan dan jadwal lontar dari Mutawwif demi kepadatan yang aman.',
    ],
    arabicText: 'بِسْمِ اللَّهِ وَاللَّهُ أَكْبَرُ',
    transliteration: 'Bismillāhi wallāhu akbar',
    translation: 'Dengan nama Allah, Allah Maha Besar.',
  },
  {
    id: 'haji_hadyu_tahallul_awal',
    guideType: 'haji',
    order: 5,
    title: 'Menyembelih Hadyu & Tahallul Awal',
    category: 'WAJIB',
    locationHint: 'Mina / Layanan Dam Resmi',
    landmark: {
      name: 'Area Tenda & Layanan Mina',
      latitude: 21.4133,
      longitude: 39.8933,
      radiusMeters: 1600,
    },
    summary:
      'Menunaikan hadyu/dam bagi Haji Tamattu’ atau Qiran lalu mencukur/memotong rambut untuk Tahallul Awal.',
    details: [
      'Pastikan penyembelihan hewan hadyu telah dikonfirmasi melalui otoritas/panitia resmi.',
      'Mencukur atau memendekkan rambut (pria utama mencukur habis; wanita memotong ujung rambut sebatas ruas jari).',
      'Setelah dua dari tiga amalan (Jumrah Aqabah, Tahallul, Tawaf Ifadah) terlaksana, jamaah berstatus Tahallul Awal.',
    ],
  },
  {
    id: 'haji_tawaf_ifadah',
    guideType: 'haji',
    order: 6,
    title: 'Tawaf Ifadah (Rukun Haji)',
    category: 'RUKUN',
    locationHint: "Masjidil Haram (Area Mataf Ka'bah)",
    landmark: {
      name: "Pelataran Mataf · Ka'bah",
      latitude: 21.4225,
      longitude: 39.8262,
      radiusMeters: 380,
    },
    summary:
      "Kembali ke Masjidil Haram untuk melaksanakan Tawaf Ifadah 7 putaran beserta shalat sunnah 2 rakaat.",
    details: [
      'Dapat dilaksanakan mulai 10 Dzulhijjah setelah melontar Jumrah Aqabah & Tahallul Awal, atau pada hari Tasyrik.',
      'Tata cara sama seperti Tawaf 7 putaran dimulai sejajar Hajar Aswad dan ditutup shalat sunnah di belakang Maqam Ibrahim.',
      'Setelah Tawaf Ifadah dan Sa’i Haji selesai, status menjadi Tahallul Tsani (seluruh larangan ihram gugur sempurna).',
    ],
  },
  {
    id: 'haji_sai',
    guideType: 'haji',
    order: 7,
    title: "Sa'i Haji (Safa ke Marwah 7 Kali)",
    category: 'RUKUN',
    locationHint: "Mas'a (Bukit Safa & Marwah, Masjidil Haram)",
    landmark: {
      name: "Koridor Mas'a (Area Sa'i)",
      latitude: 21.4234,
      longitude: 39.8273,
      radiusMeters: 420,
    },
    summary:
      "Melaksanakan Sa'i 7 perjalanan antara Safa dan Marwah setelah Tawaf Ifadah (wajib bagi Haji Tamattu').",
    details: [
      'Dimulai dari Bukit Safa dan berakhir di Bukit Marwah pada lintasan ke-7.',
      'Bagi jamaah Haji Ifrad atau Qiran yang sudah Sa’i setelah Tawaf Qudum, tidak perlu mengulangi Sa’i lagi.',
    ],
  },
  {
    id: 'haji_mabit_mina_tasyrik',
    guideType: 'haji',
    order: 8,
    title: 'Mabit Mina & Lontar 3 Jumrah (11–13 Dzulhijjah)',
    category: 'WAJIB',
    locationHint: 'Mina & Kompleks Jamarat (Ula, Wustha, Aqabah)',
    landmark: {
      name: 'Kompleks Jamarat & Mina',
      latitude: 21.417,
      longitude: 39.883,
      radiusMeters: 1600,
    },
    summary:
      'Bermalam di Mina selama hari Tasyrik dan melontar ketiga jumrah secara berurutan setelah Dzuhur.',
    details: [
      'Melontar Jumrah Ula (7 kerikil), berdoa menghadap kiblat, lalu Jumrah Wustha (7 kerikil), berdoa, lalu Jumrah Aqabah (7 kerikil).',
      'Nafar Awal: meninggalkan Mina pada 12 Dzulhijjah sebelum matahari terbenam setelah melontar hari ke-11 dan ke-12.',
      'Nafar Tsani: menyempurnakan hingga melontar hari ke-13 Dzulhijjah sebelum kembali ke Makkah.',
    ],
  },
  {
    id: 'haji_tawaf_wada',
    guideType: 'haji',
    order: 9,
    title: "Tawaf Wada' (Tawaf Perpisahan)",
    category: 'WAJIB',
    locationHint: 'Masjidil Haram (Sebelum Meninggalkan Makkah)',
    landmark: {
      name: "Masjidil Haram · Area Ka'bah",
      latitude: 21.4225,
      longitude: 39.8262,
      radiusMeters: 480,
    },
    summary:
      'Tawaf penghormatan terakhir 7 putaran sebelum bertolak meninggalkan kota Makkah.',
    details: [
      'Dilaksanakan menjelang kepulangan meninggalkan Makkah agar menjadi amalan penutup di Baitullah.',
      'Wanita yang sedang haid atau nifas mendapat keringanan (rukhshah) tidak wajib melaksanakan Tawaf Wada’.',
      'Setelah selesai, panjatkan doa agar seluruh ibadah diterima sebagai Haji Mabrur dan dimudahkan perjalanan pulang.',
    ],
  },
];

export const MANASIK_GUIDE_MAP: Record<ManasikGuideType, ManasikStepItem[]> = {
  umrah: UMRAH_MANASIK_STEPS,
  haji: HAJJ_MANASIK_STEPS,
};

export interface LandmarkProximityMatch {
  landmark: ManasikLandmark;
  distanceMeters: number;
  isWithinRadius: boolean;
}

export function evaluateManasikLandmarkProximity(
  step: ManasikStepItem,
  userLat: number,
  userLng: number
): LandmarkProximityMatch {
  const candidates = [step.landmark, ...(step.alternateLandmarks || [])];
  let best: LandmarkProximityMatch | null = null;

  for (const lm of candidates) {
    const distanceMeters = Math.round(
      haversineMeters(
        { lat: userLat, lng: userLng },
        { lat: lm.latitude, lng: lm.longitude }
      )
    );
    const isWithinRadius = distanceMeters <= lm.radiusMeters;
    if (
      !best ||
      (isWithinRadius && !best.isWithinRadius) ||
      (isWithinRadius === best.isWithinRadius && distanceMeters < best.distanceMeters)
    ) {
      best = {
        landmark: lm,
        distanceMeters,
        isWithinRadius,
      };
    }
  }

  return (
    best || {
      landmark: step.landmark,
      distanceMeters: 999999,
      isWithinRadius: false,
    }
  );
}
