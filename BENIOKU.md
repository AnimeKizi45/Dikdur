# DikDur – Android uygulaması

**Boynun dik, geleceğin parlak!**

DikDur telefonda **arka planda** çalışır: ön kamerayla boyun eğimini yapay zekâ (MediaPipe) ile ölçer.
Boyun eşiği (15°) 2 saniyeden uzun geçerse ekranın üstünde "Boynunu dik tut!" uyarısı çıkar,
telefon titrer ve **tüm telefon ekranı** eğilme miktarına göre %10'dan %70'e kadar kararır.
Dik durunca (10° altı, 1 sn) ekran yumuşak geçişle eski haline döner. YouTube, oyun, WhatsApp… hangi
uygulamadaysan çalışmaya devam eder.

## APK'yı alma (bilgisayara bir şey kurmadan)

1. github.com'da ücretsiz hesap aç → sağ üstte **+ → New repository** → adı `DikDur` → **Create**.
2. Açılan sayfada **uploading an existing file** bağlantısına tıkla.
3. Bu klasörün (**android**) **içindeki her şeyi** sürükleyip bırak (`.github` klasörü dahil) → **Commit changes**.
   - `.github` klasörü gizli görünebilir; görünmüyorsa zip'i bilgisayarda açıp gizli dosyaları göster.
4. Üstteki **Actions** sekmesine gir → "DikDur APK oluştur" çalışır (~5–8 dk). Yeşil tik çıkınca tıkla.
5. Sayfanın altındaki **Artifacts → DikDur-APK**'yı indir, zip'i aç → `app-debug.apk`.
6. APK'yı telefona at (WhatsApp/Drive/USB) ve aç. "Bilinmeyen kaynaklardan yükleme" izni iste­nirse ver.

## Alternatif: Android Studio ile

Android Studio → **Open** → bu klasör → üst menü **Build → Build App Bundle(s)/APK(s) → Build APK(s)**.
İlk derlemede yapay zekâ model dosyaları otomatik indirilir (internet gerekir).

## İlk açılış

1. **Takibi başlat** → kamera ve bildirim iznini ver.
2. "Diğer uygulamaların üzerinde göster" ayarı açılır → DikDur'u aç → geri dön → tekrar **Takibi başlat**.
3. Kalibrasyon: 3 sn dik otur, sonra 2,5 sn başını hafifçe öne eğ.
4. Artık uygulamadan çıkabilirsin; bildirim çubuğunda "DikDur çalışıyor" görünür. Durdurmak için bildirimdeki **Durdur**.

> Xiaomi/Huawei/Oppo gibi telefonlarda ayrıca Ayarlar → Pil → DikDur → "Kısıtlama yok" seçilmesi gerekebilir.

## Dosyalar

| Dosya | Görevi |
| --- | --- |
| `MainActivity.kt` | Ana ekran, izinler, başlat/durdur |
| `PostureService.kt` | Arka plan servisi, kamera, bildirim |
| `PostureAnalyzer.kt` | MediaPipe ile yüz + omuz noktaları, açı ölçümü |
| `PostureLogic.kt` | Kalibrasyon, eşikler, %10–%70 karartma hesabı |
| `DimOverlay.kt` | Tüm ekranı karartan katman ve uyarı yazısı |
