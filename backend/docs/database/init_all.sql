-- ============================================
-- 考研英语陪跑 - 数据库初始化脚本
-- 运行: mysql -u root -proot < init_all.sql
-- ============================================

SOURCE migrations/001_create_user_table.sql;

SELECT 'All tables created successfully.' AS status;
