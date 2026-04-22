# Deep Snow Stack (TFC) - NeoForge 1.21.1

Deep Snow Stack, TerraFirmaCraft (TFC) ile uyumlu şekilde kar birikimini vanilla 8-layer sınırının ötesine taşır ve kar fırtınalarında gerçekçi blizzard fog uygular.

## 1.3.0 özet

- **Sıcaklık-bağlı birikim ölçeklemesi:** TFC anlık sıcaklığına göre kar birikim hızı katmanlandı. 0..-2 °C hafif yavaşlama, -2..-5 °C baseline, -5..-8 1.75x, -8..-10 2.25x, -10..-15 3x, -15..-20 4x, ≤-20 5x. Tamamı `temperature` config bloğundan ayarlanabilir.
- **Kar tabakası eşitleme (settling):** Kar yağmadığında, oyuncuların yakınındaki yüklü chunk'larda yüksek kar layer'ları yavaş yavaş aynı yükseklikteki düşük layer komşularına bir tabaka aktararak doğal düzlenme sağlar. Yamaçlardaki kar dökülmez (komşu zemin yüksekliği farklıysa atlanır). `leveling` config bloğundan tamamen kapatılabilir / hız ayarlanabilir.
- TFC 4.1.0 jar ile derlenir; mevcut mixin imzaları korunur.

## 1.2.0 özet

- TFC `4.0.19-beta` için `WeatherHelpers.placeSnowOrSnowPileAt(ServerLevel, BlockPos, BlockState)` hook imzası güncellendi.
- Kar birikim adımları için yoğunluk-ağırlıklı (intensity-weighted) hesaplama eklendi (`enableIntensityWeightedSteps`).
- Yağış yoğunluğu artık `0..1` aralığına clamp'leniyor.
- Gradle mod metadata alanları (`mod_id`, `mod_name`, `mod_version`, `group`) proje ile senkronlandı.

## Özellikler

- **8 katman üstü kar kolonları**
  - Full snow layer üzerinde hayatta kalabilen yeni layer'lar ile dikey birikim.
- **TFC precipitation entegrasyonu**
  - `WorldTracker` + `ClimateModel` + `WeatherHelpers.calculateRealRainIntensity(...)` üzerinden gerçek yoğunluk.
- **Yoğunluk tabanlı birikim adımı**
  - Hafif/şiddetli/eşiktən büyük kar senaryolarında config ile ayarlanabilir step sistemi.
- **Blizzard fog (yalnızca snow)**
  - Rain sırasında devreye girmez; shader fog start/end ve renk blend ile çalışır.

## Konfigürasyon dosyaları

- `config/deepsnowstack-common.toml`
  - accumulation ayarları (eşikler, adım sayısı, şanslar, dikey büyüme seçenekleri)
- `config/deepsnowstack-client.toml`
  - fog aç/kapat, curve, hedef mesafe ve renk ayarları

## Geliştirme notu

Bu repoda TFC jar analizi için notlar `docs/TFC_RESEARCH_NOTES.md` içinde tutulur.

