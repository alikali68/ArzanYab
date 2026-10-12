package ir.arzanyab.app.ml

/**
 * نگاشت برچسب‌های انگلیسی ML Kit به کلمات کلیدی فارسی برای جست‌وجو.
 * اگر برچسبی در نقشه نباشد، همان متن انگلیسی برمی‌گردد (ترب انگلیسی را هم می‌فهمد).
 */
object PersianLabelMap {

    private val map = mapOf(
        // دیجیتال
        "Mobile phone" to "گوشی موبایل",
        "Telephone" to "تلفن",
        "Laptop" to "لپ تاپ",
        "Computer" to "کامپیوتر",
        "Television" to "تلویزیون",
        "Tv" to "تلویزیون",
        "Camera" to "دوربین",
        "Headphones" to "هدفون",
        "Headset" to "هدست",
        "Earbuds" to "ایربادز",
        "Tablet" to "تبلت",
        "Watch" to "ساعت",
        "Smartwatch" to "ساعت هوشمند",
        "Keyboard" to "کیبورد",
        "Mouse" to "ماوس",
        "Printer" to "پرینتر",
        "Speaker" to "اسپیکر",
        "Microphone" to "میکروفون",
        "Drone" to "پهپاد",
        "Game controller" to "دسته بازی",
        "Remote control" to "کنترل",
        "Charger" to "شارژر",
        "Battery" to "باتری",
        // پوشاک
        "Shoe" to "کفش",
        "Sneakers" to "کفش ورزشی",
        "Boot" to "بوت",
        "Shirt" to "پیراهن",
        "T-shirt" to "تیشرت",
        "Dress" to "پیراهن زنانه",
        "Jeans" to "شلوار جین",
        "Trousers" to "شلوار",
        "Jacket" to "کاپشن",
        "Coat" to "پالتو",
        "Bag" to "کیف",
        "Handbag" to "کیف دستی",
        "Backpack" to "کوله پشتی",
        "Suitcase" to "چمدان",
        "Sunglasses" to "عینک آفتابی",
        "Glasses" to "عینک",
        "Hat" to "کلاه",
        "Cap" to "کلاه",
        "Scarf" to "روسری",
        "Glove" to "دستکش",
        "Socks" to "جوراب",
        "Suit" to "کت و شلوار",
        "Skirt" to "دامن",
        "Sweater" to "بافت",
        // خانه
        "Chair" to "صندلی",
        "Table" to "میز",
        "Sofa" to "مبل",
        "Couch" to "مبل",
        "Lamp" to "لامپ",
        "Bed" to "تخت",
        "Pillow" to "بالش",
        "Blanket" to "پتو",
        "Curtain" to "پرده",
        "Carpet" to "فرش",
        "Rug" to "فرش",
        "Mirror" to "آینه",
        "Vase" to "گلدان",
        "Clock" to "ساعت دیواری",
        "Fan" to "پنکه",
        "Refrigerator" to "یخچال",
        "Microwave" to "مایکروویو",
        "Oven" to "فر",
        "Washing machine" to "ماشین لباسشویی",
        "Vacuum cleaner" to "جاروبرقی",
        "Kettle" to "کتری",
        "Pot" to "قابلمه",
        "Pan" to "ماهیتابه",
        "Knife" to "چاقو",
        "Spoon" to "قاشق",
        "Fork" to "چنگال",
        "Plate" to "بشقاب",
        "Cup" to "لیوان",
        "Mug" to "ماگ",
        // خوراکی
        "Bottle" to "بطری",
        "Soda" to "نوشابه",
        "Coffee" to "قهوه",
        "Tea" to "چای",
        "Bread" to "نان",
        "Cake" to "کیک",
        "Apple" to "سیب",
        "Banana" to "موز",
        "Orange" to "پرتقال",
        "Honey" to "عسل",
        "Milk" to "شیر",
        "Cheese" to "پنیر",
        "Chocolate" to "شکلات",
        // آرایشی
        "Perfume" to "عطر",
        "Lipstick" to "رژ لب",
        "Cosmetics" to "لوازم آرایشی",
        "Shampoo" to "شامپو",
        "Cream" to "کرم",
        // ورزشی
        "Bicycle" to "دوچرخه",
        "Ball" to "توپ",
        "Football" to "توپ فوتبال",
        "Treadmill" to "تردمیل",
        "Dumbbell" to "دمبل",
        // کودک
        "Toy" to "اسباب بازی",
        "Doll" to "عروسک",
        "Teddy bear" to "عروسک خرسی",
        // متفرقه
        "Book" to "کتاب",
        "Pen" to "خودکار",
        "Car" to "خودرو",
        "Motorcycle" to "موتورسیکلت",
        "Tire" to "لاستیک",
        "Plant" to "گلدان",
        "Flower" to "گل",
        "Candle" to "شمع",
        "Umbrella" to "چتر",
        "Luggage" to "چمدان",
        "Wallet" to "کیف پول",
        "Belt" to "کمربند",
        "Ring" to "انگشتر",
        "Necklace" to "گردنبند",
        "Earring" to "گوشواره",
        "Bracelet" to "دستبند",
        "Tent" to "چادر مسافرتی",
        "Guitar" to "گیتار",
        "Piano" to "پیانو"
    )

    /** برچسب انگلیسی → کلیدواژه فارسی (یا همان انگلیسی اگر نگاشت نشد) */
    fun toPersian(label: String): String = map[label] ?: label

    /**
     * از میان برچسب‌ها، بهترین کلیدواژه‌های فارسی را می‌سازد.
     * @param labeled فهرست (متن برچسب، اطمینان)
     */
    fun buildQuery(labeled: List<Pair<String, Float>>): String {
        val keywords = labeled
            .filter { it.second >= 0.4f }
            .take(2)
            .map { toPersian(it.first) }
            .distinct()
        if (keywords.isNotEmpty()) return keywords.joinToString(" ")
        // سقوط: بهترین برچسب خام
        return labeled.firstOrNull()?.first ?: ""
    }
}
