package com.jianzhang.app.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** 内置分类种子 SQL（与桌面版迁移脚本一致；创建与"重置数据"共用） */
const val SEED_CATEGORIES_SQL = """
    INSERT OR IGNORE INTO categories (type, name, icon, sort, builtin) VALUES
    ('expense', '餐饮', 'utensils', 1, 1),
    ('expense', '交通', 'car', 2, 1),
    ('expense', '购物', 'shopping-bag', 3, 1),
    ('expense', '居住', 'home', 4, 1),
    ('expense', '娱乐', 'gamepad-2', 5, 1),
    ('expense', '医疗', 'heart-pulse', 6, 1),
    ('expense', '教育', 'graduation-cap', 7, 1),
    ('expense', '人情', 'gift', 8, 1),
    ('expense', '其他', 'more-horizontal', 99, 1),
    ('income', '工资', 'briefcase', 1, 1),
    ('income', '奖金', 'medal', 2, 1),
    ('income', '理财', 'trending-up', 3, 1),
    ('income', '兼职', 'laptop', 4, 1),
    ('income', '其他', 'more-horizontal', 99, 1)
    """

/** 首次创建数据库时写入内置分类（与桌面版迁移脚本一致） */
class SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL(SEED_CATEGORIES_SQL)
        db.execSQL(SEED_DEFAULT_ACCOUNT_SQL)
    }
}
