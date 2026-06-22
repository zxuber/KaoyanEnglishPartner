-- ============================================
-- 考研英语陪跑 - 数据库初始化脚本
-- 运行: mysql -u root -proot < init_all.sql
-- ============================================

SOURCE migrations/001_create_user_table.sql;
SOURCE migrations/002_create_word_tables.sql;
SOURCE migrations/003_reassign_word_units.sql;
SOURCE migrations/004_expand_user_profile.sql;

SELECT 'All tables created successfully.' AS status;
