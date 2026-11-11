# DWG ⇄ DXF (Android)

AutoCAD **DWG** dosyalarını **DXF**'e (ve DXF'i DWG'ye) çeviren, sade, **reklamsız**,
**internet gerektirmeyen** ücretsiz Android uygulaması.

## Telefona kurmak

1. Bu sayfada sağdaki (telefonda aşağıdaki) **Releases** bölümüne gir.
2. En üstteki sürümde `DwgToDxf-v1.0.X.apk` dosyasına dokun, indir.
3. Açınca telefon "bilinmeyen kaynak" uyarısı verirse **izin ver** de.

Yeni sürümler eskisinin üstüne kurulur, kaldırmana gerek yok.

## Ne yapabiliyor

| Dönüşüm | Durum |
|---|---|
| DWG → DXF | ✅ R13 – AutoCAD 2018+ (DWG R14, 2000, 2004, 2007, 2010, 2013, 2018) |
| DXF → DWG | ⚠️ Deneysel. Çıktı her zaman AutoCAD 2000 DWG. Karmaşık çizimlerde eksik olabilir. R12 DXF desteklenmez. |
| DWG → RVT | ❌ Açık kaynaklı bir çözüm yok (bkz. aşağı) |

Dosyayı dosya yöneticisinden **"Birlikte aç"** ya da WhatsApp vb. yerden **"Paylaş"** ile
de uygulamaya gönderebilirsin.

## Nasıl çalışıyor

- Dönüşüm motoru: [GNU LibreDWG](https://www.gnu.org/software/libredwg/) (C, GPLv3), NDK ile telefonda çalışır.
- `app/src/main/cpp/` → C köprüsü, `app/src/main/java/` → arayüz (Kotlin).
- Her `main` güncellemesinde GitHub Actions önce motoru örnek DWG'lerle test eder, sonra APK'yı derleyip Releases'e koyar.

## Lisans

LibreDWG GPLv3 olduğu için bu uygulama da **GPLv3**'tür (bkz. `LICENSE`).

## Google Play notu

`app/shared.keystore` herkese açık bir imza anahtarıdır; sadece GitHub'dan kurulum için.
Play Store'a yüklemeden önce gizli bir anahtar oluşturulup GitHub Secrets'a konmalıdır.
