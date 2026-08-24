# FriendsPlugin

بلوجن نظام أصدقاء متكامل لسيرفرات Paper 1.21.x، بيدعم العمل عبر أكتر من سيرفر (cross-server) باستخدام MySQL + Redis.

## المتطلبات
- Java 17+
- Paper/Spigot 1.21.x
- سيرفر MySQL (نفس القاعدة تتشارك بين كل السيرفرات)
- سيرفر Redis (اختياري بس موصى بيه لو عندك أكتر من سيرفر)

## البناء (Build)
```bash
mvn clean package
```
الملف الناتج هيكون في `target/FriendsPlugin.jar`

> ملاحظة: البيئة اللي بنيت فيها البلوجن معندهاش اتصال بـ Maven Central، فلم أقدر أعمل build فعلي وأختبر الـ jar. الكود اتراجع يدويًا بعناية (توازن الأقواس، كل الـ method calls متأكد إنها موجودة، كل الـ config keys متأكد إنها معرفة) لكن يُفضل تعمل build وتجرب على سيرفر تجريبي قبل الاستخدام في بيئة حقيقية.

## الإعداد
1. حط الـ jar في فولدر `plugins/` بتاع كل سيرفر.
2. شغل السيرفر مرة عشان ينشئ `config.yml`.
3. افتح `plugins/FriendsPlugin/config.yml` وظبط:
   - `server-name`: اسم مميز لكل سيرفر (لازم يكون مختلف في كل سيرفر عشان الـ cross-server يشتغل صح)
   - بيانات `mysql` (نفس البيانات في كل السيرفرات، عشان يشتغلوا على نفس قاعدة البيانات)
   - بيانات `redis` (نفس السيرفر Redis لكل السيرفرات)
4. أعد تشغيل السيرفر.

## الأوامر
| الأمر | الوصف |
|---|---|
| `/friend` | يفتح الـ GUI الأساسي |
| `/friend add <player>` | يبعت طلب صداقة |
| `/friend remove <player>` | يشيل صديق |
| `/friend accept <player>` | يقبل طلب صداقة |
| `/friend decline <player>` | يرفض طلب صداقة |
| `/fmsg <player> <message>` | يبعت رسالة خاصة لصديق |

## الصلاحيات (Permissions)
- `friends.use` (default: true) — استخدام النظام
- `friends.admin` (default: op) — صلاحيات إدارية (محجوزة للتوسعة المستقبلية)

## المميزات
- **GUI رئيسي**: قائمة الأصدقاء (heads) + Add Friend + Friend Requests + Change Sorting + Privacy Settings + Friends Info
- **الترتيب (Sorting)**: Default (أونلاين الأول) / Alphabetical / Last seen، مع إمكانية عكس الترتيب (Shift+Click)
- **Privacy Settings**:
  - Status: Online / Invisible / DND (Do Not Disturb)
  - Friend Requests: تفعيل/تعطيل استقبال طلبات الصداقة
  - Friend Notifications: إشعار عند دخول/خروج صديق
  - Message Notifications: صوت عند وصول رسالة
  - Friend Messages: السماح للأصدقاء بإرسال رسائل خاصة
- **Friend Requests GUI**: قائمة الطلبات الواردة، قبول/رفض بالـ Click
- **طلبات الصداقة في الشات**: أزرار Accept/Decline قابلة للضغط
- **منع الطلبات المكررة**: مينفعش تبعت طلب تاني للاعب لسه ماردش
- **حد أقصى 30 صديق** (قابل للتعديل من config)
- **دعم Cross-Server**: عن طريق MySQL (تخزين دائم) + Redis (pub/sub فوري للإشعارات والرسائل بين السيرفرات)

## هيكلة قاعدة البيانات
- `friends_settings`: بيانات كل لاعب (status, toggles, online state, last seen)
- `friends_relations`: علاقات الصداقة (pair-based, unique)
- `friends_requests`: طلبات الصداقة المعلقة
- `friends_sort_prefs`: تفضيل الترتيب لكل لاعب في الـ GUI

## آلية عمل Cross-Server
1. كل تغيير (online/offline, friend request, message, إلخ) بيتسجل في MySQL أولاً كمصدر الحقيقة.
2. بعد كده، بتتبعت رسالة Redis pub/sub لكل السيرفرات المتصلة بنفس الـ channel.
3. كل سيرفر بيستقبل الرسالة، ولو اللاعب المستهدف أونلاين عنده، بينفذ الفعل المناسب (إشعار، رسالة، إلخ) محليًا.
4. لو Redis مش متاح، البلوجن يشتغل عادي على سيرفر واحد بس من غير مزامنة فورية بين السيرفرات (البيانات لسه بتتخزن في MySQL المشترك).
