package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.KingdomType;

public class HeroFactory {

    public static Hero createHeroFor(KingdomType type) {
        switch (type) {
            case WINTERFELL:
                return new Hero("Direwolves", type,
                        "گرگ‌های وحشی شمال، مقاوم در برابر سرما و حملات وایت‌واکرها",
                        5, 500, 600, true , 300 , 300);

            case EYRIE:
                return new Hero("Brave Knight", type,
                        "شوالیه‌ی کوهستانی با توان دفاعی بالا و مقاومت در برابر حملات مستقیم",
                        5, 300, 700, false , 350 , 350);

            case IRON_ISLANDS:
                return new Hero("Ironborn Reaver", type,
                        "دزد دریایی بی‌رحم، متخصص در حملات غافلگیرانه دریایی",
                        5, 600, 400, true , 300 , 300);

            case CASTERLY_ROCK:
                return new Hero("Lion Commander", type,
                        "فرمانده قدرتمند با ارتش حرفه‌ای و تجهیزات پیشرفته",
                        5, 650, 450, false , 400 , 400);

            case STORMS_END:
                return new Hero("Theurgist", type,
                        "جادوگر مدافع، مقاوم در برابر حملات نهایی و تقویت دیوار دفاعی",
                        5, 400, 650, true , 250 , 250);

            case HIGHGARDEN:
                return new Hero("Green Warden", type,
                        "محافظ طبیعت، تقویت تولید غذا و منابع کشاورزی",
                        5, 350, 400, true , 350 , 350);

            case DRAGONSTONE:
                return new Hero("Dragons", type,
                        "اژدهایان تارگرین، قدرت تخریب بی‌نظیر در نبردهای هوایی و زمینی",
                        5, 800, 300, false , 350 , 350);

            default:
                return null;
        }
    }
}
